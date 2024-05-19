package com.github.simple_mocks.storage.local.service;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.Content;
import com.github.simple_mocks.storage.api.StorageErrors;
import com.github.simple_mocks.storage.api.StorageService;
import com.github.simple_mocks.storage.local.conf.LocalStorageServiceEnabled;
import com.github.simple_mocks.storage.local.conf.LocalStorageServiceProperties;
import com.github.simple_mocks.storage.local.dto.LocalContent;
import com.github.simple_mocks.storage.local.entity.ContentEntity;
import com.github.simple_mocks.storage.local.entity.ContentMetaEntity;
import com.github.simple_mocks.storage.local.dto.ContentStorageFormat;
import com.github.simple_mocks.storage.local.repository.BucketEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentMetaEntityRepository;
import com.github.simple_mocks.storage.local.service.codec.StorageCodec;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Local implementation of {@link StorageService}
 *
 * @author sibmaks
 * @since 0.0.1
 */
@Service
@ConditionalOnBean(LocalStorageServiceEnabled.class)
public class LocalStorageService implements StorageService {
    private final BucketEntityRepository bucketEntityRepository;
    private final ContentEntityRepository contentEntityRepository;
    private final ContentMetaEntityRepository contentMetaEntityRepository;
    private final Map<ContentStorageFormat, StorageCodec> storageCodecs;
    private final LocalStorageServiceProperties properties;

    /**
     * Construct local storage service
     *
     * @param bucketEntityRepository      bucket entity repository
     * @param contentEntityRepository     content entity repository
     * @param contentMetaEntityRepository content meta entity repository
     * @param storageCodecs               storage codecs
     * @param properties                  local storage service properties
     */
    @Autowired
    public LocalStorageService(BucketEntityRepository bucketEntityRepository,
                               ContentEntityRepository contentEntityRepository,
                               ContentMetaEntityRepository contentMetaEntityRepository,
                               List<StorageCodec> storageCodecs,
                               LocalStorageServiceProperties properties) {
        this.bucketEntityRepository = bucketEntityRepository;
        this.contentEntityRepository = contentEntityRepository;
        this.contentMetaEntityRepository = contentMetaEntityRepository;
        this.storageCodecs = storageCodecs.stream()
                .collect(Collectors.toMap(StorageCodec::getFormat, Function.identity()));
        this.properties = properties;
    }

    /**
     * Set up local storage service
     */
    @PostConstruct
    public void setUp() {
        var folder = properties.getFolder();
        var path = Path.of(folder);
        createDirectoriesIfNotExists(path);
    }

    private static void createDirectoriesIfNotExists(Path path) {
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                throw new IllegalArgumentException("Path: '%s' exists and is not directory".formatted(path));
            }
        } else {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't create dirs: %s".formatted(path));
            }
        }
    }

    @Override
    public Content get(String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, StorageErrors.CONTENT_NOT_FOUND, "Content not found"));

        var storageFormat = contentEntity.getStorageFormat();
        var storageCodec = storageCodecs.get(storageFormat);
        if (storageCodec == null) {
            throw new ServiceException(
                    StorageErrors.UNEXPECTED_ERROR,
                    "Unsupported storage format: %s".formatted(storageFormat)
            );
        }
        var bucket = contentEntity.getBucket();
        var bucketId = bucket.getId();

        var path = getPath(bucketId, id);
        var content = readContent(path);
        var decodedContent = storageCodec.decode(content);

        var meta = contentMetaEntityRepository.findAllByContentUid(id)
                .stream()
                .collect(Collectors.toMap(ContentMetaEntity::getKey, ContentMetaEntity::getValue));

        return LocalContent.builder()
                .id(contentEntity.getUid())
                .name(contentEntity.getName())
                .content(decodedContent)
                .meta(meta)
                .createdAt(contentEntity.getCreatedAt())
                .modifiedAt(contentEntity.getModifiedAt())
                .build();
    }

    private byte[] readContent(Path path) {
        try (var channel = FileChannel.open(path, StandardOpenOption.READ);
             var out = new ByteArrayOutputStream()) {

            var readBufferSize = Math.max(1, properties.getBufferSize());
            if (readBufferSize > channel.size()) {
                readBufferSize = (int) channel.size();
            }
            var buff = ByteBuffer.allocate(readBufferSize);

            while (channel.read(buff) > 0) {
                out.write(buff.array(), 0, buff.position());
                buff.clear();
            }

            return out.toByteArray();
        } catch (NoSuchFileException e) {
            throw new ServiceException(404, StorageErrors.CONTENT_NOT_FOUND, "File not found", e);
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Unexpected error", e);
        }
    }

    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW,
            transactionManager = "localStorageTransactionManager"
    )
    public void delete(String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElse(null);
        if (contentEntity == null) {
            return;
        }
        var bucket = contentEntity.getBucket();
        if (bucket.isReadonly()) {
            throw new ServiceException(403, StorageErrors.BUCKET_READONLY, "Bucket is readonly");
        }
        var bucketId = bucket.getId();
        var path = getPath(bucketId, id);
        contentMetaEntityRepository.deleteAllByContentUid(id);
        contentEntityRepository.delete(contentEntity);
        if (Files.notExists(path)) {
            return;
        }
        // Maybe better do it by scheduler or via async tasks
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't delete file");
        }
    }

    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW,
            transactionManager = "localStorageTransactionManager"
    )
    public String create(String bucket, String name, Map<String, String> meta, byte[] content) {
        var bucketEntity = bucketEntityRepository.findByCode(bucket)
                .orElseThrow(() -> new ServiceException(404, StorageErrors.BUCKET_NOT_EXISTS, "Bucket not exists"));

        if (bucketEntity.isReadonly()) {
            throw new ServiceException(403, StorageErrors.BUCKET_READONLY, "Bucket is readonly");
        }
        var storageFormat = properties.getStorageFormat();
        var storageCodec = storageCodecs.get(storageFormat);
        if (storageCodec == null) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Unsupported storage format: %s".formatted(storageFormat));
        }

        var uid = UUID.randomUUID().toString();
        var entity = ContentEntity.builder()
                .uid(uid)
                .name(name)
                .bucket(bucketEntity)
                .storageFormat(storageFormat)
                .createdAt(ZonedDateTime.now())
                .modifiedAt(ZonedDateTime.now())
                .build();
        contentEntityRepository.save(entity);

        var metaEntities = meta.entrySet().stream()
                .map(it -> ContentMetaEntity.builder()
                        .key(it.getKey())
                        .value(it.getValue())
                        .contentUid(uid)
                        .build()
                )
                .toList();
        contentMetaEntityRepository.saveAll(metaEntities);

        var bucketId = bucketEntity.getId();
        var path = getPath(bucketId, uid);
        createDirectoriesIfNotExists(path.getParent());

        var encodedContent = storageCodec.encode(content);
        try (var channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            var buffer = ByteBuffer.wrap(encodedContent);
            if (channel.write(buffer) != encodedContent.length) {
                throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't write content");
            }
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't create content", e);
        }

        return uid;
    }

    private Path getPath(long bucketId, String id) {
        var folder = properties.getFolder();
        return Path.of(folder, String.valueOf(bucketId), "%s.data".formatted(id));
    }

}

package com.github.simple_mocks.storage.embedded.service;

import com.github.simple_mocks.storage.api.dto.BucketFile;
import com.github.simple_mocks.storage.api.dto.BucketFileDescription;
import com.github.simple_mocks.storage.api.rq.SaveFileRq;
import com.github.simple_mocks.storage.api.service.StorageService;
import com.github.simple_mocks.storage.embedded.conf.StorageServiceEmbeddedCondition;
import com.github.simple_mocks.storage.embedded.conf.StorageServiceEmbeddedProperties;
import com.github.simple_mocks.storage.embedded.dto.BucketFileDescriptionImpl;
import com.github.simple_mocks.storage.embedded.dto.BucketFileImpl;
import com.github.simple_mocks.storage.embedded.dto.BucketFileMetadataImpl;
import com.github.simple_mocks.storage.embedded.dto.ContentStorageFormat;
import com.github.simple_mocks.storage.embedded.entity.ContentEntity;
import com.github.simple_mocks.storage.embedded.entity.ContentMetaEntity;
import com.github.simple_mocks.storage.embedded.exception.BucketNotExistsException;
import com.github.simple_mocks.storage.embedded.exception.BucketReadonlyException;
import com.github.simple_mocks.storage.embedded.exception.FileNotFoundException;
import com.github.simple_mocks.storage.embedded.exception.UnexpectedErrorException;
import com.github.simple_mocks.storage.embedded.repository.BucketEntityRepository;
import com.github.simple_mocks.storage.embedded.repository.ContentEntityRepository;
import com.github.simple_mocks.storage.embedded.repository.ContentMetaEntityRepository;
import com.github.simple_mocks.storage.embedded.service.codec.StorageCodec;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
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
 * @author sibmaks
 * @since 0.0.1
 */
@Service
@Conditional(StorageServiceEmbeddedCondition.class)
public class StorageServiceEmbedded implements StorageService {
    private final BucketEntityRepository bucketEntityRepository;
    private final ContentEntityRepository contentEntityRepository;
    private final ContentMetaEntityRepository contentMetaEntityRepository;
    private final Map<ContentStorageFormat, StorageCodec> storageCodecs;
    private final StorageServiceEmbeddedProperties properties;

    /**
     * Construct embedded storage service
     *
     * @param bucketEntityRepository      bucket entity repository
     * @param contentEntityRepository     content entity repository
     * @param contentMetaEntityRepository content meta entity repository
     * @param storageCodecs               storage codecs
     * @param properties                  embedded storage service properties
     */
    @Autowired
    public StorageServiceEmbedded(BucketEntityRepository bucketEntityRepository,
                                  ContentEntityRepository contentEntityRepository,
                                  ContentMetaEntityRepository contentMetaEntityRepository,
                                  List<StorageCodec> storageCodecs,
                                  StorageServiceEmbeddedProperties properties) {
        this.bucketEntityRepository = bucketEntityRepository;
        this.contentEntityRepository = contentEntityRepository;
        this.contentMetaEntityRepository = contentMetaEntityRepository;
        this.storageCodecs = storageCodecs.stream()
                .collect(Collectors.toMap(StorageCodec::getFormat, Function.identity()));
        this.properties = properties;
    }

    /**
     * Set up embedded storage service
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
                throw new UnexpectedErrorException("Can't create dirs: %s".formatted(path));
            }
        }
    }

    @Override
    @Nonnull
    public BucketFile get(@Nonnull String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("Content not found"));

        var storageFormat = contentEntity.getStorageFormat();
        var storageCodec = storageCodecs.get(storageFormat);
        if (storageCodec == null) {
            throw new UnexpectedErrorException("Unsupported storage format: %s".formatted(storageFormat));
        }
        var bucket = contentEntity.getBucket();
        var bucketId = bucket.getId();

        var path = getPath(bucketId, id);
        var content = readContent(path);
        var decodedContent = storageCodec.decode(content);

        var meta = contentMetaEntityRepository.findAllByContentUid(id)
                .stream()
                .collect(Collectors.toMap(ContentMetaEntity::getKey, ContentMetaEntity::getValue));

        var bucketMeta = new BucketFileMetadataImpl(meta);

        var description = BucketFileDescriptionImpl.builder()
                .id(contentEntity.getUid())
                .name(contentEntity.getName())
                .meta(bucketMeta)
                .createdAt(contentEntity.getCreatedAt())
                .modifiedAt(contentEntity.getModifiedAt())
                .build();

        return BucketFileImpl.builder()
                .description(description)
                .data(decodedContent)
                .build();
    }

    @Nonnull
    @Override
    public BucketFileDescription getDescription(@Nonnull String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("Content not found"));

        var meta = contentMetaEntityRepository.findAllByContentUid(id)
                .stream()
                .collect(Collectors.toMap(ContentMetaEntity::getKey, ContentMetaEntity::getValue));

        var bucketMeta = new BucketFileMetadataImpl(meta);

        return BucketFileDescriptionImpl.builder()
                .id(contentEntity.getUid())
                .name(contentEntity.getName())
                .meta(bucketMeta)
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
            throw new FileNotFoundException("File not found", e);
        } catch (IOException e) {
            throw new UnexpectedErrorException("Unexpected error", e);
        }
    }

    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    public void delete(@Nonnull String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElse(null);
        if (contentEntity == null) {
            return;
        }
        var bucket = contentEntity.getBucket();
        if (bucket.isReadonly()) {
            throw new BucketReadonlyException("Bucket is readonly");
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
            throw new UnexpectedErrorException("Can't delete file");
        }
    }

    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    @Nonnull
    public String save(@Nonnull SaveFileRq rq) {
        var bucket = rq.bucket();

        var bucketEntity = bucketEntityRepository.findByCode(bucket)
                .orElseThrow(() -> new BucketNotExistsException("Bucket does not exists"));

        if (bucketEntity.isReadonly()) {
            throw new BucketReadonlyException("Bucket is readonly");
        }
        var storageFormat = properties.getStorageFormat();
        var storageCodec = storageCodecs.get(storageFormat);
        if (storageCodec == null) {
            throw new UnexpectedErrorException("Unsupported storage format: %s".formatted(storageFormat));
        }

        var uid = UUID.randomUUID().toString();
        var entity = ContentEntity.builder()
                .uid(uid)
                .name(rq.name())
                .bucket(bucketEntity)
                .storageFormat(storageFormat)
                .createdAt(ZonedDateTime.now())
                .modifiedAt(ZonedDateTime.now())
                .build();
        contentEntityRepository.save(entity);

        var metaEntities = rq.meta()
                .entrySet()
                .stream()
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

        var data = rq.data();
        var encodedContent = storageCodec.encode(data);
        try (var channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            var buffer = ByteBuffer.wrap(encodedContent);
            if (channel.write(buffer) != encodedContent.length) {
                throw new UnexpectedErrorException("Can't write content");
            }
        } catch (IOException e) {
            throw new UnexpectedErrorException("Can't create content", e);
        }

        return uid;
    }

    private Path getPath(long bucketId, String id) {
        var folder = properties.getFolder();
        return Path.of(folder, String.valueOf(bucketId), "%s.data".formatted(id));
    }

}

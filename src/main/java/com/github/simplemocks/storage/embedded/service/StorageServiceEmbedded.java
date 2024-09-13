package com.github.simplemocks.storage.embedded.service;

import com.github.simplemocks.common.api.rs.StandardRs;
import com.github.simplemocks.storage.api.rq.SaveFileRq;
import com.github.simplemocks.storage.api.rs.GetBucketFileDescriptionRs;
import com.github.simplemocks.storage.api.rs.GetBucketFileRs;
import com.github.simplemocks.storage.api.rs.SaveFileRs;
import com.github.simplemocks.storage.api.service.StorageService;
import com.github.simplemocks.storage.embedded.conf.StorageServiceEmbeddedCondition;
import com.github.simplemocks.storage.embedded.conf.StorageServiceEmbeddedProperties;
import com.github.simplemocks.storage.embedded.dto.BucketFileDescriptionImpl;
import com.github.simplemocks.storage.embedded.dto.BucketFileImpl;
import com.github.simplemocks.storage.embedded.dto.BucketFileMetadataImpl;
import com.github.simplemocks.storage.embedded.dto.ContentStorageFormat;
import com.github.simplemocks.storage.embedded.entity.ContentEntity;
import com.github.simplemocks.storage.embedded.entity.ContentMetaEntity;
import com.github.simplemocks.storage.embedded.exception.BucketNotExistsException;
import com.github.simplemocks.storage.embedded.exception.BucketReadonlyException;
import com.github.simplemocks.storage.embedded.exception.FileNotFoundException;
import com.github.simplemocks.storage.embedded.exception.UnexpectedErrorException;
import com.github.simplemocks.storage.embedded.repository.BucketEntityRepository;
import com.github.simplemocks.storage.embedded.repository.ContentEntityRepository;
import com.github.simplemocks.storage.embedded.repository.ContentMetaEntityRepository;
import com.github.simplemocks.storage.embedded.service.codec.StorageCodec;
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
    public GetBucketFileRs get(@Nonnull String id) {
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

        var bucketMeta = getBucketFileMetadata(id);

        var description = BucketFileDescriptionImpl.builder()
                .id(contentEntity.getUid())
                .name(contentEntity.getName())
                .meta(bucketMeta)
                .createdAt(contentEntity.getCreatedAt())
                .modifiedAt(contentEntity.getModifiedAt())
                .build();

        var bucketFile = BucketFileImpl.builder()
                .description(description)
                .data(decodedContent)
                .build();
        return new GetBucketFileRs(bucketFile);
    }

    private BucketFileMetadataImpl getBucketFileMetadata(String id) {
        var meta = contentMetaEntityRepository.findAllByContentUid(id)
                .stream()
                .collect(Collectors.toMap(ContentMetaEntity::getKey, ContentMetaEntity::getValue));

        var bucketMeta = new BucketFileMetadataImpl(meta);
        return bucketMeta;
    }

    @Nonnull
    @Override
    public GetBucketFileDescriptionRs getDescription(@Nonnull String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("Content not found"));

        var bucketMeta = getBucketFileMetadata(id);

        var bucketFileDescription = BucketFileDescriptionImpl.builder()
                .id(contentEntity.getUid())
                .name(contentEntity.getName())
                .meta(bucketMeta)
                .createdAt(contentEntity.getCreatedAt())
                .modifiedAt(contentEntity.getModifiedAt())
                .build();
        return new GetBucketFileDescriptionRs(bucketFileDescription);
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

    @Nonnull
    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    public StandardRs delete(@Nonnull String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElse(null);
        if (contentEntity == null) {
            return new StandardRs();
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
            return new StandardRs();
        }
        // Maybe better do it by scheduler or via async tasks
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new UnexpectedErrorException("Can't delete file");
        }
        return new StandardRs();
    }

    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    @Nonnull
    public SaveFileRs save(@Nonnull SaveFileRq rq) {
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

        return new SaveFileRs(uid);
    }

    private Path getPath(long bucketId, String id) {
        var folder = properties.getFolder();
        return Path.of(folder, String.valueOf(bucketId), "%s.data".formatted(id));
    }

}

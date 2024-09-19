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
import com.github.simplemocks.storage.embedded.service.storage.StorageContainer;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
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
    private final Map<String, StorageContainer> storageContainers;

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
                                  @Qualifier("storageCodecsMap")
                                  Map<ContentStorageFormat, StorageCodec> storageCodecs,
                                  StorageServiceEmbeddedProperties properties,
                                  @Qualifier("storageContainerMap")
                                  Map<String, StorageContainer> storageContainers) {
        this.bucketEntityRepository = bucketEntityRepository;
        this.contentEntityRepository = contentEntityRepository;
        this.contentMetaEntityRepository = contentMetaEntityRepository;
        this.storageCodecs = storageCodecs;
        this.properties = properties;
        this.storageContainers = storageContainers;
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

        var storageContainerType = properties.getDefaultStorageContainer();
        var storageContainer = storageContainers.get(storageContainerType);
        var content = storageContainer.get(bucket.getId(), id);
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

        return new BucketFileMetadataImpl(meta);
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
        contentMetaEntityRepository.deleteAllByContentUid(id);
        contentEntityRepository.delete(contentEntity);

        var storageContainerType = properties.getDefaultStorageContainer();
        var storageContainer = storageContainers.get(storageContainerType);
        storageContainer.delete(bucket.getId(), id);

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

        var storageContainerType = properties.getDefaultStorageContainer();
        var storageContainer = storageContainers.get(storageContainerType);
        var encodedContent = storageCodec.encode(rq.data());
        storageContainer.save(bucketEntity.getId(), uid, encodedContent);

        return new SaveFileRs(uid);
    }

}

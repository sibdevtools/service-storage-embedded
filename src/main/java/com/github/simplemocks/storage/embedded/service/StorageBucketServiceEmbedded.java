package com.github.simplemocks.storage.embedded.service;

import com.github.simplemocks.storage.api.dto.Bucket;
import com.github.simplemocks.storage.api.dto.BucketFileDescription;
import com.github.simplemocks.storage.api.rq.SetReadOnlyModeRq;
import com.github.simplemocks.storage.api.service.StorageBucketService;
import com.github.simplemocks.storage.embedded.conf.StorageServiceEmbeddedCondition;
import com.github.simplemocks.storage.embedded.dto.BucketFileDescriptionImpl;
import com.github.simplemocks.storage.embedded.dto.BucketFileMetadataImpl;
import com.github.simplemocks.storage.embedded.dto.BucketImpl;
import com.github.simplemocks.storage.embedded.entity.BucketEntity;
import com.github.simplemocks.storage.embedded.entity.ContentEntity;
import com.github.simplemocks.storage.embedded.entity.ContentMetaEntity;
import com.github.simplemocks.storage.embedded.exception.BucketNotEmptyException;
import com.github.simplemocks.storage.embedded.exception.BucketNotExistsException;
import com.github.simplemocks.storage.embedded.repository.BucketEntityRepository;
import com.github.simplemocks.storage.embedded.repository.ContentEntityRepository;
import com.github.simplemocks.storage.embedded.repository.ContentMetaEntityRepository;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@Service
@Conditional(StorageServiceEmbeddedCondition.class)
public class StorageBucketServiceEmbedded implements StorageBucketService {
    private final BucketEntityRepository bucketEntityRepository;
    private final ContentEntityRepository contentEntityRepository;
    private final ContentMetaEntityRepository contentMetaEntityRepository;

    /**
     * Construct embedded storage bucket service
     *
     * @param bucketEntityRepository      bucket entity repository
     * @param contentEntityRepository     content entity repository
     * @param contentMetaEntityRepository content meta entity repository
     */
    @Autowired
    public StorageBucketServiceEmbedded(BucketEntityRepository bucketEntityRepository,
                                        ContentEntityRepository contentEntityRepository,
                                        ContentMetaEntityRepository contentMetaEntityRepository) {
        this.bucketEntityRepository = bucketEntityRepository;
        this.contentEntityRepository = contentEntityRepository;
        this.contentMetaEntityRepository = contentMetaEntityRepository;
    }

    @Override
    public void create(@Nonnull String bucket) {
        var bucketEntity = BucketEntity.builder()
                .code(bucket)
                .createdAt(ZonedDateTime.now())
                .modifiedAt(ZonedDateTime.now())
                .build();
        bucketEntityRepository.save(bucketEntity);
    }

    @Nonnull
    @Override
    public Bucket get(@Nonnull String bucket) {
        return bucketEntityRepository.findByCode(bucket)
                .map(it -> BucketImpl.builder()
                        .code(it.getCode())
                        .createdAt(it.getCreatedAt())
                        .modifiedAt(it.getModifiedAt())
                        .readOnly(it.isReadonly())
                        .contents(buildBucketContents(it))
                        .build())
                .orElseThrow(() -> new BucketNotExistsException("Bucket does not exists"));
    }

    private List<BucketFileDescription> buildBucketContents(BucketEntity bucketEntity) {
        return contentEntityRepository.findAllByBucket(bucketEntity)
                .stream()
                .map(this::buildBucketFileDescription)
                .toList();
    }

    private BucketFileDescription buildBucketFileDescription(ContentEntity it) {
        var uid = it.getUid();

        var meta = contentMetaEntityRepository.findAllByContentUid(uid)
                .stream()
                .collect(Collectors.toMap(ContentMetaEntity::getKey, ContentMetaEntity::getValue));

        var bucketMeta = new BucketFileMetadataImpl(meta);

        return BucketFileDescriptionImpl.builder()
                .id(uid)
                .name(it.getName())
                .meta(bucketMeta)
                .createdAt(it.getCreatedAt())
                .modifiedAt(it.getModifiedAt())
                .build();
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void setReadOnly(@Nonnull SetReadOnlyModeRq rq) {
        var bucket = rq.code();
        var readOnly = rq.readOnly();

        var bucketEntity = bucketEntityRepository.findByCode(bucket)
                .orElseThrow(() -> new BucketNotExistsException("Bucket does not exists"));

        if (bucketEntity.isReadonly() == readOnly) {
            return;
        }

        bucketEntity.setReadonly(readOnly);
        bucketEntity.setModifiedAt(ZonedDateTime.now());

        bucketEntityRepository.save(bucketEntity);
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void delete(@Nonnull String bucket) {
        var optionalBucketEntity = bucketEntityRepository.findByCode(bucket);

        if (optionalBucketEntity.isEmpty()) {
            return;
        }

        var bucketEntity = optionalBucketEntity.get();
        var contents = contentEntityRepository.countAllByBucket(bucketEntity);
        if (contents > 0) {
            throw new BucketNotEmptyException("Bucket not empty");
        }

        bucketEntityRepository.delete(bucketEntity);
    }
}

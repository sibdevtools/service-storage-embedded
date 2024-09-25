package com.github.sibdevtools.storage.embedded.service;

import com.github.sibdevtools.common.api.rs.StandardRs;
import com.github.sibdevtools.storage.api.dto.BucketFileDescription;
import com.github.sibdevtools.storage.api.rq.SetReadOnlyModeRq;
import com.github.sibdevtools.storage.api.rs.GetBucketRs;
import com.github.sibdevtools.storage.api.service.StorageBucketService;
import com.github.sibdevtools.storage.embedded.dto.BucketFileDescriptionImpl;
import com.github.sibdevtools.storage.embedded.dto.BucketFileMetadataImpl;
import com.github.sibdevtools.storage.embedded.dto.BucketImpl;
import com.github.sibdevtools.storage.embedded.entity.BucketEntity;
import com.github.sibdevtools.storage.embedded.entity.ContentEntity;
import com.github.sibdevtools.storage.embedded.entity.ContentMetaEntity;
import com.github.sibdevtools.storage.embedded.exception.BucketNotEmptyException;
import com.github.sibdevtools.storage.embedded.exception.BucketNotExistsException;
import com.github.sibdevtools.storage.embedded.repository.BucketEntityRepository;
import com.github.sibdevtools.storage.embedded.repository.ContentEntityRepository;
import com.github.sibdevtools.storage.embedded.repository.ContentMetaEntityRepository;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "service.storage.mode", havingValue = "EMBEDDED")
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

    @Nonnull
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public StandardRs create(@Nonnull String bucket) {
        bucketEntityRepository.saveIfNotExists(bucket);
        return new StandardRs();
    }

    @Nonnull
    @Override
    public GetBucketRs get(@Nonnull String bucketCode) {
        var bucket = bucketEntityRepository.findByCode(bucketCode)
                .map(it -> BucketImpl.builder()
                        .code(it.getCode())
                        .createdAt(it.getCreatedAt())
                        .modifiedAt(it.getModifiedAt())
                        .readOnly(it.isReadonly())
                        .contents(buildBucketContents(it))
                        .build())
                .orElseThrow(() -> new BucketNotExistsException("Bucket does not exists"));
        return new GetBucketRs(bucket);
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

    @Nonnull
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public StandardRs setReadOnly(@Nonnull SetReadOnlyModeRq rq) {
        var bucket = rq.code();
        var readOnly = rq.readOnly();

        var bucketEntity = bucketEntityRepository.findByCode(bucket)
                .orElseThrow(() -> new BucketNotExistsException("Bucket does not exists"));

        if (bucketEntity.isReadonly() == readOnly) {
            return new StandardRs();
        }

        bucketEntity.setReadonly(readOnly);
        bucketEntity.setModifiedAt(ZonedDateTime.now());

        bucketEntityRepository.save(bucketEntity);
        return new StandardRs();
    }

    @Nonnull
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public StandardRs delete(@Nonnull String bucket) {
        var optionalBucketEntity = bucketEntityRepository.findByCode(bucket);

        if (optionalBucketEntity.isEmpty()) {
            return new StandardRs();
        }

        var bucketEntity = optionalBucketEntity.get();
        var contents = contentEntityRepository.countAllByBucket(bucketEntity);
        if (contents > 0) {
            throw new BucketNotEmptyException("Bucket not empty");
        }

        bucketEntityRepository.delete(bucketEntity);
        return new StandardRs();
    }
}

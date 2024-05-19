package com.github.simple_mocks.storage.local.service;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.Bucket;
import com.github.simple_mocks.storage.api.BucketContent;
import com.github.simple_mocks.storage.api.StorageBucketService;
import com.github.simple_mocks.storage.api.StorageErrors;
import com.github.simple_mocks.storage.local.conf.LocalStorageServiceEnabled;
import com.github.simple_mocks.storage.local.dto.LocalBucket;
import com.github.simple_mocks.storage.local.dto.LocalBucketContent;
import com.github.simple_mocks.storage.local.entity.BucketEntity;
import com.github.simple_mocks.storage.local.entity.ContentEntity;
import com.github.simple_mocks.storage.local.entity.ContentMetaEntity;
import com.github.simple_mocks.storage.local.repository.BucketEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentMetaEntityRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.stream.Collectors;

/**
 * Local implementation of {@link StorageBucketService}
 *
 * @author sibmaks
 * @since 0.1.4
 */
@Service
@ConditionalOnBean(LocalStorageServiceEnabled.class)
public class LocalStorageBucketService implements StorageBucketService {
    private final BucketEntityRepository bucketEntityRepository;
    private final ContentEntityRepository contentEntityRepository;
    private final ContentMetaEntityRepository contentMetaEntityRepository;

    public LocalStorageBucketService(BucketEntityRepository bucketEntityRepository,
                                     ContentEntityRepository contentEntityRepository,
                                     ContentMetaEntityRepository contentMetaEntityRepository) {
        this.bucketEntityRepository = bucketEntityRepository;
        this.contentEntityRepository = contentEntityRepository;
        this.contentMetaEntityRepository = contentMetaEntityRepository;
    }

    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW,
            transactionManager = "localStorageTransactionManager"
    )
    public void create(String code) {
        bucketEntityRepository.findByCode(code)
                .ifPresent(it -> {
                    throw new ServiceException(404, StorageErrors.BUCKET_ALREADY_EXISTS, "Bucket already exists");
                });

        bucketEntityRepository.save(
                BucketEntity.builder()
                        .code(code)
                        .readonly(false)
                        .createdAt(ZonedDateTime.now())
                        .modifiedAt(ZonedDateTime.now())
                        .build()
        );
    }

    @Override
    public Bucket get(String code) {
        var bucketEntity = bucketEntityRepository.findByCode(code)
                .orElseThrow(() -> new ServiceException(404, StorageErrors.BUCKET_NOT_EXISTS, "Bucket not exists"));

        var contents = contentEntityRepository.findAllByBucket(bucketEntity)
                .stream()
                .map(this::toLocalBucketContent)
                .toList();

        return LocalBucket.builder()
                .code(bucketEntity.getCode())
                .readOnly(bucketEntity.isReadonly())
                .createdAt(bucketEntity.getCreatedAt())
                .modifiedAt(bucketEntity.getModifiedAt())
                .contents(contents)
                .build();
    }

    private BucketContent toLocalBucketContent(ContentEntity entity) {
        var meta = contentMetaEntityRepository.findAllByContentUid(entity.getUid())
                .stream()
                .collect(Collectors.toMap(ContentMetaEntity::getKey, ContentMetaEntity::getValue));

        return LocalBucketContent.builder()
                .id(entity.getUid())
                .name(entity.getName())
                .meta(meta)
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .build();

    }

    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW,
            transactionManager = "localStorageTransactionManager"
    )
    public void setReadOnly(String code, boolean readOnly) {
        var bucketEntity = bucketEntityRepository.findByCode(code)
                .orElseThrow(() -> new ServiceException(404, StorageErrors.BUCKET_NOT_EXISTS, "Bucket not exists"));

        if (bucketEntity.isReadonly() == readOnly) {
            return;
        }
        bucketEntity.setReadonly(readOnly);
        bucketEntity.setModifiedAt(ZonedDateTime.now());

        bucketEntityRepository.save(bucketEntity);
    }

    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW,
            transactionManager = "localStorageTransactionManager"
    )
    public void delete(String code) {
        var bucketEntity = bucketEntityRepository.findByCode(code)
                .orElse(null);
        if (bucketEntity == null) {
            return;
        }

        var contents = contentEntityRepository.countAllByBucket(bucketEntity);
        if (contents > 0) {
            throw new ServiceException(404, StorageErrors.BUCKET_NOT_EMPTY, "Bucket not empty");
        }

        bucketEntityRepository.delete(bucketEntity);
    }
}

package com.github.simple_mocks.storage.local.repository;

import com.github.simple_mocks.storage.local.entity.BucketEntity;
import com.github.simple_mocks.storage.local.entity.ContentEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author sibmaks
 * @since 0.0.1
 */
public interface ContentEntityRepository extends JpaRepository<ContentEntity, String> {

    /**
     * Count all contents by bucket.
     *
     * @param bucket bucket instance
     * @return number of contents
     */
    long countAllByBucket(@Nonnull BucketEntity bucket);

    /**
     * Find all contents by bucket.
     *
     * @param bucketEntity bucket instance
     * @return list of contents
     */
    List<ContentEntity> findAllByBucket(@Nonnull BucketEntity bucketEntity);
}

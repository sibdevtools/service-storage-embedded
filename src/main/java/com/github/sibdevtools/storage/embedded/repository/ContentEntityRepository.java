package com.github.sibdevtools.storage.embedded.repository;

import com.github.sibdevtools.storage.embedded.entity.BucketEntity;
import com.github.sibdevtools.storage.embedded.entity.ContentEntity;
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
     * Find all content in buckets
     *
     * @param bucketEntity bucket
     * @return list of content
     */
    List<ContentEntity> findAllByBucket(BucketEntity bucketEntity);
}

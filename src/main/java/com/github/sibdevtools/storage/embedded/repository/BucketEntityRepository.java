package com.github.sibdevtools.storage.embedded.repository;

import com.github.sibdevtools.storage.embedded.entity.BucketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * @author sibmaks
 * @since 0.0.1
 */
public interface BucketEntityRepository extends JpaRepository<BucketEntity, Long> {
    /**
     * Retrieves an entity by its code.
     *
     * @param code must not be {@literal null}.
     * @return the entity with the given id or {@literal Optional#empty()} if none found.
     */
    Optional<BucketEntity> findByCode(String code);


    /**
     * Create a bucket if not exists.
     *
     * @param bucketCode bucket code
     */
    @Modifying
    @Query(
            value = "INSERT INTO storage_service.bucket (code, created_at, modified_at, readonly) " +
                    "SELECT :bucketCode, current_timestamp, current_timestamp, false WHERE NOT EXISTS(" +
                    "SELECT 1 FROM storage_service.bucket WHERE code = :bucketCode" +
                    ")",
            nativeQuery = true
    )
    void saveIfNotExists(
            @Param("bucketCode") String bucketCode
    );
}

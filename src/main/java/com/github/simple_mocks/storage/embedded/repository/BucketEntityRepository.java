package com.github.simple_mocks.storage.embedded.repository;

import com.github.simple_mocks.storage.embedded.entity.BucketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

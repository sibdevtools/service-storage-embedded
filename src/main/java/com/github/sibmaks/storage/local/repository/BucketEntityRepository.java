package com.github.sibmaks.storage.local.repository;

import com.github.sibmaks.storage.local.entity.BucketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author sibmaks
 * @since 2023-04-22
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

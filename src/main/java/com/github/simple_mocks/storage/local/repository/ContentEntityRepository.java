package com.github.simple_mocks.storage.local.repository;

import com.github.simple_mocks.storage.local.entity.ContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author sibmaks
 * @since 0.0.1
 */
public interface ContentEntityRepository extends JpaRepository<ContentEntity, String> {
}

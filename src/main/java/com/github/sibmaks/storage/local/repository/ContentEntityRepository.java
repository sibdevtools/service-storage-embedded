package com.github.sibmaks.storage.local.repository;

import com.github.sibmaks.storage.local.entity.ContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author sibmaks
 * @since 2023-04-22
 */
public interface ContentEntityRepository extends JpaRepository<ContentEntity, String> {
}

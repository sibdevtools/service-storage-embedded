package com.github.simple_mocks.storage.local.repository;

import com.github.simple_mocks.storage.local.entity.ContentMetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author sibmaks
 * @since 0.0.1
 */
public interface ContentMetaEntityRepository extends JpaRepository<ContentMetaEntity, String> {

    /**
     * Get all meta linked to content
     *
     * @param uid content identifier
     * @return list of meta-info
     */
    List<ContentMetaEntity> findAllByContentUid(String uid);

    /**
     * Remove all meta linked to content
     * @param uid content identifier
     */
    void deleteAllByContentUid(String uid);

}

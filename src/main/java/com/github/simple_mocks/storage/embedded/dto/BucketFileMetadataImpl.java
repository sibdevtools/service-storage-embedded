package com.github.simple_mocks.storage.embedded.dto;

import com.github.simple_mocks.storage.api.dto.BucketFileMetadata;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@AllArgsConstructor
public class BucketFileMetadataImpl implements BucketFileMetadata {
    private final Map<String, String> meta;

    @Nonnull
    @Override
    public String get(String key) {
        return meta.get(key);
    }
}

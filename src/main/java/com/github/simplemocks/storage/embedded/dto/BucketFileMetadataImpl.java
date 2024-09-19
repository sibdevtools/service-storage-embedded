package com.github.simplemocks.storage.embedded.dto;

import com.github.simplemocks.storage.api.dto.BucketFileMetadata;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@AllArgsConstructor
public class BucketFileMetadataImpl implements BucketFileMetadata {
    private final Map<String, String> meta;

    @Nonnull
    @Override
    public String get(@Nonnull String key) {
        return meta.get(key);
    }

    @Override
    public Set<String> getAttributeNames() {
        return new HashSet<>(meta.keySet());
    }
}

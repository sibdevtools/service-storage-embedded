package com.github.simple_mocks.storage.embedded.dto;

import com.github.simple_mocks.storage.api.dto.BucketFile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BucketFileImpl implements BucketFile {
    /**
     * File content description
     */
    private final BucketFileDescriptionImpl description;
    /**
     * File data
     */
    private final byte[] data;
}

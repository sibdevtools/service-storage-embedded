package com.github.sibdevtools.storage.embedded.dto;

import com.github.sibdevtools.storage.api.dto.Bucket;
import com.github.sibdevtools.storage.api.dto.BucketFileDescription;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author sibmaks
 * @since 0.0.1
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BucketImpl implements Bucket {
    /**
     * Content name
     */
    private final String code;
    /**
     * Bucket creation date time
     */
    private final ZonedDateTime createdAt;
    /**
     * Bucket modification date time
     */
    private final ZonedDateTime modifiedAt;
    /**
     * Bucket is read-only
     */
    private final boolean readOnly;
    /**
     * Bucket's contents
     */
    private final List<BucketFileDescription> contents;
}

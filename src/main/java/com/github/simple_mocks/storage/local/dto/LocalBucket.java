package com.github.simple_mocks.storage.local.dto;

import com.github.simple_mocks.storage.api.Bucket;
import com.github.simple_mocks.storage.api.BucketContent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author sibmaks
 * @since 0.1.4
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalBucket implements Bucket {
    private final String code;
    private final boolean readOnly;
    private final List<BucketContent> contents;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime modifiedAt;
}

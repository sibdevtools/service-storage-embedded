package com.github.simple_mocks.storage.local.dto;

import com.github.simple_mocks.storage.api.BucketContent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * @author sibmaks
 * @since 0.1.4
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalBucketContent implements BucketContent {
    private final String id;
    private final String name;
    private final Map<String, String> meta;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime modifiedAt;
}

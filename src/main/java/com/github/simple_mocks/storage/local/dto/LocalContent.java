package com.github.simple_mocks.storage.local.dto;

import com.github.simple_mocks.storage.api.Content;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * @author sibmaks
 * @since 0.0.1
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalContent implements Content {
    /**
     * Content identifier
     */
    private final String id;
    /**
     * Content name
     */
    private final String name;
    /**
     * Content meta data
     */
    private final Map<String, String> meta;
    /**
     * Content
     */
    private final byte[] content;
    /**
     * Content creation date time
     */
    private final ZonedDateTime createdAt;
    /**
     * Content modification date time
     */
    private final ZonedDateTime modifiedAt;

    @Override
    public Map<String, String> getMeta() {
        return Collections.unmodifiableMap(meta);
    }

    @Override
    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

}

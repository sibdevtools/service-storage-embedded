package com.github.sibmaks.storage.local.dto;

import com.github.sibmaks.storage.api.Content;
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
 * @since 2023-04-11
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalContent implements Content {
    private final String id;
    private final String name;
    private final Map<String, String> meta;
    private final byte[] content;
    private final ZonedDateTime createdAt;
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

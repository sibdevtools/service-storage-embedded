package com.github.sibdevtools.storage.embedded.dto;

import com.github.sibdevtools.storage.api.dto.BucketFileDescription;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

/**
 * @author sibmaks
 * @since 0.0.1
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BucketFileDescriptionImpl implements BucketFileDescription {
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
    private final BucketFileMetadataImpl meta;
    /**
     * Content creation date time
     */
    private final ZonedDateTime createdAt;
    /**
     * Content modification date time
     */
    private final ZonedDateTime modifiedAt;
}

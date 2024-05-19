package com.github.simple_mocks.storage.local.service.codec;

import com.github.simple_mocks.storage.local.dto.ContentStorageFormat;

/**
 * Storage codec interface. Used for encoding/decoding content storage.
 *
 * @author sibmaks
 * @since 0.1.0
 */
public interface StorageCodec {
    /**
     * Encode source content into encoded byte array.
     *
     * @param bytes source content
     * @return encoded content
     */
    byte[] encode(byte[] bytes);

    /**
     * Decode encoded content into a source byte array.
     *
     * @param bytes encoded content
     * @return source content
     */
    byte[] decode(byte[] bytes);

    /**
     * Get a format of encoded content.
     *
     * @return format of encoded content
     */
    ContentStorageFormat getFormat();
}

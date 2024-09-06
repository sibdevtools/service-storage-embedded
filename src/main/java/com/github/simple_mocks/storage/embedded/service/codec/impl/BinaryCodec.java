package com.github.simple_mocks.storage.embedded.service.codec.impl;

import com.github.simple_mocks.storage.embedded.dto.ContentStorageFormat;
import com.github.simple_mocks.storage.embedded.service.codec.StorageCodec;
import org.springframework.stereotype.Component;

/**
 * No operation codec. Store data as is.
 *
 * @author sibmaks
 * @since 0.1.0
 */
@Component
public class BinaryCodec implements StorageCodec {

    @Override
    public byte[] encode(byte[] bytes) {
        return bytes;
    }

    @Override
    public byte[] decode(byte[] bytes) {
        return bytes;
    }

    @Override
    public ContentStorageFormat getFormat() {
        return ContentStorageFormat.BINARY;
    }
}

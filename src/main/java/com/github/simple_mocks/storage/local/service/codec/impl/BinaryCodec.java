package com.github.simple_mocks.storage.local.service.codec.impl;

import com.github.simple_mocks.storage.local.dto.ContentStorageFormat;
import com.github.simple_mocks.storage.local.service.codec.StorageCodec;
import org.springframework.stereotype.Component;

/**
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

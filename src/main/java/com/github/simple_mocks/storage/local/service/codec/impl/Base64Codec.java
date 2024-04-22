package com.github.simple_mocks.storage.local.service.codec.impl;

import com.github.simple_mocks.storage.local.dto.ContentStorageFormat;
import com.github.simple_mocks.storage.local.service.codec.StorageCodec;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * @author sibmaks
 * @since 0.1.0
 */
@Component
public class Base64Codec implements StorageCodec {
    private final Base64.Encoder encoder;
    private final Base64.Decoder decoder;

    /**
     * Construct default base64 codec
     */
    public Base64Codec() {
        this.encoder = Base64.getEncoder();
        this.decoder = Base64.getDecoder();
    }

    @Override
    public byte[] encode(byte[] bytes) {
        return encoder.encode(bytes);
    }

    @Override
    public byte[] decode(byte[] bytes) {
        return decoder.decode(bytes);
    }

    @Override
    public ContentStorageFormat getFormat() {
        return ContentStorageFormat.BASE64;
    }
}

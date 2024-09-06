package com.github.simple_mocks.storage.embedded.service.codec.impl;

import com.github.simple_mocks.storage.embedded.dto.ContentStorageFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author sibmaks
 * @since 0.1.0
 */
@ExtendWith(MockitoExtension.class)
class Base64CodecTest {
    @InjectMocks
    private Base64Codec codec;

    @Test
    void testEncodeDecodeCycle() {
        var source = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

        var encoded = codec.encode(source);
        assertNotNull(encoded);

        var decoded = codec.decode(encoded);

        assertArrayEquals(source, decoded);
    }

    @Test
    void testGetFormat() {
        var format = codec.getFormat();
        assertEquals(ContentStorageFormat.BASE64, format);
    }

}
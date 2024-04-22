package com.github.simple_mocks.storage.local.service.codec.impl;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.StorageErrors;
import com.github.simple_mocks.storage.local.dto.ContentStorageFormat;
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
class GZipCodecTest {
    @InjectMocks
    private GZipCodec codec;

    @Test
    void testEncodeDecodeCycle() {
        var source = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

        var encoded = codec.encode(source);
        assertNotNull(encoded);

        var decoded = codec.decode(encoded);

        assertArrayEquals(source, decoded);
    }

    @Test
    void testDecodeWhenContentIsCorrupted() {
        var source = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

        var exception = assertThrows(ServiceException.class, () -> codec.decode(source));
        assertEquals(503, exception.getStatus());
        assertEquals("Can't decode bytes to GZip", exception.getMessage());
        assertEquals(StorageErrors.UNEXPECTED_ERROR, exception.getServiceError());

    }

    @Test
    void testGetFormat() {
        var format = codec.getFormat();
        assertEquals(ContentStorageFormat.GZIP, format);
    }

}
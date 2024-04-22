package com.github.simple_mocks.storage.local.service.codec.impl;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.StorageErrors;
import com.github.simple_mocks.storage.local.dto.ContentStorageFormat;
import com.github.simple_mocks.storage.local.service.codec.StorageCodec;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author sibmaks
 * @since 0.1.0
 */
@Component
public class GZipCodec implements StorageCodec {

    @Override
    public byte[] encode(byte[] bytes) {
        var out = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't encode bytes to GZip", e);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] decode(byte[] bytes) {
        var out = new ByteArrayInputStream(bytes);
        try (var gzip = new GZIPInputStream(out)) {
            return gzip.readAllBytes();
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't decode bytes to GZip", e);
        }
    }

    @Override
    public ContentStorageFormat getFormat() {
        return ContentStorageFormat.GZIP;
    }
}

package com.github.sibdevtools.storage.embedded.service.codec.impl;

import com.github.sibdevtools.storage.embedded.dto.ContentStorageFormat;
import com.github.sibdevtools.storage.embedded.exception.UnexpectedErrorException;
import com.github.sibdevtools.storage.embedded.service.codec.StorageCodec;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZip codec. Store content in a compressed format.
 *
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
            throw new UnexpectedErrorException("Can't encode bytes to GZip", e);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] decode(byte[] bytes) {
        var out = new ByteArrayInputStream(bytes);
        try (var gzip = new GZIPInputStream(out)) {
            return gzip.readAllBytes();
        } catch (IOException e) {
            throw new UnexpectedErrorException("Can't decode bytes to GZip", e);
        }
    }

    @Override
    public ContentStorageFormat getFormat() {
        return ContentStorageFormat.GZIP;
    }
}

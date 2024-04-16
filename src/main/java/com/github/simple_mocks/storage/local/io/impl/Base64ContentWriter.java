package com.github.simple_mocks.storage.local.io.impl;

import com.github.simple_mocks.storage.local.conf.LocalStorageServiceEnabled;
import com.github.simple_mocks.storage.local.io.ContentWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author sibmaks
 * @since 0.0.1
 */
@Component("localBase64ContentWriter")
@ConditionalOnProperty(name = "service.local-storage.content.encode", havingValue = "base64", matchIfMissing = true)
@ConditionalOnBean(LocalStorageServiceEnabled.class)
public class Base64ContentWriter implements ContentWriter {
    private final Base64.Encoder base64Encoder;

    /**
     * Creator base64 content writer
     *
     * @param base64Encoder base 64 encoder instance
     */
    @Autowired
    public Base64ContentWriter(Base64.Encoder base64Encoder) {
        this.base64Encoder = base64Encoder;
    }

    @Override
    public void write(byte[] content, FileChannel fileChannel) throws IOException {
        try (var writer = new BufferedWriter(Channels.newWriter(fileChannel, StandardCharsets.UTF_8))) {
            var content64 = base64Encoder.encodeToString(content);
            writer.write(content64);
        }
    }
}

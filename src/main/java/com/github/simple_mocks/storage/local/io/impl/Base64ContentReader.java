package com.github.simple_mocks.storage.local.io.impl;

import com.github.simple_mocks.storage.local.conf.LocalStorageServiceEnabled;
import com.github.simple_mocks.storage.local.io.ContentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author sibmaks
 * @since 0.0.1
 */
@Component("base64ContentReader")
@ConditionalOnProperty(name = "service.local-storage.content.encode", havingValue = "base64", matchIfMissing = true)
@ConditionalOnBean(LocalStorageServiceEnabled.class)
public class Base64ContentReader implements ContentReader {
    private final Base64.Decoder base64Decoder;

    /**
     * Creator base64 content reader
     *
     * @param base64Decoder base 64 decoder instance
     */
    @Autowired
    public Base64ContentReader(Base64.Decoder base64Decoder) {
        this.base64Decoder = base64Decoder;
    }

    @Override
    public byte[] read(FileChannel fileChannel) throws IOException {
        try (var reader = new BufferedReader(Channels.newReader(fileChannel, StandardCharsets.UTF_8))) {
            var builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            var content64 = builder.toString();
            return base64Decoder.decode(content64);
        }
    }
}

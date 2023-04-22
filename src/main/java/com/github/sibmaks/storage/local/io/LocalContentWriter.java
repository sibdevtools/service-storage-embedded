package com.github.sibmaks.storage.local.io;

import com.github.sibmaks.storage.local.conf.LocalStorageServiceEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author sibmaks
 * @since 2023-04-22
 */
@Component
@ConditionalOnBean(LocalStorageServiceEnabled.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalContentWriter {
    private final Base64.Encoder base64Encoder;

    /**
     * Method write local content to file channel
     *
     * @param content content to write
     * @param fileChannel file channel
     * @throws IOException file write error or content serialization error
     */
    public void write(byte[] content, FileChannel fileChannel) throws IOException {
        try(var writer = new BufferedWriter(Channels.newWriter(fileChannel, StandardCharsets.UTF_8))) {
            String content64 = base64Encoder.encodeToString(content);
            writer.write(content64);
        }
    }
}

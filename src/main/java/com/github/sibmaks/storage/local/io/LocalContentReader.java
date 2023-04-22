package com.github.sibmaks.storage.local.io;

import com.github.sibmaks.storage.local.conf.LocalStorageServiceEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
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
public class LocalContentReader {
    private final Base64.Decoder base64Decoder;

    /**
     * Method read local content from file channel
     *
     * @param fileChannel file channel
     * @return deserialized local content
     * @throws IOException file read error or content parsing error
     */
    public byte[] read(FileChannel fileChannel) throws IOException {
        try(var reader = new BufferedReader(Channels.newReader(fileChannel, StandardCharsets.UTF_8))) {
            var builder = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
            var content64 = builder.toString();
            return base64Decoder.decode(content64);
        }
    }
}

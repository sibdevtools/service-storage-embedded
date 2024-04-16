package com.github.simple_mocks.storage.local.io;

import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author sibmaks
 * @since 0.0.4
 */
public interface ContentWriter {

    /**
     * Method write local content to file a channel
     *
     * @param content     content to write
     * @param fileChannel file channel
     * @throws IOException file write error or content serialization error
     */
    void write(byte[] content, FileChannel fileChannel) throws IOException;

}

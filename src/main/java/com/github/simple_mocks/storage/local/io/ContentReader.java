package com.github.simple_mocks.storage.local.io;

import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author sibmaks
 * @since 0.0.4
 */
public interface ContentReader {

    /**
     * Method read local content from a file channel
     *
     * @param fileChannel file channel
     * @return deserialized local content
     * @throws IOException file read error or content parsing error
     */
    byte[] read(FileChannel fileChannel) throws IOException;

}

package com.github.simplemocks.storage.embedded.exception;

import com.github.simplemocks.error_service.exception.ServiceException;
import com.github.simplemocks.storage.embedded.constant.Constants;

/**
 * @author sibmaks
 * @since 0.1.5
 */
public class FileNotFoundException extends ServiceException {

    /**
     * Construct a file not found exception.
     *
     * @param systemMessage system message
     */
    public FileNotFoundException(String systemMessage) {
        super(404, Constants.ERROR_SOURCE, "FILE_NOT_FOUND", systemMessage);
    }

    /**
     * Construct a file not found exception with cause.
     *
     * @param systemMessage system message
     * @param cause         cause
     */
    public FileNotFoundException(String systemMessage, Throwable cause) {
        super(404, Constants.ERROR_SOURCE, "FILE_NOT_FOUND", systemMessage, cause);
    }

}

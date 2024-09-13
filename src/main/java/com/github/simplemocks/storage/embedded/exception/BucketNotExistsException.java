package com.github.simplemocks.storage.embedded.exception;

import com.github.simplemocks.error_service.exception.ServiceException;
import com.github.simplemocks.storage.embedded.constant.Constants;

/**
 * @author sibmaks
 * @since 0.1.5
 */
public class BucketNotExistsException extends ServiceException {

    /**
     * Construct a bucket not exists exception.
     *
     * @param systemMessage system message
     */
    public BucketNotExistsException(String systemMessage) {
        super(404, Constants.ERROR_SOURCE, "BUCKET_NOT_EXISTS", systemMessage);
    }

}

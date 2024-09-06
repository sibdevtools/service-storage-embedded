package com.github.simple_mocks.storage.embedded.exception;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.embedded.constant.Constants;

/**
 * @author sibmaks
 * @since 0.1.5
 */
public class BucketNotEmptyException extends ServiceException {

    /**
     * Construct a bucket not empty exception.
     *
     * @param systemMessage system message
     */
    public BucketNotEmptyException(String systemMessage) {
        super(403, Constants.ERROR_SOURCE, "BUCKET_NOT_EMPTY", systemMessage);
    }

}

package com.github.simple_mocks.storage.embedded.exception;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.embedded.constant.Constants;

/**
 * @author sibmaks
 * @since 0.1.5
 */
public class BucketReadonlyException extends ServiceException {

    /**
     * Construct a bucket readonly exception.
     *
     * @param systemMessage system message
     */
    public BucketReadonlyException(String systemMessage) {
        super(403, Constants.ERROR_SOURCE, "BUCKET_READ_ONLY", systemMessage);
    }

}

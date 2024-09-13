package com.github.simplemocks.storage.embedded.exception;

import com.github.simplemocks.error_service.exception.ServiceException;
import com.github.simplemocks.storage.embedded.constant.Constants;

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

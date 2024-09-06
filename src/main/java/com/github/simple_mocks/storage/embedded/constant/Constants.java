package com.github.simple_mocks.storage.embedded.constant;

import com.github.simple_mocks.error_service.api.ErrorSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final ErrorSource ERROR_SOURCE = new ErrorSource("STORAGE_SERVICE");

}

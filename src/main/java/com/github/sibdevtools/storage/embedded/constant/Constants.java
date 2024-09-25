package com.github.sibdevtools.storage.embedded.constant;

import com.github.sibdevtools.error.api.dto.ErrorSourceId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final ErrorSourceId ERROR_SOURCE = new ErrorSourceId("STORAGE_SERVICE");

}

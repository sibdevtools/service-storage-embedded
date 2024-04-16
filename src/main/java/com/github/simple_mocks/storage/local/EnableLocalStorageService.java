package com.github.simple_mocks.storage.local;

import com.github.simple_mocks.storage.local.conf.LocalStorageServiceConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enabler for local implementation of storage service.
 *
 * @author sibmaks
 * @since 0.0.1
 * @see com.github.simple_mocks.storage.api.StorageService
 * @see com.github.simple_mocks.storage.local.service.LocalStorageService
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(LocalStorageServiceConfig.class)
public @interface EnableLocalStorageService {
}

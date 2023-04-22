package com.github.sibmaks.storage.local;

import com.github.sibmaks.storage.local.conf.LocalStorageServiceConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enabler for local implementation of storage service.
 *
 * @author sibmaks
 * @since 2023-04-11
 * @see com.github.sibmaks.storage.api.StorageService
 * @see com.github.sibmaks.storage.local.service.LocalStorageService
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(LocalStorageServiceConfig.class)
public @interface EnableLocalStorageService {
}

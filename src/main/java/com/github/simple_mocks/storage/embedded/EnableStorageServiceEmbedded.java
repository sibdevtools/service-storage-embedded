package com.github.simple_mocks.storage.embedded;

import com.github.simple_mocks.storage.embedded.conf.StorageServiceEmbeddedConfig;
import com.github.simple_mocks.storage.embedded.service.StorageServiceEmbedded;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enabler for embedded implementation of storage service.
 *
 * @author sibmaks
 * @see com.github.simple_mocks.storage.api.service.StorageService
 * @see StorageServiceEmbedded
 * @since 0.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(StorageServiceEmbeddedConfig.class)
public @interface EnableStorageServiceEmbedded {
}

package com.github.simplemocks.storage.embedded;

import com.github.simplemocks.storage.embedded.conf.StorageServiceEmbeddedConfig;
import com.github.simplemocks.storage.embedded.service.StorageServiceEmbedded;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enabler for embedded implementation of storage service.
 *
 * @author sibmaks
 * @see com.github.simplemocks.storage.api.service.StorageService
 * @see StorageServiceEmbedded
 * @since 0.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(StorageServiceEmbeddedConfig.class)
public @interface EnableStorageServiceEmbedded {
}

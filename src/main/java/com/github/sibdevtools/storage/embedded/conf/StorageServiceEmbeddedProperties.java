package com.github.sibdevtools.storage.embedded.conf;

import com.github.sibdevtools.storage.embedded.dto.ContentStorageFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author sibmaks
 * @since 0.1.0
 */
@Setter
@Getter
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("service.storage.embedded")
public class StorageServiceEmbeddedProperties {
    private String folder;
    private int bufferSize;
    private ContentStorageFormat storageFormat;
    private String defaultStorageContainer;
}

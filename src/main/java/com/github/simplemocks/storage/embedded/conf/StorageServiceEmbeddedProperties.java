package com.github.simplemocks.storage.embedded.conf;

import com.github.simplemocks.storage.embedded.dto.ContentStorageFormat;
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
@ConfigurationProperties("service.embedded.storage")
public class StorageServiceEmbeddedProperties {
    private String folder = "data";
    private int bufferSize = 1024;
    private ContentStorageFormat storageFormat = ContentStorageFormat.GZIP;
    private String defaultStorageContainer = "FILE";
}

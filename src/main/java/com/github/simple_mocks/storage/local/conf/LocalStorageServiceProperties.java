package com.github.simple_mocks.storage.local.conf;

import com.github.simple_mocks.storage.local.dto.ContentStorageFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author sibmaks
 * @since 0.1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("service.local.storage")
public class LocalStorageServiceProperties {
    private String folder = "data";
    private int bufferSize = 1024;
    private ContentStorageFormat storageFormat = ContentStorageFormat.GZIP;
}

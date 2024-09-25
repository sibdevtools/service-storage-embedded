package com.github.sibdevtools.storage.embedded.conf;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author sibmaks
 * @since 0.1.16
 */
@Setter
@Getter
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("service.storage.embedded.flyway")
public class StorageServiceEmbeddedFlywayProperties {
    private String encoding;
    private String[] locations;
    private String schema;
}

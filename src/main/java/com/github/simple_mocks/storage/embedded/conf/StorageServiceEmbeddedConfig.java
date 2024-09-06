package com.github.simple_mocks.storage.embedded.conf;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;

/**
 * @author sibmaks
 * @since 0.0.1
 */
@PropertySource("classpath:embedded-storage-application.properties")
public class StorageServiceEmbeddedConfig {

    @Bean
    @ConfigurationProperties("spring.flyway.embedded-storage")
    public ClassicConfiguration storageFlywayConfiguration(DataSource dataSource) {
        var classicConfiguration = new ClassicConfiguration();
        classicConfiguration.setDataSource(dataSource);
        return classicConfiguration;
    }

    @Bean
    public Flyway storageFlyway(@Qualifier("storageFlywayConfiguration") ClassicConfiguration configuration) {
        var flyway = new Flyway(configuration);
        flyway.migrate();
        return flyway;
    }
}

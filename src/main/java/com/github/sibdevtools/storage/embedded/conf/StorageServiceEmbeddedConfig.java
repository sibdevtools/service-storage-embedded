package com.github.sibdevtools.storage.embedded.conf;

import com.github.sibdevtools.error.mutable.api.source.ErrorLocalizationsJsonSource;
import com.github.sibdevtools.storage.embedded.dto.ContentStorageFormat;
import com.github.sibdevtools.storage.embedded.service.codec.StorageCodec;
import com.github.sibdevtools.storage.embedded.service.storage.StorageContainer;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sibmaks
 * @since 0.0.1
 */
@ErrorLocalizationsJsonSource(
        systemCode = "STORAGE_SERVICE",
        iso3Code = "eng",
        path = "classpath:/embedded/storage/content/errors/eng.json"
)
@ErrorLocalizationsJsonSource(
        systemCode = "STORAGE_SERVICE",
        iso3Code = "rus",
        path = "classpath:/embedded/storage/content/errors/rus.json"
)
@Configuration
@PropertySource("classpath:/embedded/storage/application.properties")
@ConditionalOnProperty(name = "service.storage.mode", havingValue = "EMBEDDED")
public class StorageServiceEmbeddedConfig {

    @Bean
    public Flyway embeddedStorageFlyway(StorageServiceEmbeddedFlywayProperties configuration,
                                        DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .encoding(configuration.getEncoding())
                .locations(configuration.getLocations())
                .defaultSchema(configuration.getSchema())
                .schemas(configuration.getSchema())
                .placeholders(
                        Map.of(
                                "schema", configuration.getSchema()
                        )
                )
                .load();
    }

    @Bean
    public MigrateResult embeddedStorageFlywayMigrateResult(
            @Qualifier("embeddedStorageFlyway") Flyway embeddedStorageFlyway
    ) {
        return embeddedStorageFlyway.migrate();
    }

    @Bean("storageContainerMap")
    public Map<String, StorageContainer> storageContainerMap(
            List<StorageContainer> storageContainers
    ) {
        return storageContainers.stream()
                .collect(Collectors.toMap(StorageContainer::getType, Function.identity()));
    }

    @Bean("storageCodecsMap")
    public Map<ContentStorageFormat, StorageCodec> storageCodecsMap(
            List<StorageCodec> storageCodecs
    ) {
        return storageCodecs.stream()
                .collect(Collectors.toMap(StorageCodec::getFormat, Function.identity()));
    }
}

package com.github.sibmaks.storage.local.conf;

import com.github.sibmaks.storage.local.EnableLocalStorageService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Properties;

/**
 * @author sibmaks
 * @since 2023-04-11
 */
@PropertySource("classpath:storage-local-application.properties")
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackageClasses = {EnableLocalStorageService.class},
        entityManagerFactoryRef = "localStorageEntityManagerFactory",
        transactionManagerRef = "localStorageTransactionManager"
)
public class LocalStorageServiceConfig {

    @Bean
    public LocalStorageServiceEnabled localStorageServiceEnabled() {
        return new LocalStorageServiceEnabled();
    }

    @Bean
    @Qualifier("localStorageDataSourceProperties")
    @ConfigurationProperties("spring.datasource.local-storage")
    public DataSourceProperties localStorageDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("localStorageDataSource")
    public DataSource localStorageDataSource(
            @Qualifier("localStorageDataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    @Qualifier("localStorageEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean localStorageEntityManagerFactory(
            @Qualifier("localStorageDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder managerFactoryBuilder) {
        return managerFactoryBuilder
                .dataSource(dataSource)
                .packages(EnableLocalStorageService.class)
                .build();
    }

    @Bean
    @Qualifier("localStorageJpaProperties")
    @ConfigurationProperties("spring.jpa.local-storage")
    public Properties localStorageJpaProperties() {
        return new Properties();
    }

    @Bean
    @Qualifier("localStorageTransactionManager")
    public PlatformTransactionManager localStorageTransactionManager(
            @Qualifier("localStorageEntityManagerFactory") LocalContainerEntityManagerFactoryBean managerFactoryBean,
            @Qualifier("localStorageJpaProperties") Properties localStorageJpaProperties) {
        var entityManagerFactory = managerFactoryBean.getObject();
        Objects.requireNonNull(entityManagerFactory);
        var jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
        jpaTransactionManager.setJpaProperties(localStorageJpaProperties);
        return jpaTransactionManager;
    }

    @Bean
    @Qualifier("localStorageFlyway")
    public Flyway localStorageFlyway(@Qualifier("localStorageDataSource") DataSource dataSource) {
        var configuration = new FluentConfiguration();
        configuration.dataSource(dataSource);
        configuration.locations("db/migration/local-storage");
        var flyway = new Flyway(configuration);
        flyway.migrate();
        return flyway;
    }
}

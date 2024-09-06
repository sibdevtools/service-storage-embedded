package com.github.simple_mocks.storage.embedded;

import com.github.simple_mocks.storage.api.service.StorageBucketService;
import com.github.simple_mocks.storage.embedded.exception.BucketNotExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@ActiveProfiles("startup-test")
@EnableStorageServiceEmbedded
@SpringBootApplication
class StorageBucketServiceEmbeddedIntegrationTest {

    @Test
    void testCreateAndGet() {
        try (var context = SpringApplication.run(StorageBucketServiceEmbeddedIntegrationTest.class)) {
            assertNotNull(context);

            var storageBucketService = context.getBean(StorageBucketService.class);
            assertNotNull(storageBucketService);

            var bucketCode = UUID.randomUUID().toString();
            storageBucketService.create(bucketCode);

            var bucket = storageBucketService.get(bucketCode);
            assertNotNull(bucket);

            assertEquals(bucketCode, bucket.getCode());
            assertNotNull(bucket.getCreatedAt());
            assertNotNull(bucket.getModifiedAt());
            assertNotNull(bucket.getContents());
        }
    }

    @Test
    void testCreateAndDelete() {
        try (var context = SpringApplication.run(StorageBucketServiceEmbeddedIntegrationTest.class)) {
            assertNotNull(context);

            var storageBucketService = context.getBean(StorageBucketService.class);
            assertNotNull(storageBucketService);

            var bucketCode = UUID.randomUUID().toString();
            storageBucketService.create(bucketCode);

            storageBucketService.delete(bucketCode);

            var exception = assertThrows(
                    BucketNotExistsException.class,
                    () -> storageBucketService.get(bucketCode)
            );
            assertEquals("Bucket does not exists", exception.getMessage());
        }
    }
}

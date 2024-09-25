package com.github.sibdevtools.storage.embedded;

import com.github.sibdevtools.storage.api.service.StorageBucketService;
import com.github.sibdevtools.storage.embedded.exception.BucketNotExistsException;
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

            var bucketRs = storageBucketService.get(bucketCode);
            assertNotNull(bucketRs);

            var bucket = bucketRs.getBody();

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

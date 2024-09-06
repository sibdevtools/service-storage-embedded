package com.github.simple_mocks.storage.embedded;

import com.github.simple_mocks.storage.api.rq.SaveFileRq;
import com.github.simple_mocks.storage.api.service.StorageBucketService;
import com.github.simple_mocks.storage.api.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@ActiveProfiles("startup-test")
@EnableStorageServiceEmbedded
@SpringBootApplication
class StorageServiceEmbeddedIntegrationTest {

    @Test
    void testSaveAndGet() {
        try (var context = SpringApplication.run(StorageServiceEmbeddedIntegrationTest.class)) {
            assertNotNull(context);

            var storageBucketService = context.getBean(StorageBucketService.class);
            assertNotNull(storageBucketService);

            var bucket = UUID.randomUUID().toString();
            storageBucketService.create(bucket);

            var storageService = context.getBean(StorageService.class);

            var name = UUID.randomUUID().toString();

            var metaKey = UUID.randomUUID().toString();
            var metaValue = UUID.randomUUID().toString();
            byte[] data = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

            var fileId = storageService.save(
                    SaveFileRq.builder()
                            .bucket(bucket)
                            .name(name)
                            .meta(Map.of(metaKey, metaValue))
                            .data(data)
                            .build()
            );

            assertNotNull(fileId);

            var bucketFile = storageService.get(fileId);
            assertNotNull(bucketFile);

            assertArrayEquals(data, bucketFile.getData());

            var description = bucketFile.getDescription();
            assertNotNull(description);

            assertEquals(name, description.getName());
            assertNotNull(description.getCreatedAt());
            assertNotNull(description.getModifiedAt());

            var meta = description.getMeta();
            assertNotNull(meta);

            assertEquals(metaValue, meta.get(metaKey));

            description = storageService.getDescription(fileId);
            assertNotNull(description);

            assertEquals(name, description.getName());
            assertNotNull(description.getCreatedAt());
            assertNotNull(description.getModifiedAt());

            meta = description.getMeta();
            assertNotNull(meta);

            assertEquals(metaValue, meta.get(metaKey));
        }
    }
}

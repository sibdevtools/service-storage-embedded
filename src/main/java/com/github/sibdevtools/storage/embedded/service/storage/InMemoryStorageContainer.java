package com.github.sibdevtools.storage.embedded.service.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author sibmaks
 * @since 0.1.14
 */
@Service
@ConditionalOnProperty(name = "service.storage.mode", havingValue = "EMBEDDED")
public class InMemoryStorageContainer implements StorageContainer {
    private final Map<Long, Map<String, byte[]>> contents;

    public InMemoryStorageContainer() {
        this.contents = new ConcurrentHashMap<>();
    }

    @Override
    public byte[] get(long bucketId, String contentId) {
        return contents.get(bucketId)
                .get(contentId);
    }

    @Override
    public void save(long id, String uid, byte[] data) {
        var bucket = contents.computeIfAbsent(id, it -> new ConcurrentHashMap<>());
        bucket.put(uid, data);
    }

    @Override
    public void delete(long bucketId, String contentId) {
        contents.get(bucketId)
                .remove(contentId);
    }

    @Override
    public String getType() {
        return "IN_MEMORY";
    }

}

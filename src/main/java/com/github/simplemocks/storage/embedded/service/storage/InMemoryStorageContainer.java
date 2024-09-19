package com.github.simplemocks.storage.embedded.service.storage;

import com.github.simplemocks.storage.embedded.conf.StorageServiceEmbeddedCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author sibmaks
 * @since 0.1.14
 */
@Service
@Conditional(StorageServiceEmbeddedCondition.class)
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

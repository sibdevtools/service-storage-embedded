package com.github.sibdevtools.storage.embedded.service.storage;

/**
 * @author sibmaks
 * @since 0.1.14
 */
public interface StorageContainer {
    byte[] get(long bucketId, String contentId);

    void save(long id, String uid, byte[] data);

    void delete(long bucketId, String contentId);

    String getType();

}

package com.github.simple_mocks.storage.local.service;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.StorageErrors;
import com.github.simple_mocks.storage.local.entity.BucketEntity;
import com.github.simple_mocks.storage.local.entity.ContentEntity;
import com.github.simple_mocks.storage.local.entity.ContentMetaEntity;
import com.github.simple_mocks.storage.local.repository.BucketEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentMetaEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalStorageBucketServiceTest {

    @Mock
    private BucketEntityRepository bucketEntityRepository;

    @Mock
    private ContentEntityRepository contentEntityRepository;

    @Mock
    private ContentMetaEntityRepository contentMetaEntityRepository;

    @InjectMocks
    private LocalStorageBucketService localStorageBucketService;

    @Test
    void testCreateBucketSuccessfully() {
        var bucketCode = UUID.randomUUID().toString();

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.empty());

        localStorageBucketService.create(bucketCode);

        verify(bucketEntityRepository)
                .findByCode(bucketCode);
        verify(bucketEntityRepository)
                .save(any(BucketEntity.class));
    }

    @Test
    void testCreateBucketAlreadyExists() {
        var bucketCode = UUID.randomUUID().toString();

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.of(new BucketEntity()));

        var exception = assertThrows(ServiceException.class, () -> localStorageBucketService.create(bucketCode));

        assertEquals(404, exception.getStatus());
        assertEquals(StorageErrors.BUCKET_ALREADY_EXISTS, exception.getServiceError());
        assertEquals("Bucket already exists", exception.getMessage());
    }

    @Test
    void testGetBucketSuccessfully() {
        var bucketCode = UUID.randomUUID().toString();

        var bucketEntity = new BucketEntity();
        bucketEntity.setCode(bucketCode);
        bucketEntity.setReadonly(false);
        bucketEntity.setCreatedAt(ZonedDateTime.now());
        bucketEntity.setModifiedAt(ZonedDateTime.now());

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.of(bucketEntity));
        when(contentEntityRepository.findAllByBucket(bucketEntity))
                .thenReturn(Collections.emptyList());

        var bucket = localStorageBucketService.get(bucketCode);

        assertEquals(bucketCode, bucket.getCode());
        assertFalse(bucket.isReadOnly());

        verify(contentEntityRepository)
                .findAllByBucket(bucketEntity);
    }

    @Test
    void testGetBucketNotExists() {
        var bucketCode = UUID.randomUUID().toString();

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class, () -> localStorageBucketService.get(bucketCode));

        assertEquals(404, exception.getStatus());
        assertEquals(StorageErrors.BUCKET_NOT_EXISTS, exception.getServiceError());
        assertEquals("Bucket not exists", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSetReadOnlySuccessfully(boolean readonly) {
        var bucketCode = UUID.randomUUID().toString();

        var bucketEntity = new BucketEntity();
        bucketEntity.setCode(bucketCode);
        bucketEntity.setReadonly(!readonly);
        bucketEntity.setModifiedAt(ZonedDateTime.now());

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.of(bucketEntity));

        localStorageBucketService.setReadOnly(bucketCode, readonly);

        assertEquals(readonly, bucketEntity.isReadonly());
        verify(bucketEntityRepository)
                .save(bucketEntity);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSetReadOnlySuccessfullyWhenNothingChanged(boolean readonly) {
        var bucketCode = UUID.randomUUID().toString();

        var bucketEntity = new BucketEntity();
        bucketEntity.setCode(bucketCode);
        bucketEntity.setReadonly(readonly);
        bucketEntity.setModifiedAt(ZonedDateTime.now());

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.of(bucketEntity));

        localStorageBucketService.setReadOnly(bucketCode, readonly);

        verify(bucketEntityRepository, never())
                .save(bucketEntity);
    }

    @Test
    void testSetReadOnlyBucketNotExists() {
        var bucketCode = UUID.randomUUID().toString();

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class, () -> localStorageBucketService.setReadOnly(bucketCode, true));

        assertEquals(404, exception.getStatus());
        assertEquals(StorageErrors.BUCKET_NOT_EXISTS, exception.getServiceError());
        assertEquals("Bucket not exists", exception.getMessage());
    }

    @Test
    void testDeleteBucketSuccessfully() {
        var bucketCode = UUID.randomUUID().toString();

        var bucketEntity = new BucketEntity();
        bucketEntity.setCode(bucketCode);

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.of(bucketEntity));
        when(contentEntityRepository.countAllByBucket(bucketEntity))
                .thenReturn(0L);

        localStorageBucketService.delete(bucketCode);

        verify(bucketEntityRepository)
                .delete(bucketEntity);
    }

    @Test
    void testDeleteBucketNotExists() {
        var bucketCode = UUID.randomUUID().toString();

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.empty());

        localStorageBucketService.delete(bucketCode);

        verify(bucketEntityRepository, never())
                .delete(any(BucketEntity.class));
    }

    @Test
    void testDeleteBucketNotEmpty() {
        var bucketCode = UUID.randomUUID().toString();

        var bucketEntity = new BucketEntity();
        bucketEntity.setCode(bucketCode);

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.of(bucketEntity));
        when(contentEntityRepository.countAllByBucket(bucketEntity))
                .thenReturn(1L);

        var exception = assertThrows(ServiceException.class, () -> localStorageBucketService.delete(bucketCode));

        assertEquals(404, exception.getStatus());
        assertEquals(StorageErrors.BUCKET_NOT_EMPTY, exception.getServiceError());
        assertEquals("Bucket not empty", exception.getMessage());
    }

    @Test
    void testGetBucketSuccessfullyWithContent() {

        var bucketCode = UUID.randomUUID().toString();

        var bucketEntity = new BucketEntity();
        bucketEntity.setCode(bucketCode);
        bucketEntity.setReadonly(false);
        bucketEntity.setCreatedAt(ZonedDateTime.now().minusHours(1));
        bucketEntity.setModifiedAt(ZonedDateTime.now());

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.of(bucketEntity));

        var contentEntity = new ContentEntity();
        when(contentEntityRepository.findAllByBucket(bucketEntity))
                .thenReturn(List.of(contentEntity));

        contentEntity.setUid(UUID.randomUUID().toString());
        contentEntity.setName(UUID.randomUUID().toString());
        contentEntity.setCreatedAt(ZonedDateTime.now().minusHours(1));
        contentEntity.setModifiedAt(ZonedDateTime.now());

        var metaEntity = new ContentMetaEntity();
        metaEntity.setKey(UUID.randomUUID().toString());
        metaEntity.setValue(UUID.randomUUID().toString());

        when(contentMetaEntityRepository.findAllByContentUid(contentEntity.getUid()))
                .thenReturn(List.of(metaEntity));

        when(contentEntityRepository.findAllByBucket(bucketEntity))
                .thenReturn(List.of(contentEntity));

        var bucket = localStorageBucketService.get(bucketCode);

        assertEquals(bucketCode, bucket.getCode());
        assertFalse(bucket.isReadOnly());

        verify(contentEntityRepository)
                .findAllByBucket(bucketEntity);

        var bucketContents = bucket.getContents();
        assertNotNull(bucketContents);

        assertEquals(1, bucketContents.size());

        var bucketContent = bucketContents.get(0);

        assertEquals(contentEntity.getUid(), bucketContent.getId());
        assertEquals(contentEntity.getName(), bucketContent.getName());
        assertEquals(metaEntity.getValue(), bucketContent.getMeta().get(metaEntity.getKey()));
    }
}

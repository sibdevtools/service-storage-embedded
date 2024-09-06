package com.github.simple_mocks.storage.embedded.service;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.rq.SetReadOnlyModeRq;
import com.github.simple_mocks.storage.embedded.entity.BucketEntity;
import com.github.simple_mocks.storage.embedded.entity.ContentEntity;
import com.github.simple_mocks.storage.embedded.entity.ContentMetaEntity;
import com.github.simple_mocks.storage.embedded.repository.BucketEntityRepository;
import com.github.simple_mocks.storage.embedded.repository.ContentEntityRepository;
import com.github.simple_mocks.storage.embedded.repository.ContentMetaEntityRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author sibmaks
 * @since 0.1.5
 */
@ExtendWith(MockitoExtension.class)
class StorageBucketServiceEmbeddedTest {

    @Mock
    private BucketEntityRepository bucketEntityRepository;

    @Mock
    private ContentEntityRepository contentEntityRepository;

    @Mock
    private ContentMetaEntityRepository contentMetaEntityRepository;

    @InjectMocks
    private StorageBucketServiceEmbedded serviceEmbedded;

    @Test
    void testCreateBucketSuccessfully() {
        var bucketCode = UUID.randomUUID().toString();

        serviceEmbedded.create(bucketCode);

        verify(bucketEntityRepository)
                .save(any(BucketEntity.class));
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

        var bucket = serviceEmbedded.get(bucketCode);

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

        var exception = assertThrows(ServiceException.class, () -> serviceEmbedded.get(bucketCode));

        assertEquals(404, exception.getStatus());
        assertEquals("BUCKET_NOT_EXISTS", exception.getCode());
        assertEquals("Bucket does not exists", exception.getMessage());
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

        var rq = SetReadOnlyModeRq.builder()
                .code(bucketCode)
                .readOnly(readonly)
                .build();
        serviceEmbedded.setReadOnly(rq);

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

        var rq = SetReadOnlyModeRq.builder()
                .code(bucketCode)
                .readOnly(readonly)
                .build();
        serviceEmbedded.setReadOnly(rq);

        verify(bucketEntityRepository, never())
                .save(bucketEntity);
    }

    @Test
    void testSetReadOnlyBucketNotExists() {
        var bucketCode = UUID.randomUUID().toString();

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.empty());

        var rq = SetReadOnlyModeRq.builder()
                .code(bucketCode)
                .readOnly(true)
                .build();
        var exception = assertThrows(
                ServiceException.class,
                () -> serviceEmbedded.setReadOnly(rq)
        );

        assertEquals(404, exception.getStatus());
        assertEquals("BUCKET_NOT_EXISTS", exception.getCode());
        assertEquals("Bucket does not exists", exception.getMessage());
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

        serviceEmbedded.delete(bucketCode);

        verify(bucketEntityRepository)
                .delete(bucketEntity);
    }

    @Test
    void testDeleteBucketNotExists() {
        var bucketCode = UUID.randomUUID().toString();

        when(bucketEntityRepository.findByCode(bucketCode))
                .thenReturn(Optional.empty());

        serviceEmbedded.delete(bucketCode);

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

        var exception = assertThrows(ServiceException.class, () -> serviceEmbedded.delete(bucketCode));

        assertEquals(403, exception.getStatus());
        assertEquals("BUCKET_NOT_EMPTY", exception.getCode());
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

        var bucket = serviceEmbedded.get(bucketCode);

        assertEquals(bucketCode, bucket.getCode());
        assertFalse(bucket.isReadOnly());

        verify(contentEntityRepository)
                .findAllByBucket(bucketEntity);

        var bucketContents = bucket.getContents();
        assertNotNull(bucketContents);

        assertEquals(1, bucketContents.size());

        var bucketContent = bucketContents.getFirst();

        assertEquals(contentEntity.getUid(), bucketContent.getId());
        assertEquals(contentEntity.getName(), bucketContent.getName());
        assertEquals(metaEntity.getValue(), bucketContent.getMeta().get(metaEntity.getKey()));
    }
}
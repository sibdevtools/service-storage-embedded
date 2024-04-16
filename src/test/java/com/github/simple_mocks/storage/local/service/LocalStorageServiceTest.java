package com.github.simple_mocks.storage.local.service;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.StorageErrors;
import com.github.simple_mocks.storage.local.entity.BucketEntity;
import com.github.simple_mocks.storage.local.entity.ContentEntity;
import com.github.simple_mocks.storage.local.entity.ContentMetaEntity;
import com.github.simple_mocks.storage.local.io.ContentReader;
import com.github.simple_mocks.storage.local.io.ContentWriter;
import com.github.simple_mocks.storage.local.repository.BucketEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentMetaEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author sibmaks
 * @since 0.0.4
 */
@ExtendWith(MockitoExtension.class)
class LocalStorageServiceTest {
    @Mock
    private ContentReader contentReader;
    @Mock
    private ContentWriter contentWriter;
    @Mock
    private BucketEntityRepository bucketEntityRepository;
    @Mock
    private ContentEntityRepository contentEntityRepository;
    @Mock
    private ContentMetaEntityRepository contentMetaEntityRepository;
    @InjectMocks
    private LocalStorageService service;

    @Test
    void testSetUpWhenFolderExistsAsFile() {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples/mock.data")).getPath();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        var exception = assertThrows(IllegalArgumentException.class, () -> service.setUp());
        assertEquals("Path: %s exists and is not directory".formatted(folder), exception.getMessage());
    }

    @Test
    void testSetUpWhenFolderExists() {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples")).getPath();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        try {
            service.setUp();
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testGetContentWhenContentNotExists() {
        var id = UUID.randomUUID().toString();
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class, () -> service.get(id));
        assertEquals(404, exception.getStatus());
        assertEquals("Content not found", exception.getMessage());
        assertEquals(StorageErrors.NOT_FOUND, exception.getServiceError());
    }

    @Test
    void testGetContentWhenFileNotExists() {
        var folder = UUID.randomUUID().toString();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        var id = UUID.randomUUID().toString();
        var content = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(content));

        var exception = assertThrows(ServiceException.class, () -> service.get(id));
        assertEquals(404, exception.getStatus());
        assertEquals("File not found", exception.getMessage());
        assertEquals(StorageErrors.NOT_FOUND, exception.getServiceError());

        var cause = exception.getCause();
        assertNotNull(cause);

        assertInstanceOf(NoSuchFileException.class, cause);
    }

    @Test
    void testGetContentWhenCannotReadContent() throws IOException {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples")).getPath();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        var id = "mock";
        var content = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(content));

        var cause = new IOException(UUID.randomUUID().toString());
        when(contentReader.read(any()))
                .thenThrow(cause);

        var exception = assertThrows(ServiceException.class, () -> service.get(id));
        assertEquals(503, exception.getStatus());
        assertEquals("Unexpected error", exception.getMessage());
        assertEquals(StorageErrors.UNEXPECTED_ERROR, exception.getServiceError());

        var actualCause = exception.getCause();
        assertEquals(cause, actualCause);
    }

    @Test
    void testGetContent() throws IOException {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples")).getPath();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        var id = "mock";
        var contentEntity = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(contentEntity));

        var contentEntityUid = UUID.randomUUID().toString();
        when(contentEntity.getUid())
                .thenReturn(contentEntityUid);

        var contentEntityName = UUID.randomUUID().toString();
        when(contentEntity.getName())
                .thenReturn(contentEntityName);

        var contentEntityCreatedAt = ZonedDateTime.now().minusMinutes(5);
        when(contentEntity.getCreatedAt())
                .thenReturn(contentEntityCreatedAt);

        var contentEntityModifiedAt = ZonedDateTime.now().minusMinutes(1);
        when(contentEntity.getModifiedAt())
                .thenReturn(contentEntityModifiedAt);

        var content = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        when(contentReader.read(any()))
                .thenReturn(content);

        var contentMetaEntity = mock(ContentMetaEntity.class);
        when(contentMetaEntityRepository.findAllByContentUid(id))
                .thenReturn(List.of(contentMetaEntity));

        var contentMetaEntityKey = UUID.randomUUID().toString();
        when(contentMetaEntity.getKey())
                .thenReturn(contentMetaEntityKey);

        var contentMetaEntityValue = UUID.randomUUID().toString();
        when(contentMetaEntity.getValue())
                .thenReturn(contentMetaEntityValue);

        var actualContent = service.get(id);
        assertNotNull(actualContent);

        assertEquals(contentEntityUid, actualContent.getId());
        assertEquals(contentEntityName, actualContent.getName());
        assertArrayEquals(content, actualContent.getContent());
        assertEquals(contentEntityCreatedAt, actualContent.getCreatedAt());
        assertEquals(contentEntityModifiedAt, actualContent.getModifiedAt());

        var meta = actualContent.getMeta();
        assertNotNull(meta);

        assertEquals(1, meta.size());

        assertEquals(contentMetaEntityValue, meta.get(contentMetaEntityKey));
    }

    @Test
    void testDeleteWhenAlreadyRemoved() {
        var id = UUID.randomUUID().toString();
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.empty());

        try {
            service.delete(id);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testDeleteWhenBucketIsReadOnly() {
        var id = UUID.randomUUID().toString();
        var contentEntity = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(contentEntity));

        var bucketEntity = mock(BucketEntity.class);
        when(contentEntity.getBucket())
                .thenReturn(bucketEntity);

        when(bucketEntity.isReadonly())
                .thenReturn(true);

        var exception = assertThrows(ServiceException.class, () -> service.delete(id));
        assertEquals(403, exception.getStatus());
        assertEquals("Bucket is readonly", exception.getMessage());
        assertEquals(StorageErrors.BUCKET_READONLY, exception.getServiceError());
    }

    @Test
    void testDeleteWhenPhysicallyRemoved() {
        var folder = UUID.randomUUID().toString();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        var id = UUID.randomUUID().toString();
        var contentEntity = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(contentEntity));

        var bucketEntity = mock(BucketEntity.class);
        when(contentEntity.getBucket())
                .thenReturn(bucketEntity);

        when(bucketEntity.isReadonly())
                .thenReturn(false);

        service.delete(id);

        var once = times(1);

        verify(contentMetaEntityRepository, once)
                .deleteAllByContentUid(id);

        verify(contentEntityRepository, once)
                .delete(contentEntity);
    }

    @Test
    void testDelete() throws IOException {
        var tmpFile = Files.createTempFile(UUID.randomUUID().toString(), ".data");
        try(var writer = new FileOutputStream(tmpFile.toFile())) {
            writer.write(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        }
        var folder = tmpFile.getParent().toAbsolutePath().toString();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        var id = tmpFile.toFile().getName().split("\\.data")[0];
        var contentEntity = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(contentEntity));

        var bucketEntity = mock(BucketEntity.class);
        when(contentEntity.getBucket())
                .thenReturn(bucketEntity);

        when(bucketEntity.isReadonly())
                .thenReturn(false);

        service.delete(id);

        var once = times(1);

        verify(contentMetaEntityRepository, once)
                .deleteAllByContentUid(id);

        verify(contentEntityRepository, once)
                .delete(contentEntity);
    }

    @Test
    void testCreateWhenBucketNotExists() {
        var bucket = UUID.randomUUID().toString();
        when(bucketEntityRepository.findByCode(bucket))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class, () -> service.create(bucket, null, null, null));
        assertEquals(404, exception.getStatus());
        assertEquals("Bucket not exists", exception.getMessage());
        assertEquals(StorageErrors.BUCKET_NOT_EXISTS, exception.getServiceError());
    }

    @Test
    void testCreateWhenBucketIsReadOnly() {
        var bucket = UUID.randomUUID().toString();
        var bucketEntity = mock(BucketEntity.class);
        when(bucketEntityRepository.findByCode(bucket))
                .thenReturn(Optional.of(bucketEntity));

        when(bucketEntity.isReadonly())
                .thenReturn(true);

        var exception = assertThrows(ServiceException.class, () -> service.create(bucket, null, null, null));
        assertEquals(403, exception.getStatus());
        assertEquals("Bucket is readonly", exception.getMessage());
        assertEquals(StorageErrors.BUCKET_READONLY, exception.getServiceError());
    }

    @Test
    void testCreateWhenExceptionDuringWriting() throws IOException {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples")).getPath();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        var bucket = UUID.randomUUID().toString();
        var bucketEntity = mock(BucketEntity.class);
        when(bucketEntityRepository.findByCode(bucket))
                .thenReturn(Optional.of(bucketEntity));

        when(bucketEntity.isReadonly())
                .thenReturn(false);

        var content = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        var cause = new IOException(UUID.randomUUID().toString());
        doThrow(cause)
                .when(contentWriter).write(eq(content), any());

        var name = UUID.randomUUID().toString();
        var meta = mock(Map.class);
        var exception = assertThrows(ServiceException.class, () -> service.create(bucket, name, meta, content));
        assertEquals(503, exception.getStatus());
        assertEquals("Can't create content", exception.getMessage());
        assertEquals(StorageErrors.UNEXPECTED_ERROR, exception.getServiceError());
    }

    @Test
    void testCreate() {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples")).getPath();
        var folderField = Objects.requireNonNull(ReflectionUtils.findField(LocalStorageService.class, "folder"));
        ReflectionUtils.makeAccessible(folderField);
        ReflectionUtils.setField(folderField, service, folder);

        var bucket = UUID.randomUUID().toString();
        var bucketEntity = mock(BucketEntity.class);
        when(bucketEntityRepository.findByCode(bucket))
                .thenReturn(Optional.of(bucketEntity));

        when(bucketEntity.isReadonly())
                .thenReturn(false);

        var content = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

        var metaKey = UUID.randomUUID().toString();
        var metaValue = UUID.randomUUID().toString();

        var name = UUID.randomUUID().toString();
        var meta = Map.of(metaKey, metaValue);

        var contentUid = service.create(bucket, name, meta, content);
        assertNotNull(contentUid);

        var contentEntityArgumentCaptor = ArgumentCaptor.forClass(ContentEntity.class);
        var once = times(1);
        verify(contentEntityRepository, once)
                .save(contentEntityArgumentCaptor.capture());

        var contentEntity = contentEntityArgumentCaptor.getValue();
        assertNotNull(contentEntity);

        assertEquals(contentUid, contentEntity.getUid());
        assertEquals(name, contentEntity.getName());
        assertEquals(bucketEntity, contentEntity.getBucket());
        assertNotNull(contentEntity.getCreatedAt());
        assertNotNull(contentEntity.getModifiedAt());

        var metaEntitiesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(contentMetaEntityRepository, once)
                .saveAll(metaEntitiesArgumentCaptor.capture());

        var metaEntities = metaEntitiesArgumentCaptor.getValue();
        assertNotNull(metaEntities);

        assertEquals(1, metaEntities.size());

        var contentMetaEntity = (ContentMetaEntity) metaEntities.get(0);
        assertEquals(metaKey, contentMetaEntity.getKey());
        assertEquals(metaValue, contentMetaEntity.getValue());
        assertEquals(contentUid, contentMetaEntity.getContentUid());
    }
}
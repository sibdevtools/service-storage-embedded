package com.github.simple_mocks.storage.local.service;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.StorageErrors;
import com.github.simple_mocks.storage.local.WhiteBox;
import com.github.simple_mocks.storage.local.conf.LocalStorageServiceProperties;
import com.github.simple_mocks.storage.local.dto.ContentStorageFormat;
import com.github.simple_mocks.storage.local.entity.BucketEntity;
import com.github.simple_mocks.storage.local.entity.ContentEntity;
import com.github.simple_mocks.storage.local.entity.ContentMetaEntity;
import com.github.simple_mocks.storage.local.repository.BucketEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentMetaEntityRepository;
import com.github.simple_mocks.storage.local.service.codec.StorageCodec;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author sibmaks
 * @since 0.0.4
 */
@ExtendWith(MockitoExtension.class)
class LocalStorageServiceTest {
    @Mock
    private BucketEntityRepository bucketEntityRepository;
    @Mock
    private ContentEntityRepository contentEntityRepository;
    @Mock
    private ContentMetaEntityRepository contentMetaEntityRepository;
    @Mock
    private List<StorageCodec> storageCodecs;
    @Mock
    private LocalStorageServiceProperties properties;
    @InjectMocks
    private LocalStorageService service;

    @Test
    void testSetUpWhenFolderExistsAsFile() {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples/1/mock.data")).getPath();

        when(properties.getFolder())
                .thenReturn(folder);

        var exception = assertThrows(IllegalArgumentException.class, () -> service.setUp());
        assertEquals("Path: '%s' exists and is not directory".formatted(folder), exception.getMessage());
    }

    @Test
    void testSetUpWhenFolderExists() {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples")).getPath();

        when(properties.getFolder())
                .thenReturn(folder);

        try {
            service.setUp();
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testSetUpWhenDirectoryCanNotBeCreated() {
        var folder = StringUtils.repeat('-', 1024);

        when(properties.getFolder())
                .thenReturn(folder);

        var exception = assertThrows(ServiceException.class, () -> service.setUp());
        assertEquals(503, exception.getStatus());
        assertEquals("Can't create dirs: " + folder, exception.getMessage());
        assertEquals(StorageErrors.UNEXPECTED_ERROR, exception.getServiceError());
    }

    @Test
    void testGetContentWhenContentNotExists() {
        var id = UUID.randomUUID().toString();
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class, () -> service.get(id));
        assertEquals(404, exception.getStatus());
        assertEquals("Content not found", exception.getMessage());
        assertEquals(StorageErrors.CONTENT_NOT_FOUND, exception.getServiceError());
    }

    @Test
    void testGetContentWhenFileNotExists() {
        var folder = UUID.randomUUID().toString();

        when(properties.getFolder())
                .thenReturn(folder);

        var storageFormat = mock(ContentStorageFormat.class);
        var storageCodec = mock(StorageCodec.class);
        var storageCodecs = Map.of(
                storageFormat, storageCodec
        );
        WhiteBox.set(service, "storageCodecs", storageCodecs);

        var id = UUID.randomUUID().toString();
        var content = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(content));

        when(content.getStorageFormat())
                .thenReturn(storageFormat);

        var bucketEntity = mock(BucketEntity.class);
        when(content.getBucket())
                .thenReturn(bucketEntity);

        var bucketId = UUID.randomUUID().getLeastSignificantBits();
        when(bucketEntity.getId())
                .thenReturn(bucketId);

        var exception = assertThrows(ServiceException.class, () -> service.get(id));
        assertEquals(404, exception.getStatus());
        assertEquals("File not found", exception.getMessage());
        assertEquals(StorageErrors.CONTENT_NOT_FOUND, exception.getServiceError());

        var cause = exception.getCause();
        assertNotNull(cause);

        assertInstanceOf(NoSuchFileException.class, cause);
    }

    @Test
    void testGetWhenCodecIsUnsupported() {
        WhiteBox.set(service, "storageCodecs", Collections.emptyMap());

        var id = "mock";
        var contentEntity = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(contentEntity));

        var storageFormat = mock(ContentStorageFormat.class);
        when(contentEntity.getStorageFormat())
                .thenReturn(storageFormat);

        var exception = assertThrows(ServiceException.class, () -> service.get(id));
        assertEquals(503, exception.getStatus());
        assertEquals("Unsupported storage format: %s".formatted(storageFormat), exception.getMessage());
        assertEquals(StorageErrors.UNEXPECTED_ERROR, exception.getServiceError());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 32, 1024})
    void testGet(int bufferSize) {
        var folder = Objects.requireNonNull(LocalStorageServiceTest.class.getResource("/samples")).getPath();

        when(properties.getFolder())
                .thenReturn(folder);

        when(properties.getBufferSize())
                .thenReturn(bufferSize);

        var storageFormat = mock(ContentStorageFormat.class);
        var storageCodec = mock(StorageCodec.class);
        var storageCodecs = Map.of(
                storageFormat, storageCodec
        );
        WhiteBox.set(service, "storageCodecs", storageCodecs);

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

        when(contentEntity.getStorageFormat())
                .thenReturn(storageFormat);

        var bucket = mock(BucketEntity.class);
        when(contentEntity.getBucket())
                .thenReturn(bucket);

        var bucketId = 1L;
        when(bucket.getId())
                .thenReturn(bucketId);

        var content = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        when(storageCodec.decode(any()))
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

        when(properties.getFolder())
                .thenReturn(folder);

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
        var tmpDirectory = Files.createTempDirectory(UUID.randomUUID().toString());

        var id = UUID.randomUUID().toString();
        var bucketId = Math.absExact(UUID.randomUUID().hashCode());
        var bucketDir = Files.createDirectory(
                tmpDirectory
                        .resolve(String.valueOf(bucketId))
        );
        var tmpFile = Files.createFile(
                bucketDir
                        .resolve(id + ".data")
        );
        try (var writer = new FileOutputStream(tmpFile.toFile())) {
            writer.write(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        }
        var folder = tmpDirectory.toAbsolutePath().toString();

        when(properties.getFolder())
                .thenReturn(folder);

        var contentEntity = mock(ContentEntity.class);
        when(contentEntityRepository.findById(id))
                .thenReturn(Optional.of(contentEntity));

        var bucketEntity = mock(BucketEntity.class);
        when(contentEntity.getBucket())
                .thenReturn(bucketEntity);

        when(bucketEntity.getId())
                .thenReturn((long) bucketId);

        when(bucketEntity.isReadonly())
                .thenReturn(false);

        service.delete(id);

        var once = times(1);

        verify(contentMetaEntityRepository, once)
                .deleteAllByContentUid(id);

        verify(contentEntityRepository, once)
                .delete(contentEntity);

        assertTrue(Files.notExists(tmpFile));
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
    void testCreateWhenCodecIsUnsupported() {
        var storageFormat = mock(ContentStorageFormat.class);
        WhiteBox.set(service, "storageCodecs", Collections.emptyMap());

        when(properties.getStorageFormat())
                .thenReturn(storageFormat);

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

        var exception = assertThrows(
                ServiceException.class,
                () -> service.create(bucket, name, meta, content)
        );
        assertEquals(503, exception.getStatus());
        assertEquals("Unsupported storage format: %s".formatted(storageFormat), exception.getMessage());
        assertEquals(StorageErrors.UNEXPECTED_ERROR, exception.getServiceError());
    }

    @Test
    void testCreate() throws IOException {
        var folder = Files.createTempDirectory(UUID.randomUUID().toString());

        when(properties.getFolder())
                .thenReturn(folder.toAbsolutePath().toString());

        var storageFormat = mock(ContentStorageFormat.class);
        var storageCodec = mock(StorageCodec.class);
        var storageCodecs = Map.of(
                storageFormat, storageCodec
        );
        WhiteBox.set(service, "storageCodecs", storageCodecs);

        when(properties.getStorageFormat())
                .thenReturn(storageFormat);

        var bucket = UUID.randomUUID().toString();
        var bucketEntity = mock(BucketEntity.class);
        when(bucketEntityRepository.findByCode(bucket))
                .thenReturn(Optional.of(bucketEntity));

        when(bucketEntity.isReadonly())
                .thenReturn(false);

        when(storageCodec.encode(any()))
                .thenAnswer(it -> it.getArgument(0));

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
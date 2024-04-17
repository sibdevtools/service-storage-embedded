package com.github.simple_mocks.storage.local.service;

import com.github.simple_mocks.error_service.exception.ServiceException;
import com.github.simple_mocks.storage.api.Content;
import com.github.simple_mocks.storage.api.StorageErrors;
import com.github.simple_mocks.storage.api.StorageService;
import com.github.simple_mocks.storage.local.conf.LocalStorageServiceEnabled;
import com.github.simple_mocks.storage.local.dto.LocalContent;
import com.github.simple_mocks.storage.local.entity.ContentEntity;
import com.github.simple_mocks.storage.local.entity.ContentMetaEntity;
import com.github.simple_mocks.storage.local.io.ContentReader;
import com.github.simple_mocks.storage.local.io.ContentWriter;
import com.github.simple_mocks.storage.local.repository.BucketEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentEntityRepository;
import com.github.simple_mocks.storage.local.repository.ContentMetaEntityRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author sibmaks
 * @since 0.0.1
 */
@Service
@ConditionalOnBean(LocalStorageServiceEnabled.class)
public class LocalStorageService implements StorageService {
    @Value("${app.local.storage.folder}")
    private String folder;

    private final ContentReader contentReader;
    private final ContentWriter contentWriter;
    private final BucketEntityRepository bucketEntityRepository;
    private final ContentEntityRepository contentEntityRepository;
    private final ContentMetaEntityRepository contentMetaEntityRepository;

    /**
     * Construct local storage service
     *
     * @param contentReader               content reader
     * @param contentWriter               content writer
     * @param bucketEntityRepository      bucket entity repository
     * @param contentEntityRepository     content entity repository
     * @param contentMetaEntityRepository content meta entity repository
     */
    @Autowired
    public LocalStorageService(ContentReader contentReader,
                               ContentWriter contentWriter,
                               BucketEntityRepository bucketEntityRepository,
                               ContentEntityRepository contentEntityRepository,
                               ContentMetaEntityRepository contentMetaEntityRepository) {
        this.contentReader = contentReader;
        this.contentWriter = contentWriter;
        this.bucketEntityRepository = bucketEntityRepository;
        this.contentEntityRepository = contentEntityRepository;
        this.contentMetaEntityRepository = contentMetaEntityRepository;
    }

    /**
     * Set up local storage service
     */
    @PostConstruct
    public void setUp() {
        var file = new File(folder);
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalArgumentException("Path: %s exists and is not directory".formatted(folder));
            }
        } else {
            if (!file.mkdirs()) {
                throw new IllegalArgumentException("Can't create dirs: %s".formatted(folder));
            }
        }
    }

    @Override
    public Content get(String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, StorageErrors.NOT_FOUND, "Content not found"));

        var path = getPath(id);
        byte[] content;
        try (var channel = FileChannel.open(path, StandardOpenOption.READ)) {
            content = contentReader.read(channel);
        } catch (NoSuchFileException e) {
            throw new ServiceException(404, StorageErrors.NOT_FOUND, "File not found", e);
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Unexpected error", e);
        }

        var meta = contentMetaEntityRepository.findAllByContentUid(id)
                .stream()
                .collect(Collectors.toMap(ContentMetaEntity::getKey, ContentMetaEntity::getValue));

        return LocalContent.builder()
                .id(contentEntity.getUid())
                .name(contentEntity.getName())
                .content(content)
                .meta(meta)
                .createdAt(contentEntity.getCreatedAt())
                .modifiedAt(contentEntity.getModifiedAt())
                .build();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void delete(String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElse(null);
        if (contentEntity == null) {
            return;
        }
        var bucket = contentEntity.getBucket();
        if (bucket.isReadonly()) {
            throw new ServiceException(403, StorageErrors.BUCKET_READONLY, "Bucket is readonly");
        }
        var path = getPath(id);
        contentMetaEntityRepository.deleteAllByContentUid(id);
        contentEntityRepository.delete(contentEntity);
        if (Files.notExists(path)) {
            return;
        }
        // Maybe better do it by scheduler or via async tasks
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't delete file");
        }
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public String create(String bucket, String name, Map<String, String> meta, byte[] content) {
        var bucketEntity = bucketEntityRepository.findByCode(bucket)
                .orElseThrow(() -> new ServiceException(404, StorageErrors.BUCKET_NOT_EXISTS, "Bucket not exists"));

        if (bucketEntity.isReadonly()) {
            throw new ServiceException(403, StorageErrors.BUCKET_READONLY, "Bucket is readonly");
        }

        var uid = UUID.randomUUID().toString();
        var entity = ContentEntity.builder()
                .uid(uid)
                .name(name)
                .bucket(bucketEntity)
                .createdAt(ZonedDateTime.now())
                .modifiedAt(ZonedDateTime.now())
                .build();
        contentEntityRepository.save(entity);

        var metaEntities = meta.entrySet().stream()
                .map(it -> ContentMetaEntity.builder()
                        .key(it.getKey())
                        .value(it.getValue())
                        .contentUid(uid)
                        .build()
                )
                .toList();
        contentMetaEntityRepository.saveAll(metaEntities);

        var path = getPath(uid);
        try (var channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            contentWriter.write(content, channel);
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't create content");
        }

        return uid;
    }

    private Path getPath(String id) {
        return Path.of(folder, "%s.data".formatted(id));
    }

}

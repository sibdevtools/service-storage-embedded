package com.github.sibmaks.storage.local.service;

import com.github.sibmaks.error_service.exception.ServiceException;
import com.github.sibmaks.storage.api.Content;
import com.github.sibmaks.storage.api.StorageErrors;
import com.github.sibmaks.storage.api.StorageService;
import com.github.sibmaks.storage.local.conf.LocalStorageServiceEnabled;
import com.github.sibmaks.storage.local.dto.LocalContent;
import com.github.sibmaks.storage.local.entity.ContentEntity;
import com.github.sibmaks.storage.local.entity.ContentMetaEntity;
import com.github.sibmaks.storage.local.io.LocalContentReader;
import com.github.sibmaks.storage.local.io.LocalContentWriter;
import com.github.sibmaks.storage.local.repository.BucketEntityRepository;
import com.github.sibmaks.storage.local.repository.ContentEntityRepository;
import com.github.sibmaks.storage.local.repository.ContentMetaEntityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author sibmaks
 * @since 2023-04-11
 */
@Service
@ConditionalOnBean(LocalStorageServiceEnabled.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalStorageService implements StorageService {
    @Value("${app.local.storage.folder}")
    private String folder;

    private final LocalContentReader localContentReader;
    private final LocalContentWriter localContentWriter;
    private final BucketEntityRepository bucketEntityRepository;
    private final ContentEntityRepository contentEntityRepository;
    private final ContentMetaEntityRepository contentMetaEntityRepository;

    @PostConstruct
    public void setUp() {
        var file = new File(folder);
        if(file.exists()) {
            if(!file.isDirectory()) {
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
        var meta = contentMetaEntityRepository.findAllByContentUid(id)
                .stream()
                .collect(Collectors.toMap(ContentMetaEntity::getKey, ContentMetaEntity::getValue));

        var path = getPath(id);
        byte[] content;
        try (var channel = FileChannel.open(path, StandardOpenOption.READ)) {
            content = localContentReader.read(channel);
        } catch (NoSuchFileException e) {
            throw new ServiceException(404, StorageErrors.NOT_FOUND, "File not found", e);
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Unexpected error", e);
        }

        return LocalContent.builder()
                .id(contentEntity.getUid())
                .name(contentEntity.getName())
                .content(content)
                .meta(meta)
                .createdAt(contentEntity.getCreatedAt())
                .createdAt(contentEntity.getModifiedAt())
                .build();
    }

    @Override
    @Transactional
    public void delete(String id) {
        var contentEntity = contentEntityRepository.findById(id)
                .orElse(null);
        if(contentEntity == null) {
            return;
        }
        var bucket = contentEntity.getBucket();
        if(bucket.isReadonly()) {
            throw new ServiceException(StorageErrors.BUCKET_READONLY, "Bucket is readonly");
        }
        var path = getPath(id);
        var file = path.toFile();
        if (!file.exists()) {
            contentMetaEntityRepository.deleteAllByContentUid(id);
            contentEntityRepository.delete(contentEntity);
            return;
        }
        contentMetaEntityRepository.deleteAllByContentUid(id);
        contentEntityRepository.delete(contentEntity);
        // Maybe better do it by scheduler or via async tasks
        if(!file.delete()) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't delete file");
        }
    }

    @Override
    @Transactional
    public String create(String bucket, String name, Map<String, String> meta, byte[] content) {
        var bucketEntity = bucketEntityRepository.findByCode(bucket)
                .orElseThrow(() -> new ServiceException(404, StorageErrors.BUCKET_NOT_EXISTS, "Bucket not exists"));

        if(bucketEntity.isReadonly()) {
            throw new ServiceException(StorageErrors.BUCKET_READONLY, "Bucket is readonly");
        }
        var metaEntities = meta.entrySet().stream()
                .map(it -> ContentMetaEntity.builder()
                        .key(it.getKey())
                        .value(it.getValue())
                        .build())
                .collect(Collectors.toList());
        contentMetaEntityRepository.saveAll(metaEntities);

        var entity = ContentEntity.builder()
                .uid(UUID.randomUUID().toString())
                .name(name)
                .bucket(bucketEntity)
                .createdAt(ZonedDateTime.now())
                .modifiedAt(ZonedDateTime.now())
                .build();
        entity = contentEntityRepository.save(entity);

        String uid = entity.getUid();
        var path = getPath(uid);
        try (var channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            localContentWriter.write(content, channel);
        } catch (IOException e) {
            throw new ServiceException(StorageErrors.UNEXPECTED_ERROR, "Can't create content");
        }

        return uid;
    }

    private Path getPath(String id) {
        return Path.of(folder, "%s.data".formatted(id));
    }

}

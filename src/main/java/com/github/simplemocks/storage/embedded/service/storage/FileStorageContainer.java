package com.github.simplemocks.storage.embedded.service.storage;

import com.github.simplemocks.storage.embedded.conf.StorageServiceEmbeddedCondition;
import com.github.simplemocks.storage.embedded.conf.StorageServiceEmbeddedProperties;
import com.github.simplemocks.storage.embedded.exception.FileNotFoundException;
import com.github.simplemocks.storage.embedded.exception.UnexpectedErrorException;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author sibmaks
 * @since 0.1.14
 */
@Service
@Conditional(StorageServiceEmbeddedCondition.class)
public class FileStorageContainer implements StorageContainer {
    private final StorageServiceEmbeddedProperties properties;

    public FileStorageContainer(StorageServiceEmbeddedProperties properties) {
        this.properties = properties;
    }

    /**
     * Set up embedded storage service
     */
    @PostConstruct
    public void setUp() {
        var folder = properties.getFolder();
        var path = Path.of(folder);
        createDirectoriesIfNotExists(path);
    }

    @Override
    public byte[] get(long bucketId, String contentId) {
        var path = getPath(bucketId, contentId);
        return readContent(path);
    }

    @Override
    public void save(long id, String uid, byte[] data) {
        var path = getPath(id, uid);
        createDirectoriesIfNotExists(path.getParent());

        try (var channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            var buffer = ByteBuffer.wrap(data);
            if (channel.write(buffer) != data.length) {
                throw new UnexpectedErrorException("Can't write content");
            }
        } catch (IOException e) {
            throw new UnexpectedErrorException("Can't create content", e);
        }
    }

    @Override
    public void delete(long bucketId, String contentId) {
        var path = getPath(bucketId, contentId);
        if (Files.notExists(path)) {
            return;
        }
        // Maybe better do it by scheduler or via async tasks
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new UnexpectedErrorException("Can't delete file");
        }
    }

    @Override
    public String getType() {
        return "FILE";
    }

    private Path getPath(long bucketId, String id) {
        var folder = properties.getFolder();
        return Path.of(folder, String.valueOf(bucketId), "%s.data".formatted(id));
    }

    private byte[] readContent(Path path) {
        try (var channel = FileChannel.open(path, StandardOpenOption.READ);
             var out = new ByteArrayOutputStream()) {

            var readBufferSize = Math.max(1, properties.getBufferSize());
            if (readBufferSize > channel.size()) {
                readBufferSize = (int) channel.size();
            }
            var buff = ByteBuffer.allocate(readBufferSize);

            while (channel.read(buff) > 0) {
                out.write(buff.array(), 0, buff.position());
                buff.clear();
            }

            return out.toByteArray();
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException("File not found", e);
        } catch (IOException e) {
            throw new UnexpectedErrorException("Unexpected error", e);
        }
    }

    private static void createDirectoriesIfNotExists(Path path) {
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                throw new IllegalArgumentException("Path: '%s' exists and is not directory".formatted(path));
            }
        } else {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new UnexpectedErrorException("Can't create dirs: %s".formatted(path));
            }
        }
    }
}

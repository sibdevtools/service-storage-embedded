package com.github.simplemocks.storage.embedded.service.storage;

import com.github.simplemocks.error_service.exception.ServiceException;
import com.github.simplemocks.storage.embedded.conf.StorageServiceEmbeddedProperties;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author sibmaks
 * @since 0.1.14
 */
@ExtendWith(MockitoExtension.class)
class FileStorageContainerTest {
    @Mock
    private StorageServiceEmbeddedProperties properties;
    @InjectMocks
    private FileStorageContainer container;

    @Test
    void testSetUpWhenFolderExistsAsFile() {
        var folder = Objects.requireNonNull(FileStorageContainerTest.class.getResource("/samples/1/mock.data")).getPath();

        when(properties.getFolder())
                .thenReturn(folder);

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> container.setUp()
        );
        assertEquals("Path: '%s' exists and is not directory".formatted(folder), exception.getMessage());
    }

    @Test
    void testSetUpWhenFolderExists() {
        var folder = Objects.requireNonNull(FileStorageContainerTest.class.getResource("/samples")).getPath();

        when(properties.getFolder())
                .thenReturn(folder);

        try {
            container.setUp();
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testSetUpWhenDirectoryCanNotBeCreated() {
        var folder = StringUtils.repeat('-', 1024);

        when(properties.getFolder())
                .thenReturn(folder);

        var exception = assertThrows(
                ServiceException.class,
                () -> container.setUp()
        );

        assertEquals(503, exception.getStatus());
        assertEquals("Can't create dirs: " + folder, exception.getMessage());
        assertEquals("UNEXPECTED_ERROR", exception.getCode());
    }

}
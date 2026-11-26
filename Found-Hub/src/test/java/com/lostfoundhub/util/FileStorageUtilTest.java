package com.lostfoundhub.util;

import com.lostfoundhub.config.FileStorageProperties;
import com.lostfoundhub.exception.BadRequestException;
import com.lostfoundhub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageUtilTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};

    @TempDir
    Path tempDir;

    private FileStorageProperties properties;
    private FileStorageUtil util;

    @BeforeEach
    void setUp() {
        properties = new FileStorageProperties();
        properties.setUploadDir(tempDir.toString());
        util = new FileStorageUtil(properties);
        util.init();
    }

    @Test
    void storesValidPngAndCanDeleteIt() {
        String name = util.store(new MockMultipartFile("image", "photo.PNG", "image/png", PNG));

        assertTrue(name.endsWith(".png"));
        assertTrue(Files.exists(tempDir.resolve(name)));
        assertEquals("image/png", util.contentTypeOf(name));
        assertTrue(util.load(name).exists());

        util.delete(name);
        assertFalse(Files.exists(tempDir.resolve(name)));
    }

    @Test
    void storesValidJpeg() {
        String name = util.store(new MockMultipartFile("image", "photo.jpeg", "image/jpeg", JPG));
        assertTrue(name.endsWith(".jpeg"));
        assertEquals("image/jpeg", util.contentTypeOf(name));
    }

    @Test
    void rejectsWrongExtension() {
        assertThrows(BadRequestException.class,
                () -> util.store(new MockMultipartFile("image", "notes.txt", "image/png", PNG)));
    }

    @Test
    void rejectsWrongContentType() {
        assertThrows(BadRequestException.class,
                () -> util.store(new MockMultipartFile("image", "photo.png", "application/pdf", PNG)));
    }

    @Test
    void rejectsFileWhoseContentIsNotAnImage() {
        byte[] fake = "this is not an image".getBytes();
        assertThrows(BadRequestException.class,
                () -> util.store(new MockMultipartFile("image", "photo.png", "image/png", fake)));
    }

    @Test
    void rejectsTooLargeFile() {
        properties.setMaxSizeBytes(5);
        assertThrows(BadRequestException.class,
                () -> util.store(new MockMultipartFile("image", "photo.png", "image/png", PNG)));
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(BadRequestException.class,
                () -> util.store(new MockMultipartFile("image", "photo.png", "image/png", new byte[0])));
    }

    @Test
    void blocksPathTraversalAndMissingFiles() {
        assertThrows(BadRequestException.class, () -> util.load("../secret.png"));
        assertThrows(ResourceNotFoundException.class, () -> util.load("missing.png"));
    }
}

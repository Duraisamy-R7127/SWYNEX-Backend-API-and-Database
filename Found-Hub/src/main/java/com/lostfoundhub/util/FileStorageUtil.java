package com.lostfoundhub.util;

import com.lostfoundhub.config.FileStorageProperties;
import com.lostfoundhub.exception.BadRequestException;
import com.lostfoundhub.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Saves, loads and deletes uploaded images on disk. Only the generated file name is stored in MySQL.
 * Validation: not empty, size limit, extension (jpg/jpeg/png), content type and real file signature.
 */
@Component
public class FileStorageUtil {

    private static final Logger log = LoggerFactory.getLogger(FileStorageUtil.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");

    private final FileStorageProperties properties;
    private Path uploadPath;

    public FileStorageUtil(FileStorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        this.uploadPath = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory: " + uploadPath, e);
        }
        log.info("Image upload directory: {}", uploadPath);
    }

    /** Validates and saves the image. Returns the stored (relative) file name, e.g. "3f2a...c1.png". */
    public String store(MultipartFile file) {
        validate(file);
        String extension = extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = uploadPath.resolve(storedName).normalize();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save image", e);
        }
        return storedName;
    }

    /** Deletes a stored image; silently ignores missing files. */
    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Path target = resolveSafely(storedName);
            Files.deleteIfExists(target);
        } catch (IOException | BadRequestException e) {
            log.warn("Could not delete image {}: {}", storedName, e.getMessage());
        }
    }

    public Resource load(String storedName) {
        Path file = resolveSafely(storedName);
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new ResourceNotFoundException("Image not found: " + storedName);
        }
        return new PathResource(file);
    }

    public String contentTypeOf(String storedName) {
        return storedName.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg";
    }

    /** Resolves a file name inside the upload folder and blocks path traversal ("../"). */
    private Path resolveSafely(String storedName) {
        Path resolved = uploadPath.resolve(storedName).normalize();
        if (!resolved.startsWith(uploadPath) || !resolved.getParent().equals(uploadPath)) {
            throw new BadRequestException("Invalid image file name");
        }
        return resolved;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is empty");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new BadRequestException("Image is too large. Maximum allowed size is "
                    + (properties.getMaxSizeBytes() / (1024 * 1024)) + " MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw new BadRequestException("Invalid image type. Only JPG, JPEG and PNG files are allowed");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid content type. Only image/jpeg and image/png are allowed");
        }
        if (!hasValidSignature(file, extension)) {
            throw new BadRequestException("File content is not a valid JPG or PNG image");
        }
    }

    private String extensionOf(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            throw new BadRequestException("Invalid image type. Only JPG, JPEG and PNG files are allowed");
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /** Checks the first bytes of the file ("magic number") so a renamed .exe cannot be uploaded as .png. */
    private boolean hasValidSignature(MultipartFile file, String extension) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(8);
            if ("png".equals(extension)) {
                byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
                return header.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(header, 8), png);
            }
            return header.length >= 3
                    && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
        } catch (IOException e) {
            return false;
        }
    }
}

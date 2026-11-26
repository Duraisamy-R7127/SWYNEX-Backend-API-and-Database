package com.lostfoundhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Binds the app.file.* settings from application.properties. */
@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {

    /** Folder where images are saved (relative to where the app is started, or absolute). */
    private String uploadDir = "uploads";

    /** Maximum image size in bytes (default 5 MB). */
    private long maxSizeBytes = 5L * 1024 * 1024;

    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png");

    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public long getMaxSizeBytes() { return maxSizeBytes; }
    public void setMaxSizeBytes(long maxSizeBytes) { this.maxSizeBytes = maxSizeBytes; }
    public List<String> getAllowedExtensions() { return allowedExtensions; }
    public void setAllowedExtensions(List<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }
}

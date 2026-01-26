package com.shophub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ImageUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ImageUploadService.class);

    @Value("${upload.directory:uploads/products/}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    private static final List<String> ALLOWED_MIME_TYPES =
            Arrays.asList("image/jpeg", "image/png", "image/webp");

    public String uploadImage(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new IOException("File is empty");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IOException("File size exceeds 5MB");
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
                throw new IOException("Invalid MIME type");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || containsPathTraversal(originalFilename)) {
                throw new IOException("Invalid filename");
            }

            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IOException("Invalid file extension");
            }

            String filename = UUID.randomUUID() + extension;
            Path uploadPath = Paths.get(uploadDir);

            // ✅ FIX #1 — inside try
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(filename);

            // ✅ FIX #2 — inside try
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/products/" + filename;

        } catch (IOException e) {
            logger.error("Image upload failed", e);
            throw new RuntimeException("Image upload failed");
        }
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) return;

        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            if (containsPathTraversal(filename)) {
                logger.warn("Blocked path traversal delete attempt: {}", filename);
                return;
            }

            Path filePath = Paths.get(uploadDir).resolve(filename);

            // ✅ FIX #3 — inside try
            Files.deleteIfExists(filePath);

        } catch (IOException e) {
            logger.error("Failed to delete image", e);
        }
    }

    private String getFileExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i) : "";
    }

    private boolean containsPathTraversal(String filename) {
        String lower = filename.toLowerCase();
        return filename.contains("..")
                || filename.contains("/")
                || filename.contains("\\")
                || lower.contains("%2e")
                || lower.contains("%2f")
                || lower.contains("%5c");
    }
}

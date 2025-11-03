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

/**
 * Secure Image Upload Service
 *
 * Security Features:
 * - File size limit (5MB max)
 * - Extension whitelist (only .jpg, .jpeg, .png, .webp)
 * - MIME type validation
 * - Path traversal prevention
 * - UUID-based filename generation
 * - Safe file deletion
 *
 * This prevents:
 * - Malicious file uploads (web shells)
 * - Disk space exhaustion (DoS)
 * - Path traversal attacks
 * - Arbitrary code execution
 */
@Service
public class ImageUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ImageUploadService.class);

    @Value("${upload.directory:uploads/products/}")
    private String uploadDir;

    // Security: Maximum file size 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Security: Whitelist only safe image extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    // Security: Whitelist MIME types
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );

    /**
     * Upload image file with comprehensive security checks
     *
     * @param file Uploaded file
     * @return URL path to uploaded image
     * @throws IOException if validation fails or upload error occurs
     */
    public String uploadImage(MultipartFile file) throws IOException {
        // Validation 1: Check file is not empty
        if (file.isEmpty()) {
            logger.warn("Upload attempt with empty file");
            throw new IOException("File is empty");
        }

        // Validation 2: Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            logger.warn("Upload attempt with file size {} exceeding limit {}", file.getSize(), MAX_FILE_SIZE);
            throw new IOException("File size exceeds 5MB limit");
        }

        // Validation 3: Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            logger.warn("Upload attempt with invalid MIME type: {}", contentType);
            throw new IOException("Invalid file type. Only JPG, PNG, and WebP images are allowed");
        }

        // Validation 4: Validate filename and extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            logger.warn("Upload attempt with null or empty filename");
            throw new IOException("Invalid filename");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            logger.warn("Upload attempt with invalid extension: {}", extension);
            throw new IOException("Invalid file extension. Only .jpg, .jpeg, .png, and .webp are allowed");
        }

        // Validation 5: Prevent path traversal attacks
        if (containsPathTraversal(originalFilename)) {
            logger.error("Path traversal attack attempt detected: {}", originalFilename);
            throw new IOException("Invalid filename: security violation detected");
        }

        // Generate secure filename with UUID
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        // Save file securely
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/uploads/products/" + uniqueFilename;
        logger.info("File uploaded successfully: {}", fileUrl);

        return fileUrl;
    }

    /**
     * Securely delete uploaded image
     *
     * @param imageUrl URL of image to delete
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return; // Invalid URL format
        }

        try {
            // Extract filename from URL
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            // Security: Validate filename before deletion
            if (containsPathTraversal(filename)) {
                logger.error("Path traversal attack attempt in delete operation: {}", filename);
                return;
            }

            // Delete file
            Path filePath = Paths.get(uploadDir + filename);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                logger.info("File deleted successfully: {}", filename);
            }
        } catch (IOException e) {
            logger.error("Error deleting file: {}", imageUrl, e);
            // Don't throw exception - file deletion failure shouldn't break application
        }
    }

    /**
     * Extract file extension from filename
     *
     * @param filename Filename
     * @return Extension with dot (e.g., ".jpg") or empty string
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return ""; // No extension or dot at end
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * Check if filename contains path traversal sequences
     *
     * Security: Prevents attacks using:
     * - "../" (relative path traversal)
     * - Absolute paths ("/", "\")
     * - URL encoding ("%2e%2e", "%2f")
     * - Mixed encoding
     *
     * @param filename Filename to check
     * @return true if path traversal detected
     */
    private boolean containsPathTraversal(String filename) {
        if (filename == null) {
            return true;
        }

        // Check for common path traversal patterns
        String lowerFilename = filename.toLowerCase();
        return filename.contains("..") ||
                filename.contains("/") ||
                filename.contains("\\") ||
                lowerFilename.contains("%2e") || // URL encoded dot
                lowerFilename.contains("%2f") || // URL encoded forward slash
                lowerFilename.contains("%5c");   // URL encoded backslash
    }
}

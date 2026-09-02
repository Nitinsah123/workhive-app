package com.workhive.module.document.service;

import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final String minioEndpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    private MinioClient minioClient;
    private boolean minioAvailable = false;

    private final Path localStorageDir = Paths.get("data/storage");

    public StorageService(
            @Value("${minio.endpoint:http://localhost:9000}") String minioEndpoint,
            @Value("${minio.access-key:workhive_minio}") String accessKey,
            @Value("${minio.secret-key:workhive_minio_secret}") String secretKey,
            @Value("${minio.bucket:workhive-documents}") String bucketName) {
        this.minioEndpoint = minioEndpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;

        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            minioAvailable = true;
            log.info("MinIO storage initialized with bucket: {}", bucketName);
        } catch (Exception e) {
            log.warn("MinIO not available, using local filesystem fallback: {}", e.getMessage());
            try {
                Files.createDirectories(localStorageDir);
            } catch (Exception ex) {
                log.error("Failed to create local storage directory", ex);
            }
        }
    }

    public String storeFile(UUID tenantId, MultipartFile file) throws Exception {
        String cleanOriginalName = sanitizeFileName(file.getOriginalFilename());
        String objectKey = "tenant_" + tenantId + "/" + UUID.randomUUID() + "_" + cleanOriginalName;

        if (minioAvailable && minioClient != null) {
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
                return objectKey;
            } catch (Exception e) {
                log.warn("MinIO upload failed, falling back to local: {}", e.getMessage());
            }
        }

        // Local storage fallback
        Path targetDir = localStorageDir.resolve("tenant_" + tenantId);
        Files.createDirectories(targetDir);
        Path targetPath = localStorageDir.resolve(objectKey);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return objectKey;
    }

    public InputStream getFileStream(String objectKey) throws Exception {
        if (minioAvailable && minioClient != null) {
            try {
                return minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .build());
            } catch (Exception e) {
                log.warn("MinIO get failed, trying local: {}", e.getMessage());
            }
        }

        Path localPath = localStorageDir.resolve(objectKey).normalize();
        // Path traversal protection
        if (!localPath.startsWith(localStorageDir.normalize())) {
            throw new SecurityException("Invalid file path");
        }
        return Files.newInputStream(localPath);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "file";
        return new File(fileName).getName().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

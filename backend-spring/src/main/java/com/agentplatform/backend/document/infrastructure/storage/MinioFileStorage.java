package com.agentplatform.backend.document.infrastructure.storage;

import com.agentplatform.backend.document.application.FileStorage;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 对象存储实现。
 */
@Component
@ConditionalOnProperty(
        name = "app.storage.mode",
        havingValue = "minio"
)
public class MinioFileStorage implements FileStorage {

    private final MinioClient minioClient;
    private final String bucket;
    private final long maxFileSizeBytes;

    public MinioFileStorage(
            @Value("${app.storage.minio.endpoint}") String endpoint,
            @Value("${app.storage.minio.access-key}") String accessKey,
            @Value("${app.storage.minio.secret-key}") String secretKey,
            @Value("${app.storage.minio.bucket}") String bucket,
            @Value("${app.storage.max-file-size-bytes:52428800}")
            long maxFileSizeBytes
    ) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public StoredFile store(
            String tenantId,
            String knowledgeBaseId,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "上传文件不能为空"
            );
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "文件大小超过系统限制"
            );
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String objectKey = tenantId + "/" + knowledgeBaseId + "/"
                + java.util.UUID.randomUUID() + "-" + fileName;

        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build())) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build());
            }

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType() == null
                            ? "application/octet-stream"
                            : file.getContentType())
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO 文件保存失败", exception);
        }

        return new StoredFile(objectKey, file.getSize(), file.getContentType());
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO 文件清理失败", exception);
        }
    }

    private String sanitizeFileName(String fileName) {
        String normalized = fileName == null ? "uploaded-file" : fileName;
        normalized = normalized.replace("\\", "/");
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        normalized = normalized.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        return normalized.isBlank() ? "uploaded-file" : normalized;
    }
}

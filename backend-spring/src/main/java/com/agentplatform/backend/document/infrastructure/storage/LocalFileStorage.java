package com.agentplatform.backend.document.infrastructure.storage;

import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.document.application.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 本地文件存储实现。
 *
 * <p>它适合开发环境和单机部署。生产多实例部署时将自动切换到
 * MinIO 实现，避免不同实例之间的本地磁盘不一致。</p>
 */
@Component
@ConditionalOnProperty(
        name = "app.storage.mode",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalFileStorage implements FileStorage {

    private final Path root;
    private final long maxFileSizeBytes;

    public LocalFileStorage(
            @Value("${app.storage.local-root:./data/files}") String root,
            @Value("${app.storage.max-file-size-bytes:52428800}")
            long maxFileSizeBytes
    ) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
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

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String objectKey = tenantId + "/" + knowledgeBaseId + "/"
                + UUID.randomUUID() + "-" + originalFileName;
        Path target = root.resolve(objectKey).normalize();

        if (!target.startsWith(root)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "非法文件名"
            );
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败", exception);
        }

        return new StoredFile(
                objectKey,
                file.getSize(),
                file.getContentType()
        );
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "非法文件路径"
            );
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("文件清理失败", exception);
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

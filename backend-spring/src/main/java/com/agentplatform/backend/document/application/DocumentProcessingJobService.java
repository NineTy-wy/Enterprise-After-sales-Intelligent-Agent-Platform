package com.agentplatform.backend.document.application;

import com.agentplatform.backend.agent.application.AgentIngestRequest;
import com.agentplatform.backend.agent.application.AgentServiceClient;
import com.agentplatform.backend.document.domain.Document;
import com.agentplatform.backend.document.domain.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 文档异步处理作业。
 *
 * <p>作业负责协调状态机和 Agent 入库服务：
 * UPLOADED -> PROCESSING -> INDEXED/FAILED。</p>
 */
@Service
public class DocumentProcessingJobService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocumentProcessingJobService.class);

    private final DocumentRepository documentRepository;
    private final AgentServiceClient agentServiceClient;

    public DocumentProcessingJobService(
            DocumentRepository documentRepository,
            AgentServiceClient agentServiceClient
    ) {
        this.documentRepository = documentRepository;
        this.agentServiceClient = agentServiceClient;
    }

    @Async("documentProcessingExecutor")
    public void processAsync(DocumentProcessingMessage message) {
        process(message);
    }

    public void process(DocumentProcessingMessage message) {
        Document document = documentRepository.findById(message.documentId())
                .filter(item -> message.tenantId().equals(item.getTenantId()))
                .orElse(null);
        if (document == null) {
            LOGGER.warn("Document disappeared before processing: {}", message.documentId());
            return;
        }
        if (!message.knowledgeBaseId().equals(document.getKnowledgeBaseId())) {
            LOGGER.warn(
                    "Document task knowledge base mismatch: document={}, taskKb={}, actualKb={}",
                    message.documentId(),
                    message.knowledgeBaseId(),
                    document.getKnowledgeBaseId()
            );
            return;
        }

        if (document.getStatus() == com.agentplatform.backend.document.domain.DocumentStatus.INDEXED) {
            LOGGER.info("Document is already indexed, skip duplicate message: {}",
                    message.documentId());
            return;
        }

        try {
            if (document.getStatus()
                    == com.agentplatform.backend.document.domain.DocumentStatus.UPLOADED
                    || document.getStatus()
                    == com.agentplatform.backend.document.domain.DocumentStatus.FAILED) {
                document.markProcessing();
                documentRepository.save(document);
            }

            var result = agentServiceClient.ingest(new AgentIngestRequest(
                    message.documentId(),
                    message.tenantId(),
                    message.knowledgeBaseId(),
                    message.fileName(),
                    message.fileType(),
                    message.storagePath()
            ));
            if (result == null || !"INDEXED".equalsIgnoreCase(result.status())) {
                throw new IllegalStateException("Agent 未完成文档索引");
            }

            document.markIndexed();
            documentRepository.save(document);
        } catch (Exception exception) {
            LOGGER.error("Document processing failed: {}", message.documentId(), exception);
            try {
                if (document.getStatus()
                        != com.agentplatform.backend.document.domain.DocumentStatus.INDEXED) {
                    String reason = exception.getMessage() == null
                            ? "Agent 文档处理失败，请查看服务日志"
                            : exception.getMessage();
                    document.markFailed(reason.substring(0, Math.min(reason.length(), 1000)));
                    documentRepository.save(document);
                }
            } catch (Exception stateException) {
                LOGGER.error("Failed to persist document failure state: {}",
                        message.documentId(), stateException);
            }
        }
    }
}

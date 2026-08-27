package com.agentplatform.backend.tool.api;

import com.agentplatform.backend.audit.application.AuditService;
import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.common.security.CurrentUserProvider;
import com.agentplatform.backend.tool.application.ToolCallRequest;
import com.agentplatform.backend.tool.application.ToolCallResponse;
import com.agentplatform.backend.tool.application.ToolDefinition;
import com.agentplatform.backend.tool.application.ToolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工具目录和工具调用接口。
 */
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolService toolService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public ToolController(
            ToolService toolService,
            CurrentUserProvider currentUserProvider,
            AuditService auditService
    ) {
        this.toolService = toolService;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<ToolDefinition>> listTools() {
        return ApiResponse.success(toolService.listTools(
                currentUserProvider.currentUser()
        ));
    }

    @PostMapping("/{toolName}/invoke")
    public ApiResponse<ToolCallResponse> invoke(
            @PathVariable String toolName,
            @RequestBody(required = false) ToolCallRequest request
    ) {
        CurrentUser user = currentUserProvider.currentUser();
        ToolCallResponse response = toolService.invoke(user, toolName, request);
        auditService.record(
                user.tenantId(),
                user.userId(),
                "INVOKE",
                "TOOL",
                toolName,
                "{\"traceId\":\"" + response.traceId() + "\"}"
        );
        return ApiResponse.success(response);
    }
}

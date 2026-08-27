package com.agentplatform.backend.tool.application.impl;

import com.agentplatform.backend.agent.application.AgentChatRequest;
import com.agentplatform.backend.agent.application.AgentServiceClient;
import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import com.agentplatform.backend.ticket.api.dto.CreateTicketRequest;
import com.agentplatform.backend.ticket.api.dto.TicketResponse;
import com.agentplatform.backend.ticket.application.TicketService;
import com.agentplatform.backend.ticket.domain.TicketPriority;
import com.agentplatform.backend.tool.application.ToolCallRequest;
import com.agentplatform.backend.tool.application.ToolCallResponse;
import com.agentplatform.backend.tool.application.ToolDefinition;
import com.agentplatform.backend.tool.application.ToolService;
import com.agentplatform.backend.user.domain.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 工具注册和调用服务实现。
 */
@Service
public class ToolServiceImpl implements ToolService {

    private final TicketService ticketService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final AgentServiceClient agentServiceClient;

    public ToolServiceImpl(
            TicketService ticketService,
            KnowledgeBaseService knowledgeBaseService,
            AgentServiceClient agentServiceClient
    ) {
        this.ticketService = ticketService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.agentServiceClient = agentServiceClient;
    }

    @Override
    public List<ToolDefinition> listTools(CurrentUser user) {
        return definitions().stream()
                .filter(definition -> canUse(user, definition.requiredRole()))
                .toList();
    }

    @Override
    public ToolCallResponse invoke(
            CurrentUser user,
            String toolName,
            ToolCallRequest request
    ) {
        ToolDefinition definition = definitions().stream()
                .filter(item -> item.name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "工具不存在"
                ));
        if (!canUse(user, definition.requiredRole())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "当前角色无权调用该工具"
            );
        }

        Map<String, Object> arguments = request == null || request.arguments() == null
                ? Map.of()
                : request.arguments();
        Object result = switch (toolName) {
            case "create_ticket" -> createTicket(user, arguments);
            case "get_ticket" -> ticketService.getTicket(
                    user.tenantId(),
                    requiredString(arguments, "ticketId")
            );
            case "list_knowledge_bases" -> knowledgeBaseService
                    .listKnowledgeBases(user.tenantId());
            case "search_knowledge_base" -> agentServiceClient.chat(
                    new AgentChatRequest(
                            user.tenantId(),
                            user.userId(),
                            null,
                            requiredString(arguments, "query"),
                            stringList(arguments.get("knowledgeBaseIds")),
                            "react",
                            null
                    )
            );
            default -> throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "工具未实现"
            );
        };
        return new ToolCallResponse(toolName, result, UUID.randomUUID().toString());
    }

    private TicketResponse createTicket(
            CurrentUser user,
            Map<String, Object> arguments
    ) {
        String priority = String.valueOf(
                arguments.getOrDefault("priority", TicketPriority.MEDIUM.name())
        );
        return ticketService.createTicket(
                user.tenantId(),
                user.userId(),
                new CreateTicketRequest(
                        requiredString(arguments, "customerName"),
                        optionalString(arguments, "productModel"),
                        requiredString(arguments, "issueDescription"),
                        TicketPriority.valueOf(priority.toUpperCase())
                )
        );
    }

    private List<ToolDefinition> definitions() {
        return List.of(
                new ToolDefinition(
                        "create_ticket",
                        "创建售后工单",
                        UserRole.OPERATOR,
                        Map.of(
                                "type", "object",
                                "required", List.of("customerName", "issueDescription")
                        )
                ),
                new ToolDefinition(
                        "get_ticket",
                        "查询指定工单",
                        UserRole.VIEWER,
                        Map.of(
                                "type", "object",
                                "required", List.of("ticketId")
                        )
                ),
                new ToolDefinition(
                        "list_knowledge_bases",
                        "查询当前租户知识库",
                        UserRole.VIEWER,
                        Map.of("type", "object")
                ),
                new ToolDefinition(
                        "search_knowledge_base",
                        "通过 Agent 检索知识库并返回引用",
                        UserRole.AGENT,
                        Map.of(
                                "type", "object",
                                "required", List.of("query")
                        )
                )
        );
    }

    private boolean canUse(CurrentUser user, UserRole requiredRole) {
        Set<UserRole> roles = user.roles();
        if (roles.contains(UserRole.ADMIN)) {
            return true;
        }
        if (requiredRole == UserRole.VIEWER) {
            return !roles.isEmpty();
        }
        return roles.contains(requiredRole);
    }

    private String requiredString(Map<String, Object> arguments, String key) {
        String value = optionalString(arguments, key);
        if (value == null || value.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "工具参数缺失: " + key
            );
        }
        return value;
    }

    private String optionalString(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }
}

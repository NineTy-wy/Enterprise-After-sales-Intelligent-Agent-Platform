package com.agentplatform.backend.system.controller;

import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统测试接口。
 *
 * <p>该 Controller 用于验证统一响应、业务异常、参数校验等基础能力。
 * 后续真实业务模块完成后，可以删除或改造成内部健康诊断接口。</p>
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    /**
     * 验证统一成功响应格式。
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("pong");
    }

    /**
     * 验证业务异常是否会被 GlobalExceptionHandler 捕获。
     */
    @GetMapping("/business-error")
    public ApiResponse<Void> businessError() {
        throw new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "测试资源不存在"
        );
    }

    /**
     * 验证 @Valid 参数校验失败是否会被 GlobalExceptionHandler 捕获。
     */
    @PostMapping("/validate")
    public ApiResponse<String> validate(@Valid @RequestBody ValidateRequest request) {
        return ApiResponse.success("hello " + request.name());
    }

    /**
     * 测试请求对象。
     *
     * <p>这里使用 record 是因为它非常适合表达简单、不可变的请求 DTO。
     * 后续复杂业务对象也可以使用普通 Class。</p>
     */
    public record ValidateRequest(

            /** 姓名不能为空，用于演示参数校验。 */
            @NotBlank(message = "姓名不能为空")
            String name
    ) {
    }
}
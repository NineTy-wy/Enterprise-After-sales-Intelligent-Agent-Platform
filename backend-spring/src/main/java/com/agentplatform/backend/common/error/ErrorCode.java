package com.agentplatform.backend.common.error;

/**
 * 系统统一业务错误码。
 *
 * <p>接口不能在各处散落 400、500 或自定义字符串，
 * 而应使用统一、可检索、可统计的业务错误码。</p>
 */
public enum ErrorCode {

    /** 请求参数不合法，例如必填字段缺失、格式错误。 */
    INVALID_REQUEST(40000, "请求参数错误"),

    /** 当前请求未携带有效登录凭证。 */
    UNAUTHORIZED(40100, "未登录或登录已失效"),

    /** 已登录，但没有访问当前资源或调用当前工具的权限。 */
    FORBIDDEN(40300, "无权限访问"),

    /** 请求的业务资源不存在，例如知识库、文档或工单不存在。 */
    RESOURCE_NOT_FOUND(40400, "资源不存在"),

    /** 当前资源状态不允许执行该操作，例如已归档的知识库不能重复归档。 */
    RESOURCE_STATE_CONFLICT(40900, "资源状态冲突"),

    /** 系统未预期的内部异常。 */
    INTERNAL_ERROR(50000, "系统内部错误");

    /** 供前端、FastAPI Agent 服务和日志系统识别的稳定业务编码。 */
    private final int code;

    /** 默认错误说明；必要时可在异常中使用更具体的描述覆盖。 */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
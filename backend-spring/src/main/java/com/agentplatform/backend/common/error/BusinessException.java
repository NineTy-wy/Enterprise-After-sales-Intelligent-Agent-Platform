package com.agentplatform.backend.common.error;

/**
 * 业务异常基类。
 *
 * <p>业务代码中遇到可预期错误时抛出该异常，例如：
 * 知识库不存在、用户无权限、文档状态不允许操作等。</p>
 *
 * <p>它和系统异常的区别是：
 * BusinessException 是我们主动抛出的、可以给前端明确提示的异常；
 * NullPointerException、SQLException 这类则属于系统异常。</p>
 */
public class BusinessException extends RuntimeException {

    /** 统一错误码，用于前端展示、日志检索和监控统计。 */
    private final ErrorCode errorCode;

    /**
     * 使用错误码中的默认提示信息创建业务异常。
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义提示信息创建业务异常。
     *
     * <p>例如 ErrorCode.RESOURCE_NOT_FOUND 的默认提示是“资源不存在”，
     * 但具体业务里可以改成“知识库不存在”或“文档不存在”。</p>
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
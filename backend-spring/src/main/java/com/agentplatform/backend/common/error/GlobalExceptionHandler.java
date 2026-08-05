package com.agentplatform.backend.common.error;

import com.agentplatform.backend.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>Controller 或 Service 中抛出的异常会在这里被统一捕获，
 * 再转换为前端能够稳定解析的 ApiResponse 格式。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 只记录系统异常，便于后续接入日志平台和告警系统。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务代码主动抛出的异常。
     *
     * <p>例如知识库不存在、当前用户无权访问某个文档等。</p>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception) {

        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(resolveHttpStatus(errorCode))
                .body(ApiResponse.failure(
                        errorCode.getCode(),
                        exception.getMessage()
                ));
    }

    /**
     * 处理 @Valid 参数校验失败的异常。
     *
     * <p>后续接口请求对象加上 @NotBlank、@Size 等校验规则后，
     * 参数不合法会自动进入这里。</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(
                        ErrorCode.INVALID_REQUEST.getCode(),
                        message
                ));
    }

    /**
     * 处理所有未被单独处理的系统异常。
     *
     * <p>不把异常细节直接返回给调用方，避免泄露数据库、路径、
     * 配置等内部信息；完整堆栈保留在服务端日志中。</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception) {

        LOGGER.error("Unhandled system exception", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getMessage()
                ));
    }

    /**
     * 将业务错误码映射为合适的 HTTP 状态码。
     */
    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RESOURCE_STATE_CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * 将字段校验错误格式化为便于前端展示的信息。
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
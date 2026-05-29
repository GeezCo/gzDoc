package com.gzdoc.common.exception;

import com.gzdoc.common.enums.AuthErrorCode;
import lombok.Getter;

/**
 * 认证异常基类
 * 所有认证相关的异常都继承此类
 */
@Getter
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP 状态码
     */
    private final int httpStatus;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 使用错误码枚举构造异常
     *
     * @param errorCode 错误码枚举
     */
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = errorCode.getHttpStatus();
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 使用错误码枚举和原因构造异常
     *
     * @param errorCode 错误码枚举
     * @param cause     原因
     */
    public AuthException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.httpStatus = errorCode.getHttpStatus();
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 自定义构造异常（用于特殊场景）
     *
     * @param httpStatus HTTP 状态码
     * @param code       错误码
     * @param message    错误消息
     */
    public AuthException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
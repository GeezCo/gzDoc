package com.gzdoc.common.exception;

import com.gzdoc.common.enums.AuthErrorCode;

/**
 * 未授权异常（401）
 * 用于表示用户未登录或 token 无效的场景
 */
public class UnauthorizedException extends AuthException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用默认错误码构造异常
     */
    public UnauthorizedException() {
        super(AuthErrorCode.UNAUTHORIZED);
    }

    /**
     * 使用指定错误码构造异常
     *
     * @param errorCode 错误码枚举
     */
    public UnauthorizedException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用错误码和原因构造异常
     *
     * @param errorCode 错误码枚举
     * @param cause     原因
     */
    public UnauthorizedException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 使用自定义消息构造异常
     *
     * @param message 错误消息
     */
    public UnauthorizedException(String message) {
        super(401, "UNAUTHORIZED", message);
    }
}
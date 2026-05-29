package com.gzdoc.common.exception;

import com.gzdoc.common.enums.AuthErrorCode;

/**
 * 禁止访问异常（403）
 * 用于表示用户已登录但权限不足的场景
 */
public class ForbiddenException extends AuthException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用默认错误码构造异常
     */
    public ForbiddenException() {
        super(AuthErrorCode.FORBIDDEN);
    }

    /**
     * 使用指定错误码构造异常
     *
     * @param errorCode 错误码枚举
     */
    public ForbiddenException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用错误码和原因构造异常
     *
     * @param errorCode 错误码枚举
     * @param cause     原因
     */
    public ForbiddenException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 使用自定义消息构造异常
     *
     * @param message 错误消息
     */
    public ForbiddenException(String message) {
        super(403, "FORBIDDEN", message);
    }
}

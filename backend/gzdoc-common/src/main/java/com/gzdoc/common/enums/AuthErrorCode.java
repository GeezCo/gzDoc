package com.gzdoc.common.enums;

import lombok.Getter;

/**
 * 认证错误码枚举
 * 统一管理所有认证相关的错误码、HTTP 状态码和错误消息
 * 遵循单点修改原则，方便后期国际化扩展
 */
@Getter
public enum AuthErrorCode {

    /**
     * 未登录（无 token 或 token 无效）
     */
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录或登录已过期，请重新登录"),

    /**
     * Token 已过期
     */
    TOKEN_EXPIRED(401, "TOKEN_EXPIRED", "登录已过期，请重新登录"),

    /**
     * Token 无效
     */
    TOKEN_INVALID(401, "TOKEN_INVALID", "无效的访问令牌"),

    /**
     * 权限不足（角色不匹配）
     */
    FORBIDDEN(403, "FORBIDDEN", "权限不足，无法访问该资源"),

    /**
     * 角色不匹配
     */
    ROLE_MISMATCH(403, "ROLE_MISMATCH", "当前角色无权访问该资源");

    /**
     * HTTP 状态码
     */
    private final int httpStatus;

    /**
     * 错误码（用于客户端识别具体错误类型）
     */
    private final String code;

    /**
     * 错误消息（用于显示给用户）
     */
    private final String message;

    AuthErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    /**
     * 获取国际化消息（预留扩展点）
     *
     * @return 错误消息
     */
    public String getI18nMessage() {
        // TODO: 后期可以集成 Spring MessageSource 实现国际化
        return this.message;
    }
}
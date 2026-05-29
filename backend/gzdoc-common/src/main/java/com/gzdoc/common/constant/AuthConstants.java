package com.gzdoc.common.constant;

/**
 * 认证常量
 * 统一管理认证相关的常量，遵循单点修改原则
 */
public final class AuthConstants {

    private AuthConstants() {
        // 工具类，禁止实例化
    }

    /**
     * Authorization 请求头名称
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Token 前缀长度
     */
    public static final int TOKEN_PREFIX_LENGTH = 7;

    /**
     * 请求属性：用户 ID
     */
    public static final String REQUEST_ATTR_USER_ID = "userId";

    /**
     * 请求属性：用户角色
     */
    public static final String REQUEST_ATTR_USER_ROLE = "userRole";

    /**
     * 请求属性：租户 ID
     */
    public static final String REQUEST_ATTR_TENANT_ID = "tenantId";

    /**
     * JWT Claims：角色
     */
    public static final String JWT_CLAIM_ROLE = "role";

    /**
     * JWT Claims：租户 ID
     */
    public static final String JWT_CLAIM_TENANT_ID = "tenantId";

    /**
     * JWT Claims：用户名
     */
    public static final String JWT_CLAIM_USERNAME = "username";
}
package com.gzdoc.common.context;

/**
 * 租户上下文 - 使用 ThreadLocal 存储当前请求的租户 ID
 */
public class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    /**
     * 设置当前租户 ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * 获取当前租户 ID
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 清除当前租户 ID
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}

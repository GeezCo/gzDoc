package com.gzdoc.common.enums;

import lombok.Getter;

/**
 * 角色枚举
 * 统一管理系统中的所有角色，遵循单点修改原则
 */
@Getter
public enum RoleEnum {

    /**
     * 管理员角色
     */
    ADMIN("admin", "管理员"),

    /**
     * 普通用户角色
     */
    USER("user", "普通用户");

    /**
     * 角色代码（存储在数据库中的值）
     */
    private final String code;

    /**
     * 角色名称（用于显示）
     */
    private final String name;

    RoleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据代码获取角色枚举
     *
     * @param code 角色代码
     * @return 角色枚举，如果不存在返回 null
     */
    public static RoleEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 检查角色代码是否有效
     *
     * @param code 角色代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }

    /**
     * 检查是否为管理员角色
     *
     * @param code 角色代码
     * @return 是否为管理员
     */
    public static boolean isAdmin(String code) {
        return ADMIN.code.equals(code);
    }
}
package com.gzdoc.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解
 * 用于标记需要特定角色才能访问的方法
 *
 * 使用示例：
 * <pre>
 * {@code
 * @RequireRole({"admin"})
 * public Result<List<User>> listUsers() {
 *     // 仅管理员可访问
 * }
 *
 * @RequireRole({"admin", "user"})
 * public Result<String> getProfile() {
 *     // 管理员或普通用户都可访问
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色列表
     * 用户只需满足其中任意一个角色即可访问
     *
     * @return 角色代码数组
     */
    String[] value();
}

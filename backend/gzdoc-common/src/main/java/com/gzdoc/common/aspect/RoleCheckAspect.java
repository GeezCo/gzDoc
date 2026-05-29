package com.gzdoc.common.aspect;

import com.gzdoc.common.annotation.RequireRole;
import com.gzdoc.common.constant.AuthConstants;
import com.gzdoc.common.enums.AuthErrorCode;
import com.gzdoc.common.enums.RoleEnum;
import com.gzdoc.common.exception.ForbiddenException;
import com.gzdoc.common.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 角色权限检查切面
 * 拦截所有标注了 @RequireRole 注解的方法，验证用户角色是否匹配
 *
 * 设计模式：
 * - 策略模式：不同的错误场景使用不同的异常处理策略
 * - 责任链模式：先检查登录状态，再检查角色权限
 */
@Slf4j
@Aspect
@Component
public class RoleCheckAspect {

    /**
     * 环绕通知：拦截所有标注了 @RequireRole 的方法
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(com.gzdoc.common.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取方法签名和注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);

        if (requireRole == null) {
            // 理论上不会发生，因为切点已经限定了注解
            return joinPoint.proceed();
        }

        // 2. 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.error("无法获取当前请求上下文");
            throw new UnauthorizedException(AuthErrorCode.UNAUTHORIZED);
        }

        HttpServletRequest request = attributes.getRequest();

        // 3. 获取用户信息（由 JwtAuthInterceptor 设置）
        Long userId = (Long) request.getAttribute(AuthConstants.REQUEST_ATTR_USER_ID);
        String userRole = (String) request.getAttribute(AuthConstants.REQUEST_ATTR_USER_ROLE);

        // 4. 检查用户是否已登录
        if (userId == null || userRole == null) {
            log.warn("未登录用户尝试访问受保护资源: {}", method.getName());
            throw new UnauthorizedException(AuthErrorCode.UNAUTHORIZED);
        }

        // 5. 验证角色是否匹配
        String[] allowedRoles = requireRole.value();
        boolean hasPermission = Arrays.asList(allowedRoles).contains(userRole);

        if (!hasPermission) {
            log.warn("用户 [userId={}, role={}] 尝试访问需要角色 {} 的资源: {}",
                    userId, userRole, Arrays.toString(allowedRoles), method.getName());
            throw new ForbiddenException(AuthErrorCode.ROLE_MISMATCH);
        }

        // 6. 权限验证通过，执行目标方法
        log.debug("用户 [userId={}, role={}] 通过权限验证，访问: {}",
                userId, userRole, method.getName());
        return joinPoint.proceed();
    }
}
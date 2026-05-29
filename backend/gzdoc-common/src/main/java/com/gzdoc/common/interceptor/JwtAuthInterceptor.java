package com.gzdoc.common.interceptor;

import com.gzdoc.common.context.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证拦截器
 * 从请求头中提取 JWT token，解析并设置租户 ID 到 TenantContext
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Value("${jwt.secret:gzdoc-secret-key-for-jwt-token-generation-min-256-bits}")
    private String jwtSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头获取 token
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // 解析 JWT token
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // 提取租户 ID 并设置到 TenantContext
                Long tenantId = claims.get("tenantId", Long.class);
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }

                // 提取用户 ID 并设置到请求属性（供后续使用）
                String userId = claims.getSubject();
                request.setAttribute("userId", Long.parseLong(userId));

                // 提取角色并设置到请求属性
                String role = claims.get("role", String.class);
                request.setAttribute("userRole", role);

            } catch (Exception e) {
                log.warn("JWT token 解析失败: {}", e.getMessage());
                // 解析失败不阻止请求，由具体的权限控制决定是否拒绝
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除 TenantContext，避免内存泄漏
        TenantContext.clear();
    }
}

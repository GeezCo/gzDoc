package com.gzdoc.auth.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gzdoc.auth.dto.LoginRequest;
import com.gzdoc.auth.dto.LoginResponse;
import com.gzdoc.auth.dto.RegisterRequest;
import com.gzdoc.auth.dto.RegisterResponse;
import com.gzdoc.auth.entity.User;
import com.gzdoc.auth.mapper.UserMapper;
import com.gzdoc.common.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.secret:gzdoc-secret-key-for-jwt-token-generation-min-256-bits}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}")
    private Long jwtExpiration;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest request) {
        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(usernameWrapper) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 2. 校验邮箱是否已存在
        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, request.getEmail());
        if (userMapper.selectCount(emailWrapper) > 0) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        // 3. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword())); // BCrypt 加密
        user.setEmail(request.getEmail());
        user.setNickname(StrUtil.isNotBlank(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setRole("user"); // 默认角色为普通用户
        user.setStatus(1); // 默认启用

        // 如果提供了租户 ID，使用提供的；否则使用用户 ID 作为租户 ID（每个用户独立租户）
        if (request.getTenantId() != null) {
            user.setTenantId(request.getTenantId());
        }

        userMapper.insert(user);

        // 如果没有提供租户 ID，使用用户 ID 作为租户 ID
        if (request.getTenantId() == null) {
            user.setTenantId(user.getId());
            userMapper.updateById(user);
        }

        // 4. 返回注册结果
        return new RegisterResponse(user.getId(), user.getUsername(), user.getEmail(), user.getTenantId());
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (request.getTenantId() != null) {
            wrapper.eq(User::getTenantId, request.getTenantId());
        }
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(400, "用户已被禁用");
        }

        // 生成token
        String accessToken = generateToken(user);
        String refreshToken = generateRefreshToken(user);

        // 存储token到Redis
        String tokenKey = "token:" + user.getId();
        redisTemplate.opsForValue().set(tokenKey, accessToken, jwtExpiration, TimeUnit.SECONDS);

        // 构建响应
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar(),
                user.getRole(),
                user.getTenantId()
        );

        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtExpiration, userInfo);
    }

    /**
     * 刷新 Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        try {
            // 1. 验证 refresh token
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            // 2. 检查是否是 refresh token
            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new BusinessException(400, "无效的刷新令牌");
            }

            // 3. 获取用户信息
            Long userId = Long.parseLong(claims.getSubject());
            User user = userMapper.selectById(userId);

            if (user == null) {
                throw new BusinessException(400, "用户不存在");
            }

            if (user.getStatus() == 0) {
                throw new BusinessException(400, "用户已被禁用");
            }

            // 4. 生成新的 access token 和 refresh token
            String newAccessToken = generateToken(user);
            String newRefreshToken = generateRefreshToken(user);

            // 5. 更新 Redis 中的 token
            String tokenKey = "token:" + user.getId();
            redisTemplate.opsForValue().set(tokenKey, newAccessToken, jwtExpiration, TimeUnit.SECONDS);

            // 6. 构建响应
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    user.getEmail(),
                    user.getAvatar(),
                    user.getRole(),
                    user.getTenantId()
            );

            return new LoginResponse(newAccessToken, newRefreshToken, "Bearer", jwtExpiration, userInfo);

        } catch (Exception e) {
            log.error("刷新 Token 失败", e);
            throw new BusinessException(400, "刷新令牌无效或已过期");
        }
    }

    /**
     * 用户登出
     */
    public void logout(Long userId) {
        String tokenKey = "token:" + userId;
        redisTemplate.delete(tokenKey);
    }

    /**
     * 验证token
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token验证失败", e);
            return false;
        }
    }

    /**
     * 生成访问令牌
     */
    private String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration * 1000);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .claim("tenantId", user.getTenantId())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    private String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration * 1000 * 7); // 7天

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 获取所有用户列表（管理员功能）
     */
    public List<User> listAllUsers() {
        return userMapper.selectList(null);
    }

    /**
     * 根据用户 ID 获取用户信息
     */
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}

package com.gzdoc.auth.controller;

import com.gzdoc.auth.dto.LoginRequest;
import com.gzdoc.auth.dto.LoginResponse;
import com.gzdoc.auth.dto.RefreshTokenRequest;
import com.gzdoc.auth.dto.RegisterRequest;
import com.gzdoc.auth.dto.RegisterResponse;
import com.gzdoc.auth.entity.User;
import com.gzdoc.auth.service.AuthService;
import com.gzdoc.common.annotation.RequireRole;
import com.gzdoc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return Result.success(response);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return Result.success(response);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId) {
        authService.logout(userId);
        return Result.success();
    }

    @Operation(summary = "验证Token")
    @GetMapping("/validate")
    public Result<Boolean> validate(@RequestParam String token) {
        boolean valid = authService.validateToken(token);
        return Result.success(valid);
    }

    // ==================== 权限测试接口 ====================

    @Operation(summary = "管理员接口 - 获取所有用户列表")
    @RequireRole({"admin"})
    @GetMapping("/admin/users")
    public Result<List<User>> listUsers() {
        List<User> users = authService.listAllUsers();
        return Result.success(users);
    }

    @Operation(summary = "用户接口 - 获取个人信息")
    @RequireRole({"user", "admin"})
    @GetMapping("/user/profile")
    public Result<User> getProfile(@RequestAttribute("userId") Long userId) {
        User user = authService.getUserById(userId);
        return Result.success(user);
    }
}

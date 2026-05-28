package com.gzdoc.auth.controller;

import com.gzdoc.auth.dto.LoginRequest;
import com.gzdoc.auth.dto.LoginResponse;
import com.gzdoc.auth.service.AuthService;
import com.gzdoc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
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
}

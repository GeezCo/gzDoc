package com.gzdoc.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户注册响应")
public class RegisterResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "租户ID")
    private Long tenantId;
}

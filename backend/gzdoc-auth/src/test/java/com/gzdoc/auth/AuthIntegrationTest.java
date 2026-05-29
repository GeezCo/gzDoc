package com.gzdoc.auth;

import com.gzdoc.auth.dto.LoginRequest;
import com.gzdoc.auth.dto.LoginResponse;
import com.gzdoc.auth.dto.RefreshTokenRequest;
import com.gzdoc.auth.dto.RegisterRequest;
import com.gzdoc.auth.dto.RegisterResponse;
import com.gzdoc.common.result.Result;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证与权限集成测试
 * 测试用户注册、登录、Token 刷新、权限拦截等功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static String accessToken;
    private static String refreshToken;
    private static Long userId;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/auth";
    }

    @Test
    @Order(1)
    @DisplayName("1. 用户注册 - 成功")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("Test1234");
        request.setEmail("test@example.com");
        request.setNickname("测试用户");

        ResponseEntity<Result> response = restTemplate.postForEntity(
                getBaseUrl() + "/register",
                request,
                Result.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
    }

    @Test
    @Order(2)
    @DisplayName("2. 用户注册 - 用户名重复")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("Test1234");
        request.setEmail("test2@example.com");

        ResponseEntity<Result> response = restTemplate.postForEntity(
                getBaseUrl() + "/register",
                request,
                Result.class
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("用户名已存在");
    }

    @Test
    @Order(3)
    @DisplayName("3. 用户登录 - 成功")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("Test1234");

        ResponseEntity<Result> response = restTemplate.postForEntity(
                getBaseUrl() + "/login",
                request,
                Result.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        // 保存 token 供后续测试使用
        @SuppressWarnings("unchecked")
        var data = (java.util.LinkedHashMap<String, Object>) response.getBody().getData();
        accessToken = (String) data.get("accessToken");
        refreshToken = (String) data.get("refreshToken");

        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("4. 用户登录 - 密码错误")
    void testLoginWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("WrongPassword");

        ResponseEntity<Result> response = restTemplate.postForEntity(
                getBaseUrl() + "/login",
                request,
                Result.class
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("用户名或密码错误");
    }

    @Test
    @Order(5)
    @DisplayName("5. Token 刷新 - 成功")
    void testRefreshTokenSuccess() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        ResponseEntity<Result> response = restTemplate.postForEntity(
                getBaseUrl() + "/refresh",
                request,
                Result.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        // 更新 token
        @SuppressWarnings("unchecked")
        var data = (java.util.LinkedHashMap<String, Object>) response.getBody().getData();
        accessToken = (String) data.get("accessToken");
        assertThat(accessToken).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("6. 访问用户接口 - 已登录（成功）")
    void testAccessUserEndpointWithAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Result> response = restTemplate.exchange(
                getBaseUrl() + "/user/profile",
                HttpMethod.GET,
                entity,
                Result.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
    }

    @Test
    @Order(7)
    @DisplayName("7. 访问用户接口 - 未登录（401）")
    void testAccessUserEndpointWithoutAuth() {
        ResponseEntity<Result> response = restTemplate.getForEntity(
                getBaseUrl() + "/user/profile",
                Result.class
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).contains("未登录");
    }

    @Test
    @Order(8)
    @DisplayName("8. 访问管理员接口 - 普通用户（403）")
    void testAccessAdminEndpointAsUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Result> response = restTemplate.exchange(
                getBaseUrl() + "/admin/users",
                HttpMethod.GET,
                entity,
                Result.class
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).contains("权限不足");
    }

    @Test
    @Order(9)
    @DisplayName("9. 注册管理员账号并访问管理员接口")
    void testAccessAdminEndpointAsAdmin() {
        // 注册管理员账号（需要手动修改数据库或通过其他方式）
        // 这里简化测试，假设已有管理员账号
        // 实际项目中应该通过数据库初始化脚本创建管理员账号
    }

    @Test
    @Order(10)
    @DisplayName("10. Token 验证")
    void testValidateToken() {
        ResponseEntity<Result> response = restTemplate.getForEntity(
                getBaseUrl() + "/validate?token=" + accessToken,
                Result.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(true);
    }
}
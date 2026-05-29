# PR4: 权限控制功能实现

## 实现概述

本次实现完成了基于角色的权限控制（RBAC）功能，包括注解、AOP 拦截器、异常处理和测试用例。

## 实现的功能

### 1. 枚举和常量层
- **RoleEnum**: 角色枚举（ADMIN, USER），统一管理系统角色
- **AuthConstants**: 认证常量（token 前缀、请求属性名等）
- **AuthErrorCode**: 认证错误码枚举（包含 HTTP 状态码、错误码、错误消息）

### 2. 异常层
- **AuthException**: 认证异常基类
- **UnauthorizedException**: 401 未授权异常
- **ForbiddenException**: 403 禁止访问异常
- **GlobalExceptionHandler**: 更新全局异常处理器，统一处理认证异常

### 3. 注解层
- **@RequireRole**: 权限注解，支持多角色（满足任一即可）

### 4. AOP 拦截器
- **RoleCheckAspect**: 权限拦截器
  - 从请求中获取 userId 和 userRole（JwtAuthInterceptor 已设置）
  - 验证角色是否匹配
  - 使用策略模式处理不同错误场景

### 5. 测试接口
- **GET /auth/admin/users**: 管理员接口（仅 admin 可访问）
- **GET /auth/user/profile**: 用户接口（admin 和 user 都可访问）

### 6. 集成测试
- **AuthIntegrationTest**: 完整的集成测试
  - 测试注册流程
  - 测试登录流程
  - 测试 Token 刷新
  - 测试权限拦截（401/403）

## 设计原则

### 单点修改原则
所有常量、错误消息、角色定义都提取为枚举/常量类，修改一处，全局生效。

### 设计模式
- **策略模式**: 不同的错误场景使用不同的异常类（UnauthorizedException, ForbiddenException）
- **责任链模式**: 先检查登录状态，再检查角色权限

### 预留扩展点
- **国际化**: AuthErrorCode 预留 getI18nMessage() 方法
- **审计日志**: 在 RoleCheckAspect 中可以添加审计日志记录
- **配置外部化**: 所有常量都可以轻松迁移到配置文件

## 错误处理规范

| 场景 | HTTP 状态码 | 错误码 | 说明 |
|------|------------|--------|------|
| 未登录（无 token） | 401 | UNAUTHORIZED | 用户未提供 token 或 token 无效 |
| Token 过期 | 401 | TOKEN_EXPIRED | Token 已过期，需要刷新 |
| 角色不足 | 403 | FORBIDDEN | 用户已登录但权限不足 |

## 使用示例

### 1. 在 Controller 中使用

```java
@RestController
@RequestMapping("/api")
public class UserController {

    // 仅管理员可访问
    @RequireRole({"admin"})
    @GetMapping("/admin/users")
    public Result<List<User>> listUsers() {
        // ...
    }

    // 管理员和普通用户都可访问
    @RequireRole({"admin", "user"})
    @GetMapping("/user/profile")
    public Result<User> getProfile(@RequestAttribute("userId") Long userId) {
        // ...
    }
}
```

### 2. 前端调用

```javascript
// 登录后获取 token
const { accessToken } = await login(username, password);

// 在请求头中携带 token
axios.get('/auth/user/profile', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

## 测试验证

### 运行集成测试

```bash
cd backend
mvn test -Dtest=AuthIntegrationTest
```

### 手动测试

1. **注册用户**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test1234",
    "email": "test@example.com"
  }'
```

2. **登录获取 token**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test1234"
  }'
```

3. **访问用户接口（成功）**
```bash
curl -X GET http://localhost:8080/auth/user/profile \
  -H "Authorization: Bearer <access_token>"
```

4. **访问管理员接口（403）**
```bash
curl -X GET http://localhost:8080/auth/admin/users \
  -H "Authorization: Bearer <access_token>"
```

## 文件清单

### 新增文件

#### 枚举和常量
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/enums/RoleEnum.java`
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/enums/AuthErrorCode.java`
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/constant/AuthConstants.java`

#### 异常类
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/exception/AuthException.java`
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/exception/UnauthorizedException.java`
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/exception/ForbiddenException.java`

#### 注解和切面
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/annotation/RequireRole.java`
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/aspect/RoleCheckAspect.java`

#### 测试
- `backend/gzdoc-auth/src/test/java/com/gzdoc/auth/AuthIntegrationTest.java`
- `backend/gzdoc-auth/src/test/resources/application-test.yml`

### 修改文件
- `backend/gzdoc-common/pom.xml` - 添加 AOP 依赖
- `backend/gzdoc-common/src/main/java/com/gzdoc/common/exception/GlobalExceptionHandler.java` - 添加认证异常处理
- `backend/gzdoc-auth/src/main/java/com/gzdoc/auth/controller/AuthController.java` - 添加测试接口
- `backend/gzdoc-auth/src/main/java/com/gzdoc/auth/service/AuthService.java` - 添加 listAllUsers 和 getUserById 方法

## 技术亮点

1. **高内聚低耦合**: 所有认证相关的逻辑都集中在 common 模块，业务模块只需添加注解即可
2. **单点修改**: 所有常量、错误消息统一管理，修改一处全局生效
3. **可扩展性**: 预留国际化、审计日志等扩展点
4. **类型安全**: 使用枚举代替字符串，避免硬编码
5. **清晰的错误处理**: 401 和 403 明确区分，错误消息统一管理

## 后续优化建议

1. **国际化**: 集成 Spring MessageSource，支持多语言错误消息
2. **审计日志**: 在 RoleCheckAspect 中记录权限检查日志
3. **配置外部化**: 将角色定义移到配置文件，支持动态配置
4. **缓存优化**: 缓存用户角色信息，减少数据库查询
5. **更细粒度的权限**: 支持资源级别的权限控制（如只能访问自己的数据）

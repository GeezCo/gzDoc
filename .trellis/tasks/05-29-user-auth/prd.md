# 实现用户认证与授权功能

## 目标

完善 gzDoc 系统的用户认证与授权功能，包括用户注册、登录、权限控制、Token 刷新等核心功能，确保系统安全性和多租户隔离。

## 已有基础

通过代码检查，发现系统已有以下基础实现：

### 后端 (gzdoc-auth 模块)
- ✅ **User 实体**: 包含用户基本信息、租户ID、角色、状态等字段
- ✅ **登录功能**: 用户名/密码登录，支持多租户
- ✅ **JWT Token**: 使用 JWT 生成 access_token 和 refresh_token
- ✅ **密码加密**: 使用 BCrypt 加密存储
- ✅ **Token 存储**: Redis 存储 token，支持过期时间
- ✅ **登出功能**: 删除 Redis 中的 token
- ✅ **Token 验证**: 验证 JWT token 有效性

### 技术栈
- Spring Boot 3.x
- MyBatis-Plus
- JWT (jjwt)
- Redis
- BCrypt

## 缺失功能（需要实现）

### 1. 用户注册
- [ ] 注册接口
- [ ] 用户名/邮箱唯一性校验
- [ ] 密码强度校验
- [ ] 邮箱验证（可选）

### 2. Token 刷新
- [ ] 使用 refresh_token 刷新 access_token
- [ ] refresh_token 的存储和验证

### 3. 权限控制
- [ ] 基于角色的访问控制 (RBAC)
- [ ] 接口权限拦截器
- [ ] 租户隔离拦截器

### 4. 用户管理
- [ ] 修改密码
- [ ] 修改个人信息
- [ ] 用户列表查询（管理员）
- [ ] 用户状态管理（启用/禁用）

### 5. 前端集成
- [ ] 登录页面
- [ ] 注册页面
- [ ] Token 存储和自动刷新
- [ ] 路由守卫

## 设计决策

### 1. MVP 范围 ✅
**决策**: 阶段 1 (MVP - 核心认证)
**原因**: 专而精，快速上线核心功能，避免过度设计

### 2. 权限模型 ✅
**决策**: 简单角色模型（admin/user）
**原因**: 
- 轻量级实现，高内聚低耦合
- 使用自定义注解 `@RequireRole` 进行权限控制
- 不引入 Spring Security，保持系统简洁

### 3. 租户隔离 ✅
**决策**: 自动租户隔离
**实现**: 
- 使用 MyBatis-Plus 的 TenantLineHandler
- 从 JWT token 中提取 tenantId，自动注入到所有查询
- 业务代码无感知，自动添加 `WHERE tenant_id = ?`

## 功能需求（MVP）

### 1. 用户注册
- [ ] POST `/auth/register` 接口
- [ ] 用户名唯一性校验
- [ ] 邮箱唯一性校验
- [ ] 密码强度校验（最少 8 位，包含字母和数字）
- [ ] 自动分配默认角色 `user`
- [ ] 密码 BCrypt 加密存储

### 2. Token 刷新
- [ ] POST `/auth/refresh` 接口
- [ ] 验证 refresh_token 有效性
- [ ] 生成新的 access_token
- [ ] 更新 Redis 中的 token

### 3. 权限拦截器
- [ ] 实现 `@RequireRole` 注解
- [ ] 实现 AOP 拦截器，验证用户角色
- [ ] 从请求头 `Authorization: Bearer <token>` 中提取 token
- [ ] 解析 JWT 获取用户信息和角色
- [ ] 权限不足返回 403

### 4. 租户隔离
- [ ] 配置 MyBatis-Plus TenantLineHandler
- [ ] 实现 TenantContext（ThreadLocal 存储当前租户 ID）
- [ ] 在拦截器中从 JWT 提取 tenantId 并设置到 TenantContext
- [ ] 自动在所有查询中添加租户过滤条件

### 5. 前端集成
- [ ] 登录页面（/login）
- [ ] 注册页面（/register）
- [ ] Token 存储（localStorage）
- [ ] Axios 拦截器自动添加 Authorization 头
- [ ] Token 过期自动刷新
- [ ] 路由守卫（未登录跳转到登录页）

## 接受标准

- [ ] 用户可以成功注册账号
- [ ] 用户可以使用用户名/密码登录
- [ ] Token 过期后可以使用 refresh_token 刷新
- [ ] 管理员接口普通用户无法访问（返回 403）
- [ ] 不同租户的数据自动隔离
- [ ] 前端登录/注册页面可用
- [ ] 所有接口通过 Postman/前端测试验证
- [ ] 单元测试覆盖核心逻辑

## 实现计划

### 后端实现（按顺序）

**PR1: 租户隔离基础设施**
- 配置 MyBatis-Plus TenantLineHandler
- 实现 TenantContext (ThreadLocal)
- 实现 JWT 拦截器提取 tenantId

**PR2: 用户注册功能**
- RegisterRequest/RegisterResponse DTO
- 注册接口实现
- 用户名/邮箱唯一性校验
- 密码强度校验

**PR3: Token 刷新功能**
- 刷新接口实现
- refresh_token 验证逻辑
- Redis token 更新

**PR4: 权限控制**
- `@RequireRole` 注解
- AOP 权限拦截器
- 测试用例

### 前端实现

**PR5: 认证页面**
- 登录页面
- 注册页面
- Axios 拦截器
- Token 自动刷新
- 路由守卫

## Definition of Done

- [ ] 所有单元测试通过
- [ ] 集成测试通过
- [ ] Lint / TypeCheck / CI 绿色
- [ ] 前后端联调成功
- [ ] Postman 测试通过
- [ ] 代码 Review 完成

## 技术细节

### 数据库表结构
已有 `t_user` 表，字段包括：
- id, tenant_id, username, password, nickname, email, phone
- avatar, status, role, create_time, update_time, deleted

### JWT Payload 结构
```json
{
  "sub": "userId",
  "username": "xxx",
  "role": "admin|user",
  "tenantId": 123,
  "iat": 1234567890,
  "exp": 1234567890
}
```

### API 接口设计

**注册**
```
POST /auth/register
Request: { username, password, email, nickname?, tenantId? }
Response: { userId, username, email }
```

**刷新 Token**
```
POST /auth/refresh
Request: { refreshToken }
Response: { accessToken, refreshToken, expiresIn }
```

### 前端路由
- `/login` - 登录页
- `/register` - 注册页
- `/` - 首页（需要登录）

## 技术约束

- 后端：Spring Boot 3.x + MyBatis-Plus
- 数据库：MySQL（已有 t_user 表）
- 缓存：Redis
- 前端：React + Ant Design
- 认证方式：JWT
- 设计原则：专而精、高内聚低耦合、轻量级

## Out of Scope（明确不做）

- ❌ 邮箱验证
- ❌ 密码找回
- ❌ OAuth2 第三方登录
- ❌ 多因素认证 (MFA)
- ❌ 完整的 RBAC 权限系统
- ❌ Spring Security 集成
- ❌ 用户管理界面（列表、编辑等）
- ❌ 审计日志

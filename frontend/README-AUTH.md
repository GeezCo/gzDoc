# 前端认证系统实现文档

## 概述

本文档描述了 gzDoc 前端认证系统的完整实现，包括登录、注册、Token 管理、路由守卫等核心功能。

## 架构设计

### 设计原则

1. **统一常量管理**: 所有 API 路径、localStorage key、错误消息统一管理
2. **单点修改原则**: 配置集中在 `constants/auth.ts`，修改一处全局生效
3. **用户体验优先**: Token 自动刷新对用户透明，错误提示友好
4. **高内聚低耦合**: 功能模块职责清晰，易于维护和测试

### 目录结构

```
frontend/src/
├── constants/
│   └── auth.ts              # 认证相关常量（API路径、Storage Key、错误消息等）
├── utils/
│   ├── token.ts             # Token 管理工具
│   └── request.ts           # Axios 封装（自动刷新 Token）
├── services/
│   └── auth.ts              # 认证 API 封装
├── components/
│   └── ProtectedRoute.tsx   # 路由守卫组件
├── pages/
│   ├── Login.tsx            # 登录页面
│   ├── Login.css
│   ├── Register.tsx         # 注册页面
│   └── Register.css
└── types/
    └── index.ts             # TypeScript 类型定义
```

## 核心功能实现

### 1. 常量管理 (`constants/auth.ts`)

所有认证相关的常量集中管理：

- **API 路径**: `AUTH_API.LOGIN`, `AUTH_API.REGISTER`, `AUTH_API.REFRESH` 等
- **Storage Keys**: `STORAGE_KEY.ACCESS_TOKEN`, `STORAGE_KEY.REFRESH_TOKEN` 等
- **路由路径**: `ROUTE_PATH.LOGIN`, `ROUTE_PATH.DASHBOARD` 等
- **错误消息**: `ERROR_MESSAGE.*`
- **成功消息**: `SUCCESS_MESSAGE.*`
- **验证规则**: `VALIDATION_RULES.*`

**优势**: 修改 API 路径或消息文案时，只需修改一处即可全局生效。

### 2. Token 管理 (`utils/token.ts`)

提供统一的 Token 和用户信息存储管理：

```typescript
// 获取和设置 Access Token
getAccessToken(): string | null
setAccessToken(token: string): void

// 获取和设置 Refresh Token
getRefreshToken(): string | null
setRefreshToken(token: string): void

// 获取和设置用户信息
getUserInfo(): User | null
setUserInfo(userInfo: User): void

// 清除所有认证数据
clearAuthData(): void

// 检查是否已登录
isAuthenticated(): boolean
```

**优势**: 
- 封装 localStorage 操作，统一管理 key
- 自动处理 JSON 序列化/反序列化
- 提供类型安全的 API

### 3. 自动 Token 刷新 (`utils/request.ts`)

核心功能：当 API 返回 401 时，自动使用 refresh_token 刷新 access_token。

**关键特性**:

1. **防止并发刷新**: 使用 `isRefreshing` 标志和请求队列，确保同一时间只有一个刷新请求
2. **队列重试**: 刷新期间的其他请求加入队列，刷新成功后使用新 Token 自动重试
3. **失败处理**: 刷新失败时清除认证数据并跳转登录页
4. **透明体验**: 用户无感知，自动完成 Token 刷新

**实现逻辑**:

```typescript
响应拦截器
  ↓
收到 401 错误
  ↓
检查是否正在刷新? 
  ├─ 是 → 加入队列等待
  └─ 否 → 开始刷新
          ↓
      调用 /auth/refresh
          ↓
      刷新成功?
        ├─ 是 → 更新 Token → 重试原请求 → 处理队列
        └─ 否 → 清除认证 → 跳转登录页
```

### 4. 路由守卫 (`components/ProtectedRoute.tsx`)

保护需要登录才能访问的路由：

- 检查用户是否已登录（通过 `isAuthenticated()`）
- 未登录自动重定向到登录页
- 记录原目标路径，登录后自动跳转回去

**使用方式**:

```tsx
<Route
  path="/"
  element={
    <ProtectedRoute>
      <Layout />
    </ProtectedRoute>
  }
>
  <Route path="dashboard" element={<Dashboard />} />
  <Route path="documents" element={<DocumentList />} />
</Route>
```

### 5. 登录页面 (`pages/Login.tsx`)

**功能**:
- 用户名 + 密码登录
- 可选租户 ID（高级选项折叠）
- 支持从注册页跳转后自动填充用户名
- 登录后跳转到原目标页面或首页
- 使用统一的常量和工具函数

**表单验证**:
- 用户名必填
- 密码必填

### 6. 注册页面 (`pages/Register.tsx`)

**功能**:
- 用户名（3-20字符，字母数字下划线）
- 邮箱（格式验证）
- 密码（至少8位，包含字母和数字）
- 确认密码（一致性验证）
- 昵称（可选）
- 租户 ID（可选，高级选项）
- 注册成功后跳转登录页

**表单验证**:
- 所有验证规则从 `VALIDATION_RULES` 常量读取
- 密码强度校验：`/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]+$/`
- 用户名格式校验：`/^[a-zA-Z0-9_]+$/`
- 邮箱格式校验：`/^[^\s@]+@[^\s@]+\.[^\s@]+$/`

## API 接口

### 登录

```typescript
POST /api/auth/login
Request: {
  username: string
  password: string
  tenantId?: number
}
Response: {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: User
}
```

### 注册

```typescript
POST /api/auth/register
Request: {
  username: string
  password: string
  email: string
  nickname?: string
  tenantId?: number
}
Response: {
  userId: number
  username: string
  email: string
}
```

### 刷新 Token

```typescript
POST /api/auth/refresh
Request: {
  refreshToken: string
}
Response: {
  accessToken: string
  refreshToken?: string
  expiresIn: number
}
```

### 登出

```typescript
POST /api/auth/logout
Response: { code: 200, message: "success" }
```

## 用户流程

### 登录流程

1. 用户访问登录页 `/login`
2. 输入用户名、密码（可选租户ID）
3. 提交表单，调用 `authApi.login()`
4. 成功后：
   - 保存 accessToken、refreshToken 到 localStorage
   - 保存 userInfo 到 localStorage
   - 跳转到原目标页面或 `/dashboard`
5. 失败后：显示错误消息

### 注册流程

1. 用户访问注册页 `/register`
2. 填写表单（用户名、邮箱、密码等）
3. 前端验证通过后，调用 `authApi.register()`
4. 成功后：
   - 显示成功提示
   - 1.5秒后跳转到登录页
   - 登录页自动填充用户名
5. 失败后：显示错误消息

### Token 刷新流程

1. 用户访问受保护的 API
2. API 返回 401 错误
3. Axios 响应拦截器捕获：
   - 使用 refresh_token 调用 `/auth/refresh`
   - 成功：更新 accessToken，重试原请求
   - 失败：清除认证数据，跳转登录页
4. 用户无感知，继续操作

### 登出流程

1. 用户点击"退出登录"
2. 调用 `authApi.logout()`（通知后端清除 Token）
3. 清除本地所有认证数据
4. 跳转到登录页

## 数据存储

使用 localStorage 存储认证数据：

| Key | 说明 | 示例 |
|-----|------|------|
| `gzdoc_access_token` | 访问令牌 | `eyJhbGc...` |
| `gzdoc_refresh_token` | 刷新令牌 | `eyJhbGc...` |
| `gzdoc_user_info` | 用户信息（JSON） | `{"id":1,"username":"admin",...}` |
| `gzdoc_tenant_id` | 租户ID | `1` |
| `gzdoc_user_id` | 用户ID | `1` |

**注意**: 所有 key 使用 `gzdoc_` 前缀，避免与其他应用冲突。

## 安全性

1. **Token 存储**: 
   - Access Token 和 Refresh Token 分别存储
   - Refresh Token 仅用于刷新，不用于 API 调用

2. **自动过期处理**:
   - Token 过期时自动刷新
   - Refresh Token 过期时跳转登录页

3. **密码强度**:
   - 前端验证：至少8位，包含字母和数字
   - 后端加密：BCrypt 哈希存储

4. **HTTPS**:
   - 生产环境必须使用 HTTPS 传输 Token

5. **XSS 防护**:
   - React 自动转义输出
   - 不使用 `dangerouslySetInnerHTML`

## 测试指南

### 手动测试

1. **注册测试**:
   - 访问 `/register`
   - 测试表单验证（用户名格式、密码强度、邮箱格式）
   - 测试注册成功后跳转登录页

2. **登录测试**:
   - 访问 `/login`
   - 测试正确/错误的用户名密码
   - 测试登录成功后跳转

3. **路由守卫测试**:
   - 未登录访问 `/dashboard`，应跳转 `/login`
   - 登录后访问 `/dashboard`，正常显示

4. **Token 刷新测试**:
   - 登录后等待 Token 过期（或手动修改 Token）
   - 调用任意 API，观察 Network 面板：
     - 第一次请求返回 401
     - 自动调用 `/auth/refresh`
     - 原请求自动重试并成功

5. **登出测试**:
   - 点击"退出登录"
   - 应跳转到登录页
   - localStorage 应清空

### 浏览器 DevTools 验证

**检查 localStorage**:
```javascript
// 打开浏览器控制台
localStorage.getItem('gzdoc_access_token')
localStorage.getItem('gzdoc_refresh_token')
localStorage.getItem('gzdoc_user_info')
```

**检查 Network 请求**:
- 查看请求头是否包含 `Authorization: Bearer <token>`
- 查看 401 后是否自动调用 `/auth/refresh`

## 常见问题

### 1. Token 刷新失败怎么办？

**现象**: 用户操作中突然跳转到登录页

**原因**: 
- Refresh Token 已过期
- 后端 Redis 中的 Token 已被清除
- 网络错误

**解决**: 重新登录即可

### 2. 为什么访问页面时跳转到登录页？

**原因**: 
- 未登录（localStorage 中没有 token）
- Token 已过期且 Refresh Token 也过期

**解决**: 登录即可

### 3. 如何调试 Token 刷新？

1. 打开浏览器 DevTools → Network 面板
2. 登录后，手动修改 localStorage 中的 `gzdoc_access_token` 为无效值
3. 调用任意 API（如刷新文档列表）
4. 观察 Network：
   - 第一次请求返回 401
   - 自动发起 `/auth/refresh` 请求
   - 原请求自动重试

### 4. 如何修改 Token 过期时间？

**后端配置** (gzdoc-auth 模块):
```yaml
# application.yml
jwt:
  access-token-expire: 3600000  # 1小时（毫秒）
  refresh-token-expire: 604800000  # 7天（毫秒）
```

**前端无需修改**，自动适应后端配置。

## 扩展功能

### 未来可以添加的功能

1. **记住我**: 延长 Token 有效期
2. **多设备管理**: 显示所有登录设备，支持远程登出
3. **密码找回**: 邮箱验证码重置密码
4. **邮箱验证**: 注册后发送验证邮件
5. **OAuth2 登录**: 支持 GitHub、Google 等第三方登录
6. **多因素认证 (MFA)**: 增强安全性

## 维护指南

### 修改 API 路径

只需修改 `constants/auth.ts` 中的 `AUTH_API` 常量：

```typescript
export const AUTH_API = {
  LOGIN: '/auth/login',      // 修改这里
  REGISTER: '/auth/register', // 修改这里
  // ...
}
```

### 修改错误消息

只需修改 `constants/auth.ts` 中的 `ERROR_MESSAGE` 常量：

```typescript
export const ERROR_MESSAGE = {
  LOGIN_FAILED: '登录失败，请检查用户名和密码', // 修改这里
  // ...
}
```

### 修改验证规则

只需修改 `constants/auth.ts` 中的 `VALIDATION_RULES` 常量：

```typescript
export const VALIDATION_RULES = {
  PASSWORD: {
    MIN_LENGTH: 8,  // 修改最小长度
    MAX_LENGTH: 32, // 修改最大长度
    // ...
  },
}
```

## 总结

本实现遵循以下原则：

✅ **统一常量管理**: 所有配置集中在 `constants/auth.ts`  
✅ **单点修改**: 修改一处，全局生效  
✅ **自动 Token 刷新**: 用户无感知，体验流畅  
✅ **路由守卫**: 自动保护受保护路由  
✅ **类型安全**: 完整的 TypeScript 类型定义  
✅ **错误处理**: 友好的错误提示  
✅ **可维护性**: 代码结构清晰，易于扩展  

**核心优势**: 高内聚低耦合、专而精、轻量级，符合 gzDoc 项目的设计理念。

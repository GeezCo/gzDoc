# GzDoc 开发指南

## 1. 开发环境搭建

### 1.1 必需软件

- **Java**: JDK 17+
- **Node.js**: 18+
- **Python**: 3.11+
- **Docker**: 最新版
- **Git**: 最新版
- **IDE**: 
  - Java: IntelliJ IDEA
  - Python: PyCharm / VS Code
  - 前端: VS Code / WebStorm

### 1.2 安装步骤

#### macOS
```bash
# 安装 Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装 Java
brew install openjdk@17

# 安装 Node.js
brew install node@18

# 安装 Python
brew install python@3.11

# 安装 Docker
brew install --cask docker
```

#### Windows
- 下载并安装 JDK 17
- 下载并安装 Node.js 18
- 下载并安装 Python 3.11
- 下载并安装 Docker Desktop

---

## 2. 项目结构

```
gzDoc/
├── backend/                    # 后端服务
│   ├── gzdoc-gateway/         # API网关
│   ├── gzdoc-auth/            # 用户服务
│   ├── gzdoc-document/        # 文档服务
│   ├── gzdoc-qa/              # 问答服务
│   └── gzdoc-common/          # 公共模块
├── frontend/                   # 前端项目
│   └── src/
│       ├── components/        # 组件
│       ├── pages/             # 页面
│       ├── services/          # API服务
│       └── utils/             # 工具函数
├── ai-service/                # AI服务
│   └── app/
│       ├── api/               # API路由
│       ├── core/              # 核心配置
│       ├── models/            # 数据模型
│       ├── services/          # 业务逻辑
│       └── utils/             # 工具函数
├── docs/                      # 文档
├── scripts/                   # 脚本
└── infrastructure/            # 基础设施
    ├── docker/                # Docker配置
    ├── k8s/                   # Kubernetes配置
    └── monitoring/            # 监控配置
```

---

## 3. 本地开发

### 3.1 启动基础服务

```bash
cd infrastructure/docker
docker-compose up -d
```

这会启动：
- PostgreSQL (5432)
- Redis (6379)
- MinIO (9000, 9001)
- Kafka (9092)
- Weaviate (8080)
- ElasticSearch (9200)

### 3.2 启动后端服务

```bash
cd backend/gzdoc-gateway
mvn spring-boot:run

# 其他服务类似
```

### 3.3 启动AI服务

```bash
cd ai-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### 3.4 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问: http://localhost:5173

---

## 4. 开发规范

### 4.1 代码规范

#### Java
- 遵循阿里巴巴Java开发手册
- 使用 Lombok 简化代码
- 统一使用 MyBatis-Plus
- 统一异常处理
- 统一返回格式

#### Python
- 遵循 PEP 8
- 使用 Black 格式化
- 使用 Type Hints
- 使用 Pydantic 做数据验证

#### JavaScript/TypeScript
- 遵循 Airbnb 规范
- 使用 ESLint + Prettier
- 使用 TypeScript

### 4.2 Git规范

#### 分支管理
- `main`: 主分支，保护分支
- `develop`: 开发分支
- `feature/*`: 功能分支
- `bugfix/*`: 修复分支
- `hotfix/*`: 紧急修复

#### Commit规范
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型**:
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

**示例**:
```
feat(auth): 添加用户登录功能

- 实现JWT认证
- 添加登录接口
- 添加用户信息查询接口

Closes #123
```

### 4.3 API规范

#### RESTful设计
```
GET    /api/v1/documents       # 获取文档列表
GET    /api/v1/documents/:id   # 获取文档详情
POST   /api/v1/documents       # 创建文档
PUT    /api/v1/documents/:id   # 更新文档
DELETE /api/v1/documents/:id   # 删除文档
```

#### 统一返回格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1234567890
}
```

#### 错误码
- `200`: 成功
- `400`: 参数错误
- `401`: 未认证
- `403`: 无权限
- `404`: 资源不存在
- `500`: 服务器错误

---

## 5. 测试

### 5.1 单元测试

#### Java
```java
@SpringBootTest
class UserServiceTest {
    @Test
    void testCreateUser() {
        // 测试代码
    }
}
```

#### Python
```python
def test_ocr_service():
    # 测试代码
    pass
```

### 5.2 集成测试

```bash
# 后端
mvn test

# AI服务
pytest

# 前端
npm run test
```

---

## 6. 调试技巧

### 6.1 后端调试
- 使用 IntelliJ IDEA 的 Debug 功能
- 查看日志: `logs/app.log`
- 使用 Postman 测试API

### 6.2 AI服务调试
- 使用 FastAPI 自动生成的文档: http://localhost:8000/docs
- 使用 `print()` 或 `logging`
- 使用 PyCharm 的 Debug 功能

### 6.3 前端调试
- 使用浏览器开发者工具
- 使用 React DevTools
- 查看 Network 请求

---

## 7. 常见问题

### 7.1 Docker服务启动失败
```bash
# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 清理并重启
docker-compose down -v
docker-compose up -d
```

### 7.2 端口冲突
```bash
# 查看端口占用
lsof -i :8080

# 杀死进程
kill -9 <PID>
```

### 7.3 依赖安装失败
```bash
# Maven
mvn clean install -U

# npm
rm -rf node_modules package-lock.json
npm install

# pip
pip install --upgrade pip
pip install -r requirements.txt
```

---

## 8. 开发工作流

### 8.1 开发新功能

1. 从 `develop` 创建功能分支
```bash
git checkout develop
git pull
git checkout -b feature/user-login
```

2. 开发功能

3. 提交代码
```bash
git add .
git commit -m "feat(auth): 添加用户登录功能"
```

4. 推送到远程
```bash
git push origin feature/user-login
```

5. 创建 Pull Request

6. Code Review

7. 合并到 `develop`

---

## 9. 性能优化

### 9.1 后端优化
- 使用 Redis 缓存
- 数据库索引优化
- 使用连接池
- 异步处理

### 9.2 AI服务优化
- 批量处理
- 模型缓存
- 使用 GPU
- Token优化

### 9.3 前端优化
- 代码分割
- 懒加载
- 图片压缩
- CDN加速

---

## 10. 下一步

- [ ] 配置开发环境
- [ ] 熟悉项目结构
- [ ] 阅读架构文档
- [ ] 开始第一个功能开发

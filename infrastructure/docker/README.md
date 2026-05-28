# Docker 部署配置

## 环境变量配置

### 测试环境

1. 复制环境变量模板：
```bash
cd infrastructure/docker
cp .env.example .env
```

2. 编辑 `.env` 文件，设置安全的密码：
```bash
# 生成随机密码示例
POSTGRES_PASSWORD=$(openssl rand -base64 32)
REDIS_PASSWORD=$(openssl rand -base64 32)
MINIO_ROOT_PASSWORD=$(openssl rand -base64 32)
```

3. 启动服务：
```bash
docker-compose -f docker-compose.test.yml up -d
```

### 生产环境

生产环境必须使用强密码，建议：
- 密码长度至少 32 字符
- 包含大小写字母、数字和特殊字符
- 定期轮换密码
- 使用密钥管理服务（如 AWS Secrets Manager、HashiCorp Vault）

## 安全注意事项

⚠️ **重要**：
- 永远不要将 `.env` 文件提交到 Git
- 不要在代码中硬编码密码
- 不要在文档中使用真实密码作为示例
- 定期审计和轮换密码

## 文件说明

- `.env.example` - 环境变量模板（可以提交到 Git）
- `.env` - 实际环境变量（已在 .gitignore 中排除）
- `docker-compose.test.yml` - 测试环境配置
- `docker-compose.prod.yml` - 生产环境配置

# 部署指南

## 方式一：使用部署脚本（推荐）

### 基本用法

```bash
# 部署到测试环境（默认）
./scripts/deploy.sh

# 部署到生产环境
./scripts/deploy.sh -s 36.141.21.176 -f docker-compose.prod.yml -e prod

# 使用环境变量
DEPLOY_SERVER=192.168.57.10 ./scripts/deploy.sh
```

### 所有选项

```bash
选项:
  -s, --server <IP>       目标服务器IP (默认: 192.168.57.10)
  -u, --user <USER>       SSH用户名 (默认: root)
  -d, --dir <DIR>         远程部署目录 (默认: /opt/gzdoc)
  -f, --file <FILE>       docker-compose文件 (默认: docker-compose.test.yml)
  -e, --env <ENV>         环境名称 (默认: test)
  --skip-check            跳过环境检查
  --skip-upload           跳过文件上传
  --only-restart          仅重启服务
  -h, --help              显示帮助信息
```

### 常见场景

```bash
# 1. 首次部署到测试环境
./scripts/deploy.sh

# 2. 首次部署到生产环境
./scripts/deploy.sh -s 36.141.21.176 -f docker-compose.prod.yml -e prod

# 3. 更新配置后重新部署
./scripts/deploy.sh --skip-check

# 4. 仅重启服务（不上传文件）
./scripts/deploy.sh --only-restart

# 5. 部署到自定义服务器
./scripts/deploy.sh -s 10.0.0.100 -u admin -d /home/admin/gzdoc
```

---

## 方式二：手动部署

### 测试环境 (192.168.57.10)

```bash
# 1. SSH登录
ssh root@192.168.57.10

# 2. 创建目录
mkdir -p /opt/gzdoc
cd /opt/gzdoc

# 3. 上传文件（在本地执行）
scp infrastructure/docker/docker-compose.test.yml root@192.168.57.10:/opt/gzdoc/docker-compose.yml
scp infrastructure/docker/init-db.sql root@192.168.57.10:/opt/gzdoc/

# 4. 启动服务（在服务器上执行）
cd /opt/gzdoc
docker-compose up -d

# 5. 查看状态
docker-compose ps

# 6. 查看日志
docker-compose logs -f
```

### 生产环境 (36.141.21.176)

```bash
# 1. SSH登录
ssh root@36.141.21.176

# 2. 创建目录
mkdir -p /opt/gzdoc /data/gzdoc
cd /opt/gzdoc

# 3. 创建.env文件
cat > .env << EOF
POSTGRES_PASSWORD=your_strong_password
REDIS_PASSWORD=your_redis_password
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=your_minio_password
WEAVIATE_API_KEY=your_weaviate_key
ELASTIC_PASSWORD=your_elastic_password
EOF

# 4. 上传文件（在本地执行）
scp infrastructure/docker/docker-compose.prod.yml root@36.141.21.176:/opt/gzdoc/docker-compose.yml
scp infrastructure/docker/init-db.sql root@36.141.21.176:/opt/gzdoc/

# 5. 启动服务（在服务器上执行）
cd /opt/gzdoc
docker-compose up -d

# 6. 查看状态
docker-compose ps
```

---

## 常用命令

### 查看服务状态
```bash
docker-compose ps
```

### 查看日志
```bash
# 所有服务
docker-compose logs -f

# 单个服务
docker-compose logs -f postgres
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 重启单个服务
docker-compose restart postgres
```

### 停止服务
```bash
docker-compose down
```

### 停止并删除数据
```bash
docker-compose down -v
```

### 进入容器
```bash
# PostgreSQL
docker exec -it gzdoc-postgres-test psql -U gzdoc -d gzdoc_test

# Redis
docker exec -it gzdoc-redis-test redis-cli -a gzdoc123
```

---

## 验证服务

### PostgreSQL
```bash
docker exec gzdoc-postgres-test pg_isready -U gzdoc
```

### Redis
```bash
docker exec gzdoc-redis-test redis-cli -a gzdoc123 ping
```

### MinIO
```bash
curl http://192.168.57.10:9000/minio/health/live
```

### ElasticSearch
```bash
curl http://192.168.57.10:9200/_cluster/health
```

---

## 故障排查

### 服务无法启动

```bash
# 查看日志
docker-compose logs [service_name]

# 查看容器状态
docker ps -a

# 重启服务
docker-compose restart [service_name]
```

### 端口冲突

```bash
# 查看端口占用
netstat -tlnp | grep [port]

# 或使用lsof
lsof -i :[port]

# 修改docker-compose.yml中的端口映射
```

### 磁盘空间不足

```bash
# 查看磁盘使用
df -h

# 清理Docker
docker system prune -a

# 清理日志
docker-compose logs --tail=0
```

### 内存不足

```bash
# 查看内存使用
free -h

# 查看容器资源使用
docker stats

# 调整docker-compose.yml中的资源限制
```

---

## 备份与恢复

### 备份数据库

```bash
# PostgreSQL
docker exec gzdoc-postgres-test pg_dump -U gzdoc gzdoc_test > backup_$(date +%Y%m%d).sql

# 恢复
docker exec -i gzdoc-postgres-test psql -U gzdoc gzdoc_test < backup_20260528.sql
```

### 备份数据目录

```bash
# 测试环境
docker-compose down
tar -czf gzdoc_backup_$(date +%Y%m%d).tar.gz /var/lib/docker/volumes/

# 生产环境
tar -czf gzdoc_backup_$(date +%Y%m%d).tar.gz /data/gzdoc/
```

---

## 环境检查清单

### 部署前检查

- [ ] SSH可以连接到目标服务器
- [ ] 服务器已安装Docker和Docker Compose
- [ ] 服务器有足够的磁盘空间（至少50GB）
- [ ] 服务器有足够的内存（至少8GB）
- [ ] 防火墙已开放必要端口
- [ ] 生产环境已配置.env文件

### 部署后检查

- [ ] 所有容器都在运行
- [ ] PostgreSQL可以连接
- [ ] Redis可以连接
- [ ] MinIO可以访问
- [ ] 日志没有错误信息
- [ ] 数据库表已创建

---

## 下一步

部署完成后，可以：

1. 配置本地开发环境连接到测试环境数据库
2. 开始开发后端服务
3. 开始开发前端项目
4. 开始开发AI服务

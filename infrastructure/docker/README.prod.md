# 生产环境配置说明

## 环境变量配置

创建 `.env.prod` 文件（不要提交到Git）：

```bash
# PostgreSQL
POSTGRES_PASSWORD=your_strong_password_here

# Redis
REDIS_PASSWORD=your_redis_password_here

# MinIO
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=your_minio_password_here

# Weaviate
WEAVIATE_API_KEY=your_weaviate_api_key_here

# ElasticSearch
ELASTIC_PASSWORD=your_elastic_password_here
```

## 部署步骤

### 1. 准备服务器

```bash
# SSH登录
ssh root@36.141.21.176

# 创建数据目录
mkdir -p /data/gzdoc/{postgres,redis,minio,zookeeper,kafka,weaviate,elasticsearch,nginx/logs,ssl}

# 创建项目目录
mkdir -p /opt/gzdoc
cd /opt/gzdoc
```

### 2. 上传配置文件

```bash
# 从本地上传
scp infrastructure/docker/docker-compose.prod.yml root@36.141.21.176:/opt/gzdoc/
scp infrastructure/docker/.env.prod root@36.141.21.176:/opt/gzdoc/.env
scp infrastructure/docker/init-db.sql root@36.141.21.176:/opt/gzdoc/
```

### 3. 启动服务

```bash
# 在服务器上执行
cd /opt/gzdoc
docker-compose -f docker-compose.prod.yml up -d

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f

# 查看状态
docker-compose -f docker-compose.prod.yml ps
```

### 4. 验证服务

```bash
# PostgreSQL
docker exec -it gzdoc-postgres-prod psql -U gzdoc -d gzdoc_prod

# Redis
docker exec -it gzdoc-redis-prod redis-cli -a your_redis_password ping

# MinIO
curl http://36.141.21.176:9000/minio/health/live

# ElasticSearch
curl -u elastic:your_elastic_password http://36.141.21.176:9200/_cluster/health
```

## 监控和维护

### 查看资源使用

```bash
docker stats
```

### 备份数据

```bash
# PostgreSQL备份
docker exec gzdoc-postgres-prod pg_dump -U gzdoc gzdoc_prod > backup_$(date +%Y%m%d).sql

# 数据目录备份
tar -czf gzdoc_data_backup_$(date +%Y%m%d).tar.gz /data/gzdoc/
```

### 日志管理

```bash
# 查看日志
docker-compose -f docker-compose.prod.yml logs -f [service_name]

# 清理日志
docker-compose -f docker-compose.prod.yml logs --tail=0 -f
```

## 安全建议

1. **修改默认密码**: 所有服务的密码都要改成强密码
2. **防火墙配置**: 只开放必要的端口
3. **SSL证书**: 配置HTTPS
4. **定期备份**: 每天自动备份数据库
5. **监控告警**: 配置Prometheus + Grafana

## 性能优化

1. **PostgreSQL**: 调整 `shared_buffers`, `work_mem`
2. **Redis**: 配置持久化策略
3. **ElasticSearch**: 调整JVM堆内存
4. **Kafka**: 调整日志保留时间

## 故障排查

### 服务无法启动

```bash
# 查看日志
docker-compose -f docker-compose.prod.yml logs [service_name]

# 重启服务
docker-compose -f docker-compose.prod.yml restart [service_name]
```

### 磁盘空间不足

```bash
# 清理Docker
docker system prune -a

# 清理日志
find /data/gzdoc -name "*.log" -mtime +7 -delete
```

### 内存不足

```bash
# 查看内存使用
free -h

# 调整服务资源限制
# 编辑 docker-compose.prod.yml 中的 deploy.resources
```

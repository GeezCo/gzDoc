# 环境说明

## 三套环境

### 1. 开发环境（本地Mac）
- **用途**: 日常开发、快速调试
- **部署方式**: 本地运行代码，连接测试环境数据库
- **配置文件**: `application-dev.yml`

### 2. 测试环境（192.168.57.10）
- **用途**: 集成测试、功能验证
- **部署方式**: Docker Compose
- **配置文件**: `application-test.yml`
- **访问方式**: 内网访问

### 3. 生产环境（36.141.21.176）
- **用途**: 正式运行、客户演示
- **部署方式**: Kubernetes（后期）/ Docker Compose（初期）
- **配置文件**: `application-prod.yml`
- **访问方式**: 公网访问

---

## 环境切换

### 本地开发
```bash
# 启动后端
mvn spring-boot:run -Dspring.profiles.active=dev

# 启动AI服务
export ENV=dev
uvicorn app.main:app --reload

# 启动前端
npm run dev
```

### 测试环境部署
```bash
ssh root@192.168.57.10
cd /opt/gzdoc
docker-compose -f docker-compose.test.yml up -d
```

### 生产环境部署
```bash
ssh root@36.141.21.176
cd /opt/gzdoc
docker-compose -f docker-compose.prod.yml up -d
```

---

## 数据库连接

### 开发环境（连接测试环境数据库）
- Host: 192.168.57.10
- Port: 5432
- Database: gzdoc_test
- User: gzdoc
- Password: gzdoc123

### 测试环境
- Host: localhost
- Port: 5432
- Database: gzdoc_test
- User: gzdoc
- Password: gzdoc123

### 生产环境
- Host: localhost
- Port: 5432
- Database: gzdoc_prod
- User: gzdoc
- Password: [生产密码]

---

## 注意事项

1. **开发环境**不部署Docker，直接连接测试环境
2. **测试环境**已有Docker环境，做兜底检查即可
3. **生产环境**需要高可用配置，后期迁移到K8s

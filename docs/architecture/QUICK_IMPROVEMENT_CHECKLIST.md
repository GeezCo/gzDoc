# gzDoc 架构改进快速执行清单

**目标**: 2周内完成P0和P1优先级改进  
**负责人**: 开发团队  
**开始日期**: 2026-05-29

---

## ✅ Phase 1: 紧急修复 (Week 1-2)

### Task 1.1: 集成Resilience4j熔断降级

**预计工时**: 2天  
**优先级**: P0

#### 步骤

1. **添加依赖** (所有Java服务)

```xml
<!-- backend/gzdoc-common/pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

2. **配置熔断规则**

```yaml
# backend/gzdoc-document/src/main/resources/application.yml
resilience4j:
  circuitbreaker:
    instances:
      minioService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 5
        slow-call-duration-threshold: 5s
        slow-call-rate-threshold: 80
  
  timelimiter:
    instances:
      minioService:
        timeout-duration: 5s
  
  retry:
    instances:
      minioService:
        max-attempts: 3
        wait-duration: 1s
        retry-exceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
```

3. **应用熔断注解**

```java
// backend/gzdoc-document/src/main/java/com/gzdoc/document/service/DocumentService.java
@Service
public class DocumentService {
    
    @CircuitBreaker(name = "minioService", fallbackMethod = "uploadFallback")
    @TimeLimiter(name = "minioService")
    @Retry(name = "minioService")
    public DocumentResponse upload(MultipartFile file, Long tenantId, Long userId) {
        // 原有上传逻辑
    }
    
    private DocumentResponse uploadFallback(MultipartFile file, Throwable t) {
        log.error("MinIO上传失败,触发降级", t);
        throw new BusinessException(503, "存储服务暂时不可用,请稍后重试");
    }
}
```

4. **验证**

```bash
# 启动服务
cd backend/gzdoc-document
mvn spring-boot:run

# 测试熔断
curl -X POST http://localhost:8082/api/document/upload \
  -F "file=@test.pdf" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 查看熔断状态
curl http://localhost:8082/actuator/circuitbreakers
```

**验收标准**:
- [ ] MinIO故障时,请求在5秒内返回降级响应
- [ ] 连续失败10次后,断路器打开
- [ ] 30秒后自动进入半开状态尝试恢复
- [ ] Actuator端点显示熔断器状态

---

### Task 1.2: 配置数据库连接池

**预计工时**: 0.5天  
**优先级**: P0

#### 步骤

1. **配置HikariCP** (所有Java服务)

```yaml
# backend/gzdoc-auth/src/main/resources/application.yml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:gzdoc}
    username: ${DB_USER:gzdoc}
    password: ${DB_PASSWORD}
    hikari:
      # 最小空闲连接数
      minimum-idle: 10
      # 最大连接数 (CPU核数 * 2 + 磁盘数)
      maximum-pool-size: 20
      # 连接超时时间 (毫秒)
      connection-timeout: 30000
      # 空闲连接存活时间 (毫秒)
      idle-timeout: 600000
      # 连接最大生命周期 (毫秒, 30分钟)
      max-lifetime: 1800000
      # 连接测试查询
      connection-test-query: SELECT 1
      # 泄漏检测阈值 (毫秒, 60秒)
      leak-detection-threshold: 60000
      # 连接池名称
      pool-name: AuthHikariPool
```

2. **监控连接池指标**

```xml
<!-- backend/gzdoc-common/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

3. **验证**

```bash
# 查看连接池状态
curl http://localhost:8081/actuator/metrics/hikaricp.connections

# 查看活跃连接数
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active

# 查看等待线程数
curl http://localhost:8081/actuator/metrics/hikaricp.connections.pending
```

**验收标准**:
- [ ] 所有服务配置HikariCP连接池
- [ ] 连接泄漏检测生效 (日志警告)
- [ ] Actuator暴露连接池指标
- [ ] 高并发测试无连接耗尽错误

---

### Task 1.3: 实现深度健康检查

**预计工时**: 1天  
**优先级**: P0

#### 步骤

1. **Java服务健康检查**

```java
// backend/gzdoc-auth/src/main/java/com/gzdoc/auth/config/HealthConfig.java
@Configuration
public class HealthConfig {
    
    @Bean
    public ReactiveHealthIndicator postgresHealthIndicator(DataSource dataSource) {
        return () -> {
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(2)) {
                    return Mono.just(Health.up().withDetail("database", "PostgreSQL").build());
                } else {
                    return Mono.just(Health.down().withDetail("database", "PostgreSQL连接失败").build());
                }
            } catch (SQLException e) {
                return Mono.just(Health.down(e).withDetail("database", "PostgreSQL异常").build());
            }
        };
    }
    
    @Bean
    public ReactiveHealthIndicator redisHealthIndicator(StringRedisTemplate redisTemplate) {
        return () -> {
            try {
                String pong = redisTemplate.execute((RedisCallback<String>) connection -> 
                    connection.stringCommands().ping()
                );
                if ("PONG".equals(pong)) {
                    return Mono.just(Health.up().withDetail("cache", "Redis").build());
                } else {
                    return Mono.just(Health.down().withDetail("cache", "Redis响应异常").build());
                }
            } catch (Exception e) {
                return Mono.just(Health.down(e).withDetail("cache", "Redis异常").build());
            }
        };
    }
}
```

2. **Python服务健康检查**

```python
# ai-service/app/main.py
from app.utils.weaviate_client import check_weaviate_health
from app.config import get_settings
import redis.asyncio as redis

settings = get_settings()

async def check_redis_health():
    """检查Redis健康状态"""
    try:
        r = redis.Redis(host=settings.redis_host, port=settings.redis_port, db=0)
        await r.ping()
        await r.close()
        return True
    except Exception:
        return False

async def check_weaviate_health():
    """检查Weaviate健康状态"""
    try:
        client = get_weaviate_client()
        meta = client.get_meta()
        client.close()
        return meta is not None
    except Exception:
        return False

@app.get("/health")
async def health_check():
    """深度健康检查"""
    checks = {}
    overall_status = "healthy"
    
    # 检查Redis
    redis_ok = await check_redis_health()
    checks["redis"] = "healthy" if redis_ok else "unhealthy"
    if not redis_ok:
        overall_status = "degraded"
    
    # 检查Weaviate
    weaviate_ok = await check_weaviate_health()
    checks["weaviate"] = "healthy" if weaviate_ok else "unhealthy"
    if not weaviate_ok:
        overall_status = "degraded"
    
    status_code = 200 if overall_status == "healthy" else 503
    
    return {
        "status": overall_status,
        "version": settings.app_version,
        "checks": checks
    }, status_code

@app.get("/health/live")
async def liveness_probe():
    """K8s存活探针 (轻量级)"""
    return {"status": "alive"}

@app.get("/health/ready")
async def readiness_probe():
    """K8s就绪探针 (检查依赖)"""
    redis_ok = await check_redis_health()
    weaviate_ok = await check_weaviate_health()
    
    if redis_ok and weaviate_ok:
        return {"status": "ready"}, 200
    else:
        return {"status": "not ready"}, 503
```

3. **K8s探针配置**

```yaml
# k8s/ai-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-service
spec:
  template:
    spec:
      containers:
      - name: ai-service
        image: ai-service:latest
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8000
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8000
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
```

**验收标准**:
- [ ] `/health` 端点检查所有依赖服务
- [ ] 依赖服务故障时返回503
- [ ] K8s探针配置正确
- [ ] 模拟依赖故障,健康检查正确反映

---

### Task 1.4: 完善异常处理

**预计工时**: 1.5天  
**优先级**: P0

#### 步骤

1. **定义业务异常体系**

```java
// backend/gzdoc-common/src/main/java/com/gzdoc/common/exception/ErrorCode.java
public enum ErrorCode {
    // 通用错误 (1xxx)
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // 认证错误 (2xxx)
    AUTH_FAILED(2001, "认证失败"),
    TOKEN_EXPIRED(2002, "Token已过期"),
    USER_DISABLED(2003, "用户已被禁用"),
    
    // 文档错误 (3xxx)
    DOCUMENT_UPLOAD_FAILED(3001, "文档上传失败"),
    DOCUMENT_PARSE_FAILED(3002, "文档解析失败"),
    DOCUMENT_NOT_FOUND(3004, "文档不存在"),
    FILE_TOO_LARGE(3005, "文件过大"),
    UNSUPPORTED_FORMAT(3006, "不支持的文件格式"),
    
    // AI服务错误 (4xxx)
    AI_SERVICE_UNAVAILABLE(4001, "AI服务不可用"),
    EMBEDDING_FAILED(4002, "向量化失败"),
    LLM_TIMEOUT(4003, "LLM调用超时");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
```

2. **增强全局异常处理器**

```java
// backend/gzdoc-common/src/main/java/com/gzdoc/common/exception/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), message);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeException(MaxUploadSizeExceededException e) {
        log.warn("文件过大");
        return Result.error(ErrorCode.FILE_TOO_LARGE.getCode(), "文件大小不能超过100MB");
    }
    
    @ExceptionHandler(ConnectTimeoutException.class)
    public Result<?> handleConnectTimeoutException(ConnectTimeoutException e) {
        log.error("连接超时", e);
        return Result.error(ErrorCode.AI_SERVICE_UNAVAILABLE.getCode(), "服务连接超时,请稍后重试");
    }
    
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统内部错误");
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("未知异常", e);
        // 生产环境不暴露详细错误信息
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统内部错误");
    }
}
```

3. **Python异常处理**

```python
# ai-service/app/exceptions.py
class AppException(Exception):
    """应用基础异常"""
    def __init__(self, message: str, code: str = "UNKNOWN_ERROR"):
        self.message = message
        self.code = code
        super().__init__(message)

class DocumentParseError(AppException):
    """文档解析错误"""
    def __init__(self, message: str, original_error: Exception = None):
        self.original_error = original_error
        super().__init__(message, "DOCUMENT_PARSE_ERROR")

class EmbeddingError(AppException):
    """向量化错误"""
    def __init__(self, message: str):
        super().__init__(message, "EMBEDDING_ERROR")

class VectorStoreError(AppException):
    """向量存储错误"""
    def __init__(self, message: str):
        super().__init__(message, "VECTOR_STORE_ERROR")
```

```python
# ai-service/app/main.py
from app.exceptions import AppException

@app.exception_handler(AppException)
async def handle_app_exception(request: Request, exc: AppException):
    """处理应用异常"""
    return JSONResponse(
        status_code=400,
        content={
            "code": exc.code,
            "message": exc.message
        }
    )

@app.exception_handler(Exception)
async def handle_general_exception(request: Request, exc: Exception):
    """处理未捕获异常"""
    logger.error(f"未捕获异常: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "code": "INTERNAL_ERROR",
            "message": "服务器内部错误"
        }
    )
```

**验收标准**:
- [ ] 所有业务异常使用ErrorCode枚举
- [ ] 全局异常处理器覆盖所有异常类型
- [ ] 生产环境不暴露堆栈信息
- [ ] 异常日志包含足够调试信息

---

## ✅ Phase 2: 性能优化 (Week 3-4)

### Task 2.1: 实现文件直传MinIO

**预计工时**: 3天  
**优先级**: P1

#### 步骤

1. **后端生成预签名URL**

```java
// backend/gzdoc-document/src/main/java/com/gzdoc/document/service/DocumentService.java
@Service
public class DocumentService {
    
    public PresignedUploadResponse getPresignedUrl(String fileName, Long fileSize, Long tenantId, Long userId) {
        // 1. 验证文件大小
        if (fileSize > 100 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        
        // 2. 验证文件格式
        String fileType = FileUtil.extName(fileName);
        if (!isSupportedFormat(fileType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FORMAT);
        }
        
        // 3. 创建文档记录
        Document document = new Document();
        document.setTenantId(tenantId);
        document.setName(fileName);
        document.setFileType(fileType);
        document.setFileSize(fileSize);
        document.setStatus("uploading");
        document.setUploadUserId(userId);
        documentMapper.insert(document);
        
        // 4. 生成预签名URL (有效期15分钟)
        String objectKey = generateObjectKey(tenantId, document.getId(), fileType);
        String presignedUrl = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucketName)
                .object(objectKey)
                .expiry(15, TimeUnit.MINUTES)
                .build()
        );
        
        return new PresignedUploadResponse(
            document.getId(),
            presignedUrl,
            objectKey
        );
    }
    
    @Transactional
    public void confirmUpload(Long documentId, String objectKey) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        
        document.setStoragePath(objectKey);
        document.setStatus("pending");
        documentMapper.updateById(document);
        
        // 发送Kafka消息触发解析
        kafkaTemplate.send("document-parse", documentId.toString(), 
            JsonUtil.toJson(Map.of("documentId", documentId)));
    }
}
```

2. **前端直传MinIO**

```typescript
// frontend/src/services/document.ts
import axios from 'axios'
import request from '@/utils/request'

export interface PresignedUploadResponse {
  documentId: number
  presignedUrl: string
  objectKey: string
}

export async function uploadDocument(file: File): Promise<number> {
  // 1. 获取预签名URL
  const response = await request.post<PresignedUploadResponse>('/documents/presigned-url', {
    fileName: file.name,
    fileSize: file.size
  })
  
  const { documentId, presignedUrl, objectKey } = response
  
  // 2. 直传MinIO
  await axios.put(presignedUrl, file, {
    headers: {
      'Content-Type': file.type
    },
    onUploadProgress: (progressEvent) => {
      const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total!)
      console.log(`上传进度: ${percent}%`)
    }
  })
  
  // 3. 确认上传完成
  await request.post(`/documents/${documentId}/confirm`, {
    objectKey
  })
  
  return documentId
}
```

3. **MinIO CORS配置**

```bash
# 配置MinIO允许跨域
mc cors set myminio/gzdoc << EOF
[
  {
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["PUT", "GET", "OPTIONS"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
EOF
```

**验收标准**:
- [ ] 前端可以直传MinIO
- [ ] 上传进度实时显示
- [ ] 大文件(100MB)上传正常
- [ ] 后端不再传输文件流

---

### Task 2.2: 添加多级缓存

**预计工时**: 3天  
**优先级**: P1

#### 步骤

1. **添加Caffeine本地缓存**

```xml
<!-- backend/gzdoc-common/pom.xml -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

2. **配置两级缓存**

```java
// backend/gzdoc-auth/src/main/java/com/gzdoc/auth/config/CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Caffeine本地缓存 (L1)
     */
    @Bean("localCacheManager")
    public CacheManager localCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return manager;
    }
    
    /**
     * Redis分布式缓存 (L2)
     */
    @Bean("redisCacheManager")
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

3. **实现两级缓存策略**

```java
// backend/gzdoc-auth/src/main/java/com/gzdoc/auth/service/AuthService.java
@Service
public class AuthService {
    
    @Autowired
    @Qualifier("localCacheManager")
    private CacheManager localCacheManager;
    
    @Autowired
    @Qualifier("redisCacheManager")
    private CacheManager redisCacheManager;
    
    public User getUserByUsername(String username) {
        // L1: 查Caffeine本地缓存
        Cache l1Cache = localCacheManager.getCache("user:local");
        if (l1Cache != null) {
            User user = l1Cache.get(username, User.class);
            if (user != null) {
                log.debug("L1缓存命中: {}", username);
                return user;
            }
        }
        
        // L2: 查Redis分布式缓存
        Cache l2Cache = redisCacheManager.getCache("user:redis");
        if (l2Cache != null) {
            User user = l2Cache.get(username, User.class);
            if (user != null) {
                log.debug("L2缓存命中: {}", username);
                // 回填L1
                if (l1Cache != null) {
                    l1Cache.put(username, user);
                }
                return user;
            }
        }
        
        // L3: 查数据库
        User user = userMapper.selectByUsername(username);
        if (user != null) {
            // 写入L2
            if (l2Cache != null) {
                l2Cache.put(username, user);
            }
            // 写入L1
            if (l1Cache != null) {
                l1Cache.put(username, user);
            }
        }
        
        return user;
    }
    
    @CacheEvict(value = {"user:local", "user:redis"}, key = "#username")
    public void clearUserCache(String username) {
        log.debug("清除用户缓存: {}", username);
    }
}
```

4. **缓存监控**

```java
@Bean
public MeterBinder caffeineMetrics(CacheManager cacheManager) {
    return (registry) -> {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache = 
                    ((CaffeineCache) cache).getNativeCache();
                
                Gauge.builder("caffeine.cache.size", nativeCache, com.github.benmanes.caffeine.cache.Cache::estimatedSize)
                    .tag("cache", cacheName)
                    .register(registry);
                
                Gauge.builder("caffeine.cache.hit.rate", nativeCache, c -> c.stats().hitRate())
                    .tag("cache", cacheName)
                    .register(registry);
            }
        });
    };
}
```

**验收标准**:
- [ ] L1缓存命中率 > 60%
- [ ] L2缓存命中率 > 80%
- [ ] 用户登录响应时间 < 50ms (缓存命中)
- [ ] Prometheus暴露缓存指标

---

### Task 2.3: Kafka批量消费

**预计工时**: 2天  
**优先级**: P1

#### 步骤

1. **配置批量消费**

```yaml
# ai-service/app/config.py
class Settings(BaseSettings):
    # Kafka配置
    kafka_bootstrap_servers: str = "kafka:9092"
    kafka_group_id: str = "ai-service-group"
    kafka_auto_offset_reset: str = "earliest"
    kafka_max_poll_records: int = 100  # 每次拉取100条
    kafka_max_poll_interval_ms: int = 300000
    kafka_batch_size: int = 16384
    kafka_linger_ms: int = 10
```

2. **实现批量处理器**

```python
# ai-service/app/consumer.py
from kafka import KafkaConsumer
import json
import logging
from typing import List
from app.services.document_parser import DocumentParser
from app.services.vector_service import VectorService

logger = logging.getLogger(__name__)

class BatchConsumer:
    def __init__(self):
        self.consumer = KafkaConsumer(
            'document-parse',
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.kafka_group_id,
            auto_offset_reset=settings.kafka_auto_offset_reset,
            enable_auto_commit=False,  # 手动提交
            max_poll_records=settings.kafka_max_poll_records,
            max_poll_interval_ms=settings.kafka_max_poll_interval_ms,
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )
        self.parser = DocumentParser()
        self.vector_service = VectorService()
    
    def consume(self):
        """批量消费循环"""
        logger.info("Kafka批量消费者启动")
        
        while True:
            try:
                # 拉取消息
                messages = self.consumer.poll(timeout_ms=1000)
                
                for topic_partition, records in messages.items():
                    if not records:
                        continue
                    
                    logger.info(f"拉取到 {len(records)} 条消息")
                    
                    # 批量处理
                    batch = [record.value for record in records]
                    self.process_batch(batch)
                    
                    # 手动提交offset
                    self.consumer.commit()
                    logger.info(f"批量处理完成并提交offset")
            
            except Exception as e:
                logger.error(f"消费异常: {e}", exc_info=True)
                # 不回滚,避免无限重试
                self.consumer.commit()
    
    def process_batch(self, batch: List[dict]):
        """批量处理文档"""
        success_count = 0
        failed_count = 0
        
        for message in batch:
            try:
                document_id = message['documentId']
                self.process_single_document(document_id)
                success_count += 1
            except Exception as e:
                logger.error(f"处理文档失败: document_id={message.get('documentId')}, error={e}")
                failed_count += 1
                # 发送到死信队列
                self.send_to_dead_letter_queue(message, str(e))
        
        logger.info(f"批量处理完成: 成功={success_count}, 失败={failed_count}")
    
    def send_to_dead_letter_queue(self, message: dict, error: str):
        """发送失败消息到死信队列"""
        dead_letter_message = {
            **message,
            'error': error,
            'failed_at': datetime.now().isoformat()
        }
        # 发送到死信Topic
        self.producer.send('document-parse-dlq', value=dead_letter_message)
```

**验收标准**:
- [ ] 单次拉取100条消息
- [ ] 批量处理成功率 > 95%
- [ ] 失败消息进入死信队列
- [ ] 吞吐量从10/s提升到500/s

---

## 📊 进度跟踪

| 任务 | 负责人 | 开始日期 | 预计完成 | 实际完成 | 状态 |
|------|--------|---------|---------|---------|------|
| Task 1.1: Resilience4j | | 2026-05-29 | 2026-05-30 | | ⏳ |
| Task 1.2: 连接池配置 | | 2026-05-29 | 2026-05-29 | | ⏳ |
| Task 1.3: 健康检查 | | 2026-05-30 | 2026-05-30 | | ⏳ |
| Task 1.4: 异常处理 | | 2026-05-30 | 2026-05-31 | | ⏳ |
| Task 2.1: 文件直传 | | 2026-06-01 | 2026-06-03 | | ⏳ |
| Task 2.2: 多级缓存 | | 2026-06-03 | 2026-06-05 | | ⏳ |
| Task 2.3: Kafka批量 | | 2026-06-05 | 2026-06-06 | | ⏳ |

---

## 🎯 验收标准总结

### Phase 1 验收

- [ ] 系统可用性从95%提升到99%
- [ ] 故障恢复时间从5min缩短到1min
- [ ] 所有服务配置连接池
- [ ] 健康检查覆盖所有依赖

### Phase 2 验收

- [ ] 并发上传QPS从10提升到1000
- [ ] 问答P99延迟从5s降低到2s
- [ ] 文档处理吞吐量从10/s提升到500/s
- [ ] 缓存命中率 > 80%

---

**下一步**: 完成Phase 1和Phase 2后,进入Phase 3架构完善阶段。

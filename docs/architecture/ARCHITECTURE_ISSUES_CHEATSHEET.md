# gzDoc 架构问题与解决方案速查表

**用途**: 快速定位架构问题并找到对应解决方案  
**更新**: 2026-05-29

---

## 🔍 问题索引

### 按严重程度分类

#### 🔴 高风险 (立即解决)

| 编号 | 问题 | 影响 | 解决方案 | 文档位置 |
|------|------|------|---------|---------|
| P0-1 | 缺少熔断降级 | 雪崩风险 | Resilience4j | [Task 1.1](./QUICK_IMPROVEMENT_CHECKLIST.md#task-11-集成resilience4j熔断降级) |
| P0-2 | 同步阻塞I/O | 并发差 | 文件直传MinIO | [Task 2.1](./QUICK_IMPROVEMENT_CHECKLIST.md#task-21-实现文件直传minio) |
| P0-3 | 数据库连接池缺失 | 连接泄漏 | HikariCP配置 | [Task 1.2](./QUICK_IMPROVEMENT_CHECKLIST.md#task-12-配置数据库连接池) |
| P0-4 | Kafka无ACK机制 | 数据丢失 | 手动ACK + DLQ | [Task 2.3](./QUICK_IMPROVEMENT_CHECKLIST.md#task-23-kafka批量消费) |

#### 🟡 中风险 (本月解决)

| 编号 | 问题 | 影响 | 解决方案 | 文档位置 |
|------|------|------|---------|---------|
| P1-1 | 缺少缓存 | DB压力大 | Caffeine + Redis | [Task 2.2](./QUICK_IMPROVEMENT_CHECKLIST.md#task-22-添加多级缓存) |
| P1-2 | 硬编码服务地址 | 无法扩容 | Nacos服务发现 | [Phase 3](./ARCHITECTURE_EVOLUTION.md#phase-3-架构完善-month-2) |
| P1-3 | 单条Kafka消费 | 吞吐量低 | 批量消费 | [Task 2.3](./QUICK_IMPROVEMENT_CHECKLIST.md#task-23-kafka批量消费) |
| P1-4 | 简单向量检索 | 准确率低 | RRF + Rerank | [Section 3.4](./ARCHITECTURE_REVIEW.md#34-向量检索未优化) |

#### 🟢 低风险 (季度解决)

| 编号 | 问题 | 影响 | 解决方案 | 文档位置 |
|------|------|------|---------|---------|
| P2-1 | 贫血领域模型 | 维护难 | DDD重构 | [Section 4.1](./ARCHITECTURE_REVIEW.md#41-缺少领域驱动设计-ddd) |
| P2-2 | 无API版本管理 | 升级困难 | API版本前缀 | [Section 4.2](./ARCHITECTURE_REVIEW.md#42-缺少api版本管理) |
| P2-3 | 缺少分布式追踪 | 调试难 | Jaeger集成 | [Phase 3](./ARCHITECTURE_EVOLUTION.md#phase-3-架构完善-month-2) |
| P2-4 | 场景层未实现 | 架构价值未体现 | 创建finance模块 | [Phase 3](./ARCHITECTURE_EVOLUTION.md#phase-3-架构完善-month-2) |

---

## 🎯 按功能模块分类

### 1. API网关 (gzdoc-gateway)

#### 问题清单

| 问题 | 严重度 | 症状 | 解决方案 |
|------|--------|------|---------|
| 硬编码路由 | 🟡 | 新增服务需修改配置 | Nacos服务发现 |
| 缺少限流 | 🟡 | 恶意请求打垮后端 | Gateway限流过滤器 |
| 无灰度发布 | 🟢 | 全量发布风险高 | Istio流量管理 |

#### 快速修复

```yaml
# 当前配置 (有问题)
spring:
  cloud:
    gateway:
      routes:
        - id: gzdoc-auth
          uri: http://localhost:8081  # ❌ 硬编码

# 改进后配置
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    gateway:
      routes:
        - id: gzdoc-auth
          uri: lb://gzdoc-auth  # ✅ 服务名
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
```

**参考文档**: [ARCHITECTURE_REVIEW.md - Section 1.4](./ARCHITECTURE_REVIEW.md#14-缺少服务注册与发现)

---

### 2. 认证服务 (gzdoc-auth)

#### 问题清单

| 问题 | 严重度 | 症状 | 解决方案 |
|------|--------|------|---------|
| 每次登录查DB | 🟡 | 高频登录DB压力大 | 多级缓存 |
| BCrypt计算慢 | 🟡 | 登录耗时100ms+ | Argon2或缓存结果 |
| Token无刷新机制 | 🟢 | 用户体验差 | Refresh Token |

#### 快速修复

```java
// 当前代码 (有问题)
User user = userMapper.selectOne(wrapper);  // ❌ 每次都查DB

// 改进后代码
@Cacheable(value = "user:info", key = "#username")
public User getUserByUsername(String username) {
    return userMapper.selectByUsername(username);  // ✅ 先查缓存
}
```

**参考文档**: [QUICK_IMPROVEMENT_CHECKLIST.md - Task 2.2](./QUICK_IMPROVEMENT_CHECKLIST.md#task-22-添加多级缓存)

---

### 3. 文档服务 (gzdoc-document)

#### 问题清单

| 问题 | 严重度 | 症状 | 解决方案 |
|------|--------|------|---------|
| 同步上传MinIO | 🔴 | 大文件阻塞线程 | 预签名URL直传 |
| 无熔断保护 | 🔴 | MinIO故障雪崩 | Resilience4j |
| 缺少文件大小限制 | 🟡 | 超大文件拖垮系统 | @MaxUploadSize |
| 无进度回调 | 🟢 | 用户不知道进度 | WebSocket推送 |

#### 快速修复

```java
// 当前代码 (有问题)
public DocumentResponse upload(MultipartFile file, ...) {
    minioClient.putObject(...);  // ❌ 同步阻塞,传输文件流
    return response;
}

// 改进后代码
public PresignedUploadResponse getPresignedUrl(...) {
    String presignedUrl = minioClient.getPresignedObjectUrl(...);  // ✅ 生成URL
    return new PresignedUploadResponse(documentId, presignedUrl);  // 前端直传
}
```

**参考文档**: [QUICK_IMPROVEMENT_CHECKLIST.md - Task 2.1](./QUICK_IMPROVEMENT_CHECKLIST.md#task-21-实现文件直传minio)

---

### 4. 问答服务 (gzdoc-qa)

#### 问题清单

| 问题 | 严重度 | 症状 | 解决方案 |
|------|--------|------|---------|
| 简单向量检索 | 🟡 | 准确率68% | RRF融合 + Rerank |
| 无上下文管理 | 🟢 | 多轮对话体验差 | Conversation Memory |
| 缺少引用标注 | 🟢 | 用户无法验证答案 | 返回来源文档 |

#### 快速修复

```python
# 当前代码 (有问题)
response = collection.query.near_text(
    query=query,
    limit=limit,
)  # ❌ 仅向量检索

# 改进后代码
# Step 1: 向量检索 (Top 50)
vector_results = collection.query.near_text(query, limit=50)

# Step 2: 关键词检索 (Top 50)
keyword_results = collection.query.bm25(query, limit=50)

# Step 3: RRF融合
fused = rrf_fusion(vector_results, keyword_results)

# Step 4: Rerank (Top 10)
reranked = rerank(query, fused[:20], top_k=10)  # ✅ 准确率85%
```

**参考文档**: [ARCHITECTURE_REVIEW.md - Section 3.4](./ARCHITECTURE_REVIEW.md#34-向量检索未优化)

---

### 5. AI服务 (ai-service)

#### 问题清单

| 问题 | 严重度 | 症状 | 解决方案 |
|------|--------|------|---------|
| 单条Kafka消费 | 🔴 | 吞吐量10/s | 批量消费(batch=100) |
| 假健康检查 | 🔴 | K8s无法判断状态 | 深度健康检查 |
| PDF解析无异常处理 | 🟡 | 扫描件解析崩溃 | Try-Catch + 日志 |
| 无优雅停机 | 🟡 | 重启丢失请求 | shutdown hook |

#### 快速修复

```python
# 当前代码 (有问题)
@app.get("/health")
async def health():
    return {"status": "healthy"}  # ❌ 假健康检查

# 改进后代码
@app.get("/health/ready")
async def readiness_probe():
    redis_ok = await check_redis_health()
    weaviate_ok = await check_weaviate_health()
    
    if redis_ok and weaviate_ok:
        return {"status": "ready"}, 200  # ✅ 检查依赖
    else:
        return {"status": "not ready"}, 503
```

**参考文档**: [QUICK_IMPROVEMENT_CHECKLIST.md - Task 1.3](./QUICK_IMPROVEMENT_CHECKLIST.md#task-13-实现深度健康检查)

---

### 6. 数据库 (PostgreSQL)

#### 问题清单

| 问题 | 严重度 | 症状 | 解决方案 |
|------|--------|------|---------|
| 无连接池配置 | 🔴 | 连接泄漏 | HikariCP配置 |
| 单表无分区 | 🟡 | 千万级数据慢 | 按租户/时间分区 |
| 缺少索引 | 🟡 | 查询慢 | 添加复合索引 |
| 无主从复制 | 🟢 | 单点故障 | PostgreSQL HA |

#### 快速修复

```yaml
# 当前配置 (有问题)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gzdoc
    # ❌ 无连接池配置

# 改进后配置
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      connection-timeout: 30000
      leak-detection-threshold: 60000  # ✅ 泄漏检测
```

**参考文档**: [QUICK_IMPROVEMENT_CHECKLIST.md - Task 1.2](./QUICK_IMPROVEMENT_CHECKLIST.md#task-12-配置数据库连接池)

---

### 7. 缓存 (Redis)

#### 问题清单

| 问题 | 严重度 | 症状 | 解决方案 |
|------|--------|------|---------|
| 仅用于Token | 🟡 | 缓存利用率低 | 多级缓存 |
| 无本地缓存 | 🟡 | 网络开销大 | Caffeine L1缓存 |
| 无缓存预热 | 🟢 | 冷启动慢 | 定时预热热点数据 |

#### 快速修复

```java
// 当前代码 (有问题)
User user = userMapper.selectByUsername(username);  // ❌ 每次都查DB

// 改进后代码 - 两级缓存
// L1: Caffeine本地缓存 (1ms)
Cache l1Cache = localCacheManager.getCache("user:local");
User user = l1Cache.get(username, User.class);
if (user != null) return user;

// L2: Redis分布式缓存 (5ms)
Cache l2Cache = redisCacheManager.getCache("user:redis");
user = l2Cache.get(username, User.class);
if (user != null) {
    l1Cache.put(username, user);  // 回填L1
    return user;
}

// L3: 数据库 (50ms)
user = userMapper.selectByUsername(username);
l2Cache.put(username, user);
l1Cache.put(username, user);
return user;
```

**参考文档**: [QUICK_IMPROVEMENT_CHECKLIST.md - Task 2.2](./QUICK_IMPROVEMENT_CHECKLIST.md#task-22-添加多级缓存)

---

### 8. 消息队列 (Kafka)

#### 问题清单

| 问题 | 严重度 | 症状 | 解决方案 |
|------|--------|------|---------|
| 逐条消费 | 🔴 | 吞吐量10/s | 批量消费 |
| 自动ACK | 🔴 | 处理失败消息丢失 | 手动ACK |
| 无死信队列 | 🟡 | 失败消息无法追溯 | DLQ Topic |
| 无重试机制 | 🟡 | 临时故障直接失败 | Retry Policy |

#### 快速修复

```python
# 当前代码 (有问题)
consumer = KafkaConsumer(
    'document-parse',
    enable_auto_commit=True,  # ❌ 自动ACK
    max_poll_records=1,       # ❌ 单条消费
)

# 改进后代码
consumer = KafkaConsumer(
    'document-parse',
    enable_auto_commit=False,  # ✅ 手动ACK
    max_poll_records=100,      # ✅ 批量消费
)

messages = consumer.poll(timeout_ms=1000)
process_batch(messages)  # 批量处理
consumer.commit()        # 成功后提交
```

**参考文档**: [QUICK_IMPROVEMENT_CHECKLIST.md - Task 2.3](./QUICK_IMPROVEMENT_CHECKLIST.md#task-23-kafka批量消费)

---

## 📊 性能瓶颈分析

### 文档上传链路

```
用户上传 → Gateway → Document Service → MinIO → Kafka → AI Service
   ↓           ↓            ↓              ↓         ↓         ↓
  10ms       5ms         5000ms          5000ms     5ms     3000ms
                        (同步阻塞)      (同步阻塞)           (单条处理)

总耗时: ~13秒 (其中10秒是I/O阻塞)
```

**优化后**:
```
用户上传 → Gateway → Document Service → MinIO (前端直传)
   ↓           ↓            ↓              ↓
  10ms       5ms          50ms          并行执行
                        (生成URL)
                        
AI Service ← Kafka (批量消费)
   ↓              ↓
  100ms         200ms (100条/批)

总耗时: ~200ms (提升65倍)
```

---

### 问答响应链路

```
用户提问 → Gateway → QA Service → Weaviate → LLM → 返回
   ↓           ↓           ↓           ↓        ↓      ↓
  10ms       5ms        500ms       2000ms   2000ms   10ms

总耗时: ~4.5秒
```

**优化后**:
```
用户提问 → Gateway → QA Service → Cache Hit? → 返回
   ↓           ↓           ↓           ↓        ↓
  10ms       5ms         5ms         Yes      10ms
                          ↓
                         No
                          ↓
              Weaviate (RRF) → LLM → 返回
                  ↓             ↓      ↓
                500ms        1500ms   10ms

缓存命中: ~30ms (提升150倍)
缓存未命中: ~2秒 (提升2.25倍)
```

---

## 🛠️ 工具箱

### 诊断命令

#### 1. 检查服务健康状态

```bash
# Java服务
curl http://localhost:8081/actuator/health

# Python服务
curl http://localhost:8000/health

# PostgreSQL
docker exec gzdoc-postgres-test pg_isready -U gzdoc

# Redis
docker exec gzdoc-redis-test redis-cli ping

# MinIO
curl http://localhost:9000/minio/health/live

# Weaviate
curl http://localhost:8080/v1/.well-known/ready
```

#### 2. 性能测试

```bash
# 并发上传测试 (ab工具)
ab -n 100 -c 10 -p upload.json -T application/json \
   http://localhost:8082/api/document/upload

# 问答压力测试 (wrk工具)
wrk -t12 -c400 -d30s http://localhost:8083/api/qa/ask \
   -s qa_test.lua

# 数据库连接池监控
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
```

#### 3. 日志分析

```bash
# 查看错误日志
tail -f logs/error.log | grep "ERROR"

# 统计异常类型
grep "Exception" logs/app.log | awk '{print $NF}' | sort | uniq -c | sort -rn

# 慢查询分析
grep "took.*ms" logs/db.log | awk '{print $(NF-1)}' | sort -rn | head -20
```

---

### 监控指标

#### Prometheus关键指标

```promql
# 1. 请求QPS
rate(http_server_requests_seconds_count[5m])

# 2. P99延迟
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))

# 3. 错误率
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / 
rate(http_server_requests_seconds_count[5m])

# 4. 连接池使用率
hikaricp_connections_active / hikaricp_connections_max

# 5. 缓存命中率
rate(caffeine_cache_hit_total[5m]) / 
(rate(caffeine_cache_hit_total[5m]) + rate(caffeine_cache_miss_total[5m]))
```

---

## 📖 延伸阅读

### 按角色推荐阅读

#### 开发工程师
1. [快速改进清单](./QUICK_IMPROVEMENT_CHECKLIST.md) - 逐步骤实施指南
2. [ARCHITECTURE_REVIEW.md - Section 2](./ARCHITECTURE_REVIEW.md#2-高稳定性评估) - 稳定性最佳实践
3. [ARCHITECTURE_REVIEW.md - Section 3](./ARCHITECTURE_REVIEW.md#3-高并发能力评估) - 性能优化技巧

#### 架构师
1. [ARCHITECTURE_REVIEW.md](./ARCHITECTURE_REVIEW.md) - 完整架构分析
2. [ARCHITECTURE_EVOLUTION.md](./ARCHITECTURE_EVOLUTION.md) - 演进路线图
3. [ARCHITECTURE_REVIEW.md - Section 4](./ARCHITECTURE_REVIEW.md#4-架构设计能力评估) - 设计模式

#### 技术负责人
1. [ARCHITECTURE_REVIEW_SUMMARY.md](./ARCHITECTURE_REVIEW_SUMMARY.md) - 执行摘要
2. [ARCHITECTURE_EVOLUTION.md - 成本估算](./ARCHITECTURE_EVOLUTION.md#-成本估算) - 资源规划
3. [ARCHITECTURE_REVIEW_SUMMARY.md - 风险评估](./ARCHITECTURE_REVIEW_SUMMARY.md#-风险评估) - 风险管理

---

## 🔄 更新记录

| 日期 | 版本 | 更新内容 | 作者 |
|------|------|---------|------|
| 2026-05-29 | v1.0 | 初始版本,包含P0-P2问题 | AI架构顾问 |

---

## 💬 FAQ

**Q1: 为什么要先做Phase 1而不是直接优化性能?**  
A: 稳定性是性能的前提。如果系统频繁故障,再高的性能也没用。先确保99%可用性,再优化到1000 QPS。

**Q2: 能否跳过某些任务?**  
A: 不建议。每个任务都是基于实际问题分析得出的,跳过可能导致后续问题。但如果资源有限,可以优先做P0任务。

**Q3: 改进后如何验证效果?**  
A: 每个任务都有验收标准。建议使用JMeter或wrk进行压力测试,对比改进前后的指标。

**Q4: 是否需要重写现有代码?**  
A: 不需要。大部分改进可以通过增量方式实现,如添加注解、配置、新接口等。渐进式重构比推倒重来更安全。

**Q5: 改进期间是否影响业务?**  
A: Phase 1和Phase 2的改进都是向后兼容的,不会影响现有功能。建议灰度发布,小流量验证后再全量。

---

**最后提醒**: 这份速查表是动态更新的,随着改进推进会持续补充新问题和新方案。建议收藏并定期查看更新。

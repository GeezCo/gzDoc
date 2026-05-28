# GzDoc 系统架构设计

## 1. 架构概览

GzDoc 采用**"平台 + 插件"微服务架构**，分为五层：

1. **用户层**: Web端、移动端、开放API
2. **接入层**: Nginx + API Gateway
3. **业务服务层**: 
   - **平台层**（通用能力）：用户、文档、问答基础服务
   - **场景层**（垂直插件）：金融、法律、医疗等垂直场景
4. **AI服务层**: Python FastAPI
5. **数据层**: PostgreSQL + Weaviate + Redis + MinIO + Kafka + ES

### 1.1 核心设计理念

**"平台化 + 垂直场景插件化"**

```
┌─────────────────────────────────────────────────────────────┐
│                    通用平台层（稳定、不变）                   │
├─────────────────────────────────────────────────────────────┤
│  gzdoc-common    │  通用工具、实体、常量                      │
│  gzdoc-gateway   │  统一网关、鉴权、限流                      │
│  gzdoc-auth      │  用户、租户、权限管理                      │
│  gzdoc-document  │  文档上传、存储、管理                      │
│  gzdoc-qa        │  通用问答、RAG基础能力                     │
└─────────────────────────────────────────────────────────────┘
                              ↓ 依赖
┌─────────────────────────────────────────────────────────────┐
│                  垂直场景层（插件式、可扩展）                 │
├─────────────────────────────────────────────────────────────┤
│  gzdoc-finance   │  金融研报：解析、对比、投资建议            │
│  gzdoc-legal     │  法律合同：条款提取、风险识别（未来）      │
│  gzdoc-medical   │  医疗影像：报告解读、对比分析（未来）      │
│  gzdoc-engineer  │  工程图纸：规范审查、变更追踪（未来）      │
└─────────────────────────────────────────────────────────────┘
```

**关键特性：**
- ✅ **不侵入**：场景层不修改平台层代码
- ✅ **可复用**：场景层复用平台层通用能力
- ✅ **独立部署**：场景服务可独立扩展、按需部署
- ✅ **灵活定价**：按场景模块收费

---

## 2. 核心服务

### 2.1 平台层服务 (Java) - 通用能力

#### gzdoc-gateway (API网关)
- **职责**: 统一入口、路由转发、鉴权、限流
- **技术**: Spring Cloud Gateway
- **端口**: 8080
- **特点**: 所有请求统一入口，路由到平台层或场景层

#### gzdoc-auth (用户服务)
- **职责**: 用户注册登录、权限管理、租户管理
- **技术**: Spring Security + JWT
- **端口**: 8081
- **特点**: 平台级服务，所有场景共享

#### gzdoc-document (文档服务)
- **职责**: 文档上传、管理、预览
- **技术**: Spring Boot + MinIO
- **端口**: 8082
- **特点**: 通用文档能力，支持所有场景

#### gzdoc-qa (问答服务)
- **职责**: 问答接口、历史记录、反馈管理
- **技术**: Spring Boot + gRPC (调用AI服务)
- **端口**: 8083
- **特点**: 基础RAG能力，场景层可扩展

#### gzdoc-common (公共模块)
- **职责**: 公共工具类、实体类、常量
- **特点**: 所有服务的基础依赖

---

### 2.2 场景层服务 (Java) - 垂直插件

#### gzdoc-finance (金融研报服务) ⭐ 首个场景
- **职责**: 
  - 研报解析（提取基本信息、财务数据、估值）
  - 横向对比（多研报对比分析）
  - 趋势分析（时间序列分析）
  - 投资建议（AI生成）
- **技术**: Spring Boot + 复用平台层服务
- **端口**: 8084
- **依赖**: 
  - gzdoc-common（公共能力）
  - gzdoc-document（文档管理）
  - gzdoc-qa（问答能力）
- **数据表**: 
  - t_finance_report（研报表）
  - t_finance_data（财务数据）
  - t_finance_comparison（对比记录）

**服务调用示例：**
```java
@Service
public class FinanceReportService {
    @Autowired
    private DocumentService documentService;  // 复用平台层
    @Autowired
    private AIService aiService;              // 复用平台层
    
    public void processReport(Long reportId) {
        // 1. 获取文档（平台层）
        Document doc = documentService.getDocument(reportId);
        
        // 2. 金融专用解析
        ReportData data = parseFinanceReport(doc);
        
        // 3. 保存金融数据（场景层）
        saveFinanceData(data);
    }
}
```

#### gzdoc-legal (法律合同服务) - 未来规划
- **职责**: 合同条款提取、风险识别、合同对比
- **端口**: 8085

#### gzdoc-medical (医疗影像服务) - 未来规划
- **职责**: 影像报告解读、历史对比、就医建议
- **端口**: 8086

---

### 2.3 AI服务 (Python)

#### ai-service
- **职责**: OCR、文档解析、Embedding、向量检索、LLM调用
- **技术**: FastAPI + LangChain + PaddleOCR
- **端口**: 8000

**核心模块**:
- `app/api`: API路由
- `app/core`: 核心配置
- `app/models`: 数据模型
- `app/services`: 业务逻辑
  - **通用服务**:
    - `ocr_service.py`: OCR识别
    - `parser_service.py`: 文档解析
    - `embedding_service.py`: 向量化
    - `retrieval_service.py`: 向量检索
    - `llm_service.py`: LLM调用
    - `rag_service.py`: RAG编排
  - **场景服务** (新增):
    - `finance/`: 金融专用AI能力
      - `report_parser.py`: 研报解析
      - `table_extractor.py`: 表格提取
      - `entity_recognizer.py`: 实体识别
      - `comparison_engine.py`: 对比引擎
      - `investment_advisor.py`: 投资建议

---

## 3. 数据流

### 3.1 通用文档处理流程

```
用户上传文档
    ↓
文档服务 (gzdoc-document) - 平台层
    ↓
存储到 MinIO
    ↓
发送消息到 Kafka (topic: document-upload)
    ↓
AI服务消费消息
    ↓
1. 格式检测
2. OCR识别 (如果是图片)
3. 文档解析 (Unstructured)
4. 智能分块
5. Embedding生成 (BGE-M3)
6. 向量入库 (Weaviate)
7. 元数据提取
8. 全文索引 (ElasticSearch)
    ↓
更新文档状态 (处理完成)
    ↓
通知用户 (WebSocket)
```

---

### 3.2 金融研报处理流程（场景层扩展）

```
用户上传金融研报
    ↓
API Gateway → 金融服务 (gzdoc-finance)
    ↓
1. 调用文档服务上传（复用平台层）
   documentService.upload()
    ↓
2. 发送消息到 Kafka (topic: finance-report-upload)
    ↓
3. AI服务消费消息（场景专用处理）
   ├─ 研报解析（提取基本信息）
   ├─ 表格识别（财务数据）
   ├─ 实体提取（公司名、指标）
   └─ 向量化（复用平台层）
    ↓
4. 保存金融专用数据
   ├─ t_finance_report（研报表）
   └─ t_finance_data（财务数据）
    ↓
5. 更新状态 + 通知用户
```

**关键点：**
- ✅ 复用平台层的文档上传、存储能力
- ✅ 扩展场景专用的解析、提取能力
- ✅ 数据分层存储（通用表 + 场景表）

---

### 3.3 智能问答流程（平台层 + 场景层）

#### 通用问答流程
```
用户提问
    ↓
问答服务 (gzdoc-qa) - 平台层
    ↓
调用 AI服务 (gRPC)
    ↓
1. 问题理解
   - 意图识别
   - 实体提取
   - Query扩展
    ↓
2. 混合检索
   - 向量检索 (Weaviate) - Top 50
   - 全文检索 (ES) - Top 50
   - 结果融合 (RRF)
   - Rerank (BGE-Reranker) - Top 10
    ↓
3. 答案生成
   - 上下文构建 (Top 5)
   - Prompt组装
   - LLM调用 (流式输出)
   - 答案后处理
    ↓
返回答案 + 引用
    ↓
保存问答历史
```

#### 金融问答流程（场景层增强）
```
用户提问（关于某只股票）
    ↓
金融服务 (gzdoc-finance) - 场景层
    ↓
1. 场景过滤（只查询该股票的研报）
   reportIds = getReportsByStockCode(stockCode)
    ↓
2. 调用平台层问答服务
   response = qaService.ask(question, reportIds)
    ↓
3. 场景增强（添加金融专用信息）
   ├─ 最新财务数据
   ├─ 估值数据
   └─ 历史趋势
    ↓
返回增强后的答案
```

---

## 4. 数据库设计

### 4.1 PostgreSQL (业务数据)

#### 平台层数据表

##### 用户表 (t_user)
```sql
CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    tenant_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

##### 租户表 (t_tenant)
```sql
CREATE TABLE t_tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    plan VARCHAR(20) NOT NULL,
    status SMALLINT DEFAULT 1,
    expired_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

##### 文档表 (t_document) - 通用
```sql
CREATE TABLE t_document (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    page_count INT,
    scene_type VARCHAR(20),  -- 场景类型：finance/legal/medical/general
    status SMALLINT DEFAULT 0, -- 0:上传中 1:处理中 2:完成 3:失败
    process_progress INT DEFAULT 0,
    error_msg TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_scene_type (scene_type)
);
```

##### 问答记录表 (t_qa_record) - 通用
```sql
CREATE TABLE t_qa_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    scene_type VARCHAR(20),  -- 场景类型
    question TEXT NOT NULL,
    answer TEXT,
    sources JSONB, -- 引用来源
    feedback SMALLINT, -- 1:好 -1:差
    token_count INT,
    cost DECIMAL(10, 4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_tenant_scene (tenant_id, scene_type)
);
```

---

#### 场景层数据表 - 金融

##### 研报表 (t_finance_report)
```sql
CREATE TABLE t_finance_report (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,  -- 关联通用文档表
    
    -- 基本信息
    title VARCHAR(500) NOT NULL,
    institution VARCHAR(100),      -- 机构（中信证券）
    analyst VARCHAR(100),          -- 分析师
    publish_date DATE,
    
    -- 核心数据
    stock_code VARCHAR(20),        -- 股票代码（600519）
    stock_name VARCHAR(100),       -- 股票名称（贵州茅台）
    rating VARCHAR(20),            -- 评级（买入/增持/中性）
    target_price DECIMAL(10, 2),   -- 目标价
    
    -- 结构化数据（JSON）
    core_views JSONB,              -- 核心观点
    financial_data JSONB,          -- 财务数据
    valuation JSONB,               -- 估值数据
    risks JSONB,                   -- 风险提示
    
    -- 状态
    parse_status SMALLINT DEFAULT 0, -- 0:未解析 1:解析中 2:完成 3:失败
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (document_id) REFERENCES t_document(id),
    INDEX idx_stock_code (stock_code),
    INDEX idx_publish_date (publish_date),
    INDEX idx_institution (institution)
);
```

##### 财务数据表 (t_finance_data) - 时间序列
```sql
CREATE TABLE t_finance_data (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    
    -- 时间
    year INT NOT NULL,
    quarter SMALLINT,              -- 1/2/3/4 或 NULL（年度）
    
    -- 财务指标
    revenue DECIMAL(15, 2),        -- 营收（亿元）
    net_profit DECIMAL(15, 2),     -- 净利润
    gross_margin DECIMAL(5, 2),    -- 毛利率
    roe DECIMAL(5, 2),             -- ROE
    
    -- 估值指标
    pe DECIMAL(10, 2),
    pb DECIMAL(10, 2),
    ps DECIMAL(10, 2),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (report_id) REFERENCES t_finance_report(id),
    UNIQUE (stock_code, year, quarter),
    INDEX idx_stock_year (stock_code, year)
);
```

##### 对比记录表 (t_finance_comparison)
```sql
CREATE TABLE t_finance_comparison (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    stock_code VARCHAR(20) NOT NULL,
    report_ids BIGINT[],           -- 对比的研报ID数组
    comparison_result JSONB,       -- 对比结果
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_stock_code (stock_code)
);
```

**设计要点：**
- ✅ 通用表（t_document, t_qa_record）支持所有场景
- ✅ 场景表（t_finance_*）通过 document_id 关联通用表
- ✅ 场景表独立，不影响平台层
- ✅ 支持未来扩展（t_legal_*, t_medical_*）

---

### 4.2 Weaviate (向量数据)

#### 通用 Schema: Document
```json
{
  "class": "Document",
  "description": "通用文档向量数据",
  "properties": [
    {"name": "tenant_id", "dataType": ["int"]},
    {"name": "document_id", "dataType": ["int"]},
    {"name": "scene_type", "dataType": ["string"]},
    {"name": "chunk_id", "dataType": ["string"]},
    {"name": "content", "dataType": ["text"]},
    {"name": "page_number", "dataType": ["int"]},
    {"name": "metadata", "dataType": ["object"]}
  ],
  "vectorizer": "none"
}
```

#### 场景 Schema: FinanceReport
```json
{
  "class": "FinanceReport",
  "description": "金融研报向量数据（场景专用）",
  "properties": [
    {"name": "tenant_id", "dataType": ["int"]},
    {"name": "report_id", "dataType": ["int"]},
    {"name": "document_id", "dataType": ["int"]},
    
    {"name": "stock_code", "dataType": ["string"]},
    {"name": "stock_name", "dataType": ["string"]},
    {"name": "institution", "dataType": ["string"]},
    {"name": "publish_date", "dataType": ["date"]},
    
    {"name": "chunk_id", "dataType": ["string"]},
    {"name": "chunk_type", "dataType": ["string"]},
    {"name": "content", "dataType": ["text"]},
    {"name": "page_number", "dataType": ["int"]},
    
    {"name": "metadata", "dataType": ["object"]}
  ],
  "vectorizer": "none",
  "moduleConfig": {
    "generative-openai": {}
  }
}
```

**设计要点：**
- ✅ 通用 Schema 支持所有场景
- ✅ 场景 Schema 添加专用字段（股票代码、机构等）
- ✅ chunk_type 分类（核心观点/财务数据/风险提示）
- ✅ 支持按场景过滤检索

---

## 5. 技术选型理由

### 5.1 为什么选"平台+插件"架构？
- ✅ **快速试错**：新场景不行可以直接删除模块
- ✅ **灵活定价**：按场景收费（基础版 + 金融版 + 法律版）
- ✅ **降低风险**：场景层不影响平台层稳定性
- ✅ **并行开发**：多个场景可以同时开发
- ✅ **技术债低**：废弃场景直接删除，不留遗留代码

### 5.2 为什么选Spring Cloud？
- 成熟的微服务生态
- 丰富的组件支持
- 社区活跃，文档完善
- 适合企业级应用

### 5.3 为什么选FastAPI？
- 高性能（基于Starlette + Pydantic）
- 自动生成API文档
- 类型提示支持
- 异步支持

### 5.4 为什么选Weaviate？
- 原生支持混合检索
- 性能优秀
- 支持多租户
- 易于部署

### 5.5 为什么选Kafka？
- 高吞吐量
- 持久化消息
- 支持消息回溯
- 适合异步处理

---

## 6. 场景扩展示例

### 6.1 如何新增一个场景？

以"法律合同"场景为例：

#### Step 1: 创建服务模块
```bash
cd backend
mkdir gzdoc-legal
# 复制 gzdoc-finance 的结构
```

#### Step 2: 定义数据表
```sql
CREATE TABLE t_legal_contract (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    contract_type VARCHAR(50),
    parties JSONB,
    clauses JSONB,
    risks JSONB,
    ...
    FOREIGN KEY (document_id) REFERENCES t_document(id)
);
```

#### Step 3: 实现业务逻辑
```java
@Service
public class LegalContractService {
    @Autowired
    private DocumentService documentService;  // 复用平台层
    
    public void processContract(Long contractId) {
        // 1. 获取文档
        Document doc = documentService.getDocument(contractId);
        
        // 2. 法律专用解析
        ContractData data = parseContract(doc);
        
        // 3. 保存法律数据
        saveLegalData(data);
    }
}
```

#### Step 4: 添加AI能力
```python
# ai-service/app/services/legal/
# - contract_parser.py
# - clause_extractor.py
# - risk_analyzer.py
```

#### Step 5: 部署
```yaml
# docker-compose.yml
gzdoc-legal:
  image: gzdoc-legal:1.0
  ports: ["8085:8085"]
```

**关键：全程不需要修改平台层代码！**

---

## 7. 非功能需求

### 7.1 性能指标
- 文档上传: 支持100MB文件
- 文档处理: 100页PDF < 30秒
- 问答响应: P99 < 3秒
- 并发支持: 1000 QPS
- 系统可用性: 99.9%

### 7.2 安全要求
- 数据加密: TLS 1.3 + AES-256
- 身份认证: JWT + 双因素认证
- 权限控制: RBAC + 数据权限
- 审计日志: 所有操作可追溯

### 7.3 可扩展性
- 水平扩展: 所有服务无状态
- 数据库: 读写分离 + 分库分表
- 缓存: Redis Cluster
- 存储: MinIO分布式部署
- **场景扩展**: 插件式新增场景，不影响现有服务

---

## 8. 部署架构

### 8.1 开发环境
- Docker Compose
- 单机部署
- 快速启动

### 8.2 生产环境
- Kubernetes
- 多节点部署
- 高可用
- **按场景部署**: 可选择性部署场景服务

```yaml
# 示例：只部署金融场景
kubectl apply -f k8s/platform/  # 平台层（必须）
kubectl apply -f k8s/finance/   # 金融场景（可选）
# kubectl apply -f k8s/legal/   # 法律场景（不部署）
```

---

## 9. 监控体系

### 9.1 业务监控
- 文档处理成功率
- 问答准确率
- 响应时间 (P50/P95/P99)
- Token消耗趋势
- **场景监控**: 各场景独立监控指标

### 9.2 系统监控
- 服务健康检查
- CPU/内存/磁盘
- 数据库连接池
- 消息队列积压

### 9.3 AI监控
- 模型推理延迟
- 向量检索耗时
- LLM调用失败率
- 幻觉检测

---

## 10. 商业模式

### 10.1 按场景定价

```
基础版：$99/月
- 平台层所有功能
- 通用文档管理
- 基础问答

金融版：$499/月
- 基础版 +
- 研报解析
- 横向对比
- 投资建议

企业版：$2999/月
- 金融版 +
- 法律合同
- 医疗影像
- 私有部署
```

### 10.2 按需部署

客户可以选择：
- 只部署平台层（通用能力）
- 平台层 + 金融场景
- 平台层 + 法律场景
- 平台层 + 多个场景

**优势：**
- ✅ 降低客户成本（按需付费）
- ✅ 降低运维成本（不部署不用的服务）
- ✅ 提高销售灵活性（模块化销售）

---

## 11. 架构演进路线

### Phase 1: MVP (Month 1-3)
- ✅ 平台层基础服务
- ✅ 通用文档处理
- ✅ 基础RAG问答

### Phase 2: 首个场景 (Month 4-6)
- ✅ 金融研报场景
- ✅ 验证"平台+插件"架构
- ✅ 积累场景开发经验

### Phase 3: 场景扩展 (Month 7-9)
- ✅ 法律合同场景
- ✅ 医疗影像场景
- ✅ 完善平台能力

### Phase 4: 商业化 (Month 10-12)
- ✅ 多场景组合销售
- ✅ 私有化部署方案
- ✅ 企业级特性

---

## 12. 下一步

- [x] 架构设计完成
- [ ] 详细设计每个服务的API
- [ ] 设计数据库表结构（已完成基础设计）
- [ ] 设计消息队列Topic
- [ ] 设计缓存策略
- [ ] 设计监控指标
- [ ] **开始实施**: 创建 gzdoc-finance 模块

---

## 13. 相关文档

- [API设计文档](../api/README.md)
- [开发指南](../development/README.md)
- [部署文档](../deployment/README.md)
- [金融场景详细设计](./finance-design.md) (待创建)


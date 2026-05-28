# GzDoc AI Service

基于 FastAPI + OpenAI + Weaviate 的智能文档处理服务。

## 技术栈

- FastAPI - Web框架
- OpenAI API - LLM和Embedding
- Weaviate - 向量数据库
- Kafka - 消息队列
- PostgreSQL - 关系数据库
- MinIO - 对象存储
- Redis - 缓存

## 目录结构

```
app/
├── api/                 # API路由
│   ├── qa.py           # 问答接口
│   └── document.py     # 文档处理接口
├── services/           # 业务服务
│   ├── qa_service.py   # 问答服务
│   ├── vector_service.py # 向量服务
│   └── document_parser.py # 文档解析
├── models/             # 数据模型
│   └── schemas.py      # Pydantic模型
├── utils/              # 工具类
│   ├── weaviate_client.py # Weaviate客户端
│   └── minio_client.py    # MinIO客户端
├── config.py           # 配置管理
├── main.py             # 应用入口
└── consumer.py         # Kafka消费者
```

## 安装

```bash
# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate  # Windows

# 安装依赖
pip install -r requirements.txt
```

## 配置

复制 `.env.example` 为 `.env` 并配置：

```bash
cp .env.example .env
```

必须配置的环境变量：
- `OPENAI_API_KEY` - OpenAI API密钥
- `DATABASE_URL` - PostgreSQL连接字符串
- `WEAVIATE_URL` - Weaviate服务地址
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka服务地址
- `MINIO_ENDPOINT` - MinIO服务地址

## 运行

### 启动API服务

```bash
python -m app.main
# 或
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 启动Kafka消费者

```bash
python -m app.consumer
```

## API文档

启动服务后访问：
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## 核心功能

### 1. 文档解析
支持的文件格式：
- PDF (.pdf)
- Word (.docx, .doc)
- Excel (.xlsx, .xls)
- PowerPoint (.pptx, .ppt)
- 文本 (.txt)

### 2. 向量化
- 使用OpenAI Embedding API生成文本向量
- 存储到Weaviate向量数据库
- 支持多租户隔离

### 3. RAG问答
- 基于向量检索相关文档
- 使用GPT-4生成答案
- 支持会话上下文
- 返回相关文档引用

### 4. 异步处理
- Kafka消费者监听文档上传事件
- 自动解析和向量化
- 更新数据库状态

## 开发

### 初始化Weaviate Schema

```python
from app.utils.weaviate_client import init_weaviate_schema
init_weaviate_schema()
```

### 测试

```bash
pytest tests/
```

## 部署

使用Docker部署：

```bash
docker build -t gzdoc-ai-service .
docker run -d -p 8000:8000 --env-file .env gzdoc-ai-service
```

## 注意事项

1. OpenAI API调用需要科学上网或使用代理
2. Weaviate需要预先启动并配置
3. 生产环境建议使用Redis存储会话
4. 大文件解析可能需要较长时间

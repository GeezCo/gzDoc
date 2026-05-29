# GzDoc - 企业级智能文档处理平台

<div align="center">

**"平台+插件"架构 · 通用能力 + 垂直场景**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![Python](https://img.shields.io/badge/Python-3.11-yellow.svg)](https://www.python.org/)

[English](README_EN.md) | 简体中文

</div>

---

## 📖 项目简介

GzDoc 是一个基于"平台+插件"架构的智能文档处理平台，提供通用的文档管理和RAG能力，同时支持垂直场景的深度定制。

### 为什么选择 GzDoc？

- 🎯 **垂直场景优先**: 不做通用RAG，专注金融、法律等垂直领域
- 🧩 **插件化架构**: 平台层稳定复用，场景层灵活扩展
- 🚀 **快速上线**: 复用平台能力，新场景2周上线
- 💰 **灵活定价**: 基础版 + 场景模块，按需购买

### 核心能力

#### 平台层（通用能力）
- 📄 **文档管理**: 上传、存储、版本控制
- 👥 **用户系统**: 多租户、RBAC权限
- 🔍 **基础RAG**: 向量检索、问答生成
- 🔐 **企业安全**: 数据加密、审计日志

#### 场景层（垂直能力）
- 💼 **金融研报** (gzdoc-finance): 研报解析、横向对比、投资建议
- ⚖️ **法律合同** (gzdoc-legal): 条款提取、风险识别 *(规划中)*
- 🏥 **医疗影像** (gzdoc-medical): 报告解读、对比分析 *(规划中)*

---

## 🏗️ 架构设计

### "平台+插件"架构

```
┌─────────────────────────────────────────────────────────────┐
│                    通用平台层（稳定、复用）                   │
├─────────────────────────────────────────────────────────────┤
│  gzdoc-common    │  通用工具、实体、常量                      │
│  gzdoc-gateway   │  统一网关、鉴权、限流                      │
│  gzdoc-auth      │  用户、租户、权限管理                      │
│  gzdoc-document  │  文档上传、存储、管理                      │
│  gzdoc-qa        │  通用问答、RAG基础能力                     │
└─────────────────────────────────────────────────────────────┘
                              ↓ 依赖
┌─────────────────────────────────────────────────────────────┐
│                  垂直场景层（灵活、可扩展）                   │
├─────────────────────────────────────────────────────────────┤
│  gzdoc-finance   │  金融研报：解析、对比、投资建议            │
│  gzdoc-legal     │  法律合同：条款提取、风险识别（规划中）    │
│  gzdoc-medical   │  医疗影像：报告解读、对比分析（规划中）    │
└─────────────────────────────────────────────────────────────┘
```

**核心优势：**
- ✅ 场景层不侵入平台层代码
- ✅ 新场景2周上线，无需修改平台
- ✅ 独立部署，按需扩展
- ✅ 灵活定价，按场景收费

详见：[架构设计文档](docs/architecture/README.md)

---

## 🛠️ 技术栈

### 后端
- **平台层**: Spring Boot 3.2 + Spring Cloud + MyBatis-Plus
- **场景层**: FastAPI + LangChain
- **消息队列**: Kafka
- **缓存**: Redis 7

### 前端
- React 18 + TypeScript + Vite
- Ant Design Pro
- Zustand + React Query

### AI & 数据
- **向量数据库**: Weaviate
- **Embedding**: BGE-M3
- **LLM**: OpenAI GPT-4 / 本地模型
- **OCR**: PaddleOCR
- **对象存储**: MinIO
- **关系数据库**: PostgreSQL 15

### 基础设施
- Docker + Docker Compose
- Kubernetes (生产环境)
- Prometheus + Grafana (监控)
- ELK Stack (日志)

---

## 🚀 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Python 3.11+
- Docker & Docker Compose
- 8GB+ RAM

### 本地开发

```bash
# 1. 克隆项目
git clone https://github.com/yourusername/gzdoc.git
cd gzdoc

# 2. 启动基础服务（PostgreSQL, Redis, Weaviate, MinIO, Kafka）
cd infrastructure/docker
docker-compose up -d

# 3. 启动后端服务
cd ../../backend
# 详见 backend/README.md

# 4. 启动AI服务
cd ../ai-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload

# 5. 启动前端
cd ../frontend
npm install
npm run dev
```

访问 http://localhost:5173

---

## 📚 文档

- [架构设计](docs/architecture/) - "平台+插件"架构详解
- [开发指南](docs/development/) - 如何开发新的场景插件
- [API文档](docs/api/) - REST API接口文档
- [部署文档](docs/deployment/) - Docker/K8s部署指南
- [工作区指南](docs/workspace-guide.md) - Monorepo工作区管理

---

## 🎯 金融场景示例

### 研报横向对比

```bash
POST /finance/reports/compare
{
  "stock_code": "600519",
  "report_ids": ["report1", "report2", "report3"]
}
```

**返回：**
- 评级一致性分析
- 目标价对比
- 财务预测差异
- AI生成的投资建议

详见：[金融场景文档](docs/scenarios/finance.md)

---

## 🤝 贡献指南

欢迎贡献代码、文档或新的场景插件！

### 如何添加新场景？

1. 创建场景服务（如 `gzdoc-legal`）
2. 实现场景专用逻辑
3. 在 `plugins.yml` 注册插件
4. 提交 Pull Request

详见：[贡献指南](CONTRIBUTING.md)

---

## 📄 许可证

[MIT License](LICENSE)

---

## 📧 联系方式

- 项目主页: https://github.com/yourusername/gzdoc
- 问题反馈: https://github.com/yourusername/gzdoc/issues
- 博客: https://yourblog.com

---

## 🌟 Star History

如果这个项目对你有帮助，请给个 Star！⭐

---

## 📖 相关文章

- [从通用RAG到垂直场景：平台+插件架构设计](https://yourblog.com/platform-plugin-architecture)
- [金融研报解析：从PDF到结构化数据](https://yourblog.com/finance-report-parsing) *(即将发布)*
- [RAG成本优化：如何把Token成本降低90%](https://yourblog.com/rag-cost-optimization) *(即将发布)*

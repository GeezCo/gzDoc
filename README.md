# GzDoc - 企业级智能文档处理平台

<div align="center">

**基于AI的智能文档管理与问答系统**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![Python](https://img.shields.io/badge/Python-3.11-yellow.svg)](https://www.python.org/)

</div>

---

## 📖 项目简介

GzDoc 是一个面向企业的智能文档处理平台，提供文档上传、智能解析、向量化存储和基于RAG的智能问答功能。

### 核心功能

- 📄 **智能文档处理**: 支持PDF/Word/Excel/图片，自动OCR识别
- 🔍 **智能问答**: 基于RAG技术的文档问答系统
- 🏢 **多租户架构**: 完善的租户隔离和权限管理
- 💰 **计费系统**: 灵活的套餐制和按量付费
- 📊 **数据可视化**: 实时监控和数据分析
- 🔐 **企业级安全**: 数据加密、审计日志、RBAC权限

### 应用场景

- **律所**: 合同审查、案例检索
- **制造业**: 设备手册智能问答
- **咨询公司**: 项目文档管理
- **金融机构**: 研报分析、风控文档

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        用户层                                │
│  Web端(React)    移动端(React Native)    开放API(REST)      │
└─────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────┐
│                      接入层                                  │
│  Nginx (负载均衡 + SSL)  +  API Gateway (鉴权 + 限流)        │
└─────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────┐
│                    业务服务层 (Spring Cloud)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 用户服务      │  │ 文档服务      │  │ 问答服务      │      │
│  │ (Auth/RBAC)  │  │ (Upload/OCR) │  │ (RAG/LLM)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────┐
│                    AI服务层 (Python)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ OCR服务       │  │ Embedding服务 │  │ LLM服务       │      │
│  │ (PaddleOCR)  │  │ (BGE-M3)     │  │ (OpenAI/本地) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────┐
│                    数据层                                    │
│  PostgreSQL   Weaviate   Redis   MinIO   Kafka   ES         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ 技术栈

### 前端
- React 18 + TypeScript
- Ant Design Pro
- Zustand (状态管理)
- React Query (数据获取)
- Vite (构建工具)

### 后端 (Java)
- Spring Boot 3.2
- Spring Cloud (Nacos + Gateway + OpenFeign)
- Spring Security + JWT
- MyBatis-Plus
- Redis + Kafka

### AI服务 (Python)
- FastAPI
- LangChain
- PaddleOCR
- Transformers
- BGE-M3 (Embedding)

### 数据存储
- PostgreSQL 15
- Weaviate (向量数据库)
- Redis 7
- MinIO (对象存储)
- Kafka
- ElasticSearch 8

### 基础设施
- Docker + Docker Compose
- Kubernetes (生产环境)
- Prometheus + Grafana
- ELK Stack

---

## 🚀 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Python 3.11+
- Docker & Docker Compose
- 8GB+ RAM

### 本地开发

1. **克隆项目**
```bash
git clone https://github.com/yourusername/gzdoc.git
cd gzdoc
```

2. **启动基础服务**
```bash
cd infrastructure/docker
docker-compose up -d
```

3. **启动后端服务**
```bash
cd backend
# 详见 backend/README.md
```

4. **启动AI服务**
```bash
cd ai-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

5. **启动前端**
```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

---

## 📚 文档

- [架构设计](docs/architecture/README.md)
- [API文档](docs/api/README.md)
- [开发指南](docs/development/README.md)
- [部署文档](docs/deployment/README.md)

---

## 🗓️ 开发路线图

### Phase 1: MVP版本 (Month 1-3) ✅ 进行中
- [x] 项目初始化
- [ ] 用户系统
- [ ] 文档管理
- [ ] 文档处理流水线
- [ ] RAG问答系统

### Phase 2: 生产版本 (Month 4-6)
- [ ] 微服务架构
- [ ] 性能优化
- [ ] 多租户系统
- [ ] 计费系统
- [ ] 监控告警

### Phase 3: 商业版本 (Month 7-9)
- [ ] 企业级特性
- [ ] 私有化部署
- [ ] AI能力增强
- [ ] 成本优化

---

## 📊 项目进度

- **当前阶段**: Phase 1 - MVP开发
- **完成度**: 5%
- **预计完成**: 2026年8月

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📄 许可证

[MIT License](LICENSE)

---

## 📧 联系方式

- 项目主页: https://github.com/yourusername/gzdoc
- 问题反馈: https://github.com/yourusername/gzdoc/issues

---

**⭐ 如果这个项目对你有帮助，请给个Star！**

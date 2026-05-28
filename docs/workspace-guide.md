# GzDoc 工作区使用指南

## 工作区隔离方案

### 方案1：使用 VS Code 工作区（推荐）⭐

#### 打开完整工作区
```bash
cd /Users/adam/Documents/IdeaProjects/gzDoc
code gzdoc.code-workspace
```

#### 打开单个模块（专注开发）
```bash
# 只开发后端
code backend/

# 只开发前端
code frontend/

# 只开发AI服务
code ai-service/

# 只写文档
code docs/
```

**优点：**
- ✅ 物理隔离，互不干扰
- ✅ 每个窗口只显示相关文件
- ✅ 减少认知负担

---

### 方案2：使用 Git Worktree（高级）

如果你需要**同时运行前后端**，可以使用 Git Worktree：

```bash
# 在主目录（后端开发）
cd /Users/adam/Documents/IdeaProjects/gzDoc

# 创建前端工作区
git worktree add ../gzDoc-frontend main

# 创建AI服务工作区
git worktree add ../gzDoc-ai main

# 现在你有三个独立的工作目录：
# ~/Documents/IdeaProjects/gzDoc/           → 完整项目
# ~/Documents/IdeaProjects/gzDoc-frontend/  → 只有前端
# ~/Documents/IdeaProjects/gzDoc-ai/        → 只有AI服务
```

**使用场景：**
```bash
# 终端1：运行后端
cd ~/Documents/IdeaProjects/gzDoc/backend
./mvnw spring-boot:run

# 终端2：运行前端
cd ~/Documents/IdeaProjects/gzDoc-frontend/frontend
npm run dev

# 终端3：运行AI服务
cd ~/Documents/IdeaProjects/gzDoc-ai/ai-service
uvicorn app.main:app --reload
```

**清理 Worktree：**
```bash
# 列出所有 worktree
git worktree list

# 删除不需要的 worktree
git worktree remove ../gzDoc-frontend
```

---

### 方案3：使用 tmux/终端分屏

```bash
# 安装 tmux（如果没有）
brew install tmux

# 创建会话
tmux new -s gzdoc

# 分屏
Ctrl+b %    # 垂直分屏
Ctrl+b "    # 水平分屏
Ctrl+b o    # 切换窗格

# 示例布局：
# ┌─────────────┬─────────────┐
# │   Backend   │   Frontend  │
# ├─────────────┼─────────────┤
# │  AI Service │    Logs     │
# └─────────────┴─────────────┘
```

---

## 推荐工作流

### 场景1：开发新功能（前后端联动）

```bash
# 使用完整工作区
code gzdoc.code-workspace

# 或者使用 Git Worktree
cd ~/Documents/IdeaProjects/gzDoc
git worktree add ../gzDoc-frontend main

# 终端1：后端
cd backend/gzdoc-finance
./mvnw spring-boot:run

# 终端2：前端
cd ../gzDoc-frontend/frontend
npm run dev
```

### 场景2：只写后端

```bash
# 只打开后端目录
code backend/

# 专注开发，不被前端文件干扰
```

### 场景3：只写前端

```bash
# 只打开前端目录
code frontend/

# 专注UI开发
```

### 场景4：写文档/博客

```bash
# 写项目文档
code docs/

# 写博客（独立仓库）
code /Users/adam/Documents/WebStormProject/blog/
```

---

## 目录结构说明

```
gzDoc/  (Monorepo - 技术代码)
├── backend/          # Java 后端服务
│   ├── gzdoc-common/
│   ├── gzdoc-gateway/
│   ├── gzdoc-auth/
│   ├── gzdoc-document/
│   ├── gzdoc-qa/
│   └── gzdoc-finance/
├── frontend/         # React 前端
│   └── gzdoc-web/
├── ai-service/       # Python AI 服务
│   └── app/
├── infrastructure/   # 部署配置
│   ├── docker-compose.yml
│   └── k8s/
├── docs/            # 项目文档
│   ├── architecture/
│   └── api/
└── scripts/         # 脚本工具

blog/  (独立仓库 - 博客)
└── /Users/adam/Documents/WebStormProject/blog/
```

---

## 为什么选择 Monorepo？

### ✅ 优点（对独立开发者）

1. **统一版本管理**
   ```bash
   # 一个 commit 包含前后端改动
   git commit -m "feat: 添加金融研报上传功能（前后端）"
   ```

2. **跨模块重构方便**
   ```bash
   # 修改 API 接口
   # backend/gzdoc-finance/api/ReportController.java
   # frontend/src/api/report.ts
   # 一起改，一起提交
   ```

3. **共享配置**
   ```bash
   # 一套 Docker Compose
   docker-compose up
   
   # 一套 CI/CD
   .github/workflows/deploy.yml
   ```

4. **部署简单**
   ```bash
   # 一键部署
   ./scripts/deploy.sh
   ```

### ❌ 缺点（可以解决）

1. **工作区混乱** → 使用 VS Code 工作区 / Git Worktree
2. **仓库较大** → 你是唯一开发者，不是问题
3. **构建时间长** → 使用增量构建

---

## 为什么博客独立？

### ✅ 博客独立的理由

1. **不同性质的内容**
   - 代码：技术实现
   - 博客：内容输出

2. **更新频率不同**
   - 代码：功能开发（周级别）
   - 博客：每周1篇

3. **部署方式不同**
   - 代码：Docker → 云服务器
   - 博客：Astro → Vercel

4. **避免污染 commit 历史**
   ```bash
   # 代码仓库的 commit 历史应该是：
   feat: 添加金融研报解析
   fix: 修复向量检索bug
   refactor: 优化RAG性能
   
   # 而不是：
   blog: 写了一篇博客
   blog: 修改博客错别字
   blog: 更新博客图片
   ```

---

## 总结

### 推荐方案：Monorepo（技术） + 独立博客

```
技术代码：gzDoc/  (Monorepo)
博客内容：blog/   (独立仓库)
```

### 日常工作流

```bash
# 开发功能（前后端联动）
code gzdoc.code-workspace

# 专注后端
code backend/

# 专注前端
code frontend/

# 写博客
code /Users/adam/Documents/WebStormProject/blog/
```

### 关键原则

1. **技术代码用 Monorepo**（前后端联动频繁）
2. **博客内容独立**（不同性质，独立部署）
3. **使用工作区隔离**（减少干扰）
4. **保持简单**（不要过度设计）

---

**现在就试试：**

```bash
# 打开完整工作区
cd /Users/adam/Documents/IdeaProjects/gzDoc
code gzdoc.code-workspace

# 或者只打开后端
code backend/
```

---

*最后更新：2026-05-28*

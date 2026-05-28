# Deploy GzDoc to GitHub GeezCo org and publish docs to geezco.github.io

## Goal

将 GzDoc 项目部署到 GitHub GeezCo 组织，并通过 Git Submodule 将项目文档发布到 `https://geezco.github.io/doc/gzDoc/` 文档站点。

## What I already know

* 目标组织：https://github.com/orgs/GeezCo/repositories
* 目标仓库名：gzDoc
* 文档目标地址：https://geezco.github.io/doc/gzDoc/
* Blog 站点本地路径：/Users/adam/Documents/WebStormProject/blog
* Blog 站点技术栈：Astro (静态站点生成器)
* Blog 站点仓库：GeezCo/GeezCo.github.io (已存在，公开)
* 当前项目结构：
  - backend/ (Java Spring Boot微服务)
  - frontend/ (React + TypeScript)
  - ai-service/ (Python FastAPI)
  - docs/ (项目文档 - 4个文件)
    - architecture/README.md (系统架构设计)
    - development/README.md (开发指南)
    - deployment/README.md (部署指南)
    - deployment/ENVIRONMENTS.md (环境说明)
  - infrastructure/ (Docker配置)
  - scripts/ (脚本)

## Decision (ADR-lite)

**Context**: 需要将 gzDoc 文档同步到 blog 站点，有三种方案：Git Submodule、GitHub Actions 自动同步、手动复制

**Decision**: 使用 **Git Submodule** + **MVP 手动导航**

**Consequences**: 
- ✅ 单一数据源，文档更新自动同步
- ✅ 快速实现，无需复杂配置
- ✅ gzDoc 文档保持原样，无需添加 frontmatter
- ⚠️ 需要理解 submodule 机制
- 📝 未来可升级到自动化文档列表（添加 frontmatter + Astro Content Collection）

## Requirements

### 1. 创建 gzDoc 仓库
* 在 GeezCo 组织下创建 gzDoc 仓库（公开）
* 推送当前项目代码到 gzDoc 仓库
* 配置 .gitignore（排除 .claude/, .trellis/, node_modules/ 等）
* 添加 README.md（项目说明）

### 2. 配置 Git Submodule
* 在 blog 仓库中添加 submodule：
  - 路径：`src/content/docs/gzDoc/`
  - 源：`GeezCo/gzDoc` 仓库的 `docs/` 目录
* 配置 submodule 只跟踪 docs 目录

### 3. 创建文档导航页面（MVP）
* 在 blog 仓库创建 `src/pages/doc/gzDoc/index.astro`
* 手动列出 4 个文档链接：
  - 系统架构设计 → `/doc/gzDoc/architecture/`
  - 开发指南 → `/doc/gzDoc/development/`
  - 部署指南 → `/doc/gzDoc/deployment/`
  - 环境说明 → `/doc/gzDoc/deployment/ENVIRONMENTS`
* 简单的卡片式布局，复用 blog 站点现有样式

### 4. 更新 blog 站点首页
* 修改 `/doc/index.astro`，将 `docSystems` 从空数组改为包含 gzDoc 系统
* 或者保持简单，直接在空状态页面添加 "查看 GzDoc 文档" 链接

## Acceptance Criteria

* [ ] gzDoc 仓库在 GeezCo 组织下创建成功（公开）
* [ ] 项目代码推送到 gzDoc 仓库（排除敏感文件）
* [ ] blog 仓库配置了 gzDoc docs 的 submodule
* [ ] 文档可通过 https://geezco.github.io/doc/gzDoc/ 访问
* [ ] 4 个文档页面都能正常显示（Markdown 渲染正确）
* [ ] 文档导航页面样式美观，与 blog 站点风格一致
* [ ] README.md 包含文档更新说明（如何更新 submodule）

## Definition of Done

* gzDoc 仓库配置完成（.gitignore, README）
* blog 仓库 submodule 配置完成
* 文档导航页面实现
* 本地测试通过（`npm run build` 成功）
* 推送到 GitHub，GitHub Pages 自动部署
* 文档更新流程文档化（写入 README）

## Out of Scope (explicit)

* 自动化文档列表生成（需要 frontmatter + Content Collection）
* 文档搜索功能
* 付费文档功能
* 文档评论功能
* 生产环境部署配置（仅限代码仓库和文档发布）
* 自定义域名配置（使用默认 github.io 域名）
* GitHub Actions CI/CD（blog 站点已有自动部署）

## Technical Notes

* 当前项目位置：/Users/adam/Documents/IdeaProjects/gzDoc
* Blog 站点位置：/Users/adam/Documents/WebStormProject/blog
* Git 仓库已初始化但未推送到远程
* Blog 站点构建命令：`npm run build` → 输出到 `dist/`
* Astro 配置：`astro.config.mjs`（已配置 remarkGfm 支持 GitHub Flavored Markdown）
* Git Submodule 注意事项：
  - 只需要 docs 目录，不需要整个仓库
  - 可以使用 sparse checkout 或 subtree 方式
  - 更新文档时需要在 blog 仓库执行 `git submodule update --remote`

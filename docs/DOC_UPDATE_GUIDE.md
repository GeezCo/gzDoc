# GzDoc 文档更新指南

本文档说明如何更新 GzDoc 项目文档并同步到文档站点。

## 文档架构

- **源仓库**: [GeezCo/gzDoc](https://github.com/GeezCo/gzDoc) - 项目主仓库，包含代码和文档
- **文档站点**: [GeezCo.github.io](https://geezco.github.io/doc/gzDoc/) - 文档发布站点
- **同步方式**: Git Subtree - 自动同步 gzDoc 仓库的 `docs/` 目录

## 文档结构

```
gzDoc/docs/
├── architecture/       # 系统架构设计
│   └── README.md
├── development/        # 开发指南
│   └── README.md
├── deployment/         # 部署指南
│   ├── README.md
│   └── ENVIRONMENTS.md
└── workspace-guide.md  # 工作区指南
```

## 更新文档流程

### 1. 在 gzDoc 仓库更新文档

```bash
cd /Users/adam/Documents/IdeaProjects/gzDoc

# 编辑文档
vim docs/architecture/README.md

# 提交更改
git add docs/
git commit -m "docs: 更新架构文档"
git push origin main
```

### 2. 同步文档到站点

```bash
cd /Users/adam/Documents/WebStormProject/blog

# 运行同步脚本
./scripts/sync-gzdoc-docs.sh

# 或手动同步
git subtree pull --prefix=src/content/docs/gzDoc git@github.com:GeezCo/gzDoc.git main --squash

# 清理非文档文件（如果需要）
cd src/content/docs/gzDoc
find . -maxdepth 1 ! -name '.' ! -name '..' ! -name 'architecture' ! -name 'deployment' ! -name 'development' ! -name 'workspace-guide.md' -exec rm -rf {} +

# 提交并推送
cd /Users/adam/Documents/WebStormProject/blog
git add src/content/docs/gzDoc
git commit -m "docs: 同步 gzDoc 文档"
git push origin main
```

### 3. 验证部署

文档推送后，GitHub Actions 会自动构建并部署到 GitHub Pages。

- 查看部署状态: `gh run list`
- 访问文档站点: https://geezco.github.io/doc/gzDoc/

## 文档访问地址

- **文档首页**: https://geezco.github.io/doc/gzDoc/
- **系统架构**: https://geezco.github.io/doc/gzDoc/architecture/
- **开发指南**: https://geezco.github.io/doc/gzDoc/development/
- **部署指南**: https://geezco.github.io/doc/gzDoc/deployment/
- **工作区指南**: https://geezco.github.io/doc/gzDoc/workspace-guide/

## 注意事项

1. **文档格式**: 使用 Markdown 格式，支持 GitHub Flavored Markdown
2. **图片资源**: 图片应放在文档同级目录，使用相对路径引用
3. **链接**: 文档内部链接使用相对路径
4. **同步频率**: 建议在重要更新后手动同步，日常更新可以批量同步

## 故障排查

### 构建失败

```bash
# 本地测试构建
cd /Users/adam/Documents/WebStormProject/blog
npm run build
```

### 文档未更新

```bash
# 检查 subtree 状态
git log --oneline src/content/docs/gzDoc

# 强制重新拉取
git subtree pull --prefix=src/content/docs/gzDoc git@github.com:GeezCo/gzDoc.git main --squash
```

### 清理缓存

```bash
# 清理构建缓存
rm -rf dist/ node_modules/.astro/

# 重新构建
npm run build
```

## 自动化改进（未来）

- [ ] 配置 GitHub Actions 自动同步文档
- [ ] 添加文档版本管理
- [ ] 实现文档搜索功能
- [ ] 添加文档评论功能

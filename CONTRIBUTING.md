# 贡献指南

感谢你对 GzDoc 项目的关注！我们欢迎任何形式的贡献。

## 如何贡献

### 报告问题

如果你发现了 bug 或有功能建议：

1. 在 [Issues](https://github.com/GeezCo/gzDoc/issues) 中搜索是否已有相关问题
2. 如果没有，创建新的 Issue，并提供：
   - 清晰的标题和描述
   - 复现步骤（如果是 bug）
   - 期望行为和实际行为
   - 环境信息（操作系统、版本等）
   - 相关截图或日志

### 提交代码

1. **Fork 项目**
   ```bash
   # 在 GitHub 上 Fork 项目
   git clone https://github.com/你的用户名/gzDoc.git
   cd gzDoc
   ```

2. **创建分支**
   ```bash
   git checkout -b feature/your-feature-name
   # 或
   git checkout -b fix/your-bug-fix
   ```

3. **开发和测试**
   - 遵循项目的代码规范
   - 编写必要的测试
   - 确保所有测试通过
   - 更新相关文档

4. **提交代码**
   ```bash
   git add .
   git commit -m "feat: 添加新功能描述"
   ```

5. **推送到 GitHub**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **创建 Pull Request**
   - 在 GitHub 上创建 PR
   - 填写 PR 模板
   - 等待代码审查

## 代码规范

### Commit 消息规范

我们使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型：**
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行）
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

**示例：**
```
feat(auth): 添加 OAuth2 登录支持

- 实现 Google OAuth2 登录
- 添加用户信息同步
- 更新登录页面 UI

Closes #123
```

### 代码风格

#### Java
- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 使用 4 空格缩进
- 类名使用 PascalCase
- 方法名使用 camelCase

#### Python
- 遵循 [PEP 8](https://pep8.org/)
- 使用 4 空格缩进
- 使用 type hints
- 使用 black 格式化代码

#### TypeScript/React
- 遵循 [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)
- 使用 2 空格缩进
- 组件名使用 PascalCase
- 使用函数式组件和 Hooks

### 测试要求

- 新功能必须包含单元测试
- Bug 修复应包含回归测试
- 测试覆盖率不低于 80%
- 所有测试必须通过

```bash
# Java 测试
cd backend
mvn test

# Python 测试
cd ai-service
pytest

# 前端测试
cd frontend
npm test
```

## 开发环境设置

详见 [开发指南](docs/development/README.md)

## 分支策略

- `main`: 主分支，保持稳定
- `develop`: 开发分支
- `feature/*`: 功能分支
- `fix/*`: Bug 修复分支
- `hotfix/*`: 紧急修复分支

## Code Review 流程

1. 提交 PR 后，至少需要 1 位维护者审查
2. 解决所有审查意见
3. 确保 CI 检查全部通过
4. 维护者合并 PR

## 行为准则

- 尊重所有贡献者
- 保持友好和专业
- 接受建设性批评
- 关注项目的最佳利益

## 问题？

如有任何问题，欢迎：
- 在 [Discussions](https://github.com/GeezCo/gzDoc/discussions) 中讨论
- 发送邮件至项目维护者

---

再次感谢你的贡献！🎉
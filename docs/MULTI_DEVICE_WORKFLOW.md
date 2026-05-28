# 多设备协作与上下文保持指南

本指南说明如何在多台 Mac 上协作开发，并确保 Claude 在每个 session 都能获取完整的项目上下文。

## 核心原理

`.trellis` 工作流系统通过以下机制保持上下文：

1. **任务系统** - 记录当前工作内容和进度
2. **Spec 文档** - 存储项目规范和设计决策
3. **Memory 系统** - 保存项目知识和经验
4. **研究记录** - 记录技术调研和决策过程

## 设备间同步设置

### 1. 确保 .trellis 目录被 Git 跟踪

```bash
# 检查 .gitignore，确保 .trellis 核心文件被跟踪
cat .gitignore | grep trellis

# 如果 .trellis 被忽略，需要调整 .gitignore
# 只忽略临时文件，保留核心配置
echo "# Trellis - 只忽略临时文件" >> .gitignore
echo ".trellis/workspace/" >> .gitignore
echo ".trellis/.cache/" >> .gitignore
```

### 2. 提交 .trellis 配置到 Git

```bash
# 添加 .trellis 核心文件
git add .trellis/config.yaml
git add .trellis/spec/
git add .trellis/tasks/
git add .trellis/workflow.md

# 提交
git commit -m "chore: 添加 trellis 工作流配置"
git push origin main
```

### 3. 在新设备上克隆项目

```bash
# 克隆项目
git clone git@github.com:GeezCo/gzDoc.git
cd gzDoc

# 检查 .trellis 是否存在
ls -la .trellis/

# 如果缺少文件，拉取最新代码
git pull origin main
```

## 工作流程

### 设备 A：开始新任务

```bash
# 1. 创建新任务
python3 .trellis/scripts/task.py new "feature-name" "任务描述"

# 2. 编辑 PRD
vim .trellis/tasks/XX-feature-name/prd.md

# 3. 启动任务
python3 .trellis/scripts/task.py start .trellis/tasks/XX-feature-name

# 4. 开发过程中，Claude 会自动记录到：
#    - .trellis/tasks/XX-feature-name/research/
#    - .trellis/tasks/XX-feature-name/implement.jsonl
#    - .trellis/tasks/XX-feature-name/check.jsonl

# 5. 提交进度
git add .trellis/tasks/XX-feature-name/
git commit -m "wip: feature-name 开发中"
git push origin main
```

### 设备 B：继续任务

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 查看当前任务
python3 .trellis/scripts/task.py current

# 3. 如果没有活动任务，设置任务
python3 .trellis/scripts/task.py set .trellis/tasks/XX-feature-name

# 4. 告诉 Claude 继续工作
# 在 Claude 中输入：
# "继续 feature-name 任务，查看 .trellis/tasks/XX-feature-name/prd.md"

# 5. Claude 会自动加载：
#    - PRD 文档
#    - 研究记录
#    - 实现上下文
#    - 检查清单
```

## Claude 上下文加载机制

### 自动加载（通过 Hook）

当你设置了活动任务后，Claude 的每次对话都会自动注入：

```
<workflow-state>
Task: XX-feature-name (in_progress)
Source: session:xxx
[任务上下文自动加载]
</workflow-state>
```

### 手动加载（如果 Hook 失效）

```bash
# 方法 1：使用 trellis-brainstorm skill
/trellis:brainstorm

# 方法 2：直接告诉 Claude
"查看当前任务：.trellis/tasks/XX-feature-name/prd.md"

# 方法 3：使用 get_context.py
python3 .trellis/scripts/get_context.py --task XX-feature-name
```

## 保持上下文的最佳实践

### 1. 及时更新 Spec 文档

```bash
# 每次重要决策后更新 spec
vim .trellis/spec/backend/architecture.md

# 使用 trellis-update-spec skill
# 在 Claude 中：/trellis:update-spec
```

### 2. 记录研究结果

```bash
# 研究记录会自动保存到
.trellis/tasks/XX-feature-name/research/
├── 01-技术选型.md
├── 02-性能测试.md
└── 03-安全考虑.md
```

### 3. 使用 Memory 系统

```bash
# Claude 会自动保存重要信息到
.claude/projects/-Users-adam-Documents-IdeaProjects-gzDoc/memory/
├── user_*.md          # 用户偏好
├── feedback_*.md      # 反馈和经验
├── project_*.md       # 项目决策
└── reference_*.md     # 外部资源
```

### 4. 提交前检查

```bash
# 确保关键文件已提交
git status .trellis/

# 应该看到：
# modified:   .trellis/tasks/XX-feature-name/prd.md
# new file:   .trellis/tasks/XX-feature-name/research/01-xxx.md
# modified:   .trellis/spec/backend/xxx.md
```

## 常见问题

### Q1: Claude 没有加载任务上下文？

```bash
# 检查当前任务
python3 .trellis/scripts/task.py current

# 如果显示 "No active task"，设置任务
python3 .trellis/scripts/task.py set .trellis/tasks/XX-feature-name

# 然后告诉 Claude
"Active task: .trellis/tasks/XX-feature-name"
```

### Q2: 任务状态不同步？

```bash
# 设备 A 完成任务后
python3 .trellis/scripts/task.py finish

# 提交状态
git add .trellis/tasks/XX-feature-name/task.json
git commit -m "feat: 完成 feature-name"
git push

# 设备 B 拉取
git pull origin main
```

### Q3: Spec 文档冲突？

```bash
# 使用 merge 策略
git config merge.ours.driver true

# 或手动解决冲突
git mergetool
```

### Q4: Memory 文件冲突？

```bash
# Memory 文件通常在 .gitignore 中
# 如果需要同步，可以手动复制
rsync -av .claude/projects/*/memory/ 设备B:/.claude/projects/*/memory/
```

## 推荐工作流

### 场景 1：短期任务（1-2 天）

```bash
# 设备 A
1. 创建任务并开始开发
2. 每天结束前提交进度
3. git push

# 设备 B
1. git pull
2. 继续开发
3. 完成后 finish 任务
4. git push
```

### 场景 2：长期任务（1 周+）

```bash
# 使用分支
git checkout -b feature/XX-feature-name

# 定期同步
git push origin feature/XX-feature-name

# 设备间切换
git pull origin feature/XX-feature-name
```

### 场景 3：并行开发

```bash
# 设备 A：任务 A
python3 .trellis/scripts/task.py new "feature-a" "功能 A"

# 设备 B：任务 B
python3 .trellis/scripts/task.py new "feature-b" "功能 B"

# 各自开发，定期合并
git merge main
```

## 自动化脚本

### 同步脚本（可选）

```bash
#!/bin/bash
# sync-trellis.sh - 同步 trellis 状态

echo "📥 拉取最新代码..."
git pull origin main

echo "📋 当前任务状态："
python3 .trellis/scripts/task.py current

echo "📊 任务列表："
python3 .trellis/scripts/task.py list

echo "✅ 同步完成！"
```

## 总结

**关键点**：
1. ✅ 提交 `.trellis/tasks/` 和 `.trellis/spec/` 到 Git
2. ✅ 使用 `task.py current` 查看活动任务
3. ✅ 告诉 Claude "Active task: xxx" 加载上下文
4. ✅ 及时更新 spec 和研究记录
5. ✅ 定期 push/pull 保持同步

**不需要同步**：
- ❌ `.trellis/workspace/` - 临时工作区
- ❌ `.trellis/.cache/` - 缓存文件
- ❌ `.claude/` - 本地 memory（可选同步）

这样，无论在哪台设备，Claude 都能获取完整的项目上下文！🎯

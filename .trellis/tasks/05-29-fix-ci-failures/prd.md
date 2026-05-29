# 修复 CI 失败

## 问题描述

GitHub Actions CI 检查全部失败，需要修复以下问题：

### 1. Backend Tests 失败
- **错误**: `gzdoc-common` 模块编译失败
- **原因**: 缺少 MyBatis-Plus 依赖
- **影响**: 导致整个后端构建失败

```
[ERROR] /home/runner/work/gzDoc/gzDoc/backend/gzdoc-common/src/main/java/com/gzdoc/common/config/MyBatisPlusMetaObjectHandler.java:[3,46] package com.baomidou.mybatisplus.core.handlers does not exist
```

### 2. Frontend Tests 失败
- **错误**: 缺少 `package-lock.json`
- **原因**: CI 配置中指定了 `cache-dependency-path: frontend/package-lock.json`，但文件不存在
- **影响**: 前端测试无法运行

### 3. AI Service Tests 失败
- **可能原因**: 缺少测试文件或依赖问题
- **需要验证**: 检查 `ai-service/` 目录结构

### 4. Docker Build Test 失败
- **可能原因**: Dockerfile 不存在或配置错误
- **需要验证**: 检查各服务的 Dockerfile

## 解决方案

### 1. 修复后端依赖

在 `backend/gzdoc-common/pom.xml` 中添加 MyBatis-Plus 依赖：

```xml
<!-- MyBatis-Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatisplus-spring-boot-starter</artifactId>
</dependency>
```

### 2. 修复前端依赖

两个选择：
- **方案 A**: 生成 `package-lock.json`
  ```bash
  cd frontend
  npm install
  git add package-lock.json
  ```

- **方案 B**: 修改 CI 配置，移除 `cache-dependency-path`
  ```yaml
  - name: Setup Node.js
    uses: actions/setup-node@v4
    with:
      node-version: '18'
      cache: 'npm'
      # 移除 cache-dependency-path
  ```

### 3. 检查并修复 AI Service

- 检查是否有测试文件
- 如果没有，创建基础测试或暂时跳过测试

### 4. 检查并修复 Docker 构建

- 验证各服务的 Dockerfile 是否存在
- 如果不存在，创建或暂时禁用 Docker 构建测试

## 验证步骤

1. 本地测试后端构建：
   ```bash
   cd backend
   mvn clean test
   ```

2. 本地测试前端：
   ```bash
   cd frontend
   npm install
   npm run lint
   npm run type-check
   npm test
   ```

3. 本地测试 AI Service：
   ```bash
   cd ai-service
   pip install -r requirements.txt
   pytest
   ```

4. 本地测试 Docker 构建：
   ```bash
   docker build -t test ./backend
   docker build -t test ./frontend
   docker build -t test ./ai-service
   ```

5. 推送后验证 CI：
   ```bash
   git push origin main
   gh run watch
   ```

## 接受标准

- [ ] Backend Tests 通过
- [ ] Frontend Tests 通过
- [ ] AI Service Tests 通过
- [ ] Docker Build Test 通过
- [ ] 所有 CI 检查显示绿色 ✓

## 优先级

**P0 - 紧急**

CI 失败会阻止后续开发和部署，需要立即修复。

## 预估工作量

- 修复依赖问题: 30 分钟
- 本地验证: 30 分钟
- CI 验证: 15 分钟
- **总计**: 约 1-1.5 小时

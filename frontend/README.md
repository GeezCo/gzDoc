# GzDoc Frontend

基于 React + TypeScript + Vite + Ant Design 的前端项目。

## 技术栈

- React 18
- TypeScript
- Vite
- Ant Design 5
- React Router 6
- Axios

## 目录结构

```
src/
├── components/      # 公共组件
│   └── Layout.tsx   # 布局组件
├── pages/           # 页面组件
│   ├── Login.tsx    # 登录页
│   ├── Dashboard.tsx # 仪表盘
│   ├── DocumentList.tsx # 文档列表
│   └── QA.tsx       # 智能问答
├── services/        # API服务
│   ├── auth.ts      # 认证API
│   ├── document.ts  # 文档API
│   └── qa.ts        # 问答API
├── types/           # TypeScript类型定义
│   └── index.ts
├── utils/           # 工具函数
│   └── request.ts   # Axios封装
├── assets/          # 静态资源
├── App.tsx          # 根组件
├── main.tsx         # 入口文件
└── index.css        # 全局样式
```

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

## 功能模块

### 1. 用户认证
- 登录/登出
- Token管理
- 路由守卫

### 2. 文档管理
- 文档上传
- 文档列表（分页）
- 文档删除
- 状态展示

### 3. 智能问答
- 实时问答
- 会话管理
- 相关文档展示
- 响应时间统计

### 4. 仪表盘
- 数据统计
- 可视化展示

## API代理

开发环境下，所有 `/api` 请求会被代理到 `http://localhost:8080`（网关服务）。

配置位置：`vite.config.ts`

## 环境变量

可以创建 `.env.local` 文件配置环境变量：

```
VITE_API_BASE_URL=http://localhost:8080
```

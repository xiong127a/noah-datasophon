# Datasophon Next.js 项目

这是一个基于最新技术栈构建的 Next.js 项目，用于 Datasophon 平台的前端开发。

## 技术栈

- **Next.js**: 最新版本，使用 App Router 架构
- **React**: 最新版本
- **TypeScript**: 完全类型支持
- **TailwindCSS**: 用于快速构建现代化 UI

## 项目结构

```
/src
  /app                # App Router 根目录
  /lib                # 工具函数和共享库
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

然后在浏览器中打开 [http://localhost:3000](http://localhost:3000)

### 构建生产版本

```bash
npm run build
```

### 运行生产版本

```bash
npm start
```

## 开发指南

### 添加新页面

在 `src/app` 目录下创建新的目录和 `page.tsx` 文件：

```tsx
// src/app/about/page.tsx
export default function About() {
  return (
    <div>
      <h1>关于我们</h1>
      <p>这是一个示例页面</p>
    </div>
  );
}
```

### 创建新 API 端点

在 `src/app/api` 目录下添加新的目录和 `route.ts` 文件：

```tsx
// src/app/api/example/route.ts
import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json({
    message: '这是一个 API 端点'
  });
}
```

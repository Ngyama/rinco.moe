# Rinco VN Analytics

Visual Novel 数据分析平台（Spring Boot + Vue 前后端分离）。

## 项目结构

- `backend`：Spring Boot 后端服务
- `frontend`：Vue 3 前端应用
- `docs`：需求与实施提示文档

## 启动方式

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`  
健康检查：`GET /api/health`

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认本地地址：`http://localhost:5173`

## 当前进度

- 已完成项目骨架搭建
- 已完成首页及模块入口路由
- 子页面为占位，等待逐页开发

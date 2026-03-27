# Rinco Analytics

[日本語](README.md) | [English](README.en.md) | **简体中文**

面向 Galgame / 视觉小说作品的跨站评分与趋势分析 Web 应用。整合 Bangumi、VNDB、ErogameScape 等来源，在**匹配作品池**上提供榜单与可视化。

---

## 项目简介

仓库为 **Spring Boot 后端** + **Vue 3 前端**。主要页面与功能如下。

- **Global** — 匹配池内三站分列排行、三站加权 Top10、桑基图等
- **Trend** — 基于 Bangumi 的年份与评分趋势
- **Hot** — 热门作品列表与详情
- **Controversy** — 分数分散较大的作品（争议度）
- **Tag** — 标签相关浏览与分析
- **Staff** — 制作人员（声优、剧本等）信息
- **User** — 用户收藏联动
- **About** — 项目说明与导航入口

---

## 界面展示

> **（占位）** 

<!-- 示例: ![Global](docs/screenshots/global.png) -->

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vue Router、TypeScript、Vite |
| 后端 | Java、Spring Boot、Spring JDBC |
| 数据库 | PostgreSQL |

---

## 数据来源

本项目**汇总并展示各网站公开 API / 统计接口等获取的数据**，使用前请遵守对应服务的条款与政策。

| 来源 | 典型用途 |
|------|-----------|
| **Bangumi** | 作品信息、评分、标签、趋势类功能 |
| **VNDB** | VN 条目、评分、标签 |
| **ErogameScape（EGS）** | 平均分等（如 SQL API） |

数据准确性与更新频率取决于上游；若涉及再分发或商用，请务必自行核对各站要求。

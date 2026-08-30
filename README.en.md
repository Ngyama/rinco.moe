# Rinco Analytics

[日本語](README.md) | **English** | [简体中文](README.zh.md)

A web application for exploring visual-novel scores and trends across multiple sources. It aligns Bangumi, VNDB, and ErogameScape-style data on a **matched work pool** and provides rankings and charts.

---

## Project overview

The repo is split into a **Spring Boot** backend and a **Vue 3** frontend. Main screens and features:

- **Global** — Per-site top lists inside the match pool, weighted top 10, Sankey-style view, etc.
- **Trend** — Bangumi-based yearly score trends
- **Hot** — Hot titles list and detail
- **Controversy** — Titles with high score variance (“controversial” picks)
- **Tag** — Tag-related browsing and analysis
- **Staff** — Staff (voice, scenario, etc.) information
- **User** — User collection integration
- **About** — Project intro and navigation

---

## Screenshots

> **(Placeholder)** 

<!-- Example: ![Global](docs/screenshots/global.png) -->

---

## Tech stack

| Layer | Technologies |
|-------|----------------|
| Frontend | Vue 3, Vue Router, TypeScript, Vite |
| Backend | Java, Spring Boot, Spring JDBC |
| Database | PostgreSQL |

---

## Data sources

This project **aggregates and visualizes data from public APIs and statistics pages** of third-party services. You must comply with each service’s terms and policies.

| Source | Typical use |
|--------|-------------|
| **Bangumi** | Metadata, scores, tags, trend-related features |
| **VNDB** | VN entries, scores, tags |
| **ErogameScape (EGS)** | Averages, etc. (e.g. SQL API) |

Accuracy and freshness depend on the upstream sources. Check each site’s rules before redistribution or commercial use.

---

## Frontend-only local development (no database)

When the Spring Boot backend or PostgreSQL is unavailable, the frontend can **fall back to mock data** in development so existing pages remain inspectable. Real `/api` responses stay primary.

```bash
cd frontend
npm install
npm run dev
```

| `VITE_USE_MOCK_DATA` | Behavior |
|----------------------|----------|
| `true` (default in `.env.development`) | Try the real API; on failure return mocks |
| `force` | Skip the network; always use mocks |
| `false` | Disable mocks |

Implementation: fallback inside `frontend/src/api/client.ts` (`apiGet`). Data lives under `frontend/src/api/mocks/` (see that folder’s `README.md`). Production builds do not enable this by default.

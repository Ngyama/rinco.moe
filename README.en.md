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

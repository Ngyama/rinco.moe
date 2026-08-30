# Rinco Analytics

**日本語** | [English](README.en.md) | [简体中文](README.zh.md)

ビジュアルノベル作品向けの横断スコア・傾向を扱う Web アプリです。Bangumi / VNDB / ErogameScape など複数ソースを束ね、マッチした作品プール上でランキングや可視化を行います。

---

## プロジェクト概要

本リポジトリは **Spring Boot バックエンド** と **Vue 3 フロントエンド** で構成されています。主な画面・機能は次のとおりです。

- **Global** — マッチプール内の三サイト別トップ一覧、三站加重 Top10、サンキー図など
- **Trend** — Bangumi ベースの年次トレンド・スコア推移の閲覧
- **Hot** — ホット作品一覧と詳細
- **Controversy** — スコア分散が大きい作品（「争い」が大きい作品）の把握
- **Tag** — タグまわりの閲覧・分析
- **Staff** — スタッフ（声優・シナリオ等）情報の閲覧
- **User** — ユーザーコレクション連携の閲覧
- **About** — プロジェクト紹介とナビゲーション

---

## 画面イメージ

> **（予定）** 

<!-- 例: ![Global](docs/screenshots/global.png) -->

---

## 技術スタック

| 層 | 技術 |
|----|------|
| フロントエンド | Vue 3, Vue Router, TypeScript, Vite |
| バックエンド | Java, Spring Boot, Spring JDBC |
| DB | PostgreSQL |

---

## データの出所

本プロジェクトは **各サイトの公開 API・統計ページ等から取得したデータ** を分析・表示するものであり、各サービスの利用規約・ポリシーに従ってください。

| ソース | 用途の例 |
|--------|-----------|
| **Bangumi** | 作品メタデータ・スコア・タグ・トレンド系 |
| **VNDB** | VN 作品・スコア・タグ |
| **ErogameScape (EGS)** | 平均点など（SQL API 等） |

一次データの正確さ・更新頻度はソース側に依存します。再配布や商用利用を行う場合は、各サイトの条件を必ず確認してください。

---

## フロントエンドのみのローカル開発（DB なし）

バックエンド / PostgreSQL が使えないとき、開発モードでは **モックデータ** にフォールバックし、既存画面の確認・改修ができます。本番の `/api` が優先です。

```bash
cd frontend
npm install
npm run dev
```

| `VITE_USE_MOCK_DATA` | 動作 |
|----------------------|------|
| `true`（`.env.development` 既定） | 実 API を試し、失敗時にモック |
| `force` | 通信せず常にモック |
| `false` | モック無効 |

実装: `frontend/src/api/client.ts` の `apiGet`。データ: `frontend/src/api/mocks/`（同ディレクトリの `README.md` 参照）。本番ビルドでは既定で無効です。

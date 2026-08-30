/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Mock API fallback for local UI work without a backend/DB.
   * - unset / in DEV: fall back to mock data when real `/api` requests fail
   * - `"true"`: same fallback, also outside DEV if set
   * - `"force"` / `"always"`: skip network and always use mocks
   * - `"false"`: never use mocks
   */
  readonly VITE_USE_MOCK_DATA?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

export {};

declare module "vue-router" {
  interface RouteMeta {
    /** 主内容区不滚动，单屏占满视口（顶栏以下） */
    fixedViewport?: boolean;
  }
}

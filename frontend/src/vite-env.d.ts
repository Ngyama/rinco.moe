/// <reference types="vite/client" />

export {};

declare module "vue-router" {
  interface RouteMeta {
    /** 主内容区不滚动，单屏占满视口（顶栏以下） */
    fixedViewport?: boolean;
  }
}

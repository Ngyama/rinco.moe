import { createRouter, createWebHistory } from "vue-router";
import HomePage from "../pages/HomePage.vue";
import GlobalNewPage from "../pages/GlobalNewPage.vue";
import TrendPage from "../pages/TrendPage.vue";
import HotPage from "../pages/HotPage.vue";
import ControversyPage from "../pages/ControversyPage.vue";
import TagPage from "../pages/TagPage.vue";
import StaffPage from "../pages/StaffPage.vue";
import UserPage from "../pages/UserPage.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "home", component: HomePage },
    { path: "/global", name: "global", component: GlobalNewPage },
    { path: "/global-sankey", redirect: { name: "global" } },
    { path: "/trend", name: "trend", component: TrendPage, meta: { fixedViewport: true } },
    { path: "/hot", name: "hot", component: HotPage, meta: { fixedViewport: true } },
    { path: "/controversy", name: "controversy", component: ControversyPage, meta: { fixedViewport: true } },
    { path: "/tag", name: "tag", component: TagPage },
    { path: "/staff", name: "staff", component: StaffPage },
    { path: "/user", name: "user", component: UserPage, meta: { fixedViewport: true } }
  ]
});

export default router;

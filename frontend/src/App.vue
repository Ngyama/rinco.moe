<template>
  <div class="app-shell" :class="{ 'app-shell--fixed': fixedViewport }">
    <div class="capsule-wrapper">
      <nav ref="capsuleRef" class="capsule">
        <div class="capsule-indicator" :style="indicatorStyle" />
        <a
          v-for="item in navItems"
          :key="item.path"
          href="#"
          :ref="(el) => setLinkRef(el, item.path)"
          class="capsule-link"
          :class="{ active: route.path === item.path }"
          @click.prevent="router.push({ name: item.name })"
        >
          {{ item.label }}
        </a>
      </nav>
    </div>
    <main class="main-content" :class="{ 'main-content--fixed': fixedViewport }">
      <router-view v-slot="{ Component }">
        <Transition name="page-fade" mode="out-in">
          <div
            :key="route.path"
            class="router-view-fill"
            :class="{ 'router-view-fill--fixed': fixedViewport }"
          >
            <component :is="Component" />
          </div>
        </Transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from "vue-router";
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from "vue";

const router = useRouter();
const route = useRoute();
const capsuleRef = ref<HTMLElement | null>(null);
const linkRefs = ref<Record<string, HTMLElement>>({});
const indicatorLeft = ref(0);
const indicatorWidth = ref(0);

const navItems = [
  { path: "/global", name: "global", label: "Global" },
  { path: "/trend", name: "trend", label: "Trend" },
  { path: "/hot", name: "hot", label: "Hot" },
  { path: "/controversy", name: "controversy", label: "Controversy" },
  { path: "/tag", name: "tag", label: "Tag" },
  { path: "/staff", name: "staff", label: "Staff" },
  { path: "/user", name: "user", label: "User" },
  { path: "/", name: "home", label: "About" }
];

function setLinkRef(el: unknown, path: string) {
  if (el instanceof HTMLElement) {
    linkRefs.value[path] = el;
  }
}

function updateIndicator() {
  const activePath = navItems.find((i) => route.path === i.path)?.path ?? "/";
  const link = linkRefs.value[activePath] ?? linkRefs.value["/"];
  const capsule = capsuleRef.value;
  if (link && capsule) {
    const capsuleRect = capsule.getBoundingClientRect();
    const linkRect = link.getBoundingClientRect();
    indicatorLeft.value = linkRect.left - capsuleRect.left - 8; /* offset for capsule padding */
    indicatorWidth.value = linkRect.width;
  }
}

const indicatorStyle = computed(() => ({
  transform: `translateX(${indicatorLeft.value}px)`,
  width: `${indicatorWidth.value}px`
}));

const fixedViewport = computed(() => Boolean(route.meta.fixedViewport));

watch(
  () => route.path,
  () => {
    nextTick(updateIndicator);
  },
  { immediate: true }
);

watch(linkRefs, () => nextTick(updateIndicator), { deep: true });

onMounted(() => {
  nextTick(updateIndicator);
  window.addEventListener("resize", updateIndicator);
});
onUnmounted(() => {
  window.removeEventListener("resize", updateIndicator);
});
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-shell--fixed {
  height: 100dvh;
  max-height: 100dvh;
  overflow: hidden;
}

.capsule-wrapper {
  position: sticky;
  top: 0;
  z-index: 100;
  isolation: isolate;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 16px 20px 8px;
  background: transparent;
}

.capsule {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 8px 8px 9px;
  background: transparent;
  border: none;
  border-radius: 9999px;
  box-shadow: none;
}

.capsule-indicator {
  position: absolute;
  top: 8px;
  left: 8px;
  height: calc(100% - 16px);
  background: linear-gradient(
    180deg,
    rgba(224, 249, 181, 0.45) 0%,
    rgba(165, 222, 229, 0.4) 100%
  );
  border-radius: 9999px;
  box-shadow: 0 1px 4px rgba(74, 184, 196, 0.2);
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), width 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), background 0.3s ease;
  pointer-events: none;
}

.capsule-link {
  position: relative;
  z-index: 2;
  padding: 8px 14px;
  cursor: pointer;
  pointer-events: auto;
  font-size: 13px;
  color: var(--color-text-muted);
  border-radius: 9999px;
  transition: color 0.35s ease, transform 0.25s ease;
}

.capsule-link:hover {
  color: var(--color-accent);
  transform: scale(1.03);
}

.capsule-link.active {
  color: #1a6b73;
  font-weight: 600;
  text-shadow: 0 0 20px rgba(74, 184, 196, 0.15);
}

.capsule-link:hover.active {
  color: #138b96;
}

.main-content {
  flex: 1;
  width: 100%;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.main-content--fixed {
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
}

.main-content::-webkit-scrollbar {
  display: none;
}

.router-view-fill {
  min-height: 0;
}

.router-view-fill--fixed {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Page transition */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

<template>
  <div class="app-shell">
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
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <Transition name="page-fade" mode="out-in">
          <component :is="Component" />
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
    indicatorLeft.value = linkRect.left - capsuleRect.left - 10; /* offset for capsule padding */
    indicatorWidth.value = linkRect.width;
  }
}

const indicatorStyle = computed(() => ({
  transform: `translateX(${indicatorLeft.value}px)`,
  width: `${indicatorWidth.value}px`
}));

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

.capsule-wrapper {
  position: sticky;
  top: 0;
  z-index: 100;
  isolation: isolate;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 28px 24px 24px;
  background: linear-gradient(180deg, var(--color-bg) 0%, rgba(245, 251, 248, 0.6) 100%);
  backdrop-filter: blur(8px);
}

.capsule {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 10px 10px 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fcf9 100%);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 9999px;
  box-shadow:
    0 4px 6px rgba(165, 222, 229, 0.25),
    0 10px 30px rgba(0, 0, 0, 0.1),
    0 1px 0 rgba(255, 255, 255, 0.95) inset;
}

.capsule-indicator {
  position: absolute;
  top: 10px;
  left: 10px;
  height: calc(100% - 20px);
  background: linear-gradient(180deg, rgba(224, 249, 181, 0.95) 0%, rgba(165, 222, 229, 0.85) 100%);
  border-radius: 9999px;
  box-shadow: 0 2px 8px rgba(74, 184, 196, 0.35), 0 1px 2px rgba(165, 222, 229, 0.4);
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), width 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), background 0.3s ease;
  pointer-events: none;
}

.capsule-link {
  position: relative;
  z-index: 2;
  padding: 10px 20px;
  cursor: pointer;
  pointer-events: auto;
  font-size: 15px;
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

.main-content::-webkit-scrollbar {
  display: none;
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

<template>
  <section class="page-shell staff-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
    <div class="staff-header">
      <h2>Staff / 剧本家</h2>
      <p class="page-description">批评空间剧本家排名人物（シナリオライター）。</p>
    </div>

    <p v-if="loading" class="loading-placeholder">加载中</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>
    <div v-else-if="!persons.length" class="empty-state">
      <p>暂无数据，请先运行 <code>py scripts/import_bangumi_index_persons.py</code> 导入。</p>
    </div>

    <div v-else class="staff-grid">
      <a
        v-for="p in persons"
        :key="p.bangumiPersonId"
        :href="`https://bangumi.tv/person/${p.bangumiPersonId}`"
        target="_blank"
        rel="noopener noreferrer"
        class="staff-card"
      >
        <div class="staff-avatar">
          <img
            v-if="p.imageUrl"
            :src="avatarUrl(p.imageUrl)"
            :alt="displayName(p)"
            loading="lazy"
          />
          <div v-else class="avatar-placeholder">
            {{ (displayName(p) || "?").slice(0, 1) }}
          </div>
        </div>
        <div class="staff-info">
          <span class="staff-name">{{ displayName(p) }}</span>
          <span v-if="p.relation" class="staff-relation">{{ p.relation }}</span>
        </div>
      </a>
    </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";

interface StaffPersonItem {
  bangumiPersonId: number;
  name: string;
  nameCn: string | null;
  nameJp: string | null;
  imageUrl: string | null;
  relation: string | null;
}

const loading = ref(false);
const ready = ref(false);
const error = ref("");
const persons = ref<StaffPersonItem[]>([]);

function displayName(p: StaffPersonItem): string {
  return p.nameCn || p.nameJp || p.name || "";
}

function avatarUrl(url: string): string {
  if (url.startsWith("//")) return `https:${url}`;
  return url;
}

async function loadData() {
  loading.value = true;
  error.value = "";
  try {
    const json = await apiGet<{ persons?: StaffPersonItem[] }>("/api/staff/persons");
    persons.value = json.persons ?? [];
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    loading.value = false;
    await nextTick();
    requestAnimationFrame(() => {
      ready.value = true;
    });
  }
}

onMounted(() => loadData());
</script>

<style scoped>
.staff-page {
  padding: 16px 20px;
}

.staff-header {
  margin-bottom: 20px;
}

.staff-header h2 {
  margin: 0 0 4px;
}

.staff-header .page-description {
  margin: 0;
  color: var(--color-text-muted);
}

.loading-placeholder,
.error-text {
  padding: 24px;
}

.error-text {
  color: var(--color-error, #c55);
}

.empty-state {
  padding: 32px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.empty-state code {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}

.staff-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.staff-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.staff-card:hover {
  border-color: var(--color-accent);
  box-shadow: 0 4px 12px rgba(165, 222, 229, 0.35);
}

.staff-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--color-bg);
}

.staff-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
  color: var(--color-text-muted);
  background: linear-gradient(135deg, #fefdca 0%, #e0f9b5 50%, #a5dee5 100%);
}

.staff-info {
  margin-top: 10px;
  text-align: center;
}

.staff-name {
  display: block;
  font-weight: 500;
  font-size: 14px;
  color: var(--color-text);
}

.staff-relation {
  display: block;
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}
</style>

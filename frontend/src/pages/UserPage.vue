<template>
  <section class="page-shell user-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
    <div class="user-header">
      <h2>User / 我的评分</h2>
      <p class="page-description">输入 Bangumi 用户 ID 或用户名，查看你在 Galgame 的评分情况。</p>
    </div>

    <div class="user-input-row">
      <input
        v-model.trim="userId"
        type="text"
        placeholder="用户 ID 或用户名，如 1 或 sai"
        class="user-input"
        @keyup.enter="loadData"
      />
      <button type="button" class="query-btn" :disabled="loading" @click="loadData">
        {{ loading ? "加载中…" : "查询" }}
      </button>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div v-else-if="items.length > 0" class="user-results">
      <p class="results-summary">{{ usernameOrId }} 的 Galgame 收藏：{{ items.length }} 部</p>
      <div class="collection-list">
        <a
          v-for="item in items"
          :key="item.subjectId"
          :href="`https://bangumi.tv/subject/${item.subjectId}`"
          target="_blank"
          rel="noopener noreferrer"
          class="collection-card"
        >
          <span class="col-score" :class="scoreClass(item.rate)">{{ item.rate || "—" }}</span>
          <div class="col-info">
            <span class="col-title">{{ item.subjectNameCn || item.subjectName || `#${item.subjectId}` }}</span>
            <span v-if="item.type" class="col-type">{{ typeLabel(item.type) }}</span>
            <span v-if="item.comment" class="col-comment">{{ item.comment }}</span>
          </div>
        </a>
      </div>
    </div>

    <div v-else-if="searched && !loading && !error" class="empty-state">
      <p>暂无 Galgame 收藏，或用户不存在。</p>
    </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { nextTick, ref } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";

interface UserCollectionItem {
  subjectId: number;
  subjectName: string;
  subjectNameCn: string;
  rate: number;
  type: string;
  comment: string;
}

const userId = ref("");
const loading = ref(false);
const ready = ref(true);
const error = ref("");
const searched = ref(false);
const usernameOrId = ref("");
const items = ref<UserCollectionItem[]>([]);

const TYPE_MAP: Record<string, string> = {
  "1": "想看",
  "2": "看过",
  "3": "在看",
  "4": "搁置",
  "5": "抛弃"
};

function typeLabel(t: string | number): string {
  return TYPE_MAP[String(t)] || String(t);
}

function scoreClass(rate: number): string {
  if (!rate) return "score-empty";
  if (rate >= 8) return "score-high";
  if (rate >= 6) return "score-mid";
  return "score-low";
}

async function loadData() {
  const id = userId.value;
  if (!id) {
    error.value = "请输入用户 ID 或用户名";
    return;
  }
  loading.value = true;
  ready.value = false;
  error.value = "";
  searched.value = true;
  try {
    const json = await apiGet<{ usernameOrId: string; items: UserCollectionItem[] }>(
      "/api/user/collections",
      { id }
    );
    usernameOrId.value = json.usernameOrId;
    items.value = json.items ?? [];
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
    items.value = [];
  } finally {
    loading.value = false;
    await nextTick();
    requestAnimationFrame(() => {
      ready.value = true;
    });
  }
}
</script>

<style scoped>
.user-page {
  padding: 16px 20px;
  min-height: 200px;
  display: block;
}

.user-header {
  margin-bottom: 20px;
}

.user-header h2 {
  margin: 0 0 4px;
}

.user-header .page-description {
  margin: 0;
  color: var(--color-text-muted);
}

.user-input-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 420px;
}

.user-input {
  flex: 1;
  padding: 10px 14px;
  font-size: 15px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
}

.user-input:focus {
  outline: none;
  border-color: var(--color-accent);
}

.query-btn {
  padding: 10px 20px;
  font-size: 15px;
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  white-space: nowrap;
}

.query-btn:hover:not(:disabled) {
  filter: brightness(1.05);
}

.query-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-text {
  color: var(--color-error, #c55);
  margin: 0 0 16px;
}

.results-summary {
  margin: 0 0 12px;
  color: var(--color-text-muted);
}

.collection-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.collection-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s;
}

.collection-card:hover {
  border-color: var(--color-accent);
}

.col-score {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  border-radius: 6px;
}

.score-high {
  background: rgba(165, 222, 229, 0.4);
  color: var(--color-accent);
}

.score-mid {
  background: rgba(180, 150, 80, 0.2);
  color: #b49650;
}

.score-low {
  background: rgba(180, 80, 80, 0.2);
  color: #b45050;
}

.score-empty {
  background: var(--color-bg);
  color: var(--color-text-muted);
}

.col-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.col-title {
  font-weight: 500;
  color: var(--color-text);
}

.col-type {
  font-size: 12px;
  color: var(--color-text-muted);
}

.col-comment {
  font-size: 13px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  padding: 24px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}
</style>

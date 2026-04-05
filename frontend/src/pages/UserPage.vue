<template>
  <section class="page-shell page-shell--fixed user-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
      <div class="user-header">
        <h2>User / 我的 Galgame</h2>
        <p class="page-description">
          数据来自 Bangumi 游戏收藏（subject_type=4），并<strong>仅保留本站 Galgame 标签库</strong>中的条目。评分与短评为你在条目上填写的内容。
        </p>
      </div>

      <div v-if="!isDev" class="user-input-row">
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
      <p v-else class="dev-banner">
        开发模式：已固定用户 <code>{{ defaultDevUserId }}</code>，自动加载 Galgame 收藏。
      </p>

      <p v-if="error" class="error-text">{{ error }}</p>

      <template v-else-if="searched && !loading">
        <div class="user-dashboard">
          <aside class="user-dist-column">
            <UserRatingHistogram
              :buckets="ratingBuckets"
              :total-rated="ratedCount"
              :avg-rating="avgRating"
            />
          </aside>

          <div class="user-list-column">
            <template v-if="items.length > 0">
              <div class="user-list-head">
                <h3 class="section-title">条目详情与短评</h3>
                <p class="results-summary">
                  {{ usernameOrId }} · Galgame <strong>{{ items.length }}</strong> 部（已打分
                  <strong>{{ ratedCount }}</strong> 部）
                </p>
              </div>
              <div class="user-list-scroll">
                <div class="collection-list">
                  <article v-for="item in sortedItems" :key="item.subjectId" class="collection-card">
                    <div class="col-head">
                      <span class="col-score" :class="scoreClass(item.rate)">{{ item.rate || "—" }}</span>
                      <div class="col-head-text">
                        <a
                          :href="bangumiSubjectUrl(item.subjectId)"
                          target="_blank"
                          rel="noopener noreferrer"
                          class="col-title"
                        >
                          {{ item.subjectNameCn || item.subjectName || `#${item.subjectId}` }}
                        </a>
                        <span v-if="item.type" class="col-type">{{ typeLabel(item.type) }}</span>
                      </div>
                    </div>
                    <blockquote v-if="item.comment && item.comment.trim()" class="col-comment-block">
                      {{ item.comment }}
                    </blockquote>
                    <p v-else class="col-no-comment">未填写短评</p>
                  </article>
                </div>
              </div>
            </template>
            <div v-else class="user-list-empty">
              <p>暂无 Galgame 收藏，或用户不存在。</p>
            </div>
          </div>
        </div>
      </template>

      <p v-else-if="!isDev && !searched && !loading" class="hint-text">输入 Bangumi 用户名或 ID 后点击查询。</p>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";
import UserRatingHistogram from "../components/UserRatingHistogram.vue";

const isDev = import.meta.env.DEV;
const defaultDevUserId = "linxyc29300";

interface UserCollectionItem {
  subjectId: number;
  subjectName: string;
  subjectNameCn: string;
  rate: number;
  type: string;
  comment: string;
}

const userId = ref(isDev ? defaultDevUserId : "");
const loading = ref(false);
const ready = ref(!isDev);
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

const ratingBuckets = computed(() => {
  const b = Array(10).fill(0);
  for (const it of items.value) {
    if (it.rate >= 1 && it.rate <= 10) b[it.rate - 1]++;
  }
  return b;
});

const ratedCount = computed(() => items.value.filter((i) => i.rate >= 1 && i.rate <= 10).length);

const avgRating = computed<number | null>(() => {
  const rated = items.value.filter((i) => i.rate >= 1 && i.rate <= 10);
  if (rated.length === 0) return null;
  const sum = rated.reduce((s, i) => s + i.rate, 0);
  return sum / rated.length;
});

const sortedItems = computed(() => {
  return [...items.value].sort((a, b) => {
    const ar = a.rate >= 1 && a.rate <= 10 ? a.rate : -1;
    const br = b.rate >= 1 && b.rate <= 10 ? b.rate : -1;
    if (br !== ar) return br - ar;
    const an = a.subjectNameCn || a.subjectName || "";
    const bn = b.subjectNameCn || b.subjectName || "";
    return an.localeCompare(bn, "zh-Hans");
  });
});

function typeLabel(t: string | number): string {
  return TYPE_MAP[String(t)] || String(t);
}

function scoreClass(rate: number): string {
  if (!rate) return "score-empty";
  if (rate >= 8) return "score-high";
  if (rate >= 6) return "score-mid";
  return "score-low";
}

function bangumiSubjectUrl(id: number): string {
  return `https://bangumi.tv/subject/${id}`;
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

onMounted(() => {
  if (isDev && userId.value) {
    void loadData();
  } else {
    ready.value = true;
  }
});
</script>

<style scoped>
.user-page {
  box-sizing: border-box;
  width: 100%;
  max-width: none;
  padding: 12px clamp(16px, 3vw, 40px) 12px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.user-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: clamp(16px, 2.5vw, 32px);
  align-items: stretch;
  width: 100%;
  flex: 1 1 auto;
  min-height: 0;
}

.user-dashboard > * {
  min-height: 0;
}

.user-dist-column {
  box-sizing: border-box;
  min-width: 0;
  width: 100%;
  max-width: 100%;
  padding: 16px 14px 18px;
  background: rgba(90, 157, 143, 0.06);
  border-radius: var(--radius-md);
  border: 1px solid rgba(90, 157, 143, 0.22);
}

.user-list-column {
  min-width: 0;
  max-width: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: none;
  background: transparent;
  border: none;
  border-radius: 0;
  overflow: hidden;
}

.user-list-head {
  flex-shrink: 0;
  padding: 0 0 12px;
  margin-bottom: 4px;
  border-bottom: 1px solid rgba(90, 157, 143, 0.2);
}

.user-list-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 14px 18px;
  -webkit-overflow-scrolling: touch;
  /* 隐藏滚动条，仍可用滚轮 / 触控滑动 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.user-list-scroll::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.user-list-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  color: var(--color-text-muted);
  font-size: 14px;
  text-align: center;
}

@media (max-width: 960px) {
  .user-dashboard {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .user-dist-column {
    max-width: 100%;
  }

  .user-list-column {
    max-height: none;
    min-height: 0;
  }
}

@media (min-width: 961px) {
  /* 左栏（评分分布外框）约占内容区宽度 40%，整框随栅格向右延伸，不再用 max-width 卡死 */
  .user-dashboard {
    grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
    gap: 6px;
  }

  .user-dist-column {
    padding-right: 8px;
  }

  .user-list-column .user-list-scroll {
    padding-left: 8px;
  }
}

.user-header {
  margin-bottom: 20px;
  flex-shrink: 0;
}

.user-header h2 {
  margin: 0 0 4px;
}

.user-header .page-description {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.55;
  font-size: 14px;
}

.dev-banner {
  flex-shrink: 0;
  margin: 0 0 20px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--color-text-muted);
  background: rgba(90, 157, 143, 0.08);
  border-radius: var(--radius-md);
  border: 1px dashed rgba(90, 157, 143, 0.35);
}

.dev-banner code {
  font-size: 12px;
  color: var(--color-accent);
}

.user-input-row {
  flex-shrink: 0;
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

.hint-text {
  flex-shrink: 0;
  margin: 0;
  color: var(--color-text-muted);
  font-size: 14px;
}

.error-text {
  flex-shrink: 0;
  color: var(--color-error, #c55);
  margin: 0 0 16px;
}

.section-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.user-list-head .results-summary {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.collection-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.collection-card {
  padding: 14px 0 16px;
  background: transparent;
  border: none;
  border-bottom: 1px solid rgba(90, 157, 143, 0.18);
  border-radius: 0;
}

.collection-card:last-child {
  border-bottom: none;
}

.col-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.col-score {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  border-radius: 8px;
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

.col-head-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.col-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text);
  text-decoration: none;
}

.col-title:hover {
  color: var(--color-accent);
  text-decoration: underline;
}

.col-type {
  font-size: 12px;
  color: var(--color-text-muted);
}

.col-comment-block {
  margin: 12px 0 0;
  padding: 0 0 0 12px;
  border-left: 3px solid rgba(90, 157, 143, 0.4);
  background: transparent;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.col-no-comment {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--color-text-muted);
  font-style: italic;
}

</style>

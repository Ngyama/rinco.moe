<template>
  <section class="page-shell">
    <PageProgressBar v-if="!ready" />
    <template v-else>
    <div class="page-header-row">
      <div>
        <h2>热门游戏评分结构</h2>
        <p class="page-description">{{ year }} 年发售、标记数最多的 {{ limit }} 部 Galgame，展示各分数人数、中位数等。</p>
      </div>
      <div class="controls">
        <label>
          年份：
          <select v-model.number="year" @change="loadHotGames">
            <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
          </select>
        </label>
        <label>
          数量：
          <select v-model.number="limit" @change="loadHotGames">
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
          </select>
        </label>
        <button type="button" class="reload-btn" @click="loadHotGames">刷新</button>
      </div>
    </div>

    <p v-if="loading" class="loading-placeholder">加载中</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <div v-else class="hot-layout">
      <aside class="hot-list">
        <h3>{{ year }} 最热门 Top {{ limit }}</h3>
        <ul>
          <li
            v-for="(g, i) in games"
            :key="g.bangumiSubjectId"
            class="hot-item"
            :class="{ active: selectedId === g.bangumiSubjectId }"
            @click="selectGame(g.bangumiSubjectId)"
          >
            <span class="hot-rank">{{ i + 1 }}</span>
            <div class="hot-info">
              <span class="hot-title">{{ g.titleJp }}</span>
              <span class="hot-meta">评分 {{ formatScore(g.score) }} · {{ g.ratingTotal }} 人</span>
            </div>
          </li>
        </ul>
      </aside>

      <main class="hot-detail">
        <p v-if="!selectedId" class="hint">点击左侧作品查看评分结构</p>
        <p v-else-if="detailLoading" class="loading-placeholder">加载详情</p>
        <template v-else-if="detail">
          <h3 class="detail-title">{{ detail.titleJp }}</h3>
          <div class="detail-meta">
            平均 {{ formatScore(detail.score) }} · 中位数 {{ detail.median.toFixed(1) }} · 众数 {{ detail.mode }} 分 ·
            标准差 {{ detail.stdDev.toFixed(2) }} · 极端分占比 {{ (detail.extremeRatio * 100).toFixed(1) }}% ·
            {{ detail.ratingTotal }} 人
          </div>

          <div class="bar-chart-wrap">
            <h4>各分数人数</h4>
            <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" class="bar-chart">
              <g v-for="(cnt, i) in detail.ratingCounts" :key="i">
                <rect
                  :x="marginLeft + i * barStep"
                  :y="yScale(cnt)"
                  :width="barWidth"
                  :height="barHeight(cnt)"
                  class="bar"
                />
                <text
                  :x="marginLeft + i * barStep + barWidth / 2"
                  :y="chartHeight - marginBottom + 18"
                  class="bar-label"
                >
                  {{ i + 1 }}
                </text>
                <text
                  :x="marginLeft + i * barStep + barWidth / 2"
                  :y="yScale(cnt) - 4"
                  class="bar-value"
                >
                  {{ cnt }}
                </text>
              </g>
            </svg>
          </div>
        </template>
      </main>
    </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";

interface HotGameItem {
  bangumiSubjectId: number;
  titleJp: string;
  score: number | null;
  ratingTotal: number | null;
  releaseDate: string | null;
}

interface HotGameDetail {
  bangumiSubjectId: number;
  titleJp: string;
  score: number;
  ratingTotal: number;
  releaseDate: string | null;
  median: number;
  mode: number;
  stdDev: number;
  extremeRatio: number;
  ratingCounts: number[];
}

const year = ref(2025);
const limit = ref(5);
const loading = ref(false);
const ready = ref(false);
const error = ref("");
const games = ref<HotGameItem[]>([]);

const yearOptions = Array.from({ length: 41 }, (_, i) => 1990 + i);
const selectedId = ref<number | null>(null);
const detail = ref<HotGameDetail | null>(null);
const detailLoading = ref(false);

const chartWidth = 420;
const chartHeight = 220;
const marginLeft = 40;
const marginBottom = 36;
const barStep = 38;
const barWidth = 28;

const maxCount = computed(() => {
  if (!detail.value?.ratingCounts?.length) return 1;
  return Math.max(...detail.value.ratingCounts, 1);
});

const innerChartHeight = chartHeight - marginBottom - 24;

function yScale(cnt: number): number {
  const barHeight = maxCount.value > 0 ? (cnt / maxCount.value) * innerChartHeight : 0;
  return chartHeight - marginBottom - barHeight;
}

function barHeight(cnt: number): number {
  return maxCount.value > 0 ? (cnt / maxCount.value) * innerChartHeight : 0;
}

function formatScore(score: number | null | undefined): string {
  if (score == null) return "-";
  return score.toFixed(4);
}

async function loadHotGames() {
  loading.value = true;
  error.value = "";
  try {
    const json = await apiGet<{ games?: HotGameItem[] }>("/api/structure/hot", {
      year: year.value,
      limit: limit.value
    });
    games.value = json.games ?? [];
    if (games.value.length > 0) {
      const stillInList = selectedId.value != null && games.value.some((g) => g.bangumiSubjectId === selectedId.value);
      if (!stillInList) selectedId.value = games.value[0].bangumiSubjectId;
    } else {
      selectedId.value = null;
    }
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

async function loadDetail(id: number) {
  detailLoading.value = true;
  detail.value = null;
  try {
    detail.value = await apiGet<HotGameDetail>(`/api/structure/hot/${id}`);
  } catch {
    detail.value = null;
  } finally {
    detailLoading.value = false;
  }
}

function selectGame(id: number) {
  selectedId.value = id;
}

watch(selectedId, (id) => {
  if (id) loadDetail(id);
});

onMounted(() => {
  loadHotGames();
});
</script>

<style scoped>
.hot-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  align-items: start;
}

.hot-list h3,
.hot-detail h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: var(--color-text);
}

.hot-list ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.2s, border-color 0.2s;
}

.hot-item:hover {
  background: var(--color-accent-soft);
}

.hot-item.active {
  background: var(--color-accent-soft);
  border-color: var(--color-accent);
}

.hot-rank {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.hot-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.hot-title {
  font-size: 14px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-meta {
  font-size: 12px;
  color: var(--color-text-muted);
}

.hint {
  color: var(--color-text-muted);
  padding: 40px 0;
}

.detail-title {
  font-size: 18px !important;
  margin-bottom: 8px !important;
}

.detail-meta {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 20px;
}

.bar-chart-wrap {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 20px;
  border: 1px solid var(--color-border);
}

.bar-chart-wrap h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--color-text-muted);
}

.bar-chart {
  width: 100%;
  max-width: 420px;
  height: auto;
}

.bar {
  fill: var(--color-accent);
  opacity: 0.85;
  transition: opacity 0.2s;
}

.bar:hover {
  opacity: 1;
}

.bar-label {
  font-size: 11px;
  fill: var(--color-text-muted);
  text-anchor: middle;
}

.bar-value {
  font-size: 10px;
  fill: var(--color-text);
  text-anchor: middle;
}

@media (max-width: 640px) {
  .hot-layout {
    grid-template-columns: 1fr;
  }
}
</style>

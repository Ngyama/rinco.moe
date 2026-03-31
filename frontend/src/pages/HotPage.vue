<template>
  <section class="page-shell hot-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
    <div class="page-header-row">
      <div>
        <h2>热门游戏评分结构</h2>
        <p class="page-description">
          {{ year }} 年发售、在「Galgame 标签 Top10」内的作品中，先从标记数（投票人数）较多的候选池取一批，再按下方<strong>排序</strong>取 Top {{ limit }}。右侧为 1–10 分人数<strong>横向</strong>条形图；底部「两作分布对比」为<strong>单图双柱</strong>；列表标题链到 Bangumi 条目页。
        </p>
      </div>
      <div class="controls controls-wrap">
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
        <label>
          排序：
          <select v-model="sortBy" @change="loadHotGames">
            <option value="markers">标记数（人数）</option>
            <option value="score">均分</option>
            <option value="rating">投票人数</option>
            <option value="controversy">争议度（分分布标准差）</option>
          </select>
        </label>
        <button type="button" class="reload-btn" @click="loadHotGames">刷新</button>
      </div>
    </div>

    <p v-if="loading" class="loading-placeholder">加载中</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <div v-else class="hot-page-content">
    <div class="hot-layout" :class="{ 'hot-layout--detail': !!detail }">
      <aside class="hot-list">
        <h3>{{ year }} 热门 Top {{ limit }}</h3>
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
              <a
                class="hot-title hot-title-link"
                :href="bangumiSubjectUrl(g.bangumiSubjectId)"
                target="_blank"
                rel="noopener noreferrer"
                @click.stop
              >
                {{ g.titleJp }}
              </a>
              <span class="hot-meta">
                评分 {{ formatScore(g.score) }} · {{ g.ratingTotal ?? 0 }} 人
                <span v-if="g.controversy != null" class="hot-meta-sub"> · 争议 {{ g.controversy.toFixed(2) }}</span>
              </span>
            </div>
          </li>
        </ul>
      </aside>

      <main class="hot-detail" :class="{ 'hot-detail--filled': !!detail }">
        <p v-if="!selectedId" class="hint">点击左侧作品查看评分结构</p>
        <p v-else-if="detailLoading" class="loading-placeholder">加载详情</p>
        <template v-else-if="detail">
          <h3 class="detail-title">
            <a
              class="detail-title-link"
              :href="bangumiSubjectUrl(detail.bangumiSubjectId)"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ detail.titleJp }}
            </a>
          </h3>
          <div class="detail-meta">
            平均 {{ formatScore(detail.score) }} · 中位数 {{ detail.median.toFixed(1) }} · 众数 {{ detail.mode }} 分 ·
            标准差 {{ detail.stdDev.toFixed(2) }} ·
            <abbr class="extreme-abbr" title="极端分占比 = (打 1 分的人数 + 打 10 分的人数) ÷ 总投票人数。数值高表示两极评价多。">
              极端分占比 {{ (detail.extremeRatio * 100).toFixed(1) }}%
            </abbr>
            · {{ detail.ratingTotal }} 人
          </div>

          <div class="detail-charts-stack">
            <HotHeatStrip
              :rating-counts="detail.ratingCounts"
              :median="detail.median"
              :mode="detail.mode"
            />

            <div class="bar-chart-wrap bar-chart-wrap--flat hot-detail-bar">
              <h4 class="hot-detail-bar-heading">各分数人数（横向）</h4>
              <div class="hot-detail-bar-svg">
                <HotBarSvg
                  orientation="horizontal"
                  :rating-counts="detail.ratingCounts"
                  :chart-width="chartWidth"
                  :chart-height="chartHeight"
                />
              </div>
            </div>
          </div>
        </template>
      </main>
    </div>

    <section class="compare-panel compare-section">
      <div class="compare-head">
        <h3 class="compare-title">两作分布对比</h3>
      </div>
      <p class="compare-hint">
        从当前列表选两部作品；同一图中每个分数下两根柱分别为作品 A / B 的人数（共用纵轴刻度）。
      </p>
      <template v-if="games.length >= 2">
        <div class="compare-selects">
          <label class="compare-field">
            作品 A
            <select v-model.number="compareAId" class="compare-select">
              <option v-for="g in games" :key="`a-${g.bangumiSubjectId}`" :value="g.bangumiSubjectId">
                {{ g.titleJp }}
              </option>
            </select>
          </label>
          <label class="compare-field">
            作品 B
            <select v-model.number="compareBId" class="compare-select">
              <option v-for="g in games" :key="`b-${g.bangumiSubjectId}`" :value="g.bangumiSubjectId">
                {{ g.titleJp }}
              </option>
            </select>
          </label>
        </div>
        <p v-if="compareLoading" class="loading-placeholder">加载对比</p>
        <div v-else-if="detailCompareA && detailCompareB" class="compare-dual-chart">
          <HotCompareBarSvg
            :rating-counts-a="detailCompareA.ratingCounts"
            :rating-counts-b="detailCompareB.ratingCounts"
            :title-a="detailCompareA.titleJp"
            :title-b="detailCompareB.titleJp"
            :chart-width="compareDualW"
            :chart-height="compareDualH"
          />
        </div>
      </template>
      <p v-else class="hint compare-hint-empty">当前列表不足两部，无法对比</p>
    </section>
    </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";
import HotBarSvg from "../components/HotBarSvg.vue";
import HotCompareBarSvg from "../components/HotCompareBarSvg.vue";
import HotHeatStrip from "../components/HotHeatStrip.vue";

interface HotGameItem {
  bangumiSubjectId: number;
  titleJp: string;
  score: number | null;
  ratingTotal: number | null;
  releaseDate: string | null;
  controversy?: number;
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
const limit = ref(10);
const sortBy = ref<"markers" | "score" | "rating" | "controversy">("markers");
const loading = ref(false);
const ready = ref(false);
const error = ref("");
const games = ref<HotGameItem[]>([]);

const yearOptions = Array.from({ length: 41 }, (_, i) => 1990 + i);
const selectedId = ref<number | null>(null);
const detail = ref<HotGameDetail | null>(null);
const detailLoading = ref(false);

const compareAId = ref<number | null>(null);
const compareBId = ref<number | null>(null);
const detailCompareA = ref<HotGameDetail | null>(null);
const detailCompareB = ref<HotGameDetail | null>(null);
const compareLoading = ref(false);

const chartWidth = 600;
const chartHeight = 236;
const compareDualW = 720;
const compareDualH = 268;

function bangumiSubjectUrl(id: number): string {
  return `https://bangumi.tv/subject/${id}`;
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
      limit: limit.value,
      sort: sortBy.value,
      _t: Date.now()
    });
    games.value = json.games ?? [];
    if (games.value.length > 0) {
      const stillInList = selectedId.value != null && games.value.some((g) => g.bangumiSubjectId === selectedId.value);
      if (!stillInList) selectedId.value = games.value[0].bangumiSubjectId;
      if (games.value.length >= 2) {
        compareAId.value = games.value[0].bangumiSubjectId;
        compareBId.value = games.value[1].bangumiSubjectId;
      } else {
        compareAId.value = games.value[0].bangumiSubjectId;
        compareBId.value = games.value[0].bangumiSubjectId;
      }
    } else {
      selectedId.value = null;
      compareAId.value = null;
      compareBId.value = null;
    }
    detailCompareA.value = null;
    detailCompareB.value = null;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    loading.value = false;
    await nextTick();
    requestAnimationFrame(() => {
      ready.value = true;
    });
  }
  await loadCompareDetails();
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

async function loadCompareDetails() {
  if (compareAId.value == null || compareBId.value == null) {
    detailCompareA.value = null;
    detailCompareB.value = null;
    return;
  }
  compareLoading.value = true;
  try {
    const [a, b] = await Promise.all([
      apiGet<HotGameDetail>(`/api/structure/hot/${compareAId.value}`),
      apiGet<HotGameDetail>(`/api/structure/hot/${compareBId.value}`)
    ]);
    detailCompareA.value = a;
    detailCompareB.value = b;
  } catch {
    detailCompareA.value = null;
    detailCompareB.value = null;
  } finally {
    compareLoading.value = false;
  }
}

function selectGame(id: number) {
  selectedId.value = id;
}

watch(selectedId, (id) => {
  if (id) loadDetail(id);
});

watch([compareAId, compareBId], () => {
  loadCompareDetails();
});

onMounted(() => {
  loadHotGames();
});
</script>

<style scoped>
.hot-page {
  padding-bottom: 32px;
}

.hot-page-content {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.page-header-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 8px;
}

.controls-wrap {
  flex-wrap: wrap;
}

.loading-placeholder,
.error-text {
  padding: 24px;
}

.error-text {
  color: var(--color-error, #c55);
}

.hot-layout {
  display: grid;
  grid-template-columns: minmax(300px, 400px) 1fr;
  gap: 24px;
  align-items: stretch;
}

/** 有详情时：左侧列表与右侧「热力条+横向图」底边对齐（同列拉伸后内容贴底） */
.hot-layout--detail .hot-list ul {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 0;
}

.hot-list {
  display: flex;
  flex-direction: column;
  align-self: stretch;
  min-height: 0;
}

.hot-list ul {
  flex: 1 1 auto;
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

.hot-title-link {
  font-size: 14px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-decoration: none;
  font-weight: 500;
}

.hot-title-link:hover {
  color: var(--color-accent);
  text-decoration: underline;
}

.hot-meta {
  font-size: 12px;
  color: var(--color-text-muted);
}

.hot-meta-sub {
  color: var(--color-text-muted);
  opacity: 0.9;
}

.hint {
  color: var(--color-text-muted);
  padding: 24px 0;
}

.detail-title {
  font-size: 18px !important;
  margin-bottom: 8px !important;
}

.detail-title-link {
  color: inherit;
  text-decoration: none;
}

.detail-title-link:hover {
  color: var(--color-accent);
  text-decoration: underline;
}

.detail-meta {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 20px;
  line-height: 1.6;
}

.extreme-abbr {
  cursor: help;
  text-decoration: underline dotted;
  text-underline-offset: 2px;
}

.hot-detail--filled {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  min-width: 0;
}

.detail-charts-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
  flex: 1 1 auto;
  justify-content: flex-start;
  min-height: 0;
}

.bar-chart-wrap {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 20px;
  border: 1px solid var(--color-border);
}

.bar-chart-wrap--flat {
  background: transparent;
  border: none;
  padding: 0;
  border-radius: 0;
}

.bar-chart-wrap h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--color-text-muted);
}

/* 横向条形图：占满详情区剩余高度，随列拉伸放大 */
.hot-detail-bar {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  max-width: min(1080px, 100%);
}

.hot-detail-bar-heading {
  flex-shrink: 0;
}

.hot-detail-bar-svg {
  flex: 1 1 auto;
  min-height: 240px;
  display: flex;
  align-items: stretch;
  justify-content: stretch;
}

.hot-detail-bar-svg :deep(.bar-chart) {
  display: block;
  width: 100%;
  height: 100%;
  max-height: none;
}

.compare-panel.compare-section {
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid var(--color-border);
  width: 100%;
}

.compare-head {
  margin-bottom: 8px;
}

.compare-title {
  margin: 0 !important;
  font-size: 16px !important;
}

.compare-hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.compare-hint-empty {
  margin: 0;
}

.compare-selects {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.compare-selects label.compare-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--color-text-muted);
}

.compare-select {
  min-width: min(280px, 100%);
  max-width: 100%;
  padding: 8px 0 10px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--color-text);
  background: transparent;
  border: none;
  border-bottom: 1px solid rgba(90, 157, 143, 0.35);
  border-radius: 0;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
  appearance: none;
  background-image: linear-gradient(45deg, transparent 50%, rgba(90, 157, 143, 0.65) 50%),
    linear-gradient(135deg, rgba(90, 157, 143, 0.65) 50%, transparent 50%);
  background-position: calc(100% - 14px) calc(50% + 3px), calc(100% - 8px) calc(50% + 3px);
  background-size: 6px 6px, 6px 6px;
  background-repeat: no-repeat;
}

.compare-select:hover {
  border-bottom-color: rgba(90, 157, 143, 0.65);
}

.compare-select:focus {
  outline: none;
  border-bottom-color: var(--color-accent);
}

.compare-dual-chart {
  width: 100%;
  max-width: min(920px, 100%);
  margin-top: 4px;
}

.compare-dual-chart :deep(.compare-svg) {
  min-height: 240px;
}

@media (max-width: 640px) {
  .hot-layout {
    grid-template-columns: 1fr;
  }
}
</style>

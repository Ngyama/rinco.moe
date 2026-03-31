<template>
  <section class="page-shell controversy-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
    <div class="controversy-header">
      <div>
        <h2>争议分布图</h2>
        <p class="page-description">横轴为 Bangumi 评分人数（线性）；纵轴为评分（圆心纵坐标 = 分数，与网格刻度一致）；气泡半径按人数在当前数据范围内映射；颜色越红争议越大。作品范围：{{ yearFrom }}{{ yearFrom !== yearTo ? `–${yearTo}` : '' }} 年。</p>
      </div>
      <div class="controls">
        <label>
          起始年：
          <select v-model.number="yearFrom" @change="onYearChange">
            <option v-for="y in yearOptions" :key="`from-${y}`" :value="y">{{ y }}</option>
          </select>
        </label>
        <label>
          结束年：
          <select v-model.number="yearTo" @change="onYearChange">
            <option v-for="y in yearOptions" :key="`to-${y}`" :value="y">{{ y }}</option>
          </select>
        </label>
        <label>
          最低投票：
          <select v-model.number="minVotesFilter" @change="loadData">
            <option :value="0">0</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </label>
        <button type="button" class="reload-btn" @click="loadData">刷新</button>
      </div>
    </div>

    <p v-if="loading" class="loading-placeholder">加载中</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>
    <div v-else-if="!items.length" class="empty-state">
      <p>暂无数据（可能无 {{ yearFrom }}{{ yearFrom !== yearTo ? `–${yearTo}` : '' }} 年发售的 Galgame 或需降低最低投票）。</p>
    </div>

    <div v-else class="viz-container">
      <div class="chart-wrap">
        <svg
          :viewBox="`0 0 ${svgW} ${svgH}`"
          preserveAspectRatio="xMidYMid meet"
          class="viz-svg"
          :style="{ aspectRatio: `${svgW} / ${svgH}` }"
          @mousemove="onChartMouseMove"
          @mouseleave="onChartMouseLeave"
        >
          <g v-for="(tick, i) in scoreTicks" :key="`y-${i}`">
            <line :x1="marginL" :x2="marginL + innerW" :y1="yScale(tick)" :y2="yScale(tick)" stroke="rgba(90, 157, 143, 0.35)" stroke-dasharray="2,2" />
            <text :x="marginL - 8" :y="yScale(tick) + 4" class="tick-label">{{ tick }}</text>
          </g>
          <g v-for="(tick, i) in voteTicks" :key="`x-${i}`">
            <line :x1="xScale(tick)" :y1="marginT" :x2="xScale(tick)" :y2="marginT + innerH" stroke="rgba(90, 157, 143, 0.35)" stroke-dasharray="2,2" />
            <text :x="xScale(tick)" :y="svgH - 14" class="tick-label" text-anchor="middle">{{ tick }}</text>
          </g>
          <circle
            v-for="d in items"
            :key="d.bangumiSubjectId"
            :cx="bubblePos(d).cx"
            :cy="bubblePos(d).cy"
            :r="bubblePos(d).r"
            :title="`${d.titleJp} · ${d.score.toFixed(2)} 分`"
            :fill="controversyColor(d.controversy)"
            :opacity="hoveredId === d.bangumiSubjectId ? 1 : 0.8"
            stroke="rgba(90, 157, 143, 0.4)"
            stroke-width="0.5"
          />
          <g v-if="hoveredItem" class="tooltip">
            <rect :x="tooltipX" :y="tooltipY" width="220" height="64" rx="6" fill="rgba(255,255,255,0.96)" stroke="rgba(90, 157, 143, 0.5)" />
            <text :x="tooltipX + 12" :y="tooltipY + 22" class="tooltip-text">{{ hoveredItem.titleJp }}</text>
            <text :x="tooltipX + 12" :y="tooltipY + 42" class="tooltip-meta">分数 {{ hoveredItem.score.toFixed(2) }} · {{ hoveredItem.ratingTotal }} 人 · 争议 {{ hoveredItem.controversy.toFixed(2) }}</text>
          </g>
        </svg>
        <div class="color-legend">
          <span class="legend-label">争议度</span>
          <div class="legend-bar" :style="{ background: `linear-gradient(to right, ${controversyColor(0)}, ${controversyColor(maxControversy)})` }"></div>
        </div>
      </div>
    </div>
    <ControversyExtremeBars
      v-if="!loading"
      :year-from="yearFrom"
      :year-to="yearTo"
      :min-votes="minVotesFilter"
      :items="extremeItems"
      :loading="extremeLoading"
      :error="extremeError"
    />
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";
import ControversyExtremeBars from "../components/ControversyExtremeBars.vue";

interface ControversyItem {
  bangumiSubjectId: number;
  titleJp: string;
  score: number;
  ratingTotal: number;
  controversy: number;
}

interface ExtremeBarRow {
  bangumiSubjectId: number;
  titleJp: string;
  ratingTotal: number;
  lowShare: number;
  highShare: number;
}

const yearFrom = ref(2025);
const yearTo = ref(2025);
const minVotesFilter = ref(20);
const loading = ref(false);
const ready = ref(false);
const error = ref("");
const items = ref<ControversyItem[]>([]);
const extremeItems = ref<ExtremeBarRow[]>([]);
const extremeLoading = ref(false);
const extremeError = ref("");

const yearOptions = Array.from({ length: 41 }, (_, i) => 1990 + i);

function onYearChange() {
  if (yearFrom.value > yearTo.value) yearTo.value = yearFrom.value;
  loadData();
}
const hoveredId = ref<number | null>(null);
const mouseX = ref(0);
const mouseY = ref(0);

/** Only widen X; keep svgH / margins so vertical mapping (score) unchanged. */
const svgW = 1240;
/** Extra bottom margin so x-axis tick labels stay inside viewBox (not clipped). */
const svgH = 662;
const marginL = 60;
const marginT = 24;
const marginR = 40;
const marginB = 52;
const innerW = svgW - marginL - marginR;
const innerH = svgH - marginT - marginB;

/** Same vertical domain as original chart: score min/max ± 0.5, clamped to [0,10]. */
const SCORE_AXIS_PAD = 0.5;

/** Radius vs votes (linear on current data range); strong contrast 几十 vs 几百. */
const BUBBLE_R_MIN = 4;
const BUBBLE_R_MAX = 30;

const dataMinVotes = computed(() => Math.min(...items.value.map((d) => d.ratingTotal), 1));
const maxVotes = computed(() => Math.max(...items.value.map((d) => d.ratingTotal), 1));
/** Must match original semantics so yScale ↔ grid ticks stay aligned with data scores. */
const minScore = computed(() => Math.min(...items.value.map((d) => d.score), 0));
const maxScore = computed(() => Math.max(...items.value.map((d) => d.score), 10));
const maxControversy = computed(() => Math.max(...items.value.map((d) => d.controversy), 0.01));

/** Linear: 人数 → x（只改横向映射，不影响纵轴）。 */
function xScale(v: number): number {
  const lo = Math.max(0, dataMinVotes.value);
  const hi = Math.max(maxVotes.value, lo + 1);
  if (hi <= lo) return marginL + innerW / 2;
  const t = (Math.max(v, 0) - lo) / (hi - lo);
  return marginL + t * innerW;
}

/** 纵轴：与原先逻辑一致，仅用 SCORE_AXIS_PAD=0.5。 */
function yScale(s: number): number {
  const lo = Math.max(0, minScore.value - SCORE_AXIS_PAD);
  const hi = Math.min(10, maxScore.value + SCORE_AXIS_PAD);
  const t = (s - lo) / (hi - lo || 1);
  return marginT + (1 - t) * innerH;
}

function bubbleR(votes: number): number {
  const lo = Math.max(0, dataMinVotes.value);
  const hi = Math.max(maxVotes.value, lo + 1);
  if (hi <= lo) return (BUBBLE_R_MIN + BUBBLE_R_MAX) / 2;
  const t = (Math.max(votes, 0) - lo) / (hi - lo);
  return BUBBLE_R_MIN + t * (BUBBLE_R_MAX - BUBBLE_R_MIN);
}

/**
 * 圆心必须严格落在 yScale(score) 上。此前碰撞松弛会移动 cy，且 layout 里 yOf 与 yScale 的 min/max 域不一致，
 * 会出现「7 分画在 9 分附近」。此处不再做纵向错开。
 */
function bubblePos(d: ControversyItem) {
  return { cx: xScale(d.ratingTotal), cy: yScale(d.score), r: bubbleR(d.ratingTotal) };
}

function controversyColor(c: number): string {
  const t = Math.min(c / Math.max(maxControversy.value, 0.01), 1);
  const r = Math.round(107 + (74 - 107) * t);
  const g = Math.round(181 + (126 - 181) * t);
  const b = Math.round(165 + (115 - 165) * t);
  return `rgb(${r},${g},${b})`;
}

const scoreTicks = computed(() => {
  const lo = Math.floor(minScore.value);
  const hi = Math.ceil(maxScore.value);
  const arr: number[] = [];
  for (let i = lo; i <= hi; i++) arr.push(i);
  return arr.length ? arr : [0, 5, 10];
});

function linearVoteTicks(lo: number, hi: number): number[] {
  if (hi <= lo) return [Math.round(lo)];
  const span = hi - lo;
  const rough = span / 5.5;
  const pow10 = Math.pow(10, Math.floor(Math.log10(Math.max(rough, 1e-9))));
  const n = rough / pow10;
  const step = n <= 1 ? pow10 : n <= 2 ? 2 * pow10 : n <= 5 ? 5 * pow10 : 10 * pow10;
  const start = Math.ceil(lo / step) * step;
  const out: number[] = [];
  for (let v = start; v <= hi + step * 0.001; v += step) {
    out.push(Math.round(v));
  }
  if (out.length && out[0] > lo) out.unshift(Math.round(lo));
  if (out.length && out[out.length - 1] < hi) out.push(Math.round(hi));
  const uniq = [...new Set(out)].sort((a, b) => a - b);
  return uniq.slice(0, 12);
}

const voteTicks = computed(() => {
  const lo = Math.max(0, dataMinVotes.value);
  const hi = Math.max(maxVotes.value, lo + 1);
  return linearVoteTicks(lo, hi);
});

const hoveredItem = computed(() => {
  if (hoveredId.value == null) return null;
  return items.value.find((d) => d.bangumiSubjectId === hoveredId.value);
});

const tooltipX = computed(() => Math.min(Math.max(mouseX.value, 12), svgW - 232));
const tooltipY = computed(() => Math.min(Math.max(mouseY.value, 12), svgH - 80));

function onChartMouseMove(e: MouseEvent) {
  const rect = (e.currentTarget as SVGElement).getBoundingClientRect();
  mouseX.value = ((e.clientX - rect.left) / rect.width) * svgW;
  mouseY.value = ((e.clientY - rect.top) / rect.height) * svgH;
  const x = ((e.clientX - rect.left) / rect.width) * svgW;
  const y = ((e.clientY - rect.top) / rect.height) * svgH;
  let nearest: ControversyItem | null = null;
  let minD = Infinity;
  for (const d of items.value) {
    const { cx, cy, r } = bubblePos(d);
    const d2 = (x - cx) ** 2 + (y - cy) ** 2;
    if (d2 <= r * r * 4 && d2 < minD) {
      minD = d2;
      nearest = d;
    }
  }
  hoveredId.value = nearest?.bangumiSubjectId ?? null;
}

function onChartMouseLeave() {
  hoveredId.value = null;
}

async function loadData() {
  loading.value = true;
  extremeLoading.value = true;
  error.value = "";
  extremeError.value = "";
  const params = {
    yearFrom: yearFrom.value,
    yearTo: yearTo.value,
    minVotes: minVotesFilter.value
  };
  const results = await Promise.allSettled([
    apiGet<{ items?: ControversyItem[] }>("/api/structure/controversy", params),
    apiGet<{ items?: ExtremeBarRow[] }>("/api/structure/controversy/extreme-bars", params)
  ]);
  if (results[0].status === "fulfilled") {
    items.value = results[0].value.items ?? [];
    error.value = "";
  } else {
    items.value = [];
    const r = results[0].reason;
    error.value = r instanceof Error ? r.message : "加载失败";
  }
  if (results[1].status === "fulfilled") {
    extremeItems.value = results[1].value.items ?? [];
    extremeError.value = "";
  } else {
    extremeItems.value = [];
    const r = results[1].reason;
    extremeError.value = r instanceof Error ? r.message : "加载失败";
  }
  loading.value = false;
  extremeLoading.value = false;
  await nextTick();
  requestAnimationFrame(() => {
    ready.value = true;
  });
}

onMounted(() => loadData());
</script>

<style scoped>
.controversy-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 60px);
  padding: 16px 20px 40px;
  box-sizing: border-box;
  /* Let main-content scroll; fixed height + overflow:hidden was clipping the chart bottom (x-axis). */
  overflow-x: hidden;
  overflow-y: visible;
}

.controversy-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  flex-shrink: 0;
  margin-bottom: 12px;
}

.controversy-header h2 {
  margin: 0 0 4px;
}

.controversy-header .page-description {
  margin: 0;
}

.controversy-header .controls {
  flex-shrink: 0;
}

.viz-container {
  flex: 0 1 auto;
  display: flex;
  flex-direction: column;
}

.chart-wrap {
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-md);
  padding: 20px;
}

/* Uniform scale only: same aspect as viewBox so circles stay round. */
.viz-svg {
  display: block;
  width: 100%;
  max-width: 100%;
  height: auto;
  flex: 0 0 auto;
}

.tick-label {
  font-size: 9px;
  fill: var(--color-text-muted);
}

.tooltip-text {
  font-size: 13px;
  fill: var(--color-text);
}

.tooltip-meta {
  font-size: 11px;
  fill: var(--color-text-muted);
}

.color-legend {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  flex-shrink: 0;
}

.legend-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.legend-bar {
  width: 120px;
  height: 10px;
  border-radius: 4px;
}

@media (max-width: 640px) {
  .controversy-header {
    flex-direction: column;
  }
}
</style>

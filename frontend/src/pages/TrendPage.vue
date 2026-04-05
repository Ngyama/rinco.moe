<template>
  <section class="page-shell page-shell--fixed trend-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
      <div class="trend-page-head">
      <h2>时间趋势 - Bangumi 历史评分</h2>
      <p class="page-description">横轴为发售年份，点表示每部作品分数，折线表示当年平均分。悬停点可看作品名与分数。</p>

      <div class="controls">
      <label>
        最低投票：
        <select v-model.number="minVotes" @change="loadData">
          <option :value="0">0</option>
          <option :value="50">50</option>
          <option :value="100">100</option>
          <option :value="200">200</option>
        </select>
      </label>
      <label>
        起始年：
        <select v-model.number="startYear" @change="onStartYearChange">
          <option v-for="y in selectableYears" :key="`start-${y}`" :value="y">{{ y }}</option>
        </select>
      </label>
      <label>
        结束年：
        <select v-model.number="endYear" @change="onEndYearChange">
          <option v-for="y in selectableYears" :key="`end-${y}`" :value="y">{{ y }}</option>
        </select>
      </label>
      <button type="button" class="reload-btn" @click="loadData">刷新</button>
    </div>
      </div>

      <p v-if="loading" class="loading-placeholder">加载中</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>
      <div v-else-if="!hasData" class="empty-state">
        <p>暂无可用数据（可能需要先补齐 Bangumi 的发售日期字段）。</p>
      </div>

      <div v-else class="chart-block">
        <div class="year-summary-slot">
          <div v-if="hoverSummary" class="year-summary">
            <strong>{{ hoverSummary.year }} 年</strong>
            <span>均分 {{ hoverSummary.avgScore.toFixed(4) }}</span>
            <span>样本 {{ hoverSummary.count }}</span>
            <span>最高 {{ hoverSummary.top.title }} ({{ hoverSummary.top.score.toFixed(4) }})</span>
            <span>最低 {{ hoverSummary.bottom.title }} ({{ hoverSummary.bottom.score.toFixed(4) }})</span>
          </div>
        </div>
        <div class="chart-wrap">
      <svg
        :viewBox="`0 0 ${svgWidth} ${svgHeight}`"
        role="img"
        aria-label="Bangumi 年度评分趋势图"
        @mousemove="onChartMouseMove"
        @mouseleave="onChartMouseLeave"
        @click="onChartClick"
      >
        <line :x1="marginLeft" :y1="marginTop" :x2="marginLeft" :y2="svgHeight - marginBottom" class="axis" />
        <line :x1="marginLeft" :y1="svgHeight - marginBottom" :x2="svgWidth - marginRight" :y2="svgHeight - marginBottom" class="axis" />

        <g v-for="tick in scoreTicks" :key="`y-${tick}`">
          <line :x1="marginLeft - 5" :x2="marginLeft" :y1="yScale(tick)" :y2="yScale(tick)" class="tick" />
          <text :x="marginLeft - 10" :y="yScale(tick) + 4" class="tick-label tick-label-y">{{ tick.toFixed(1) }}</text>
        </g>

        <g v-for="year in yearTickLabels" :key="`x-${year}`">
          <line :x1="xScale(year)" :x2="xScale(year)" :y1="svgHeight - marginBottom" :y2="svgHeight - marginBottom + 5" class="tick" />
          <text
            :x="xScale(year)"
            :y="svgHeight - marginBottom + 24"
            class="tick-label tick-label-x tick-label-year"
            :transform="`rotate(-35 ${xScale(year)} ${svgHeight - marginBottom + 24})`"
          >{{ year }}</text>
        </g>

        <g v-for="box in yearBoxes" :key="`box-${box.year}`">
          <line :x1="box.x" :x2="box.x" :y1="box.yMin" :y2="box.yMax" class="box-whisker" />
          <rect :x="box.x - 4" :y="box.yQ3" width="8" :height="Math.max(1, box.yQ1 - box.yQ3)" class="box-body" />
          <line :x1="box.x - 4" :x2="box.x + 4" :y1="box.yMedian" :y2="box.yMedian" class="box-median" />
        </g>

        <path :d="averageLinePath" class="avg-line" />

        <line
          v-if="activeYear !== null"
          :x1="xScaleDate(`${activeYear}-07-01`, activeYear)"
          :x2="xScaleDate(`${activeYear}-07-01`, activeYear)"
          :y1="marginTop"
          :y2="svgHeight - marginBottom"
          class="hover-year-line"
        />

        <circle
          v-for="avg in averageDots"
          :key="`avg-${avg.year}`"
          :cx="avg.x"
          :cy="avg.y"
          r="3.2"
          class="avg-dot"
        >
          <title>{{ `${avg.year} 平均分 ${avg.score.toFixed(4)}（样本 ${avg.count}）` }}</title>
        </circle>

        <circle
          v-for="(pt, idx) in pointDots"
          :key="`pt-${idx}`"
          :cx="pt.x"
          :cy="pt.y"
          r="2.4"
          class="score-dot"
          :class="{ highlighted: activeYear !== null && pt.year === activeYear }"
        >
          <title>{{ `${pt.title} (${pt.releaseDate || pt.year})：${pt.score.toFixed(4)}` }}</title>
        </circle>
      </svg>
        </div>
    </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";

interface BangumiYearAverageItem {
  year: number;
  avgScore: number;
  count: number;
}

interface BangumiYearPointItem {
  year: number;
  releaseDate: string;
  title: string;
  score: number;
}

interface BangumiTrendResponse {
  minVotes: number;
  averages: BangumiYearAverageItem[];
  points: BangumiYearPointItem[];
}

const minVotes = ref(50);
const loading = ref(false);
const ready = ref(false);
const error = ref("");
const averages = ref<BangumiYearAverageItem[]>([]);
const points = ref<BangumiYearPointItem[]>([]);
const hoverYear = ref<number | null>(null);
const lockedYear = ref<number | null>(null);
const startYear = ref<number | null>(null);
const endYear = ref<number | null>(null);
const rangeInitialized = ref(false);

const svgWidth = 1120;
const svgHeight = 560;
const marginLeft = 56;
const marginRight = 28;
const marginTop = 24;
const marginBottom = 44;

const allYears = computed(() => {
  const set = new Set<number>();
  for (const item of averages.value) set.add(item.year);
  for (const item of points.value) set.add(item.year);
  return Array.from(set).sort((a, b) => a - b);
});

const selectableYears = computed(() => allYears.value);

const yearRange = computed(() => ({
  min: startYear.value ?? allYears.value[0] ?? 1990,
  max: endYear.value ?? allYears.value[allYears.value.length - 1] ?? 2026
}));

const filteredAverages = computed(() =>
  averages.value.filter((it) => it.year >= yearRange.value.min && it.year <= yearRange.value.max)
);
const filteredPoints = computed(() =>
  points.value.filter((it) => it.year >= yearRange.value.min && it.year <= yearRange.value.max)
);

const hasData = computed(() => filteredAverages.value.length > 0 || filteredPoints.value.length > 0);
const activeYear = computed(() => lockedYear.value ?? hoverYear.value);

const years = computed(() => {
  const set = new Set<number>();
  for (const item of filteredAverages.value) set.add(item.year);
  for (const item of filteredPoints.value) set.add(item.year);
  return Array.from(set).sort((a, b) => a - b);
});

const minYear = computed(() => years.value[0] ?? 1990);
const maxYear = computed(() => years.value[years.value.length - 1] ?? 2026);

const scoreExtent = computed(() => {
  const values: number[] = [];
  for (const item of filteredAverages.value) values.push(item.avgScore);
  for (const item of filteredPoints.value) values.push(item.score);
  if (values.length === 0) return { min: 0, max: 10 };
  const min = Math.max(0, Math.min(...values) - 0.15);
  const max = Math.min(10, Math.max(...values) + 0.15);
  return { min, max: Math.max(min + 0.5, max) };
});

function xScale(year: number): number {
  const domainStart = Date.UTC(minYear.value, 0, 1);
  const domainEnd = Date.UTC(maxYear.value, 11, 31);
  const span = Math.max(24 * 3600 * 1000, domainEnd - domainStart);
  const width = svgWidth - marginLeft - marginRight;
  const ms = Date.UTC(year, 0, 1);
  return marginLeft + ((ms - domainStart) / span) * width;
}

function xScaleDate(releaseDate: string | null | undefined, fallbackYear: number): number {
  const domainStart = Date.UTC(minYear.value, 0, 1);
  const domainEnd = Date.UTC(maxYear.value, 11, 31);
  const span = Math.max(24 * 3600 * 1000, domainEnd - domainStart);
  const width = svgWidth - marginLeft - marginRight;

  let ms = Date.UTC(fallbackYear, 0, 1);
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(releaseDate ?? "");
  if (match) {
    const y = Number(match[1]);
    const m = Number(match[2]);
    const d = Number(match[3]);
    ms = Date.UTC(y, Math.max(0, m - 1), Math.max(1, d));
  }
  return marginLeft + ((ms - domainStart) / span) * width;
}

function yScale(score: number): number {
  const { min, max } = scoreExtent.value;
  const span = Math.max(0.0001, max - min);
  const height = svgHeight - marginTop - marginBottom;
  return marginTop + ((max - score) / span) * height;
}

const averageLinePath = computed(() => {
  const arr = [...filteredAverages.value].sort((a, b) => a.year - b.year);
  if (arr.length === 0) return "";
  return arr
    .map((item, idx) => `${idx === 0 ? "M" : "L"} ${xScaleDate(`${item.year}-07-01`, item.year)} ${yScale(item.avgScore)}`)
    .join(" ");
});

const pointDots = computed(() =>
  filteredPoints.value.map((item) => ({
    ...item,
    x: xScaleDate(item.releaseDate, item.year),
    y: yScale(item.score)
  }))
);

const averageDots = computed(() =>
  filteredAverages.value.map((item) => ({
    year: item.year,
    score: item.avgScore,
    count: item.count,
    x: xScaleDate(`${item.year}-07-01`, item.year),
    y: yScale(item.avgScore)
  }))
);

const scoreTicks = computed(() => {
  const { min, max } = scoreExtent.value;
  const steps = 5;
  const result: number[] = [];
  for (let i = 0; i <= steps; i += 1) {
    result.push(min + ((max - min) / steps) * i);
  }
  return result;
});

const yearTickLabels = computed(() => {
  if (years.value.length === 0) return [];
  const labels: number[] = [];
  for (let y = minYear.value; y <= maxYear.value; y += 1) labels.push(y);
  return labels;
});

const pointsByYear = computed(() => {
  const m = new Map<number, BangumiYearPointItem[]>();
  for (const p of filteredPoints.value) {
    const arr = m.get(p.year) ?? [];
    arr.push(p);
    m.set(p.year, arr);
  }
  return m;
});

const averageByYear = computed(() => {
  const m = new Map<number, BangumiYearAverageItem>();
  for (const a of filteredAverages.value) m.set(a.year, a);
  return m;
});

function quantile(sortedAsc: number[], q: number): number {
  if (sortedAsc.length === 0) return 0;
  if (sortedAsc.length === 1) return sortedAsc[0];
  const pos = (sortedAsc.length - 1) * q;
  const lo = Math.floor(pos);
  const hi = Math.ceil(pos);
  if (lo === hi) return sortedAsc[lo];
  const t = pos - lo;
  return sortedAsc[lo] * (1 - t) + sortedAsc[hi] * t;
}

const yearBoxes = computed(() => {
  const result: Array<{
    year: number;
    x: number;
    yMin: number;
    yMax: number;
    yQ1: number;
    yQ3: number;
    yMedian: number;
  }> = [];
  for (const year of years.value) {
    const pts = (pointsByYear.value.get(year) ?? []).map((p) => p.score).sort((a, b) => a - b);
    if (pts.length === 0) continue;
    const min = pts[0];
    const max = pts[pts.length - 1];
    const q1 = quantile(pts, 0.25);
    const q2 = quantile(pts, 0.5);
    const q3 = quantile(pts, 0.75);
    result.push({
      year,
      x: xScaleDate(`${year}-07-01`, year),
      yMin: yScale(min),
      yMax: yScale(max),
      yQ1: yScale(q1),
      yQ3: yScale(q3),
      yMedian: yScale(q2)
    });
  }
  return result;
});

const hoverSummary = computed(() => {
  if (activeYear.value == null) return null;
  const year = activeYear.value;
  const pts = pointsByYear.value.get(year);
  const avg = averageByYear.value.get(year);
  if (!pts || pts.length === 0 || !avg) return null;
  const sorted = [...pts].sort((a, b) => b.score - a.score);
  return {
    year,
    avgScore: avg.avgScore,
    count: avg.count,
    top: sorted[0],
    bottom: sorted[sorted.length - 1]
  };
});

function onChartMouseMove(ev: MouseEvent) {
  if (lockedYear.value != null) return;
  if (years.value.length === 0) return;
  const target = ev.currentTarget as SVGElement | null;
  if (!target) return;
  const rect = target.getBoundingClientRect();
  const x = ((ev.clientX - rect.left) / rect.width) * svgWidth;
  let nearest = years.value[0];
  let minDist = Math.abs(x - xScale(nearest));
  for (const y of years.value) {
    const d = Math.abs(x - xScale(y));
    if (d < minDist) {
      minDist = d;
      nearest = y;
    }
  }
  hoverYear.value = nearest;
}

function onChartMouseLeave() {
  if (lockedYear.value == null) hoverYear.value = null;
}

function onChartClick(ev: MouseEvent) {
  if (years.value.length === 0) return;
  const target = ev.currentTarget as SVGElement | null;
  if (!target) return;
  const rect = target.getBoundingClientRect();
  const x = ((ev.clientX - rect.left) / rect.width) * svgWidth;
  let nearest = years.value[0];
  let minDist = Math.abs(x - xScale(nearest));
  for (const y of years.value) {
    const d = Math.abs(x - xScale(y));
    if (d < minDist) {
      minDist = d;
      nearest = y;
    }
  }
  lockedYear.value = lockedYear.value === nearest ? null : nearest;
  hoverYear.value = lockedYear.value == null ? nearest : null;
}

function ensureRangeAfterLoad() {
  if (allYears.value.length === 0) {
    startYear.value = null;
    endYear.value = null;
    rangeInitialized.value = false;
    return;
  }
  const first = allYears.value[0];
  const last = allYears.value[allYears.value.length - 1];
  if (!rangeInitialized.value || startYear.value == null || endYear.value == null) {
    endYear.value = last;
    startYear.value = Math.max(first, last - 9); // default recent 10 years
    rangeInitialized.value = true;
    return;
  }
  if (startYear.value < first) startYear.value = first;
  if (endYear.value > last) endYear.value = last;
  if (startYear.value > endYear.value) startYear.value = endYear.value;
}

function onStartYearChange() {
  if (startYear.value == null || endYear.value == null) return;
  if (startYear.value > endYear.value) endYear.value = startYear.value;
  lockedYear.value = null;
  hoverYear.value = null;
}

function onEndYearChange() {
  if (startYear.value == null || endYear.value == null) return;
  if (endYear.value < startYear.value) startYear.value = endYear.value;
  lockedYear.value = null;
  hoverYear.value = null;
}

async function loadData() {
  loading.value = true;
  error.value = "";
  hoverYear.value = null;
  lockedYear.value = null;
  try {
    const data = await apiGet<BangumiTrendResponse>("/api/trend/bangumi/history", {
      minVotes: minVotes.value
    });
    averages.value = data.averages ?? [];
    points.value = data.points ?? [];
    ensureRangeAfterLoad();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
    averages.value = [];
    points.value = [];
    startYear.value = null;
    endYear.value = null;
    rangeInitialized.value = false;
  } finally {
    loading.value = false;
    await nextTick();
    requestAnimationFrame(() => {
      ready.value = true;
    });
  }
}

onMounted(loadData);
</script>

<style scoped>
.trend-page-head {
  flex-shrink: 0;
}

.trend-page > .loading-placeholder,
.trend-page > .error-text,
.trend-page > .empty-state {
  flex-shrink: 0;
}

.page-description {
  margin: 0 0 14px;
}

.controls {
  margin-bottom: 0;
}

.chart-block {
  position: relative;
  padding-top: 32px;
  width: 85%;
  max-width: 100%;
  margin-left: auto;
  margin-right: auto;
  box-sizing: border-box;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.year-summary-slot {
  position: absolute;
  top: 0;
  left: 0;
  font-size: 11px;
  color: var(--color-text-muted);
  z-index: 2;
}

.year-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
}

.chart-wrap {
  position: relative;
  overflow: auto;
  border-radius: var(--radius-md);
  padding: 14px;
  flex: 1 1 auto;
  min-height: 0;
}

svg {
  width: 100%;
  /* ~85% of previous 980px floor so horizontal scroll matches new scale */
  min-width: 833px;
  display: block;
}

.axis {
  stroke: rgba(90, 157, 143, 0.45);
  stroke-width: 1;
}

.tick {
  stroke: rgba(90, 157, 143, 0.3);
  stroke-width: 1;
}

.tick-label {
  fill: var(--color-text-muted);
  font-size: 9px;
}

.tick-label-y {
  text-anchor: end;
}

.tick-label-x {
  text-anchor: middle;
}

.tick-label-year {
  font-size: 8px;
}

.avg-line {
  fill: none;
  stroke: #5a9d8f;
  stroke-width: 2.6;
}

.hover-year-line {
  stroke: rgba(90, 157, 143, 0.5);
  stroke-width: 1;
  stroke-dasharray: 4 4;
}

.avg-dot {
  fill: #6bb5a5;
}

.box-whisker {
  stroke: rgba(90, 157, 143, 0.6);
  stroke-width: 1;
  opacity: 0.7;
}

.box-body {
  fill: rgba(122, 184, 168, 0.45);
  opacity: 0.6;
}

.box-median {
  stroke: #5a9d8f;
  stroke-width: 1.2;
}

.score-dot {
  fill: #6bb5a5;
  opacity: 0.45;
}

.score-dot:hover {
  opacity: 0.95;
}

.score-dot.highlighted {
  opacity: 0.9;
  fill: #5a9d8f;
}
</style>

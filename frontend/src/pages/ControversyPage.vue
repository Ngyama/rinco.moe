<template>
  <section class="page-shell controversy-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
    <div class="controversy-header">
      <div>
        <h2>争议分布图</h2>
        <p class="page-description">横轴评分人数（对数）、纵轴评分，气泡大小 = 评分人数，颜色越红争议越大。作品范围：{{ yearFrom }}{{ yearFrom !== yearTo ? `–${yearTo}` : '' }} 年。</p>
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
        <svg :viewBox="`0 0 ${svgW} ${svgH}`" class="viz-svg" @mousemove="onChartMouseMove" @mouseleave="onChartMouseLeave">
          <g v-for="(tick, i) in scoreTicks" :key="`y-${i}`">
            <line :x1="marginL" :x2="marginL + innerW" :y1="yScale(tick)" :y2="yScale(tick)" stroke="rgba(90, 157, 143, 0.35)" stroke-dasharray="2,2" />
            <text :x="marginL - 8" :y="yScale(tick) + 4" class="tick-label">{{ tick }}</text>
          </g>
          <g v-for="(tick, i) in voteTicks" :key="`x-${i}`">
            <line :x1="xScale(tick)" :y1="marginT" :x2="xScale(tick)" :y2="marginT + innerH" stroke="rgba(90, 157, 143, 0.35)" stroke-dasharray="2,2" />
            <text :x="xScale(tick)" :y="svgH - 8" class="tick-label" text-anchor="middle">{{ tick }}</text>
          </g>
          <circle
            v-for="d in items"
            :key="d.bangumiSubjectId"
            :cx="xScale(d.ratingTotal)"
            :cy="yScale(d.score)"
            :r="bubbleR(d.ratingTotal)"
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
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";

interface ControversyItem {
  bangumiSubjectId: number;
  titleJp: string;
  score: number;
  ratingTotal: number;
  controversy: number;
}

const yearFrom = ref(2025);
const yearTo = ref(2025);
const minVotesFilter = ref(20);
const loading = ref(false);
const ready = ref(false);
const error = ref("");
const items = ref<ControversyItem[]>([]);

const yearOptions = Array.from({ length: 41 }, (_, i) => 1990 + i);

function onYearChange() {
  if (yearFrom.value > yearTo.value) yearTo.value = yearFrom.value;
  loadData();
}
const hoveredId = ref<number | null>(null);
const mouseX = ref(0);
const mouseY = ref(0);

const svgW = 1000;
const svgH = 650;
const marginL = 60;
const marginT = 24;
const marginR = 40;
const marginB = 40;
const innerW = svgW - marginL - marginR;
const innerH = svgH - marginT - marginB;

const dataMinVotes = computed(() => Math.min(...items.value.map((d) => d.ratingTotal), 1));
const maxVotes = computed(() => Math.max(...items.value.map((d) => d.ratingTotal), 1));
const minScore = computed(() => Math.min(...items.value.map((d) => d.score), 0));
const maxScore = computed(() => Math.max(...items.value.map((d) => d.score), 10));
const maxControversy = computed(() => Math.max(...items.value.map((d) => d.controversy), 0.01));

function xScale(v: number): number {
  const logMin = Math.log10(Math.max(dataMinVotes.value, 1));
  const logMax = Math.log10(Math.max(maxVotes.value, 1));
  const t = (Math.log10(Math.max(v, 1)) - logMin) / (logMax - logMin || 1);
  return marginL + t * innerW;
}

function yScale(s: number): number {
  const lo = Math.max(0, minScore.value - 0.5);
  const hi = Math.min(10, maxScore.value + 0.5);
  const t = (s - lo) / (hi - lo || 1);
  return marginT + (1 - t) * innerH;
}

function bubbleR(votes: number): number {
  const r = Math.sqrt(Math.log10(Math.max(votes, 1) + 1)) * 5;
  return Math.min(Math.max(r, 3), 18);
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

const voteTicks = computed(() => {
  const lo = Math.pow(10, Math.floor(Math.log10(Math.max(dataMinVotes.value, 1))));
  const hi = Math.pow(10, Math.ceil(Math.log10(Math.max(maxVotes.value, 1))));
  const arr: number[] = [];
  for (let v = lo; v <= hi * 1.1; v *= 10) arr.push(v);
  return arr.length ? arr : [10, 100, 1000];
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
    const cx = xScale(d.ratingTotal);
    const cy = yScale(d.score);
    const r = bubbleR(d.ratingTotal);
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
  error.value = "";
  try {
    const json = await apiGet<{ items?: ControversyItem[] }>("/api/structure/controversy", {
      yearFrom: yearFrom.value,
      yearTo: yearTo.value,
      minVotes: minVotesFilter.value
    });
    items.value = json.items ?? [];
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
.controversy-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  padding: 16px 20px;
  overflow: hidden;
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
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chart-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-md);
  padding: 20px;
}

.viz-svg {
  flex: 1;
  width: 100%;
  min-height: 400px;
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

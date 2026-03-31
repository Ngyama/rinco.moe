<template>
  <div class="compare-chart-root">
    <div class="compare-legend" aria-hidden="true">
      <span class="leg leg-a" :title="titleA">
        <span class="swatch" aria-hidden="true" />
        <span class="leg-text">{{ titleA }}</span>
      </span>
      <span class="leg leg-b" :title="titleB">
        <span class="swatch" aria-hidden="true" />
        <span class="leg-text">{{ titleB }}</span>
      </span>
    </div>
    <svg
      :viewBox="`0 0 ${chartWidth} ${chartHeight}`"
      class="compare-svg"
      preserveAspectRatio="xMidYMid meet"
      role="img"
      :aria-label="ariaLabel"
    >
      <g v-for="(tk, ti) in yTicks" :key="`grid-${ti}`">
        <line
          :x1="marginLeft - 2"
          :x2="plotRight"
          :y1="yForTick(tk)"
          :y2="yForTick(tk)"
          class="grid-line"
        />
        <text :x="marginLeft - 8" :y="yForTick(tk) + 4" class="y-tick" text-anchor="end">
          {{ tk }}
        </text>
      </g>
      <g v-for="idx in 10" :key="`g-${idx}`">
        <rect
          :x="barAX(idx - 1)"
          :y="yTop(countA(idx - 1))"
          :width="barW"
          :height="barH(countA(idx - 1))"
          class="bar bar-a"
          rx="2"
        />
        <rect
          :x="barBX(idx - 1)"
          :y="yTop(countB(idx - 1))"
          :width="barW"
          :height="barH(countB(idx - 1))"
          class="bar bar-b"
          rx="2"
        />
        <text
          :x="groupCenterX(idx - 1)"
          :y="chartHeight - marginBottom + 16"
          class="x-label"
          text-anchor="middle"
        >
          {{ idx }}
        </text>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  ratingCountsA: number[];
  ratingCountsB: number[];
  titleA: string;
  titleB: string;
  chartWidth: number;
  chartHeight: number;
}>();

const marginLeft = 52;
const marginRight = 16;
const marginBottom = 38;
const marginTop = 8;

const maxCount = computed(() => {
  const a = props.ratingCountsA ?? [];
  const b = props.ratingCountsB ?? [];
  return Math.max(1, ...a, ...b);
});

const plotLeft = marginLeft;
const plotRight = computed(() => props.chartWidth - marginRight);

const innerH = computed(() => props.chartHeight - marginBottom - marginTop);

const groupW = computed(() => (plotRight.value - plotLeft) / 10);
const pad = 3;
const barGap = 3;

const barW = computed(() => {
  const inner = groupW.value - pad * 2;
  return Math.max(6, (inner - barGap) / 2);
});

function groupInnerLeft(idx: number): number {
  return plotLeft + idx * groupW.value + pad;
}

function barAX(idx: number): number {
  return groupInnerLeft(idx);
}

function barBX(idx: number): number {
  return groupInnerLeft(idx) + barW.value + barGap;
}

function groupCenterX(idx: number): number {
  return plotLeft + idx * groupW.value + groupW.value / 2;
}

function countA(idx: number): number {
  return props.ratingCountsA[idx] ?? 0;
}

function countB(idx: number): number {
  return props.ratingCountsB[idx] ?? 0;
}

function yTop(cnt: number): number {
  const h = barH(cnt);
  return props.chartHeight - marginBottom - h;
}

function barH(cnt: number): number {
  return maxCount.value > 0 ? (cnt / maxCount.value) * innerH.value : 0;
}

function yForTick(tick: number): number {
  return props.chartHeight - marginBottom - (tick / maxCount.value) * innerH.value;
}

const yTicks = computed(() => {
  const m = maxCount.value;
  const n = 4;
  const out: number[] = [];
  for (let i = 0; i <= n; i++) {
    out.push(Math.round((m * i) / n));
  }
  return [...new Set(out)].sort((a, b) => a - b);
});

const ariaLabel = computed(
  () => `两部作品 1 到 10 分人数对比：${props.titleA} 与 ${props.titleB}`
);
</script>

<style scoped>
.compare-chart-root {
  width: 100%;
  max-width: 100%;
}

.compare-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.leg {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: min(320px, 100%);
}

.leg-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text);
}

.swatch {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
  flex-shrink: 0;
}

.leg-a .swatch {
  background: rgba(90, 157, 143, 0.95);
  border: 1px solid rgba(90, 157, 143, 0.5);
}

.leg-b .swatch {
  background: rgba(130, 110, 160, 0.92);
  border: 1px solid rgba(130, 110, 160, 0.45);
}

.compare-svg {
  display: block;
  width: 100%;
  height: auto;
  min-height: 220px;
}

.grid-line {
  stroke: rgba(90, 157, 143, 0.22);
  stroke-dasharray: 3 4;
  stroke-width: 1;
}

.y-tick {
  font-size: 9px;
  fill: var(--color-text-muted);
}

.x-label {
  font-size: 11px;
  fill: var(--color-text-muted);
}

.bar-a {
  fill: rgba(90, 157, 143, 0.9);
}

.bar-b {
  fill: rgba(130, 110, 160, 0.88);
}

.bar {
  transition: opacity 0.15s ease;
}

.bar:hover {
  opacity: 1;
}
</style>

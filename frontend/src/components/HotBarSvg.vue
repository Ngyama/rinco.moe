<template>
  <svg
    :viewBox="`0 0 ${viewW} ${viewH}`"
    class="bar-chart"
    preserveAspectRatio="xMidYMid meet"
  >
    <!-- 纵向：柱高 ∝ 人数 -->
    <template v-if="orientation === 'vertical'">
      <g v-for="(cnt, i) in ratingCounts" :key="`v-${i}`">
        <rect
          :x="vMarginLeft + i * vBarStep"
          :y="yScale(cnt)"
          :width="vBarWidth"
          :height="barHeightV(cnt)"
          class="bar"
        />
        <text
          :x="vMarginLeft + i * vBarStep + vBarWidth / 2"
          :y="viewH - vMarginBottom + 18"
          class="bar-label"
        >
          {{ i + 1 }}
        </text>
        <text
          :x="vMarginLeft + i * vBarStep + vBarWidth / 2"
          :y="yScale(cnt) - 4"
          class="bar-value bar-value-v"
        >
          {{ cnt }}
        </text>
      </g>
    </template>
    <!-- 横向：每行 1–10 分，柱长 ∝ 人数 -->
    <template v-else>
      <g v-for="(cnt, i) in ratingCounts" :key="`h-${i}`">
        <text
          :x="labelX"
          :y="rowCenterY(i) + 4"
          class="bar-label bar-label-left"
          text-anchor="end"
        >
          {{ i + 1 }}
        </text>
        <rect
          :x="barStartX"
          :y="barTopY(i)"
          :width="barLen(cnt)"
          :height="barThickness"
          class="bar"
          rx="2"
        />
        <text
          :x="countTextX(cnt)"
          :y="rowCenterY(i) + 4"
          class="bar-value bar-value-h"
          text-anchor="start"
        >
          {{ cnt }}
        </text>
      </g>
    </template>
  </svg>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    ratingCounts: number[];
    chartWidth: number;
    chartHeight: number;
    /** When set, both charts use same scale (compare mode) */
    maxCountOverride?: number;
    orientation?: "horizontal" | "vertical";
  }>(),
  {
    maxCountOverride: undefined,
    orientation: "horizontal"
  }
);

const viewW = computed(() => props.chartWidth);
const viewH = computed(() => props.chartHeight);

const maxCount = computed(() => {
  if (props.maxCountOverride != null && props.maxCountOverride > 0) {
    return props.maxCountOverride;
  }
  if (!props.ratingCounts?.length) return 1;
  return Math.max(...props.ratingCounts, 1);
});

/* ——— vertical ——— */
const vMarginLeft = 40;
const vMarginBottom = 36;
const vBarStep = 38;
const vBarWidth = 28;

const innerChartHeightV = computed(() => props.chartHeight - vMarginBottom - 24);

function yScale(cnt: number): number {
  const barH = maxCount.value > 0 ? (cnt / maxCount.value) * innerChartHeightV.value : 0;
  return props.chartHeight - vMarginBottom - barH;
}

function barHeightV(cnt: number): number {
  return maxCount.value > 0 ? (cnt / maxCount.value) * innerChartHeightV.value : 0;
}

/* ——— horizontal ——— */
const labelX = 32;
const barStartX = 40;
const marginTop = 10;
const rowStep = 20;
const barThickness = 14;
const countReserve = 48;

const innerBarWidth = computed(() =>
  Math.max(40, props.chartWidth - barStartX - countReserve)
);

function rowCenterY(i: number): number {
  return marginTop + i * rowStep + rowStep / 2;
}

function barTopY(i: number): number {
  return marginTop + i * rowStep + (rowStep - barThickness) / 2;
}

function barLen(cnt: number): number {
  if (maxCount.value <= 0) return 0;
  return (cnt / maxCount.value) * innerBarWidth.value;
}

function countTextX(cnt: number): number {
  return barStartX + barLen(cnt) + 5;
}
</script>

<style scoped>
.bar-chart {
  width: 100%;
  max-width: 100%;
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

.bar-label-left {
  text-anchor: end;
}

.bar-value {
  font-size: 10px;
  fill: var(--color-text);
}

.bar-value-v {
  text-anchor: middle;
}

.bar-value-h {
  text-anchor: start;
}
</style>

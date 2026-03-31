<template>
  <section class="dumbbell-panel">
    <h3 class="panel-title">Staff 哑铃图（全作品最低分 ↔ 最高分）</h3>
    <p class="panel-hint">
      每条横线对应<strong>一位</strong>已选 Staff：左端 = 其在 Bangumi 参与作品中评分<strong>最低</strong>的一部之分，右端 =
      <strong>最高</strong>的一部之分（跨作品比较，非同一部两端）。横轴 0–10 分。图区固定为 10 行高度，多选仅多画线。下方勾选最多 10 人。当前为示例数据，接入 API 后替换。
    </p>

    <svg
      class="dumbbell-svg"
      :viewBox="`0 0 ${svgW} ${svgH}`"
      preserveAspectRatio="xMidYMid meet"
      :style="{ aspectRatio: `${svgW} / ${svgH}` }"
    >
      <text
        v-if="!rows.length"
        :x="svgW / 2"
        :y="(marginT + (FIXED_ROWS * rowH) / 2)"
        class="dumbbell-empty-svg"
        text-anchor="middle"
        dominant-baseline="middle"
      >
        请在下方选择至少一位 Staff（最多 10 人）
      </text>
      <!-- score grid -->
      <g v-for="tick in scoreTicks" :key="`g-${tick}`">
        <line
          :x1="xFor(tick)"
          :y1="marginT"
          :x2="xFor(tick)"
          :y2="svgH - marginB"
          stroke="rgba(90, 157, 143, 0.2)"
          stroke-dasharray="3,3"
        />
        <text :x="xFor(tick)" :y="svgH - 10" class="axis-tick" text-anchor="middle">{{ tick }}</text>
      </g>
      <text :x="svgW / 2" :y="svgH - 2" class="axis-label" text-anchor="middle">Bangumi 分</text>

      <g v-for="(row, i) in rows" :key="row.personId ?? `${row.title}-${i}`">
        <text :x="labelX" :y="rowY(i)" class="row-title" dominant-baseline="middle">{{ truncate(row.title, 16) }}</text>
        <line
          :x1="xFor(row.minScore)"
          :y1="rowY(i)"
          :x2="xFor(row.maxScore)"
          :y2="rowY(i)"
          class="dumbbell-line"
          stroke-width="2"
          stroke-linecap="round"
        />
        <circle :cx="xFor(row.minScore)" :cy="rowY(i)" r="5" class="dot min-dot" />
        <circle :cx="xFor(row.maxScore)" :cy="rowY(i)" r="5" class="dot max-dot" />
        <text :x="xFor(row.minScore) - 10" :y="rowY(i) - 10" class="score-tag" text-anchor="end">低 {{ row.minScore.toFixed(1) }}</text>
        <text :x="xFor(row.maxScore) + 10" :y="rowY(i) - 10" class="score-tag" text-anchor="start">高 {{ row.maxScore.toFixed(1) }}</text>
      </g>
    </svg>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";

export interface DumbbellRow {
  /** Staff 显示名 */
  title: string;
  minScore: number;
  maxScore: number;
  personId?: number;
}

const props = defineProps<{
  rows: DumbbellRow[];
}>();

const SCORE_LO = 0;
const SCORE_HI = 10;
const marginL = 200;
const marginR = 28;
const marginT = 28;
const marginB = 44;
const rowH = 34;
const labelX = 12;

/** 固定 10 行绘图区高度，不随已选人数变化 */
const FIXED_ROWS = 10;

const svgW = 920;

const innerW = computed(() => svgW - marginL - marginR);

const svgH = computed(() => marginT + FIXED_ROWS * rowH + marginB);

function rowY(i: number): number {
  return marginT + i * rowH + rowH / 2;
}

function xFor(score: number): number {
  const t = (Math.min(Math.max(score, SCORE_LO), SCORE_HI) - SCORE_LO) / (SCORE_HI - SCORE_LO);
  return marginL + t * innerW.value;
}

const scoreTicks = [0, 2, 4, 6, 8, 10];

function truncate(s: string, max: number): string {
  if (s.length <= max) return s;
  return s.slice(0, max - 1) + "…";
}
</script>

<style scoped>
.dumbbell-panel {
  margin-bottom: 28px;
  padding: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.panel-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text);
}

.panel-sub {
  font-weight: 500;
  color: var(--color-accent);
}

.panel-hint {
  margin: 0 0 16px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted);
}

.dumbbell-empty-svg {
  font-size: 14px;
  fill: var(--color-text-muted);
}

.dumbbell-svg {
  display: block;
  width: 100%;
  max-width: 100%;
  height: auto;
}

.axis-tick {
  font-size: 10px;
  fill: var(--color-text-muted);
}

.axis-label {
  font-size: 11px;
  fill: var(--color-text-muted);
}

.row-title {
  font-size: 12px;
  fill: var(--color-text);
}

.dumbbell-line {
  stroke: rgba(90, 157, 143, 0.75);
}

.dot {
  fill: var(--color-surface);
  stroke-width: 2;
}
.min-dot {
  stroke: rgba(90, 157, 143, 0.95);
}
.max-dot {
  stroke: rgba(122, 184, 168, 0.95);
}

.score-tag {
  font-size: 9px;
  fill: var(--color-text-muted);
}
</style>

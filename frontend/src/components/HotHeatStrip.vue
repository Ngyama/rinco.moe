<template>
  <div class="heat-wrap">
    <h4 class="heat-title">分布形状（热力条）</h4>
    <p class="heat-caption">
      每格颜色深浅 ≈ 该分人数占总投票比例（对占比做 √ 缩放，弱档也可见）。格内为占比。
    </p>
    <div class="heat-row" role="img" :aria-label="ariaLabel">
      <div
        v-for="(cnt, idx) in ratingCounts"
        :key="idx"
        class="heat-cell"
        :class="{
          'is-mode': mode != null && mode === idx + 1,
          'is-median': medianRounded != null && medianRounded === idx + 1
        }"
        :style="cellStyle(cnt)"
        :title="cellTooltip(idx, cnt)"
      >
        <span class="heat-digit">{{ idx + 1 }}</span>
        <span class="heat-pct">{{ pctLabel(cnt) }}</span>
      </div>
    </div>
    <div class="heat-legend">
      <span class="leg-item"><i class="leg-swatch mode" /> 众数所在分档</span>
      <span class="leg-item"><i class="leg-swatch median" /> 中位数（四舍五入到整数分）</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  ratingCounts: number[];
  /** 1–10 */
  median?: number;
  /** 1–10 */
  mode?: number;
}>();

const totalVotes = computed(() => props.ratingCounts.reduce((a, b) => a + b, 0));

const medianRounded = computed(() => {
  if (props.median == null || Number.isNaN(props.median)) return null;
  const r = Math.round(props.median);
  return Math.min(10, Math.max(1, r));
});

const ariaLabel = computed(() => {
  const t = totalVotes.value;
  if (!t) return "无投票数据";
  return `1 到 10 分各档人数占比热力条，共 ${t} 票`;
});

function pct(cnt: number): number {
  const t = totalVotes.value;
  return t > 0 ? cnt / t : 0;
}

/** √ 缩放占比 → 填充不透明度，保留形状又避免弱档全黑 */
function cellStyle(cnt: number) {
  const p = pct(cnt);
  const t = Math.min(1, Math.sqrt(p));
  const alpha = 0.1 + 0.82 * t;
  return { background: `rgba(90, 157, 143, ${alpha})` };
}

function pctLabel(cnt: number): string {
  const t = totalVotes.value;
  if (!t) return "—";
  return `${(pct(cnt) * 100).toFixed(1)}%`;
}

function cellTooltip(idx: number, cnt: number): string {
  const t = totalVotes.value;
  const p = t > 0 ? ((cnt / t) * 100).toFixed(1) : "0";
  return `${idx + 1} 分：${cnt} 人（${p}%）`;
}
</script>

<style scoped>
.heat-wrap {
  flex-shrink: 0;
  margin-bottom: 0;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
}

.heat-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.heat-caption {
  margin: 0 0 14px;
  font-size: 12px;
  line-height: 1.45;
  color: var(--color-text-muted);
}

.heat-row {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 6px;
  align-items: stretch;
}

.heat-cell {
  min-height: 56px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 2px;
  border: 1px solid rgba(90, 157, 143, 0.25);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.heat-cell:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(90, 157, 143, 0.2);
}

.heat-digit {
  font-size: 13px;
  font-weight: 700;
  color: rgba(30, 60, 55, 0.92);
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
}

.heat-pct {
  font-size: 10px;
  font-weight: 600;
  color: rgba(25, 50, 45, 0.88);
  text-shadow: 0 0 6px rgba(255, 255, 255, 0.45);
}

.heat-cell.is-mode {
  box-shadow: inset 0 -3px 0 0 rgba(180, 120, 40, 0.95);
}

.heat-cell.is-median {
  box-shadow: inset 0 0 0 2px rgba(40, 120, 130, 0.85);
}

.heat-cell.is-mode.is-median {
  box-shadow:
    inset 0 0 0 2px rgba(40, 120, 130, 0.85),
    inset 0 -3px 0 0 rgba(180, 120, 40, 0.95);
}

.heat-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.leg-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.leg-swatch {
  width: 14px;
  height: 10px;
  border-radius: 3px;
  border: 1px solid rgba(90, 157, 143, 0.35);
  background: rgba(90, 157, 143, 0.35);
}

.leg-swatch.mode {
  box-shadow: inset 0 -2px 0 0 rgba(180, 120, 40, 0.95);
}

.leg-swatch.median {
  box-shadow: inset 0 0 0 1.5px rgba(40, 120, 130, 0.85);
}

@media (max-width: 520px) {
  .heat-row {
    gap: 4px;
  }
  .heat-cell {
    min-height: 48px;
  }
  .heat-pct {
    font-size: 9px;
  }
}
</style>

<template>
  <section class="user-dist" aria-labelledby="user-dist-title">
    <h3 id="user-dist-title" class="user-dist-title">个人评分分布（Galgame）</h3>
    <p class="user-dist-desc">
      基于当前用户<strong>全部游戏收藏</strong>中、经 Galgame 筛选后的条目；仅统计 <strong>1–10</strong> 分。
      <template v-if="totalRated > 0">
        共 <strong>{{ totalRated }}</strong> 部有评分，均分 <strong>{{ avgText }}</strong>。
      </template>
      <template v-else>暂无有效评分数据。</template>
    </p>
    <div class="user-dist-chart-wrap">
      <div class="user-dist-chart" role="img" :aria-label="ariaLabel">
        <div v-for="(n, i) in buckets" :key="i" class="user-dist-row">
          <span class="user-dist-lbl">{{ i + 1 }}</span>
          <div class="user-dist-track-h">
            <div
              class="user-dist-bar-h"
              :style="{ width: barWidthPct(n) }"
            />
          </div>
          <span class="user-dist-val">{{ n }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  /** 下标 0 = 1 分 … 9 = 10 分 */
  buckets: number[];
  totalRated: number;
  avgRating: number | null;
}>();

const maxBucket = computed(() => Math.max(0, ...props.buckets, 0));

const avgText = computed(() =>
  props.avgRating != null ? props.avgRating.toFixed(2) : "—"
);

function barWidthPct(n: number): string {
  const m = maxBucket.value;
  if (m <= 0 || n <= 0) return "0%";
  return `${(n / m) * 100}%`;
}

const ariaLabel = computed(() => {
  const parts = props.buckets.map((c, i) => `${i + 1}分${c}部`).join("，");
  return `Galgame 评分分布：${parts}`;
});
</script>

<style scoped>
.user-dist {
  margin: 0;
  padding: 0;
}

.user-dist-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.user-dist-desc {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--color-text-muted);
}

.user-dist-chart-wrap {
  width: 100%;
}

/* 竖排：每行 1–10 分，横向条形表示人数占比（相对 max） */
.user-dist-chart {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user-dist-row {
  display: grid;
  grid-template-columns: 22px 1fr 28px;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.user-dist-lbl {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
  text-align: right;
}

.user-dist-track-h {
  height: 14px;
  border-radius: 4px;
  background: rgba(90, 157, 143, 0.1);
  border: 1px solid rgba(90, 157, 143, 0.2);
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
}

.user-dist-bar-h {
  height: 100%;
  min-width: 0;
  border-radius: 3px;
  background: linear-gradient(90deg, rgba(45, 110, 98, 0.92) 0%, rgba(90, 157, 143, 0.98) 100%);
  box-shadow: 0 1px 2px rgba(30, 80, 70, 0.15);
  transition: width 0.2s ease;
}

.user-dist-val {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
  text-align: right;
}
</style>

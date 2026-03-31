<template>
  <section class="extreme-bars-section">
    <h3 class="extreme-title">极端分占比（投票前十作）</h3>
    <p class="extreme-desc">
      与上方相同筛选：{{ yearFrom }}{{ yearFrom !== yearTo ? `–${yearTo}` : "" }} 年、最低投票 ≥ {{ minVotes }}。取该范围内<strong>投票人数</strong>最多的
      10 部作品；每条横带<strong>仅两色</strong>：左 = 1–2 分、右 = 9–10 分，条内左右长度按二者在「极端票（1–2 + 9–10）」中的比例划分（占满整条）。下方数字仍为各档占<strong>总票</strong>的百分比。
    </p>
    <p v-if="loading" class="loading-placeholder">加载中</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>
    <ul v-else-if="items.length" class="extreme-list">
      <li v-for="row in items" :key="row.bangumiSubjectId" class="extreme-row">
        <a
          class="extreme-title-link"
          :href="bangumiSubjectUrl(row.bangumiSubjectId)"
          target="_blank"
          rel="noopener noreferrer"
          :title="row.titleJp"
        >
          {{ row.titleJp }}
        </a>
        <div class="extreme-bar" role="img" :aria-label="ariaFor(row)">
          <template v-if="extremeSum(row) > 0">
            <div class="seg seg-low" :style="{ width: pctNormLow(row) }" />
            <div class="seg seg-high" :style="{ width: pctNormHigh(row) }" />
          </template>
          <div v-else class="seg seg-empty" title="无 1–2 分且无 9–10 分" />
        </div>
        <div class="extreme-meta">
          <span class="meta-low">1–2：{{ (row.lowShare * 100).toFixed(1) }}%</span>
          <span class="meta-high">9–10：{{ (row.highShare * 100).toFixed(1) }}%</span>
          <span class="meta-n">{{ row.ratingTotal }} 票</span>
        </div>
      </li>
    </ul>
    <p v-else class="empty-hint">暂无数据</p>
    <div class="extreme-legend">
      <span><i class="swatch low" /> 1–2 分（左）</span>
      <span><i class="swatch high" /> 9–10 分（右）</span>
    </div>
  </section>
</template>

<script setup lang="ts">
interface ExtremeBarRow {
  bangumiSubjectId: number;
  titleJp: string;
  ratingTotal: number;
  lowShare: number;
  highShare: number;
}

defineProps<{
  yearFrom: number;
  yearTo: number;
  minVotes: number;
  items: ExtremeBarRow[];
  loading: boolean;
  error: string;
}>();

function bangumiSubjectUrl(id: number): string {
  return `https://bangumi.tv/subject/${id}`;
}

function extremeSum(row: ExtremeBarRow): number {
  return row.lowShare + row.highShare;
}

/** 在「极端票」内部的占比，两色横条左右宽度 */
function pctNormLow(row: ExtremeBarRow): string {
  const s = extremeSum(row);
  if (s <= 0) return "0%";
  return `${((row.lowShare / s) * 100).toFixed(2)}%`;
}

function pctNormHigh(row: ExtremeBarRow): string {
  const s = extremeSum(row);
  if (s <= 0) return "0%";
  return `${((row.highShare / s) * 100).toFixed(2)}%`;
}

function ariaFor(row: ExtremeBarRow): string {
  const s = extremeSum(row);
  if (s <= 0) {
    return `${row.titleJp}：无 1–2 分且无 9–10 分`;
  }
  return `${row.titleJp}：横条左段占极端票 ${((row.lowShare / s) * 100).toFixed(1)}%，右段 ${((row.highShare / s) * 100).toFixed(1)}%；总票中 1–2 分 ${(row.lowShare * 100).toFixed(1)}%，9–10 分 ${(row.highShare * 100).toFixed(1)}%`;
}
</script>

<style scoped>
.extreme-bars-section {
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid var(--color-border);
}

.extreme-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.extreme-desc {
  margin: 0 0 16px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--color-text-muted);
}

.loading-placeholder,
.error-text,
.empty-hint {
  font-size: 13px;
  margin: 0 0 8px;
}

.error-text {
  color: var(--color-error, #c55);
}

.extreme-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.extreme-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(180px, 2.2fr) auto;
  gap: 10px 14px;
  align-items: center;
}

@media (max-width: 720px) {
  .extreme-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}

.extreme-title-link {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.extreme-title-link:hover {
  color: var(--color-accent);
  text-decoration: underline;
}

.extreme-bar {
  display: flex;
  height: 14px;
  border-radius: 7px;
  overflow: hidden;
  border: 1px solid rgba(90, 157, 143, 0.35);
  min-width: 0;
}

.seg {
  height: 100%;
  min-width: 0;
  transition: width 0.2s ease;
}

.seg-low {
  background: linear-gradient(180deg, #3d7a8c 0%, #2d5f6e 100%);
}

.seg-high {
  background: linear-gradient(180deg, #c45c4a 0%, #a84838 100%);
}

.seg-empty {
  flex: 1;
  width: 100%;
  background: rgba(90, 157, 143, 0.12);
  background-image: repeating-linear-gradient(
    -45deg,
    transparent,
    transparent 3px,
    rgba(90, 157, 143, 0.08) 3px,
    rgba(90, 157, 143, 0.08) 6px
  );
}

.extreme-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  font-size: 11px;
  color: var(--color-text-muted);
  justify-content: flex-end;
}

.meta-low {
  color: #2d5f6e;
}

.meta-high {
  color: #a84838;
}

.meta-n {
  opacity: 0.85;
}

.extreme-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
  font-size: 11px;
  color: var(--color-text-muted);
  align-items: center;
}

.extreme-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.swatch {
  width: 18px;
  height: 8px;
  border-radius: 3px;
  border: 1px solid rgba(90, 157, 143, 0.35);
}

.swatch.low {
  background: linear-gradient(180deg, #3d7a8c 0%, #2d5f6e 100%);
}

.swatch.high {
  background: linear-gradient(180deg, #c45c4a 0%, #a84838 100%);
}
</style>

<template>
  <section class="combined-top-panel">
    <h3 class="panel-title">三站综合 Top 10</h3>
    <p class="panel-desc">
      EGS 分数按 <strong>百分制（0–100）</strong> 先除以 10 得到 10 分制，再与 BGM、VNDB 一起做
      <strong>50% / 25% / 25%</strong> 加权。综合分写入库表字段
      <code>vn_match_triple.combined_weighted_score</code>（仅三站齐全且三站分可解析时为数值，否则为
      NULL）。仅统计匹配池中满足上述条件的条目（与上方三列取分方式一致）。展示用综合分 =
      <span class="w-bgm">Bangumi × 50%</span>
      +
      <span class="w-vndb">VNDB × 25%</span>
      +
      <span class="w-egs">EGS × 25%</span>
      。条形为各分项对综合分的贡献占比（非原始分直接相加）。
    </p>

    <div v-if="!items.length" class="empty-hint">
      当前没有「三站均有分数」的条目（需在匹配表中同时挂 Bangumi / VNDB / EGS，且各站分数能从匹配表或作品表解析）。请检查数据或刷新。
    </div>

    <div v-else class="legend">
      <span class="leg leg-bgm"><i /> Bangumi 贡献</span>
      <span class="leg leg-vndb"><i /> VNDB 贡献</span>
      <span class="leg leg-egs"><i /> EGS 贡献</span>
    </div>

    <ul v-if="items.length" class="bar-list">
      <li
        v-for="item in items"
        :key="`c-${item.matchTripleId ?? item.rank}`"
        class="bar-row"
        @mouseenter="emitHover(item.matchTripleId)"
        @mouseleave="emitLeave"
      >
        <span class="col-rank">{{ item.rank }}</span>
        <span class="col-title" :title="item.titleJp">{{ truncate(item.titleJp, 22) }}</span>
        <div class="col-bar">
          <div
            class="stack"
            role="img"
            :aria-label="ariaStack(item)"
            :style="{ gridTemplateColumns: stackGridTemplate(item) }"
          >
            <div
              v-for="(seg, si) in segments(item)"
              :key="si"
              class="seg"
              :class="seg.cls"
            />
          </div>
        </div>
        <span class="col-score">{{ formatNum(siteScores(item).combinedScore) }}</span>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
export interface CombinedRankItem {
  rank: number;
  titleJp: string;
  bangumiScore: number;
  vndbScore: number;
  egsScore: number;
  combinedScore: number;
  matchTripleId?: number | null;
  /** Spring / JSON 可能为 snake_case */
  bangumi_score?: number;
  vndb_score?: number;
  egs_score?: number;
  combined_score?: number;
}

defineProps<{
  items: CombinedRankItem[];
}>();

const emit = defineEmits<{
  hoverItem: [matchTripleId: number | null];
  hoverEnd: [];
}>();

function truncate(s: string, len: number): string {
  if (s.length <= len) return s;
  return s.slice(0, len - 1) + "…";
}

function formatNum(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) return "-";
  return n.toFixed(4);
}

/** 兼容 camelCase / snake_case（避免分数为 undefined 导致条形不渲染）。 */
function siteScores(item: CombinedRankItem) {
  const bg = Number(item.bangumiScore ?? item.bangumi_score);
  const vnd = Number(item.vndbScore ?? item.vndb_score);
  const eg = Number(item.egsScore ?? item.egs_score);
  const comb = Number(item.combinedScore ?? item.combined_score);
  return {
    bangumiScore: Number.isFinite(bg) ? bg : 0,
    vndbScore: Number.isFinite(vnd) ? vnd : 0,
    egsScore: Number.isFinite(eg) ? eg : 0,
    combinedScore: Number.isFinite(comb) ? comb : 0
  };
}

/** Contribution shares of weighted terms (sum = combined). */
function segments(item: CombinedRankItem) {
  const s = siteScores(item);
  const wB = 0.5 * s.bangumiScore;
  const wV = 0.25 * s.vndbScore;
  /** EGS API/库中为百分制，先 /10 再参与 25% 权重（与后端一致） */
  const wE = 0.25 * (s.egsScore / 10);
  const sum = wB + wV + wE;
  if (sum <= 0) {
    return [
      { pct: 33.33, cls: "seg-bgm" },
      { pct: 33.34, cls: "seg-vndb" },
      { pct: 33.33, cls: "seg-egs" }
    ];
  }
  return [
    { pct: (wB / sum) * 100, cls: "seg-bgm" },
    { pct: (wV / sum) * 100, cls: "seg-vndb" },
    { pct: (wE / sum) * 100, cls: "seg-egs" }
  ];
}

/** Grid 列宽：按加权分项比例分配 fr，避免 flex 子项 width:% 在部分布局下宽度为 0。 */
function stackGridTemplate(item: CombinedRankItem): string {
  const s = siteScores(item);
  const wB = 0.5 * s.bangumiScore;
  const wV = 0.25 * s.vndbScore;
  const wE = 0.25 * (s.egsScore / 10);
  const sum = wB + wV + wE;
  if (sum <= 0) {
    return "1fr 1fr 1fr";
  }
  return `${wB}fr ${wV}fr ${wE}fr`;
}

function ariaStack(item: CombinedRankItem): string {
  const s = siteScores(item);
  return `综合分 ${formatNum(s.combinedScore)}，Bangumi ${formatNum(s.bangumiScore)}，VNDB ${formatNum(s.vndbScore)}，EGS ${formatNum(s.egsScore)}`;
}

function emitHover(mid: number | null | undefined) {
  emit("hoverItem", typeof mid === "number" && !Number.isNaN(mid) ? mid : null);
}

function emitLeave() {
  emit("hoverEnd");
}
</script>

<style scoped>
.combined-top-panel {
  margin-top: 28px;
  padding: 24px 0 8px;
  border-top: 1px solid rgba(90, 157, 143, 0.2);
}

.panel-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.panel-desc {
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.panel-desc strong {
  color: var(--color-text);
  font-weight: 600;
}

.w-bgm {
  color: #3d7a6a;
  font-weight: 500;
}
.w-vndb {
  color: #2d6a62;
  font-weight: 500;
}
.w-egs {
  color: #3a8a9e;
  font-weight: 500;
}

.empty-hint {
  font-size: 14px;
  color: var(--color-text-muted);
  padding: 12px 0;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 14px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.leg {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.leg i {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.leg-bgm i {
  background: linear-gradient(180deg, rgba(122, 184, 168, 0.95), rgba(90, 157, 143, 0.85));
}
.leg-vndb i {
  background: linear-gradient(180deg, rgba(74, 140, 128, 0.95), rgba(45, 106, 98, 0.9));
}
.leg-egs i {
  background: linear-gradient(180deg, rgba(165, 222, 229, 0.95), rgba(120, 190, 200, 0.85));
}

.bar-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bar-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) minmax(120px, 2.2fr) 72px;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.45);
  border: 1px solid rgba(90, 157, 143, 0.15);
  transition: background 0.15s ease, border-color 0.15s ease;
}

.bar-row:hover {
  background: rgba(122, 184, 168, 0.12);
  border-color: rgba(90, 157, 143, 0.35);
}

.col-rank {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-accent);
  text-align: right;
}

.col-title {
  font-size: 13px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-bar {
  min-width: 0;
  width: 100%;
}

.stack {
  display: grid;
  width: 100%;
  min-height: 22px;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(90, 157, 143, 0.14);
  border: 1px solid rgba(90, 157, 143, 0.35);
  box-sizing: border-box;
}

.seg {
  min-height: 22px;
  min-width: 0;
  transition: filter 0.15s ease;
}

.bar-row:hover .seg {
  filter: brightness(1.05);
}

.seg-bgm {
  background: linear-gradient(90deg, rgba(122, 184, 168, 0.95), rgba(90, 157, 143, 0.88));
}
.seg-vndb {
  background: linear-gradient(90deg, rgba(74, 140, 128, 0.95), rgba(45, 106, 98, 0.88));
}
.seg-egs {
  background: linear-gradient(90deg, rgba(165, 222, 229, 0.95), rgba(120, 190, 200, 0.85));
}

.col-score {
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text);
  text-align: right;
}

@media (max-width: 720px) {
  .bar-row {
    grid-template-columns: 24px 1fr;
    grid-template-rows: auto auto;
  }

  .col-rank {
    grid-row: 1;
  }

  .col-title {
    grid-row: 1;
  }

  .col-bar {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  .col-score {
    grid-column: 2;
    grid-row: 1;
  }
}
</style>

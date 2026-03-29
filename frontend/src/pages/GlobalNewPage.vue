<template>
  <section class="page-shell global-new-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
      <h2>评分总览 - 匹配池 Top 25</h2>
      <p class="page-description">
        数据来自脚本维护的小型 vn_match_triple 池（先 Bangumi Top25 入池并自动对齐 VNDB/EGS，再补 VNDB Top25、EGS Top25 未出现条目；允许缺站或缺分，便于在后台修正）。三列按各站在池内的评分各取前
        25。悬停高亮：优先 matchTripleId，否则标题规范化一致。
      </p>

      <div class="controls">
        <button type="button" class="reload-btn" @click="loadData">刷新</button>
      </div>

      <p v-if="loading" class="loading-placeholder">加载中</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>

      <div v-else class="triple-columns">
        <section class="column" data-site="bangumi">
          <h3>Bangumi</h3>
          <div class="column-scroll">
            <div
              v-for="(item, idx) in data.bangumi"
              :key="`bgm-${item.matchTripleId ?? 'x'}-${item.rank}-${item.titleJp}`"
              class="game-card"
              :class="{ highlighted: isHighlighted('bangumi', idx) }"
              :ref="(el) => setCardRef('bangumi', idx, el)"
              @mouseenter="onHover('bangumi', idx)"
              @mouseleave="onHoverEnd"
            >
              <div class="cover-wrap">
                <div class="cover-placeholder">
                  <span class="cover-rank">{{ item.rank }}</span>
                  <div class="cover-shimmer" />
                </div>
              </div>
              <div class="card-body">
                <span class="card-rank">{{ item.rank }}.</span>
                <span class="card-title">{{ item.titleJp }}</span>
                <span class="card-score">{{ formatScore(item.score) }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="column" data-site="vndb">
          <h3>VNDB</h3>
          <div class="column-scroll">
            <div
              v-for="(item, idx) in data.vndb"
              :key="`vndb-${item.matchTripleId ?? 'x'}-${item.rank}-${item.titleJp}`"
              class="game-card"
              :class="{ highlighted: isHighlighted('vndb', idx) }"
              :ref="(el) => setCardRef('vndb', idx, el)"
              @mouseenter="onHover('vndb', idx)"
              @mouseleave="onHoverEnd"
            >
              <div class="cover-wrap">
                <div class="cover-placeholder">
                  <span class="cover-rank">{{ item.rank }}</span>
                  <div class="cover-shimmer" />
                </div>
              </div>
              <div class="card-body">
                <span class="card-rank">{{ item.rank }}.</span>
                <span class="card-title">{{ item.titleJp }}</span>
                <span class="card-score">{{ formatScore(item.score) }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="column" data-site="egs">
          <h3>EGS</h3>
          <div class="column-scroll">
            <div
              v-for="(item, idx) in data.egs"
              :key="`egs-${item.matchTripleId ?? 'x'}-${item.rank}-${item.titleJp}`"
              class="game-card"
              :class="{ highlighted: isHighlighted('egs', idx) }"
              :ref="(el) => setCardRef('egs', idx, el)"
              @mouseenter="onHover('egs', idx)"
              @mouseleave="onHoverEnd"
            >
              <div class="cover-wrap">
                <div class="cover-placeholder">
                  <span class="cover-rank">{{ item.rank }}</span>
                  <div class="cover-shimmer" />
                </div>
              </div>
              <div class="card-body">
                <span class="card-rank">{{ item.rank }}.</span>
                <span class="card-title">{{ item.titleJp }}</span>
                <span class="card-score">{{ formatScore(item.score) }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <GlobalCombinedTopPanel
        v-if="!loading && !error"
        :items="data.combinedTop10 ?? []"
        @hover-item="onCombinedHover"
        @hover-end="onHoverEnd"
      />

      <GlobalSankeyPanel
        v-if="!loading && !error"
        :key="dataEpoch"
        :bangumi="data.bangumi"
        :vndb="data.vndb"
        :egs="data.egs"
      />
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { apiGet } from "../api/client";
import PageProgressBar from "../components/PageProgressBar.vue";
import GlobalSankeyPanel from "../components/GlobalSankeyPanel.vue";
import GlobalCombinedTopPanel from "../components/GlobalCombinedTopPanel.vue";

interface GlobalScoreItem {
  rank: number;
  titleJp: string;
  score: number | null;
  /** vn_match_triple.id when present */
  matchTripleId?: number | null;
}

interface CombinedRankItem {
  rank: number;
  titleJp: string;
  bangumiScore: number;
  vndbScore: number;
  egsScore: number;
  combinedScore: number;
  matchTripleId?: number | null;
}

interface DualTopResponse {
  limit: number;
  bangumi: GlobalScoreItem[];
  vndb: GlobalScoreItem[];
  egs: GlobalScoreItem[];
  combinedTop10?: CombinedRankItem[];
}

function normalizeTitle(s: string): string {
  return (s || "").trim().replace(/\s+/g, " ");
}

const loading = ref(false);
const ready = ref(false);
const error = ref("");
const data = ref<DualTopResponse>({
  limit: 25,
  bangumi: [],
  vndb: [],
  egs: [],
  combinedTop10: []
});

/** Bump to remount Sankey after each successful fetch (layout/cache from prior props). */
const dataEpoch = ref(0);

/** When matchTripleId is non-null, highlight by vn_match_triple id; else by normalized title. */
const hoverHighlight = ref<{ matchTripleId: number | null; titleNorm: string } | null>(null);

function formatScore(score: number | null | undefined): string {
  if (score == null) return "-";
  return score.toFixed(4);
}

function itemMatchesHighlight(item: GlobalScoreItem, h: { matchTripleId: number | null; titleNorm: string }): boolean {
  const byMatchId =
    typeof h.matchTripleId === "number" && h.matchTripleId !== null && !Number.isNaN(h.matchTripleId);
  if (byMatchId) {
    return item.matchTripleId != null && item.matchTripleId === h.matchTripleId;
  }
  return normalizeTitle(item.titleJp) === h.titleNorm;
}

const highlightedSet = computed(() => {
  const h = hoverHighlight.value;
  if (!h) return new Set<string>();

  const set = new Set<string>();

  for (const [site, list] of [
    ["bangumi", data.value.bangumi],
    ["vndb", data.value.vndb],
    ["egs", data.value.egs]
  ] as const) {
    list.forEach((item, idx) => {
      if (itemMatchesHighlight(item, h)) {
        set.add(`${site}-${idx}`);
      }
    });
  }
  return set;
});

/** Card root elements for scroll-into-view (cleared when list re-renders). */
const cardElRefs = new Map<string, HTMLElement>();

function setCardRef(site: "bangumi" | "vndb" | "egs", idx: number, el: unknown) {
  const key = `${site}-${idx}`;
  if (el instanceof HTMLElement) {
    cardElRefs.set(key, el);
  } else {
    cardElRefs.delete(key);
  }
}

function scrollCardIntoViewIfNeeded(site: "bangumi" | "vndb" | "egs", idx: number) {
  const el = cardElRefs.get(`${site}-${idx}`);
  el?.scrollIntoView({ block: "nearest", behavior: "smooth", inline: "nearest" });
}

/** Scroll other columns so the first matching card per column is visible. */
function scrollPeerColumns(sourceSite: "bangumi" | "vndb" | "egs") {
  const h = hoverHighlight.value;
  if (!h) return;
  const peers = (["bangumi", "vndb", "egs"] as const).filter((s) => s !== sourceSite);
  nextTick(() => {
    requestAnimationFrame(() => {
      for (const site of peers) {
        const list = data.value[site];
        const idx = list.findIndex((item) => itemMatchesHighlight(item, h));
        if (idx >= 0) {
          scrollCardIntoViewIfNeeded(site, idx);
        }
      }
    });
  });
}

function isHighlighted(site: "bangumi" | "vndb" | "egs", idx: number): boolean {
  return highlightedSet.value.has(`${site}-${idx}`);
}

function onHover(site: "bangumi" | "vndb" | "egs", idx: number) {
  const list = data.value[site];
  const item = list?.[idx];
  if (!item) {
    hoverHighlight.value = null;
    return;
  }
  const mid = item.matchTripleId;
  hoverHighlight.value = {
    matchTripleId: typeof mid === "number" && !Number.isNaN(mid) ? mid : null,
    titleNorm: normalizeTitle(item.titleJp)
  };
  scrollPeerColumns(site);
}

function onHoverEnd() {
  hoverHighlight.value = null;
}

function onCombinedHover(matchTripleId: number | null) {
  if (matchTripleId == null) {
    hoverHighlight.value = null;
    return;
  }
  hoverHighlight.value = { matchTripleId, titleNorm: "" };
}

async function loadData() {
  loading.value = true;
  error.value = "";
  try {
    const result = await apiGet<DualTopResponse>(
      "/api/global/dual-top",
      {
        limit: 25,
        matchedOnly: true,
        _t: Date.now()
      },
      { cache: "no-store" }
    );
    data.value = {
      limit: result.limit,
      bangumi: result.bangumi ?? [],
      vndb: result.vndb ?? [],
      egs: result.egs ?? [],
      combinedTop10: result.combinedTop10 ?? []
    };
    dataEpoch.value += 1;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
    data.value = { limit: 25, bangumi: [], vndb: [], egs: [], combinedTop10: [] };
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
.global-new-page {
  padding-bottom: 48px;
}

.page-description {
  margin: 0 0 16px;
}

.controls {
  margin-bottom: 20px;
}

.triple-columns {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  align-items: start;
}

.column h3 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  padding: 0 4px;
}

.column-scroll {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
  scrollbar-width: none;
  padding-right: 4px;
}

.column-scroll::-webkit-scrollbar {
  display: none;
}

.game-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(90, 157, 143, 0.2);
  transition: opacity 0.2s ease, background 0.2s ease, font-weight 0.2s ease, border-color 0.2s ease;
}

.game-card:not(.highlighted) {
  opacity: 1;
}

.triple-columns:has(.game-card:hover) .game-card:not(.highlighted) {
  opacity: 0.2;
}

.game-card.highlighted {
  background: rgba(122, 184, 168, 0.35);
  border-color: rgba(90, 157, 143, 0.5);
  font-weight: 600;
  opacity: 1;
}

.cover-wrap {
  flex-shrink: 0;
}

.cover-placeholder {
  position: relative;
  width: 48px;
  height: 64px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(90, 157, 143, 0.25) 0%, rgba(122, 184, 168, 0.15) 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-rank {
  position: relative;
  z-index: 1;
  font-size: 14px;
  font-weight: 600;
  color: rgba(90, 157, 143, 0.8);
}

.cover-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    110deg,
    transparent 0%,
    transparent 40%,
    rgba(255, 255, 255, 0.2) 50%,
    transparent 60%,
    transparent 100%
  );
  background-size: 200% 100%;
  animation: cover-shimmer 3s ease-in-out infinite;
}

@keyframes cover-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-rank {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--color-accent);
  font-weight: 600;
}

.card-title {
  flex: 1;
  font-size: 13px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.game-card.highlighted .card-title {
  font-weight: 600;
}

.card-score {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

@media (max-width: 900px) {
  .triple-columns {
    grid-template-columns: 1fr;
  }

  .column-scroll {
    max-height: 280px;
  }
}
</style>

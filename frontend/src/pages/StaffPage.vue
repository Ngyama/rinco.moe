<template>
  <section class="page-shell staff-page">
    <PageProgressBar v-if="!ready" />
    <template v-else>
    <div class="staff-header">
      <h2>Staff / 剧本家</h2>
      <p class="page-description">
        在下方<strong>多选</strong>最多 10 位人物；哑铃图每条线表示一位 Staff 在其参与作品中 Bangumi 分的<strong>全作品最低</strong>与<strong>全作品最高</strong>（跨作品，非同一作两端）。「Bangumi 详情」跳转人物页。
      </p>
      <p v-if="persons.length" class="selection-meta">已选 {{ selectedIds.length }} / {{ MAX_SELECTED }} 人</p>
    </div>

    <p v-if="loading" class="loading-placeholder">加载中</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>
    <div v-else-if="!persons.length" class="empty-state">
      <p>暂无数据，请先运行 <code>py scripts/import_bangumi_index_persons.py</code> 导入。</p>
    </div>

    <div v-else class="staff-layout">
      <StaffDumbbellChart :rows="dumbbellRows" />

      <div class="grid-toolbar">
        <div>
          <h3 class="grid-heading">人物总览</h3>
          <p class="grid-hint">
            固定一排最多 {{ ROW_SIZE }} 人；窗口较窄时<strong>横向滚动</strong>查看。换一批会<strong>保留本行已选</strong>，其余位置从<strong>未全局选中</strong>的 Staff 中随机换人。
          </p>
        </div>
        <button
          type="button"
          class="shuffle-btn"
          :disabled="!canShuffleNextBatch"
          @click="shuffleBatch"
        >
          换一批
        </button>
      </div>

      <div class="staff-grid-scroller">
        <div
          class="staff-grid"
          :style="gridStyle"
        >
          <div
            v-for="id in displayOrder"
            :key="id"
            class="staff-card"
            :class="{ selected: isSelected(id) }"
            role="button"
            tabindex="0"
            @click="toggleStaff(id)"
            @keydown.enter.prevent="toggleStaff(id)"
          >
            <template v-if="personById(id)">
              <div class="staff-avatar">
                <img
                  v-if="personById(id)!.imageUrl"
                  :src="avatarUrl(personById(id)!.imageUrl!)"
                  :alt="displayName(personById(id)!)"
                  loading="lazy"
                />
                <div v-else class="avatar-placeholder">
                  {{ (displayName(personById(id)!) || "?").slice(0, 1) }}
                </div>
              </div>
              <div class="staff-info">
                <span class="staff-name">{{ displayName(personById(id)!) }}</span>
                <span v-if="personById(id)!.relation" class="staff-relation">{{ personById(id)!.relation }}</span>
              </div>
              <a
                class="staff-detail-link"
                :href="bangumiPersonUrl(id)"
                target="_blank"
                rel="noopener noreferrer"
                @click.stop
              >
                Bangumi 详情
              </a>
            </template>
          </div>
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
import StaffDumbbellChart, { type DumbbellRow } from "../components/StaffDumbbellChart.vue";

interface StaffPersonItem {
  bangumiPersonId: number;
  name: string;
  nameCn: string | null;
  nameJp: string | null;
  imageUrl: string | null;
  relation: string | null;
}

const MAX_SELECTED = 10;
/** 总览区单行展示人数（总人数不足时变少） */
const ROW_SIZE = 15;
const CARD_MIN_PX = 96;

const loading = ref(false);
const ready = ref(false);
const error = ref("");
const persons = ref<StaffPersonItem[]>([]);
/** 选中顺序，哑铃图用 */
const selectedIds = ref<number[]>([]);
/** 当前这一排展示的 personId（顺序固定为「格子」顺序） */
const displayOrder = ref<number[]>([]);

const personMap = computed(() => {
  const m = new Map<number, StaffPersonItem>();
  for (const p of persons.value) m.set(p.bangumiPersonId, p);
  return m;
});

function personById(id: number): StaffPersonItem | undefined {
  return personMap.value.get(id);
}

const gridStyle = computed(() => {
  const n = Math.max(displayOrder.value.length, 1);
  return {
    gridTemplateColumns: `repeat(${n}, minmax(${CARD_MIN_PX}px, 1fr))`,
    minWidth: `calc(${n} * ${CARD_MIN_PX}px + ${(n - 1) * 12}px)`
  };
});

const canShuffleNextBatch = computed(() => {
  if (!persons.value.length || !displayOrder.value.length) return false;
  const sel = new Set(selectedIds.value);
  const hasUnselectedSlot = displayOrder.value.some((id) => !sel.has(id));
  const poolExists = persons.value.some((p) => !sel.has(p.bangumiPersonId));
  return hasUnselectedSlot && poolExists;
});

function isSelected(id: number): boolean {
  return selectedIds.value.includes(id);
}

function toggleStaff(id: number) {
  const list = selectedIds.value.slice();
  const idx = list.indexOf(id);
  if (idx >= 0) {
    list.splice(idx, 1);
  } else if (list.length < MAX_SELECTED) {
    list.push(id);
  }
  selectedIds.value = list;
}

function shuffleArray<T>(arr: T[]): T[] {
  const a = arr.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

function initDisplayOrder() {
  const ids = persons.value.map((p) => p.bangumiPersonId);
  const len = Math.min(ROW_SIZE, ids.length);
  if (len === 0) {
    displayOrder.value = [];
    return;
  }
  displayOrder.value = shuffleArray(ids).slice(0, len);
}

/**
 * 保留本行中「当前全局已选」的格子，其余格子用未全局选中的 Staff 随机填充；
 * 优先换成本行之前未出现过的（在池足够时）。
 */
function shuffleBatch() {
  const sel = new Set(selectedIds.value);
  const cur = displayOrder.value;
  const len = cur.length;
  if (!len) return;

  const next: (number | null)[] = Array(len).fill(null);
  const used = new Set<number>();

  for (let i = 0; i < len; i++) {
    if (sel.has(cur[i])) {
      next[i] = cur[i];
      used.add(cur[i]);
    }
  }

  const poolBase = persons.value.filter((p) => !sel.has(p.bangumiPersonId)).map((p) => p.bangumiPersonId);
  const oldUnselected = cur.filter((id) => !sel.has(id));
  let preferFresh = poolBase.filter((id) => !oldUnselected.includes(id));
  if (preferFresh.length === 0) preferFresh = poolBase.slice();

  let bag = shuffleArray(preferFresh);

  for (let i = 0; i < len; i++) {
    if (next[i] !== null) continue;
    let pick: number | undefined;
    while (bag.length) {
      const c = bag.pop()!;
      if (!used.has(c)) {
        pick = c;
        break;
      }
    }
    if (pick === undefined) {
      const rest = shuffleArray(poolBase.filter((id) => !used.has(id)));
      pick = rest[0];
    }
    if (pick === undefined && poolBase.length) {
      pick = poolBase[Math.floor(Math.random() * poolBase.length)];
    }
    if (pick !== undefined) {
      next[i] = pick;
      used.add(pick);
    }
  }

  for (let i = 0; i < len; i++) {
    if (next[i] === null && poolBase.length) {
      next[i] = poolBase[Math.floor(Math.random() * poolBase.length)];
    }
  }

  displayOrder.value = next.filter((x): x is number => x != null);
}

/** 占位：模拟「全作品最低分 / 全作品最高分」 */
function seededRandom(seed: number) {
  let s = seed % 2147483647;
  if (s <= 0) s += 2147483646;
  return () => {
    s = (s * 48271) % 2147483647;
    return (s - 1) / 2147483646;
  };
}

function mockPortfolioMinMax(personId: number): { minScore: number; maxScore: number } {
  const rnd = seededRandom(personId * 7919 + 12345);
  const low = 2 + rnd() * 3.5;
  const high = 6.5 + rnd() * 3.2;
  let minScore = Math.min(low, high);
  let maxScore = Math.max(low, high);
  if (maxScore - minScore < 0.35) {
    maxScore = Math.min(10, minScore + 0.4 + rnd() * 1.2);
  }
  return {
    minScore: Math.round(minScore * 100) / 100,
    maxScore: Math.round(maxScore * 100) / 100
  };
}

const dumbbellRows = computed((): DumbbellRow[] => {
  const rows: DumbbellRow[] = [];
  for (const id of selectedIds.value) {
    const p = persons.value.find((x) => x.bangumiPersonId === id);
    if (!p) continue;
    const { minScore, maxScore } = mockPortfolioMinMax(id);
    rows.push({
      personId: id,
      title: displayName(p) || `ID ${id}`,
      minScore,
      maxScore
    });
  }
  return rows;
});

function displayName(p: StaffPersonItem): string {
  return p.nameCn || p.nameJp || p.name || "";
}

function avatarUrl(url: string): string {
  if (url.startsWith("//")) return `https:${url}`;
  return url;
}

function bangumiPersonUrl(id: number): string {
  return `https://bangumi.tv/person/${id}`;
}

async function loadData() {
  loading.value = true;
  error.value = "";
  try {
    const json = await apiGet<{ persons?: StaffPersonItem[] }>("/api/staff/persons");
    persons.value = json.persons ?? [];
    initDisplayOrder();
    if (displayOrder.value.length) {
      selectedIds.value = [displayOrder.value[0]];
    } else {
      selectedIds.value = [];
    }
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
.staff-page {
  padding: 16px 20px 40px;
}

.staff-header {
  margin-bottom: 20px;
}

.staff-header h2 {
  margin: 0 0 4px;
}

.staff-header .page-description {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.selection-meta {
  margin: 10px 0 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-accent);
}

.loading-placeholder,
.error-text {
  padding: 24px;
}

.error-text {
  color: var(--color-error, #c55);
}

.empty-state {
  padding: 32px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.empty-state code {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}

.staff-layout {
  display: flex;
  flex-direction: column;
}

.grid-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.grid-heading {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.grid-hint {
  margin: 0;
  max-width: 720px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted);
}

.shuffle-btn {
  flex-shrink: 0;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.shuffle-btn:hover:not(:disabled) {
  border-color: var(--color-accent);
  background: rgba(165, 222, 229, 0.15);
}

.shuffle-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.staff-grid-scroller {
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 8px;
  margin: 0 -4px;
  padding-left: 4px;
  padding-right: 4px;
  -webkit-overflow-scrolling: touch;
}

.staff-grid {
  display: grid;
  gap: 12px;
  align-items: stretch;
}

.staff-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 10px 10px;
  background: var(--color-surface);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
  min-width: 0;
}

.staff-card:hover {
  border-color: var(--color-accent);
  box-shadow: 0 4px 12px rgba(165, 222, 229, 0.35);
}

.staff-card.selected {
  border-color: var(--color-accent);
  background: rgba(165, 222, 229, 0.12);
  box-shadow: 0 0 0 1px rgba(90, 157, 143, 0.35);
}

.staff-card:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.staff-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--color-bg);
}

.staff-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-muted);
  background: linear-gradient(135deg, #fefdca 0%, #e0f9b5 50%, #a5dee5 100%);
}

.staff-info {
  margin-top: 8px;
  text-align: center;
  flex: 1;
  min-width: 0;
}

.staff-name {
  display: block;
  font-weight: 500;
  font-size: 13px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.staff-relation {
  display: block;
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.staff-detail-link {
  margin-top: 8px;
  font-size: 11px;
  font-weight: 500;
  color: var(--color-accent);
  text-decoration: none;
}

.staff-detail-link:hover {
  text-decoration: underline;
}
</style>

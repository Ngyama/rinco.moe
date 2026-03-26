<template>
  <div class="global-sankey-panel">
    <h3 class="sankey-heading">三站 Top 20 桑基图</h3>
    <p class="sankey-description">
      数据与上方三列相同（匹配池）；图中各站仅取该列排名前 20。左侧为三站源节点，右侧为作品节点；同一 vn_match_triple 合并为一节点（与列表悬停高亮规则一致：优先
      matchTripleId，否则标题规范化一致）。
    </p>
    <div class="sankey-container">
      <svg
        :viewBox="`0 0 ${svgW} ${svgH}`"
        class="sankey-svg"
        @mousemove="onMouseMove"
        @mouseleave="hoveredNode = null"
      >
        <g class="links">
          <path
            v-for="(link, i) in linkPaths"
            :key="`link-${i}`"
            :d="link.path"
            :stroke-width="link.strokeWidth"
            class="link-path"
            :class="{ hovered: isLinkHovered(link) }"
          />
        </g>
        <g class="source-nodes">
          <rect
            v-for="(n, i) in sourceNodes"
            :key="`src-${i}`"
            :x="n.x"
            :y="n.y"
            :width="n.width"
            :height="n.height"
            rx="6"
            class="source-node"
            :class="{ hovered: hoveredNode?.type === 'source' && hoveredNode?.id === n.id }"
          />
          <text
            v-for="(n, i) in sourceNodes"
            :key="`src-label-${i}`"
            :x="n.x + n.width / 2"
            :y="n.y + n.height / 2 + 5"
            class="source-label"
          >
            {{ n.label }}
          </text>
        </g>
        <g class="work-nodes">
          <rect
            v-for="(n, i) in workNodes"
            :key="`work-${i}`"
            :x="n.x"
            :y="n.y"
            :width="n.width"
            :height="n.height"
            rx="4"
            class="work-node"
            :class="{ hovered: hoveredNode?.type === 'work' && hoveredNode?.id === n.id }"
          />
          <text
            v-for="(n, i) in workNodes"
            :key="`work-label-${i}`"
            :x="n.x + n.width + 4"
            :y="n.y + n.height / 2 + 4"
            class="work-label"
          >
            {{ truncate(n.label, 14) }}
          </text>
        </g>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";

export interface GlobalScoreItem {
  rank: number;
  titleJp: string;
  score: number | null;
  matchTripleId?: number | null;
}

const props = defineProps<{
  bangumi: GlobalScoreItem[];
  vndb: GlobalScoreItem[];
  egs: GlobalScoreItem[];
}>();

const TOP_N = 20;
const svgW = 1100;
const svgH = 680;

function normalizeTitle(s: string): string {
  return (s || "").trim().replace(/\s+/g, " ");
}

/** Same work across sites: prefer vn_match_triple id, else normalized title. */
function workMergeKey(item: GlobalScoreItem): string {
  const mid = item.matchTripleId;
  if (typeof mid === "number" && !Number.isNaN(mid)) {
    return `m:${mid}`;
  }
  return normalizeTitle(item.titleJp);
}

const hoveredNode = ref<{ type: "source" | "work"; id: string } | null>(null);

const sourceNodes = computed(() => {
  const nodes = [
    { id: "bangumi", label: "Bangumi", list: props.bangumi.slice(0, TOP_N) },
    { id: "vndb", label: "VNDB", list: props.vndb.slice(0, TOP_N) },
    { id: "egs", label: "EGS", list: props.egs.slice(0, TOP_N) }
  ];
  const totalH = svgH - 80;
  const h = totalH / 3;
  return nodes.map((n, i) => ({
    ...n,
    x: 20,
    y: 40 + i * (h + 8),
    width: 100,
    height: h - 4
  }));
});

const workNodes = computed(() => {
  const siteLists = [
    props.bangumi.slice(0, TOP_N),
    props.vndb.slice(0, TOP_N),
    props.egs.slice(0, TOP_N)
  ];
  const workMap = new Map<
    string,
    { title: string; inDegree: number; totalWeight: number }
  >();
  const rankToWeight = (r: number) => TOP_N + 1 - r;

  for (const list of siteLists) {
    for (const item of list) {
      const key = workMergeKey(item);
      const existing = workMap.get(key);
      const w = rankToWeight(item.rank);
      if (existing) {
        existing.inDegree += 1;
        existing.totalWeight += w;
      } else {
        workMap.set(key, {
          title: item.titleJp,
          inDegree: 1,
          totalWeight: w
        });
      }
    }
  }

  const works = Array.from(workMap.entries())
    .map(([key, v]) => ({ key, ...v }))
    .sort((a, b) => {
      if (b.inDegree !== a.inDegree) return b.inDegree - a.inDegree;
      return b.totalWeight - a.totalWeight;
    });

  const totalH = svgH - 80;
  const h = Math.min(28, (totalH - (works.length - 1) * 2) / Math.max(works.length, 1));
  const spacing = works.length > 1 ? (totalH - works.length * h) / (works.length - 1) : 0;

  const nodeW = 140;
  const rightX = svgW - nodeW - 120;

  return works.map((w, i) => ({
    id: w.key,
    label: w.title,
    x: rightX,
    y: 40 + i * (h + spacing),
    width: nodeW,
    height: h,
    inDegree: w.inDegree,
    totalWeight: w.totalWeight
  }));
});

const linkPaths = computed(() => {
  const links: Array<{
    sourceId: string;
    workId: string;
    path: string;
    strokeWidth: number;
    rank: number;
  }> = [];

  const srcNodes = sourceNodes.value;
  const wkNodes = workNodes.value;
  const workIndex = new Map(wkNodes.map((n, i) => [n.id, i]));

  const maxStroke = 14;
  const minStroke = 2;

  for (const src of srcNodes) {
    for (const item of src.list) {
      const wkey = workMergeKey(item);
      const wi = workIndex.get(wkey);
      if (wi == null) continue;

      const weight = TOP_N + 1 - item.rank;
      const strokeWidth = minStroke + (weight / TOP_N) * (maxStroke - minStroke);

      const sx = src.x + src.width;
      const sy = src.y + src.height / 2;
      const wx = wkNodes[wi].x;
      const wy = wkNodes[wi].y + wkNodes[wi].height / 2;

      const cx = (sx + wx) / 2;
      const path = `M ${sx} ${sy} C ${cx} ${sy}, ${cx} ${wy}, ${wx} ${wy}`;

      links.push({
        sourceId: src.id,
        workId: wkey,
        path,
        strokeWidth,
        rank: item.rank
      });
    }
  }

  return links;
});

function isLinkHovered(link: { sourceId: string; workId: string }): boolean {
  const h = hoveredNode.value;
  if (!h) return false;
  if (h.type === "source") return h.id === link.sourceId;
  return h.id === link.workId;
}

function truncate(s: string, len: number): string {
  if (s.length <= len) return s;
  return s.slice(0, len - 2) + "…";
}

function onMouseMove(ev: MouseEvent) {
  const svg = ev.currentTarget as SVGElement;
  if (!svg) return;
  const rect = svg.getBoundingClientRect();
  const x = ((ev.clientX - rect.left) / rect.width) * svgW;
  const y = ((ev.clientY - rect.top) / rect.height) * svgH;

  for (const n of sourceNodes.value) {
    if (x >= n.x && x <= n.x + n.width && y >= n.y && y <= n.y + n.height) {
      hoveredNode.value = { type: "source", id: n.id };
      return;
    }
  }
  for (const n of workNodes.value) {
    const labelWidth = Math.min(n.label.length * 8, 14 * 8);
    if (x >= n.x && x <= n.x + n.width + labelWidth && y >= n.y && y <= n.y + n.height) {
      hoveredNode.value = { type: "work", id: n.id };
      return;
    }
  }
  hoveredNode.value = null;
}
</script>

<style scoped>
.global-sankey-panel {
  margin-top: 40px;
  padding-top: 32px;
  border-top: 1px solid rgba(90, 157, 143, 0.2);
}

.sankey-heading {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.sankey-description {
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.sankey-description code {
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(90, 157, 143, 0.12);
}

.sankey-container {
  overflow-x: auto;
  border-radius: var(--radius-md);
}

.sankey-svg {
  width: 100%;
  min-width: 900px;
  display: block;
}

.links {
  pointer-events: none;
}

.link-path {
  fill: none;
  stroke: rgba(90, 157, 143, 0.35);
  stroke-linecap: round;
  transition:
    stroke 0.15s ease,
    opacity 0.15s ease;
}

.link-path.hovered {
  stroke: rgba(90, 157, 143, 0.7);
  opacity: 1;
}

.source-node {
  fill: rgba(122, 184, 168, 0.4);
  stroke: rgba(90, 157, 143, 0.5);
  stroke-width: 1.5;
  transition:
    fill 0.15s,
    stroke 0.15s;
}

.source-node.hovered {
  fill: rgba(122, 184, 168, 0.65);
  stroke: rgba(90, 157, 143, 0.8);
}

.source-label {
  font-size: 14px;
  font-weight: 600;
  fill: var(--color-text);
  text-anchor: middle;
  pointer-events: none;
}

.work-node {
  fill: rgba(165, 222, 229, 0.35);
  stroke: rgba(90, 157, 143, 0.4);
  stroke-width: 1;
  transition:
    fill 0.15s,
    stroke 0.15s;
}

.work-node.hovered {
  fill: rgba(165, 222, 229, 0.6);
  stroke: rgba(90, 157, 143, 0.7);
}

.work-label {
  font-size: 11px;
  fill: var(--color-text-muted);
  text-anchor: start;
  pointer-events: none;
}
</style>

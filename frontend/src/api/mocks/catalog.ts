/**
 * Shared fictional / representative Galgame catalog for mock responses.
 * IDs and titles are for visual realism only — not factual accuracy.
 */

export interface MockWork {
  id: number;
  titleJp: string;
  titleCn?: string;
  year: number;
  month: number;
  day: number;
  /** Bangumi-like score 1–10 */
  bangumiScore: number;
  vndbScore: number;
  egsScore: number;
  votes: number;
  /** rating_1 … rating_10 counts */
  ratingCounts: number[];
}

function releaseDate(w: MockWork): string {
  const m = String(w.month).padStart(2, "0");
  const d = String(w.day).padStart(2, "0");
  return `${w.year}-${m}-${d}`;
}

/** Deterministic-ish but readable rating distributions. */
function dist(
  r1: number,
  r2: number,
  r3: number,
  r4: number,
  r5: number,
  r6: number,
  r7: number,
  r8: number,
  r9: number,
  r10: number
): number[] {
  return [r1, r2, r3, r4, r5, r6, r7, r8, r9, r10];
}

export const MOCK_WORKS: MockWork[] = [
  { id: 10101, titleJp: "サクラノ詩", titleCn: "樱花之诗", year: 2015, month: 10, day: 23, bangumiScore: 8.72, vndbScore: 8.45, egsScore: 89.2, votes: 4200, ratingCounts: dist(12, 18, 25, 40, 80, 180, 520, 980, 1400, 945) },
  { id: 10102, titleJp: "素晴らしき日々", titleCn: "素晴日", year: 2010, month: 3, day: 26, bangumiScore: 8.91, vndbScore: 8.78, egsScore: 91.5, votes: 6800, ratingCounts: dist(40, 55, 70, 90, 120, 200, 480, 1100, 2200, 2445) },
  { id: 10103, titleJp: "CLANNAD", titleCn: "小镇家庭", year: 2004, month: 4, day: 28, bangumiScore: 8.55, vndbScore: 8.32, egsScore: 88.1, votes: 9500, ratingCounts: dist(30, 45, 60, 100, 220, 450, 980, 2100, 2800, 2715) },
  { id: 10104, titleJp: "Fate/stay night", titleCn: "命运石之门夜", year: 2004, month: 1, day: 30, bangumiScore: 8.48, vndbScore: 8.21, egsScore: 86.4, votes: 11200, ratingCounts: dist(50, 70, 95, 140, 280, 620, 1400, 2600, 3100, 2845) },
  { id: 10105, titleJp: "Rewrite", titleCn: "改写", year: 2011, month: 6, day: 24, bangumiScore: 8.12, vndbScore: 7.95, egsScore: 82.3, votes: 5100, ratingCounts: dist(25, 40, 55, 90, 180, 420, 900, 1400, 1100, 890) },
  { id: 10106, titleJp: "Summer Pockets", titleCn: "夏日口袋", year: 2018, month: 6, day: 29, bangumiScore: 8.35, vndbScore: 8.18, egsScore: 85.7, votes: 3900, ratingCounts: dist(15, 22, 35, 55, 110, 280, 650, 1050, 980, 703) },
  { id: 10107, titleJp: "リトルバスターズ！", titleCn: "Little Busters!", year: 2007, month: 7, day: 27, bangumiScore: 8.41, vndbScore: 8.28, egsScore: 87.0, votes: 7200, ratingCounts: dist(28, 38, 52, 85, 170, 380, 850, 1750, 2100, 1747) },
  { id: 10108, titleJp: "白色相簿2", titleCn: "白色相簿2", year: 2010, month: 12, day: 24, bangumiScore: 8.68, vndbScore: 8.52, egsScore: 90.1, votes: 5800, ratingCounts: dist(35, 48, 62, 95, 150, 280, 620, 1250, 1750, 1505) },
  { id: 10109, titleJp: "結城友奈は勇者である", titleCn: "结城友奈是勇者", year: 2014, month: 12, day: 26, bangumiScore: 7.85, vndbScore: 7.62, egsScore: 78.4, votes: 2100, ratingCounts: dist(18, 28, 45, 80, 160, 320, 480, 420, 310, 239) },
  { id: 10110, titleJp: "九十九の月", titleCn: "九十九之月", year: 2019, month: 8, day: 30, bangumiScore: 7.42, vndbScore: 7.28, egsScore: 74.6, votes: 980, ratingCounts: dist(22, 35, 55, 90, 140, 180, 200, 140, 78, 40) },
  { id: 10111, titleJp: "その花びらにくちづけを", titleCn: "亲吻那片花瓣", year: 2006, month: 9, day: 29, bangumiScore: 7.95, vndbScore: 7.71, egsScore: 80.2, votes: 3400, ratingCounts: dist(20, 30, 48, 75, 140, 320, 680, 900, 720, 467) },
  { id: 10112, titleJp: "はつゆきさくら", titleCn: "初雪樱", year: 2012, month: 2, day: 24, bangumiScore: 8.05, vndbScore: 7.88, egsScore: 81.5, votes: 2800, ratingCounts: dist(16, 24, 40, 70, 130, 280, 560, 720, 580, 380) },
  { id: 10113, titleJp: "まいてつ", titleCn: "铁道少女", year: 2016, month: 3, day: 25, bangumiScore: 7.78, vndbScore: 7.55, egsScore: 79.0, votes: 2200, ratingCounts: dist(14, 22, 38, 65, 120, 260, 480, 520, 400, 281) },
  { id: 10114, titleJp: "アマツツミ", titleCn: "天罪", year: 2016, month: 7, day: 29, bangumiScore: 8.22, vndbScore: 8.05, egsScore: 84.0, votes: 2500, ratingCounts: dist(12, 18, 30, 50, 100, 220, 480, 680, 560, 350) },
  { id: 10115, titleJp: "サクラノ刻", titleCn: "樱花之刻", year: 2023, month: 2, day: 24, bangumiScore: 8.58, vndbScore: 8.35, egsScore: 88.8, votes: 1800, ratingCounts: dist(8, 12, 20, 35, 70, 150, 320, 450, 420, 315) },
  { id: 10116, titleJp: "かけぬけ★青春スパーキング！", titleCn: "疾驰青春", year: 2020, month: 8, day: 28, bangumiScore: 7.55, vndbScore: 7.32, egsScore: 76.1, votes: 1600, ratingCounts: dist(15, 25, 42, 70, 140, 280, 360, 340, 220, 108) },
  { id: 10117, titleJp: "RIDDLE JOKER", titleCn: "谜语小丑", year: 2018, month: 4, day: 27, bangumiScore: 7.68, vndbScore: 7.48, egsScore: 77.5, votes: 3100, ratingCounts: dist(20, 32, 50, 85, 160, 350, 620, 700, 580, 403) },
  { id: 10118, titleJp: "千恋＊万花", titleCn: "千恋＊万花", year: 2016, month: 7, day: 29, bangumiScore: 8.15, vndbScore: 7.98, egsScore: 83.2, votes: 4500, ratingCounts: dist(18, 28, 42, 70, 140, 320, 750, 1200, 1050, 882) },
  { id: 10119, titleJp: "サノバウィッチ", titleCn: "魔女的夜宴", year: 2015, month: 2, day: 27, bangumiScore: 8.08, vndbScore: 7.92, egsScore: 82.0, votes: 4800, ratingCounts: dist(22, 35, 50, 80, 160, 380, 820, 1250, 1100, 903) },
  { id: 10120, titleJp: "トラベリングスターズ", titleCn: "旅星", year: 2021, month: 5, day: 28, bangumiScore: 7.25, vndbScore: 7.05, egsScore: 72.0, votes: 720, ratingCounts: dist(28, 45, 70, 95, 120, 140, 110, 70, 30, 12) },
  { id: 10121, titleJp: "金色ラブリッチェ", titleCn: "金色恋曲", year: 2017, month: 12, day: 22, bangumiScore: 7.92, vndbScore: 7.75, egsScore: 80.8, votes: 3600, ratingCounts: dist(16, 26, 42, 72, 140, 320, 680, 900, 780, 624) },
  { id: 10122, titleJp: "アマカネ", titleCn: "天音", year: 2017, month: 5, day: 26, bangumiScore: 7.88, vndbScore: 7.65, egsScore: 79.5, votes: 1900, ratingCounts: dist(12, 20, 35, 60, 110, 240, 420, 480, 340, 183) },
  { id: 10123, titleJp: "キメラプロジェクト", titleCn: "奇美拉计划", year: 2022, month: 9, day: 30, bangumiScore: 6.85, vndbScore: 6.62, egsScore: 68.0, votes: 540, ratingCounts: dist(45, 55, 70, 80, 75, 70, 55, 40, 30, 20) },
  { id: 10124, titleJp: "真・恋姫†無双", titleCn: "真恋姬无双", year: 2009, month: 5, day: 29, bangumiScore: 7.35, vndbScore: 7.12, egsScore: 73.5, votes: 2700, ratingCounts: dist(30, 45, 70, 110, 220, 400, 520, 480, 420, 405) },
  { id: 10125, titleJp: "アトリエシリーズ風モック", titleCn: "炼金工房风", year: 2024, month: 4, day: 26, bangumiScore: 7.62, vndbScore: 7.40, egsScore: 75.8, votes: 890, ratingCounts: dist(10, 18, 30, 55, 100, 180, 220, 160, 80, 37) },
  { id: 10126, titleJp: "月に寄りそう乙女の作法", titleCn: "近月少女的礼仪", year: 2012, month: 12, day: 21, bangumiScore: 8.28, vndbScore: 8.05, egsScore: 84.5, votes: 3200, ratingCounts: dist(14, 22, 35, 58, 110, 260, 580, 850, 780, 491) },
  { id: 10127, titleJp: "ノラと皇女と野良猫ハート", titleCn: "野良与皇女与流浪猫之心", year: 2016, month: 2, day: 26, bangumiScore: 7.48, vndbScore: 7.25, egsScore: 75.0, votes: 2400, ratingCounts: dist(25, 40, 60, 95, 180, 350, 520, 500, 380, 250) },
  { id: 10128, titleJp: "時計仕掛けのレイライン", titleCn: "发条骑士线", year: 2013, month: 7, day: 26, bangumiScore: 7.72, vndbScore: 7.50, egsScore: 78.2, votes: 1500, ratingCounts: dist(12, 20, 35, 60, 120, 250, 340, 320, 220, 123) },
  { id: 10129, titleJp: "蒼の彼方のフォーリズム", titleCn: "苍之彼方的四重奏", year: 2014, month: 11, day: 28, bangumiScore: 7.82, vndbScore: 7.60, egsScore: 79.8, votes: 4100, ratingCounts: dist(20, 32, 48, 80, 160, 380, 780, 1050, 900, 650) },
  { id: 10130, titleJp: "恋する乙女と守護の楯", titleCn: "恋爱少女与守护之盾", year: 2007, month: 6, day: 29, bangumiScore: 7.15, vndbScore: 6.95, egsScore: 71.2, votes: 1800, ratingCounts: dist(35, 50, 75, 120, 220, 320, 350, 300, 220, 110) },
  { id: 10131, titleJp: "夢幻の桜", titleCn: "梦幻之樱", year: 1998, month: 8, day: 14, bangumiScore: 7.05, vndbScore: 6.88, egsScore: 70.0, votes: 620, ratingCounts: dist(20, 30, 45, 70, 100, 120, 110, 75, 35, 15) },
  { id: 10132, titleJp: "星降る夜の物語", titleCn: "星降之夜物语", year: 2001, month: 11, day: 22, bangumiScore: 7.28, vndbScore: 7.05, egsScore: 72.5, votes: 850, ratingCounts: dist(18, 28, 42, 68, 110, 160, 180, 140, 70, 34) },
  { id: 10133, titleJp: "海辺のカケラ", titleCn: "海边的碎片", year: 2003, month: 5, day: 30, bangumiScore: 7.55, vndbScore: 7.32, egsScore: 76.0, votes: 1100, ratingCounts: dist(15, 25, 40, 65, 120, 200, 250, 220, 110, 55) },
  { id: 10134, titleJp: "虚ろな記憶の庭", titleCn: "空洞记忆之庭", year: 2025, month: 3, day: 28, bangumiScore: 7.95, vndbScore: 7.70, egsScore: 80.5, votes: 640, ratingCounts: dist(8, 12, 22, 40, 70, 120, 150, 120, 70, 28) },
  { id: 10135, titleJp: "黄昏のシンフォニー", titleCn: "黄昏交响曲", year: 2025, month: 7, day: 25, bangumiScore: 8.05, vndbScore: 7.85, egsScore: 81.8, votes: 520, ratingCounts: dist(5, 8, 15, 28, 55, 95, 130, 110, 55, 19) },
  { id: 10136, titleJp: "硝子の檻", titleCn: "玻璃牢笼", year: 2024, month: 11, day: 29, bangumiScore: 6.42, vndbScore: 6.20, egsScore: 64.5, votes: 780, ratingCounts: dist(80, 95, 110, 120, 100, 90, 70, 55, 40, 20) },
  { id: 10137, titleJp: "永久機関の恋人", titleCn: "永动机恋人", year: 1995, month: 4, day: 21, bangumiScore: 6.88, vndbScore: 6.65, egsScore: 68.8, votes: 410, ratingCounts: dist(25, 35, 48, 60, 70, 65, 50, 35, 15, 7) },
  { id: 10138, titleJp: "銀盤カレイド", titleCn: "银盘万花筒", year: 2008, month: 1, day: 25, bangumiScore: 7.65, vndbScore: 7.42, egsScore: 77.0, votes: 1350, ratingCounts: dist(14, 22, 38, 62, 115, 230, 310, 280, 180, 99) },
  { id: 10139, titleJp: "雨音ワルツ", titleCn: "雨音圆舞曲", year: 2013, month: 10, day: 25, bangumiScore: 7.90, vndbScore: 7.68, egsScore: 80.0, votes: 1680, ratingCounts: dist(12, 18, 32, 55, 100, 220, 380, 420, 300, 143) },
  { id: 10140, titleJp: "夜明けのカルテット", titleCn: "拂晓四重奏", year: 2020, month: 1, day: 31, bangumiScore: 8.18, vndbScore: 7.95, egsScore: 83.5, votes: 2100, ratingCounts: dist(10, 16, 28, 48, 95, 210, 420, 560, 450, 263) }
];

export function workReleaseDate(w: MockWork): string {
  return releaseDate(w);
}

export function sumVotes(counts: number[]): number {
  return counts.reduce((a, b) => a + b, 0);
}

export function computeMean(counts: number[]): number {
  const total = sumVotes(counts);
  if (total <= 0) return 0;
  let sum = 0;
  for (let i = 0; i < counts.length; i++) sum += (i + 1) * counts[i];
  return sum / total;
}

export function computeMedian(counts: number[], total: number): number {
  if (total <= 0) return 0;
  const mid = (total + 1) / 2;
  let acc = 0;
  for (let i = 0; i < counts.length; i++) {
    acc += counts[i];
    if (acc >= mid) return i + 1;
  }
  return 10;
}

export function computeMode(counts: number[]): number {
  let best = 1;
  let bestC = -1;
  for (let i = 0; i < counts.length; i++) {
    if (counts[i] > bestC) {
      bestC = counts[i];
      best = i + 1;
    }
  }
  return best;
}

export function computeStdDev(counts: number[], total: number): number {
  if (total <= 0) return 0;
  const mean = computeMean(counts);
  let varSum = 0;
  for (let i = 0; i < counts.length; i++) {
    const d = i + 1 - mean;
    varSum += counts[i] * d * d;
  }
  return Math.sqrt(varSum / total);
}

export function computeExtremeRatio(counts: number[], total: number): number {
  if (total <= 0) return 0;
  const extreme = counts[0] + counts[1] + counts[8] + counts[9];
  return extreme / total;
}

export function findWork(id: number): MockWork | undefined {
  return MOCK_WORKS.find((w) => w.id === id);
}

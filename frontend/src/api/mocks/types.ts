/** Shared API response shapes used by mock data and the resolver. */

export interface GlobalScoreItem {
  rank: number;
  titleJp: string;
  score: number | null;
  matchTripleId?: number | null;
}

export interface CombinedRankItem {
  rank: number;
  titleJp: string;
  bangumiScore: number;
  vndbScore: number;
  egsScore: number;
  combinedScore: number;
  matchTripleId?: number | null;
}

export interface DualTopResponse {
  limit: number;
  bangumi: GlobalScoreItem[];
  vndb: GlobalScoreItem[];
  egs: GlobalScoreItem[];
  combinedTop10?: CombinedRankItem[];
}

export interface BangumiYearAverageItem {
  year: number;
  avgScore: number;
  count: number;
}

export interface BangumiYearPointItem {
  year: number;
  releaseDate: string;
  title: string;
  score: number;
}

export interface BangumiTrendResponse {
  minVotes: number;
  averages: BangumiYearAverageItem[];
  points: BangumiYearPointItem[];
}

export interface HotGameItem {
  bangumiSubjectId: number;
  titleJp: string;
  score: number | null;
  ratingTotal: number | null;
  releaseDate: string | null;
  controversy?: number;
}

export interface HotGameDetail {
  bangumiSubjectId: number;
  titleJp: string;
  score: number;
  ratingTotal: number;
  releaseDate: string | null;
  median: number;
  mode: number;
  stdDev: number;
  extremeRatio: number;
  ratingCounts: number[];
}

export interface HotGamesResponse {
  year?: number;
  games: HotGameItem[];
}

export interface ControversyItem {
  bangumiSubjectId: number;
  titleJp: string;
  score: number;
  ratingTotal: number;
  controversy: number;
}

export interface ExtremeBarRow {
  bangumiSubjectId: number;
  titleJp: string;
  ratingTotal: number;
  lowShare: number;
  highShare: number;
}

export interface ControversyResponse {
  yearFrom?: number;
  yearTo?: number;
  items: ControversyItem[];
}

export interface ExtremeBarsResponse {
  items: ExtremeBarRow[];
}

export interface StaffPersonItem {
  bangumiPersonId: number;
  name: string;
  nameCn: string | null;
  nameJp: string | null;
  imageUrl: string | null;
  relation: string | null;
}

export interface StaffPersonsResponse {
  persons: StaffPersonItem[];
}

export interface UserCollectionItem {
  subjectId: number;
  subjectName: string;
  subjectNameCn: string;
  rate: number;
  type: string;
  comment: string;
}

export interface UserCollectionsResponse {
  usernameOrId: string;
  items: UserCollectionItem[];
}

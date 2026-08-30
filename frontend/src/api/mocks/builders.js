import { MOCK_WORKS, computeExtremeRatio, computeMedian, computeMode, computeStdDev, sumVotes, workReleaseDate } from "./catalog";
function round4(n) {
    return Math.round(n * 10000) / 10000;
}
function round2(n) {
    return Math.round(n * 100) / 100;
}
/** Top matched works shared across Bangumi / VNDB / EGS with stable matchTripleId. */
export function buildGlobalDualTop(limit = 25) {
    const take = Math.min(Math.max(limit, 1), MOCK_WORKS.length);
    const pool = MOCK_WORKS.slice(0, take);
    const byBangumi = [...pool].sort((a, b) => b.bangumiScore - a.bangumiScore);
    const byVndb = [...pool].sort((a, b) => b.vndbScore - a.vndbScore);
    const byEgs = [...pool].sort((a, b) => b.egsScore - a.egsScore);
    const toItem = (w, rank, score) => ({
        rank,
        titleJp: w.titleJp.trim(),
        score: round4(score),
        matchTripleId: w.id
    });
    const bangumi = byBangumi.map((w, i) => toItem(w, i + 1, w.bangumiScore));
    const vndb = byVndb.map((w, i) => toItem(w, i + 1, w.vndbScore));
    const egs = byEgs.map((w, i) => toItem(w, i + 1, w.egsScore / 10));
    const combined = [...pool]
        .map((w) => {
        const bangumiScore = w.bangumiScore;
        const vndbScore = w.vndbScore;
        const egsScore = round4(w.egsScore / 10);
        const combinedScore = round4(bangumiScore * 0.4 + vndbScore * 0.35 + egsScore * 0.25);
        return {
            titleJp: w.titleJp.trim(),
            bangumiScore: round4(bangumiScore),
            vndbScore: round4(vndbScore),
            egsScore,
            combinedScore,
            matchTripleId: w.id
        };
    })
        .sort((a, b) => b.combinedScore - a.combinedScore)
        .slice(0, 10)
        .map((row, i) => ({ rank: i + 1, ...row }));
    return { limit: take, bangumi, vndb, egs, combinedTop10: combined };
}
export function buildTrend(minVotes = 50) {
    const points = MOCK_WORKS.filter((w) => sumVotes(w.ratingCounts) >= minVotes || w.votes >= minVotes).map((w) => ({
        year: w.year,
        releaseDate: workReleaseDate(w),
        title: w.titleJp.trim(),
        score: round4(w.bangumiScore)
    }));
    const byYear = new Map();
    for (const p of points) {
        const cur = byYear.get(p.year) ?? { sum: 0, count: 0 };
        cur.sum += p.score;
        cur.count += 1;
        byYear.set(p.year, cur);
    }
    // Pad extra synthetic yearly points so scatter / averages span many years
    for (let y = 1995; y <= 2025; y++) {
        if (byYear.has(y))
            continue;
        const base = 6.8 + ((y * 17) % 23) / 20;
        const count = 2 + (y % 4);
        byYear.set(y, { sum: base * count, count });
        for (let i = 0; i < count; i++) {
            points.push({
                year: y,
                releaseDate: `${y}-${String(3 + (i % 9)).padStart(2, "0")}-${String(10 + i * 3).padStart(2, "0")}`,
                title: `モック作品 ${y}-${i + 1}`,
                score: round4(base + (i - 1) * 0.25)
            });
        }
    }
    const averages = Array.from(byYear.entries())
        .sort((a, b) => a[0] - b[0])
        .map(([year, { sum, count }]) => ({
        year,
        avgScore: round4(sum / count),
        count
    }));
    return { minVotes, averages, points };
}
function toHotItem(w) {
    const counts = w.ratingCounts;
    const total = sumVotes(counts);
    return {
        bangumiSubjectId: w.id,
        titleJp: w.titleJp.trim(),
        score: round4(w.bangumiScore),
        ratingTotal: total || w.votes,
        releaseDate: workReleaseDate(w),
        controversy: round4(computeStdDev(counts, total || w.votes))
    };
}
export function buildHotList(year, limit, sort) {
    const want = Math.max(1, limit);
    let games = MOCK_WORKS.filter((w) => w.year === year).map(toHotItem);
    // Widen the year window until we have enough rows for list + compare UI
    if (games.length < want) {
        const nearby = MOCK_WORKS.filter((w) => Math.abs(w.year - year) <= 3)
            .map(toHotItem)
            .filter((g) => !games.some((x) => x.bangumiSubjectId === g.bangumiSubjectId));
        games = [...games, ...nearby];
    }
    if (games.length < want) {
        const rest = MOCK_WORKS.map(toHotItem).filter((g) => !games.some((x) => x.bangumiSubjectId === g.bangumiSubjectId));
        games = [...games, ...rest];
    }
    const s = (sort || "markers").toLowerCase();
    games.sort((a, b) => {
        switch (s) {
            case "score":
                return (b.score ?? 0) - (a.score ?? 0);
            case "rating":
                return (b.ratingTotal ?? 0) - (a.ratingTotal ?? 0);
            case "controversy":
                return (b.controversy ?? 0) - (a.controversy ?? 0);
            default:
                return (b.ratingTotal ?? 0) - (a.ratingTotal ?? 0);
        }
    });
    return { year, games: games.slice(0, want) };
}
export function buildHotDetail(id) {
    const w = MOCK_WORKS.find((x) => x.id === id) ?? MOCK_WORKS[0];
    if (!w)
        return null;
    const counts = w.ratingCounts;
    const total = sumVotes(counts) || w.votes;
    return {
        bangumiSubjectId: w.id,
        titleJp: w.titleJp.trim(),
        score: round4(w.bangumiScore),
        ratingTotal: total,
        releaseDate: workReleaseDate(w),
        median: computeMedian(counts, total),
        mode: computeMode(counts),
        stdDev: round4(computeStdDev(counts, total)),
        extremeRatio: round4(computeExtremeRatio(counts, total)),
        ratingCounts: [...counts]
    };
}
function controversyItem(w) {
    const total = sumVotes(w.ratingCounts) || w.votes;
    return {
        bangumiSubjectId: w.id,
        titleJp: w.titleJp.trim(),
        score: round4(w.bangumiScore),
        ratingTotal: total,
        controversy: round4(computeStdDev(w.ratingCounts, total))
    };
}
export function buildControversy(yearFrom, yearTo, minVotes) {
    let pool = MOCK_WORKS.filter((w) => {
        const total = sumVotes(w.ratingCounts) || w.votes;
        return w.year >= yearFrom && w.year <= yearTo && total >= minVotes;
    });
    // Keep the bubble chart dense enough for redesign work when a narrow year filter is sparse
    if (pool.length < 12) {
        pool = MOCK_WORKS.filter((w) => (sumVotes(w.ratingCounts) || w.votes) >= minVotes);
    }
    const items = pool.map(controversyItem).sort((a, b) => b.controversy - a.controversy);
    return { yearFrom, yearTo, items };
}
export function buildExtremeBars(yearFrom, yearTo, minVotes) {
    const base = buildControversy(yearFrom, yearTo, minVotes).items.slice(0, 10);
    const items = base.map((it) => {
        const w = MOCK_WORKS.find((x) => x.id === it.bangumiSubjectId);
        const counts = w?.ratingCounts ?? [0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
        const total = sumVotes(counts) || it.ratingTotal || 1;
        const lowShare = round2((counts[0] + counts[1]) / total);
        const highShare = round2((counts[8] + counts[9]) / total);
        return {
            bangumiSubjectId: it.bangumiSubjectId,
            titleJp: it.titleJp,
            ratingTotal: it.ratingTotal,
            lowShare,
            highShare
        };
    });
    return { items };
}
export function buildStaffPersons() {
    const persons = [
        { bangumiPersonId: 2001, name: "涼元悠一", nameCn: "凉元悠一", nameJp: "涼元悠一", relation: "脚本" },
        { bangumiPersonId: 2002, name: "麻枝准", nameCn: "麻枝准", nameJp: "麻枝准", relation: "脚本" },
        { bangumiPersonId: 2003, name: "樋上いたる", nameCn: "樋上至", nameJp: "樋上いたる", relation: "原画" },
        { bangumiPersonId: 2004, name: "永山ゆうなん", nameCn: "永山由宇南", nameJp: "永山ゆうなん", relation: "原画" },
        { bangumiPersonId: 2005, name: "三輪士郎", nameCn: "三轮士郎", nameJp: "三輪士郎", relation: "原画" },
        { bangumiPersonId: 2006, name: "白泽晃彦", nameCn: "白泽晃彦", nameJp: "白澤晃彦", relation: "脚本" },
        { bangumiPersonId: 2007, name: "柚子奈ひよ", nameCn: "柚子奈日向", nameJp: "柚子奈ひよ", relation: "原画" },
        { bangumiPersonId: 2008, name: "あおきけい", nameCn: "青木桂", nameJp: "あおきけい", relation: "原画" },
        { bangumiPersonId: 2009, name: "声優・佐藤利奈", nameCn: "佐藤利奈", nameJp: "佐藤利奈", relation: "声优" },
        { bangumiPersonId: 2010, name: "声優・釘宮理恵", nameCn: "钉宫理惠", nameJp: "釘宮理恵", relation: "声优" },
        { bangumiPersonId: 2011, name: "声優・悠木碧", nameCn: "悠木碧", nameJp: "悠木碧", relation: "声优" },
        { bangumiPersonId: 2012, name: "声優・早見沙織", nameCn: "早见沙织", nameJp: "早見沙織", relation: "声优" },
        { bangumiPersonId: 2013, name: "折戸伸治", nameCn: "折户伸治", nameJp: "折戸伸治", relation: "音乐" },
        { bangumiPersonId: 2014, name: "水月陵", nameCn: "水月陵", nameJp: "水月陵", relation: "音乐" },
        { bangumiPersonId: 2015, name: "松本文紀", nameCn: "松本文纪", nameJp: "松本文紀", relation: "音乐" },
        { bangumiPersonId: 2016, name: "丸戸史明", nameCn: "丸户史明", nameJp: "丸戸史明", relation: "脚本" },
        { bangumiPersonId: 2017, name: "禄々", nameCn: "禄禄", nameJp: "禄々", relation: "原画" },
        { bangumiPersonId: 2018, name: "梱枝りこ", nameCn: "梱枝里子", nameJp: "梱枝りこ", relation: "原画" },
        { bangumiPersonId: 2019, name: "あめとゆき", nameCn: "雨与雪", nameJp: "あめとゆき", relation: "原画" },
        { bangumiPersonId: 2020, name: "声優・茅野愛衣", nameCn: "茅野爱衣", nameJp: "茅野愛衣", relation: "声优" },
        { bangumiPersonId: 2021, name: "声優・田村ゆかり", nameCn: "田村由香里", nameJp: "田村ゆかり", relation: "声优" },
        { bangumiPersonId: 2022, name: "魁", nameCn: "魁", nameJp: "魁", relation: "脚本" },
        { bangumiPersonId: 2023, name: "かずきふみ", nameCn: "和泉文", nameJp: "かずきふみ", relation: "脚本" },
        { bangumiPersonId: 2024, name: "永田健二郎", nameCn: "永田健二郎", nameJp: "永田健二郎", relation: "制作" },
        { bangumiPersonId: 2025, name: "声優・花澤香菜", nameCn: "花泽香菜", nameJp: "花澤香菜", relation: "声优" },
        { bangumiPersonId: 2026, name: "声優・堀江由衣", nameCn: "堀江由衣", nameJp: "堀江由衣", relation: "声优" },
        { bangumiPersonId: 2027, name: "大木伸一", nameCn: "大木伸一", nameJp: "大木伸一", relation: "脚本" },
        { bangumiPersonId: 2028, name: "鈴平ひろ", nameCn: "铃平广", nameJp: "鈴平ひろ", relation: "原画" },
        { bangumiPersonId: 2029, name: "声優・中村繪里子", nameCn: "中村绘里子", nameJp: "中村繪里子", relation: "声优" },
        { bangumiPersonId: 2030, name: "声優・斎藤千和", nameCn: "斋藤千和", nameJp: "斎藤千和", relation: "声优" }
    ].map((p) => ({
        ...p,
        // Placeholder avatars — pages already tolerate null / broken images
        imageUrl: null
    }));
    return { persons };
}
export function buildUserCollections(usernameOrId) {
    const types = ["2", "2", "2", "2", "3", "1", "4", "5", "2", "2"];
    const comments = [
        "神作。结局哭到不行。",
        "音乐和氛围一流，剧本略拖。",
        "女主全员可爱，推了。",
        "争议路线太多，但值得一玩。",
        "在看中，节奏不错。",
        "想看很久了，先入坑清单。",
        "搁置——等有空再开。",
        "剧情崩了，弃。",
        "二周目收获很大。",
        "画风喜欢，系统一般。",
        "年度最佳候补。",
        "评分虚高？感觉也就那样。",
        ""
    ];
    const items = MOCK_WORKS.slice(0, 28).map((w, i) => ({
        subjectId: w.id,
        subjectName: w.titleJp.trim(),
        subjectNameCn: w.titleCn ?? w.titleJp.trim(),
        rate: i % 7 === 0 ? 0 : 1 + ((w.id + i * 3) % 10),
        type: types[i % types.length],
        comment: comments[i % comments.length]
    }));
    return {
        usernameOrId: usernameOrId || "mock-user",
        items
    };
}

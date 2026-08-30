import { buildControversy, buildExtremeBars, buildGlobalDualTop, buildHotDetail, buildHotList, buildStaffPersons, buildTrend, buildUserCollections } from "./builders";
function num(params, key, fallback) {
    const v = params?.[key];
    if (v == null || v === "")
        return fallback;
    const n = Number(v);
    return Number.isFinite(n) ? n : fallback;
}
function str(params, key, fallback) {
    const v = params?.[key];
    if (v == null)
        return fallback;
    return String(v);
}
/**
 * Resolve mock payload for a known API path.
 * Returns `undefined` when the path has no mock (caller should keep the real error).
 */
export function resolveMock(path, params) {
    const clean = path.split("?")[0].replace(/\/+$/, "") || "/";
    if (clean === "/api/global/dual-top") {
        return buildGlobalDualTop(num(params, "limit", 25));
    }
    if (clean === "/api/trend/bangumi/history") {
        return buildTrend(num(params, "minVotes", 50));
    }
    if (clean === "/api/structure/hot") {
        return buildHotList(num(params, "year", 2025), num(params, "limit", 10), str(params, "sort", "markers"));
    }
    const hotDetail = clean.match(/^\/api\/structure\/hot\/(\d+)$/);
    if (hotDetail) {
        return buildHotDetail(Number(hotDetail[1]));
    }
    if (clean === "/api/structure/controversy") {
        return buildControversy(num(params, "yearFrom", 2025), num(params, "yearTo", 2025), num(params, "minVotes", 20));
    }
    if (clean === "/api/structure/controversy/extreme-bars") {
        return buildExtremeBars(num(params, "yearFrom", 2025), num(params, "yearTo", 2025), num(params, "minVotes", 20));
    }
    if (clean === "/api/staff/persons") {
        return buildStaffPersons();
    }
    if (clean === "/api/user/collections") {
        return buildUserCollections(str(params, "id", "mock-user"));
    }
    return undefined;
}

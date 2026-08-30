const DEFAULT_TIMEOUT_MS = 12_000;
/** Shorter timeout when mock fallback is on so a dead backend doesn't block the UI for 12s. */
const MOCK_FALLBACK_TIMEOUT_MS = 2_500;

function buildUrl(path: string, params?: Record<string, string | number | boolean>): string {
  const url = path.startsWith("/") ? path : `/${path}`;
  if (!params || Object.keys(params).length === 0) return url;
  const search = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    search.set(k, String(v));
  }
  return `${url}?${search.toString()}`;
}

function mockFlag(): string {
  return String(import.meta.env.VITE_USE_MOCK_DATA ?? "").toLowerCase();
}

/** True when mock fallback / force mode should be considered. */
function isMockEnabled(): boolean {
  const flag = mockFlag();
  if (flag === "false" || flag === "0" || flag === "off") return false;
  if (flag === "true" || flag === "1" || flag === "on" || flag === "force" || flag === "always") {
    return true;
  }
  return Boolean(import.meta.env.DEV);
}

function isMockForce(): boolean {
  const flag = mockFlag();
  return flag === "force" || flag === "always";
}

async function tryMockFallback<T>(
  path: string,
  params: Record<string, string | number | boolean> | undefined,
  reason: string
): Promise<T | undefined> {
  if (!isMockEnabled()) return undefined;
  const { resolveMock } = await import("./mocks");
  const mock = resolveMock(path, params);
  if (mock === undefined) return undefined;
  console.warn(`[mock] ${path} ← mock data (${reason})`);
  return mock as T;
}

export async function apiGet<T = unknown>(
  path: string,
  params?: Record<string, string | number | boolean>,
  options?: { timeout?: number; cache?: RequestCache }
): Promise<T> {
  if (isMockForce()) {
    const forced = await tryMockFallback<T>(path, params, "VITE_USE_MOCK_DATA=force");
    if (forced !== undefined) return forced;
  }

  const useMock = isMockEnabled();
  const timeout = options?.timeout ?? (useMock ? MOCK_FALLBACK_TIMEOUT_MS : DEFAULT_TIMEOUT_MS);
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeout);

  try {
    const url = buildUrl(path, params);
    const res = await fetch(url, {
      signal: controller.signal,
      cache: options?.cache ?? "default"
    });
    if (!res.ok) {
      const mocked = await tryMockFallback<T>(path, params, `HTTP ${res.status}`);
      if (mocked !== undefined) return mocked;
      throw new Error(`请求失败: HTTP ${res.status}`);
    }
    return (await res.json()) as T;
  } catch (e) {
    const reason =
      e instanceof Error ? (e.name === "AbortError" ? "timeout" : e.message) : "network error";
    const mocked = await tryMockFallback<T>(path, params, reason);
    if (mocked !== undefined) return mocked;

    if (e instanceof Error) {
      if (e.name === "AbortError") throw new Error("请求超时");
      throw e;
    }
    throw new Error("加载失败");
  } finally {
    window.clearTimeout(timeoutId);
  }
}

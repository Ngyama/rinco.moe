const DEFAULT_TIMEOUT_MS = 12_000;

function buildUrl(path: string, params?: Record<string, string | number | boolean>): string {
  const url = path.startsWith("/") ? path : `/${path}`;
  if (!params || Object.keys(params).length === 0) return url;
  const search = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    search.set(k, String(v));
  }
  return `${url}?${search.toString()}`;
}

export async function apiGet<T = unknown>(
  path: string,
  params?: Record<string, string | number | boolean>,
  options?: { timeout?: number; cache?: RequestCache }
): Promise<T> {
  const timeout = options?.timeout ?? DEFAULT_TIMEOUT_MS;
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeout);

  try {
    const url = buildUrl(path, params);
    const res = await fetch(url, {
      signal: controller.signal,
      cache: options?.cache ?? "default"
    });
    if (!res.ok) {
      throw new Error(`请求失败: HTTP ${res.status}`);
    }
    return (await res.json()) as T;
  } catch (e) {
    if (e instanceof Error) {
      if (e.name === "AbortError") throw new Error("请求超时");
      throw e;
    }
    throw new Error("加载失败");
  } finally {
    window.clearTimeout(timeoutId);
  }
}

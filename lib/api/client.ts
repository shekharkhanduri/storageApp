export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const ACCESS_TOKEN_COOKIE = "storeit_access_token";
export const REFRESH_TOKEN_COOKIE = "storeit-refresh";

export const ACCESS_TOKEN_MAX_AGE = 15 * 60; // 15 minutes
export const REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

type ApiOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  accessToken?: string;
  refreshToken?: string;
};

const buildHeaders = (options: ApiOptions): HeadersInit => {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  if (options.accessToken) {
    headers.Authorization = `Bearer ${options.accessToken}`;
  }

  if (options.refreshToken) {
    headers.Cookie = `${REFRESH_TOKEN_COOKIE}=${options.refreshToken}`;
  }

  return { ...headers, ...options.headers };
};

export async function apiFetch<T>(path: string, options: ApiOptions = {}) {
  const { body, cache, ...init } = options;

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    cache: cache ?? options.cache,
    headers: buildHeaders(options),
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  let data: T | null = null;
  try {
    data = (await res.json()) as T;
  } catch {
    data = null;
  }

  return { res, data };
}

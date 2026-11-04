const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const ACCESS_KEY = "pet-room.token";
const REFRESH_KEY = "pet-room.refresh";

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public fieldErrors: { field: string; message: string }[] = [],
  ) {
    super(message);
  }
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  refreshExpiresIn: number;
}

function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string | null) {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, value);
  } catch {
    /* 저장소를 쓸 수 없으면 이번 화면에서만 로그인 상태 유지 */
  }
}

export const tokenStore = {
  get: () => read(ACCESS_KEY),
  refresh: () => read(REFRESH_KEY),
  save(tokens: TokenResponse) {
    write(ACCESS_KEY, tokens.accessToken);
    write(REFRESH_KEY, tokens.refreshToken);
  },
  clear() {
    write(ACCESS_KEY, null);
    write(REFRESH_KEY, null);
  },
};

async function send(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  if (init.body) headers.set("Content-Type", "application/json");
  const token = tokenStore.get();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  try {
    return await fetch(`${BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError(0, "NETWORK", "서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인해 주세요.");
  }
}

// 여러 요청이 동시에 401을 받아도 토큰 갱신은 한 번만 합니다.
let refreshing: Promise<boolean> | null = null;

function refreshTokens(): Promise<boolean> {
  const refreshToken = tokenStore.refresh();
  if (!refreshToken) return Promise.resolve(false);
  refreshing ??= fetch(`${BASE_URL}/api/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (res) => {
      if (!res.ok) return false;
      tokenStore.save((await res.json()) as TokenResponse);
      return true;
    })
    .catch(() => false)
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response = await send(path, init);

  const isAuthCall = path.startsWith("/api/auth/");
  if (response.status === 401 && !isAuthCall) {
    if (await refreshTokens()) {
      response = await send(path, init);
    }
    if (response.status === 401) tokenStore.clear();
  }

  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(
      response.status,
      problem.code ?? "UNKNOWN",
      problem.detail ?? "요청을 처리하지 못했습니다.",
      problem.errors ?? [],
    );
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export async function logout() {
  const refreshToken = tokenStore.refresh();
  tokenStore.clear();
  if (refreshToken) {
    await fetch(`${BASE_URL}/api/auth/logout`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    }).catch(() => undefined);
  }
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    return e.fieldErrors.length ? e.fieldErrors.map((f) => f.message).join(" ") : e.message;
  }
  return "알 수 없는 오류가 발생했습니다.";
}

export type Provider = "kakao" | "google";

const STATE_KEY = "pet-room.oauth-state";

const CLIENT_IDS: Record<Provider, string | undefined> = {
  kakao: process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID,
  google: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID,
};

export const isEnabled = (p: Provider) => Boolean(CLIENT_IDS[p]);

export const redirectUri = () => `${window.location.origin}/oauth/callback`;

/** 제공자 로그인 화면으로 이동합니다. state로 요청을 위조하지 못하게 확인합니다. */
export function startLogin(provider: Provider) {
  const clientId = CLIENT_IDS[provider];
  if (!clientId) return;
  const nonce = crypto.randomUUID();
  const state = `${provider}.${nonce}`;
  try {
    sessionStorage.setItem(STATE_KEY, state);
  } catch {
    /* 저장소를 쓸 수 없으면 콜백에서 실패 처리됩니다 */
  }
  const params = new URLSearchParams({ client_id: clientId, redirect_uri: redirectUri(), response_type: "code", state });
  const url =
    provider === "kakao"
      ? `https://kauth.kakao.com/oauth/authorize?${params}`
      : `https://accounts.google.com/o/oauth2/v2/auth?${params}&scope=${encodeURIComponent("openid email profile")}`;
  window.location.assign(url);
}

/** 콜백 주소의 state가 이 브라우저에서 시작한 로그인과 같은지 확인하고 제공자를 돌려줍니다. */
export function consumeState(state: string | null): Provider | null {
  let saved: string | null = null;
  try {
    saved = sessionStorage.getItem(STATE_KEY);
    sessionStorage.removeItem(STATE_KEY);
  } catch {
    return null;
  }
  if (!state || state !== saved) return null;
  const provider = state.split(".")[0];
  return provider === "kakao" || provider === "google" ? provider : null;
}

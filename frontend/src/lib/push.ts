import { api, ApiError } from "./api";

export type PushState = "unsupported" | "denied" | "off" | "on";

function urlBase64ToUint8Array(base64: string): Uint8Array<ArrayBuffer> {
  const padded = (base64 + "=".repeat((4 - (base64.length % 4)) % 4)).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(padded);
  const out = new Uint8Array(new ArrayBuffer(raw.length));
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
  return out;
}

export function pushSupported(): boolean {
  return typeof window !== "undefined" && "serviceWorker" in navigator && "PushManager" in window && "Notification" in window;
}

async function registration(): Promise<ServiceWorkerRegistration> {
  return navigator.serviceWorker.register("/sw.js", { scope: "/", updateViaCache: "none" });
}

export async function pushState(): Promise<PushState> {
  if (!pushSupported()) return "unsupported";
  if (Notification.permission === "denied") return "denied";
  const reg = await navigator.serviceWorker.getRegistration("/");
  const sub = await reg?.pushManager.getSubscription();
  return sub && Notification.permission === "granted" ? "on" : "off";
}

/** 알림 켜기: 권한 요청 → 브라우저 구독 → 서버에 저장 */
export async function enablePush(): Promise<PushState> {
  let publicKey: string;
  try {
    publicKey = (await api<{ publicKey: string }>("/api/push/public-key")).publicKey;
  } catch (e) {
    if (e instanceof ApiError && e.code === "PUSH_DISABLED") {
      throw new ApiError(404, "PUSH_DISABLED", "아직 서버에 알림 기능이 설정되지 않았어요.");
    }
    throw e;
  }
  const permission = await Notification.requestPermission();
  if (permission !== "granted") return permission === "denied" ? "denied" : "off";
  const reg = await registration();
  await navigator.serviceWorker.ready;
  const sub =
    (await reg.pushManager.getSubscription()) ??
    (await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(publicKey) }));
  await api("/api/push-subscriptions", { method: "POST", body: JSON.stringify(sub.toJSON()) });
  return "on";
}

export async function disablePush(): Promise<PushState> {
  const reg = await navigator.serviceWorker.getRegistration("/");
  const sub = await reg?.pushManager.getSubscription();
  if (sub) {
    await api("/api/push-subscriptions", { method: "DELETE", body: JSON.stringify({ endpoint: sub.endpoint }) }).catch(
      () => undefined,
    );
    await sub.unsubscribe();
  }
  return "off";
}

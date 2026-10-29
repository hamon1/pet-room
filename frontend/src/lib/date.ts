import { useSyncExternalStore } from "react";

/** 기기 시간대 기준 오늘 (YYYY-MM-DD). */
export function todayLocal(): string {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

const noop = () => () => {};

/** 빌드(사전 렌더링) 중에는 빈 문자열, 브라우저에서는 오늘 날짜를 돌려줍니다. */
export function useToday(): string {
  return useSyncExternalStore(noop, todayLocal, () => "");
}

export function shortDate(iso: string): string {
  const [, m, d] = iso.split("-");
  return `${Number(m)}/${Number(d)}`;
}

export function dDay(iso: string, today: string): string {
  if (!today) return "";
  const diff = Math.round((Date.parse(iso) - Date.parse(today)) / 86400000);
  if (diff === 0) return "D-day";
  return diff > 0 ? `D-${diff}` : `D+${-diff}`;
}

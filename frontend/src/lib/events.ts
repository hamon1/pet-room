import type { HealthRecord, MedicalRecord, SpecialRecord } from "./records";

export type EventType = "MEDICAL" | "TREATMENT" | "EXPIRY" | "REBUY";

export interface PetEvent {
  type: EventType;
  date: string;
  daysUntil: number;
  alert: boolean;
  title: string;
  petIds: number[];
  medicalRecordId: number | null;
  foodId: number | null;
}

export interface CalendarDay {
  date: string;
  eventTypes: EventType[];
  recordCount: number;
}

export interface CalendarMonth {
  month: string;
  days: CalendarDay[];
}

export interface CalendarDayDetail {
  date: string;
  events: PetEvent[];
  health: HealthRecord[];
  medical: MedicalRecord[];
  special: SpecialRecord[];
}

export const EVENT_LABEL: Record<EventType, string> = {
  MEDICAL: "의료 일정",
  TREATMENT: "치료 정보",
  EXPIRY: "유통기한 임박",
  REBUY: "재구매 필요",
};

export const EVENT_CLASS: Record<EventType, string> = {
  MEDICAL: "med",
  TREATMENT: "treat",
  EXPIRY: "exp",
  REBUY: "rebuy",
};

/** 강조 우선순위: 한 날에 여러 이벤트가 있으면 앞의 색으로 칸을 칠합니다. */
export const EVENT_ORDER: EventType[] = ["MEDICAL", "TREATMENT", "EXPIRY", "REBUY"];

export function eventLink(e: PetEvent): string {
  if (e.foodId !== null) return "/food";
  if (e.petIds.length === 1) return `/pets/${e.petIds[0]}`;
  return "/";
}

export function daysText(days: number): string {
  if (days === 0) return "오늘";
  return days > 0 ? `D-${days}` : `${-days}일 지남`;
}

export type Condition = "NORMAL" | "LOW" | "ABNORMAL";
export type FeedStatus = "DONE" | "SKIPPED";
export type MedicalKind = "VISIT" | "VACCINATION" | "MEDICATION" | "TREATMENT";

export interface HealthRecord {
  petId: number;
  date: string;
  weightKg: number | null;
  meal: Condition | null;
  stool: Condition | null;
  fed: FeedStatus | null;
  memo: string | null;
}

export interface MedicalRecord {
  id: number;
  petId: number;
  kind: MedicalKind;
  recordDate: string;
  hospital: string | null;
  content: string;
  nextDate: string | null;
}

export interface SpecialRecord {
  id: number;
  item: string;
  label: string;
  recordDate: string;
  value: string | null;
}

export interface SpecialItem {
  item: string;
  label: string;
  userDefined: boolean;
  latest: SpecialRecord | null;
}

export interface TankFish {
  id: number;
  name: string;
  species: string | null;
}

export const CONDITION_LABEL: Record<Condition, string> = { NORMAL: "정상", LOW: "적음", ABNORMAL: "이상" };
export const FEED_LABEL: Record<FeedStatus, string> = { DONE: "완료", SKIPPED: "안 함" };
export const MEDICAL_LABEL: Record<MedicalKind, string> = {
  VISIT: "진료",
  VACCINATION: "접종",
  MEDICATION: "투약",
  TREATMENT: "치료",
};
export const CONDITIONS: Condition[] = ["NORMAL", "LOW", "ABNORMAL"];
export const FEEDS: FeedStatus[] = ["DONE", "SKIPPED"];
export const MEDICAL_KINDS: MedicalKind[] = ["VISIT", "VACCINATION", "MEDICATION", "TREATMENT"];

/** 투약·치료는 "치료 정보", 진료·접종은 "의료 일정"으로 표시합니다. */
export const isTreatment = (k: MedicalKind) => k === "MEDICATION" || k === "TREATMENT";

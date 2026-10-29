export type FoodType = "FEED" | "TREAT";

export interface Food {
  id: number;
  name: string;
  type: FoodType;
  expiryDate: string;
  openedDate: string | null;
  totalG: number | null;
  remainingG: number | null;
  perServingG: number | null;
  timesPerDay: number | null;
  petIds: number[];
  dailyUseG: number | null;
  runoutDate: string | null;
}

export interface FoodRequest {
  name: string;
  type: FoodType;
  expiryDate: string;
  openedDate: string | null;
  totalG: number | null;
  remainingG: number | null;
  perServingG: number | null;
  timesPerDay: number | null;
  petIds: number[];
}

export const FOOD_TYPE_LABEL: Record<FoodType, string> = { FEED: "사료", TREAT: "간식" };

export function grams(v: number | null): string {
  if (v === null) return "-";
  return v >= 1000 ? `${(v / 1000).toFixed(v % 1000 === 0 ? 0 : 1)}kg` : `${v}g`;
}

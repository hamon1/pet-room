export type PetCategory = "MAMMAL" | "BIRD" | "FISH";
export type PetSubCategory = "DOG" | "CAT" | "FINCH" | "PARROT";
export type ManagementType = "INDIVIDUAL" | "TANK";

export interface Pet {
  id: number;
  name: string;
  category: PetCategory;
  subCategory: PetSubCategory | null;
  managementType: ManagementType;
  species: string | null;
  birthDate: string | null;
  birthDateEstimated: boolean;
  traits: string | null;
  createdAt: string;
}

export interface PetRequest {
  name: string;
  category: PetCategory;
  subCategory: PetSubCategory | null;
  managementType: ManagementType;
  species: string | null;
  birthDate: string | null;
  ageYears: number | null;
  traits: string | null;
}

export const CATEGORY_LABEL: Record<PetCategory, string> = { MAMMAL: "포유류", BIRD: "조류", FISH: "어류" };
export const SUB_LABEL: Record<PetSubCategory, string> = { DOG: "강아지", CAT: "고양이", FINCH: "핀치류", PARROT: "앵무류" };
export const SUBS: Record<Exclude<PetCategory, "FISH">, PetSubCategory[]> = {
  MAMMAL: ["DOG", "CAT"],
  BIRD: ["FINCH", "PARROT"],
};

export function kindLabel(p: Pet): string {
  if (p.category === "FISH") return p.managementType === "TANK" ? "어항" : "어류";
  return p.subCategory ? SUB_LABEL[p.subCategory] : CATEGORY_LABEL[p.category];
}

export function ageLabel(p: Pet, today = new Date()): string {
  if (!p.birthDate) return "";
  const born = new Date(`${p.birthDate}T00:00:00`);
  const months = (today.getFullYear() - born.getFullYear()) * 12 + (today.getMonth() - born.getMonth());
  const text = months >= 12 ? `${Math.floor(months / 12)}살` : `${Math.max(months, 0)}개월`;
  return p.birthDateEstimated ? `약 ${text}` : text;
}

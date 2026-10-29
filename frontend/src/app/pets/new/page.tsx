"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, errorMessage } from "@/lib/api";
import {
  CATEGORY_LABEL,
  SUB_LABEL,
  SUBS,
  type ManagementType,
  type Pet,
  type PetCategory,
  type PetRequest,
  type PetSubCategory,
} from "@/lib/pets";
import PetSprite from "@/components/PetSprite";

const CATEGORIES: PetCategory[] = ["MAMMAL", "BIRD", "FISH"];

export default function NewPetPage() {
  const router = useRouter();
  const [category, setCategory] = useState<PetCategory>("MAMMAL");
  const [subCategory, setSubCategory] = useState<PetSubCategory | null>("DOG");
  const [management, setManagement] = useState<ManagementType>("INDIVIDUAL");
  const [ageMode, setAgeMode] = useState<"birth" | "age">("birth");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const isTank = category === "FISH" && management === "TANK";

  function pickCategory(c: PetCategory) {
    setCategory(c);
    setSubCategory(c === "FISH" ? null : SUBS[c][0]);
    setManagement("INDIVIDUAL");
  }

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const text = (k: string) => {
      const v = String(f.get(k) ?? "").trim();
      return v === "" ? null : v;
    };
    const age = text("ageYears");
    const body: PetRequest = {
      name: text("name") ?? "",
      category,
      subCategory: category === "FISH" ? null : subCategory,
      managementType: management,
      species: text("species"),
      birthDate: ageMode === "birth" ? text("birthDate") : null,
      ageYears: ageMode === "age" && age !== null ? Number(age) : null,
      traits: text("traits"),
    };
    setBusy(true);
    setError(null);
    try {
      await api<Pet>("/api/pets", { method: "POST", body: JSON.stringify(body) });
      router.replace("/pets");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} noValidate style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <Link className="link" href="/pets">
        ‹ 목록
      </Link>
      <h1>반려동물 등록</h1>

      <h2>분류 선택</h2>
      <div className="grid-3">
        {CATEGORIES.map((c) => (
          <button
            key={c}
            type="button"
            className="card pick"
            aria-pressed={category === c}
            onClick={() => pickCategory(c)}
          >
            <PetSprite category={c} subCategory={c === "FISH" ? null : SUBS[c][0]} size={40} />
            {CATEGORY_LABEL[c]}
          </button>
        ))}
      </div>

      <div className="field">
        <span className="label">{category === "FISH" ? "관리 방식" : "세부 분류"}</span>
        <div className="seg">
          {category === "FISH"
            ? (["INDIVIDUAL", "TANK"] as ManagementType[]).map((m) => (
                <button key={m} type="button" aria-pressed={management === m} onClick={() => setManagement(m)}>
                  {m === "TANK" ? "어항" : "개체"}
                </button>
              ))
            : SUBS[category].map((s) => (
                <button key={s} type="button" aria-pressed={subCategory === s} onClick={() => setSubCategory(s)}>
                  {SUB_LABEL[s]}
                </button>
              ))}
        </div>
      </div>

      <div className="card">
        <div className="field">
          <label htmlFor="name">이름</label>
          <input id="name" name="name" maxLength={30} placeholder={isTank ? "예) 거실 어항" : "이름 입력"} />
        </div>
        <div className="field">
          <div className="label">
            {isTank ? "설치일 또는 기간" : "출생일 또는 나이"}
            <span className="seg">
              <button type="button" aria-pressed={ageMode === "birth"} onClick={() => setAgeMode("birth")}>
                {isTank ? "날짜" : "출생일"}
              </button>
              <button type="button" aria-pressed={ageMode === "age"} onClick={() => setAgeMode("age")}>
                {isTank ? "기간(년)" : "나이"}
              </button>
            </span>
          </div>
          {ageMode === "birth" ? (
            <input
              id="birthDate"
              name="birthDate"
              type="date"
              aria-label="출생일"
              onFocus={(e) => {
                // 오늘 이후 날짜를 고르지 못하게 함 (서버에서도 한 번 더 검증)
                const now = new Date();
                e.currentTarget.max = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
              }}
            />
          ) : (
            <input id="ageYears" name="ageYears" type="number" min={0} max={100} step={0.5} placeholder="예) 3" aria-label="나이" />
          )}
        </div>
        <div className="field">
          <label htmlFor="species">상세 종명</label>
          <input
            id="species"
            name="species"
            maxLength={30}
            placeholder={category === "MAMMAL" ? "예) 말티즈, 코리안숏헤어" : category === "BIRD" ? "예) 금화조, 사랑앵무" : "예) 구피, 베타"}
          />
        </div>
        <div className="field">
          <label htmlFor="traits">
            특징 <span className="optional">선택</span>
          </label>
          <textarea id="traits" name="traits" maxLength={500} placeholder="성격, 습관, 주의할 점 등" />
        </div>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      <button className="btn primary block" type="submit" disabled={busy}>
        등록하기
      </button>
    </form>
  );
}

"use client";

import { useState, type FormEvent } from "react";
import { api, errorMessage } from "@/lib/api";
import { FOOD_TYPE_LABEL, type Food, type FoodRequest, type FoodType } from "@/lib/food";
import type { Pet } from "@/lib/pets";
import Seg from "./Seg";

const num = (v: string) => (v.trim() === "" ? null : Number(v));
const str = (v: number | null) => (v === null ? "" : String(v));

/** 먹이 등록·수정. 필수는 제품명과 유통기한뿐입니다. */
export default function FoodSheet({
  food,
  pets,
  onClose,
  onSaved,
}: {
  food: Food | null;
  pets: Pet[];
  onClose: () => void;
  onSaved: (msg: string) => void;
}) {
  const [name, setName] = useState(food?.name ?? "");
  const [type, setType] = useState<FoodType>(food?.type ?? "FEED");
  const [expiryDate, setExpiry] = useState(food?.expiryDate ?? "");
  const [openedDate, setOpened] = useState(food?.openedDate ?? "");
  const [remaining, setRemaining] = useState(str(food?.remainingG ?? null));
  const [total, setTotal] = useState(str(food?.totalG ?? null));
  const [perServing, setPerServing] = useState(str(food?.perServingG ?? null));
  const [times, setTimes] = useState(str(food?.timesPerDay ?? null));
  const [petIds, setPetIds] = useState<number[]>(food?.petIds ?? []);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const toggle = (id: number) => setPetIds((ids) => (ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]));

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const body: FoodRequest = {
      name,
      type,
      expiryDate,
      openedDate: openedDate || null,
      remainingG: num(remaining),
      totalG: num(total),
      perServingG: num(perServing),
      timesPerDay: num(times),
      petIds,
    };
    try {
      await api(food ? `/api/foods/${food.id}` : "/api/foods", {
        method: food ? "PUT" : "POST",
        body: JSON.stringify(body),
      });
      onSaved(food ? "먹이 정보를 고쳤어요" : `${name}을(를) 등록했어요`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!food) return;
    setBusy(true);
    try {
      await api(`/api/foods/${food.id}`, { method: "DELETE" });
      onSaved("먹이를 삭제했어요");
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  return (
    <div className="sheet-backdrop" onClick={onClose}>
      <form
        className="sheet"
        role="dialog"
        aria-modal="true"
        aria-label={food ? "먹이 수정" : "먹이 등록"}
        onClick={(e) => e.stopPropagation()}
        onSubmit={submit}
      >
        <div className="row between">
          <h2>{food ? "먹이 수정" : "먹이 등록"}</h2>
          <button type="button" className="btn sm" onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </div>
        <Seg options={["FEED", "TREAT"] as const} value={type} onChange={setType} labels={FOOD_TYPE_LABEL} ariaLabel="종류" />
        <div className="field">
          <label htmlFor="f-name">제품명</label>
          <input id="f-name" value={name} onChange={(e) => setName(e.target.value)} maxLength={50} required />
        </div>
        <div className="grid-2">
          <div className="field">
            <label htmlFor="f-exp">유통기한</label>
            <input id="f-exp" type="date" value={expiryDate} onChange={(e) => setExpiry(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="f-open">
              개봉일 <span className="optional">선택</span>
            </label>
            <input id="f-open" type="date" value={openedDate} onChange={(e) => setOpened(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="f-rem">
              남은 양 (g) <span className="optional">선택</span>
            </label>
            <input id="f-rem" type="number" inputMode="decimal" min="0" step="0.1" value={remaining} onChange={(e) => setRemaining(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="f-total">
              전체 양 (g) <span className="optional">선택</span>
            </label>
            <input id="f-total" type="number" inputMode="decimal" min="0" step="0.1" value={total} onChange={(e) => setTotal(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="f-per">
              1회 급여량 (g) <span className="optional">선택</span>
            </label>
            <input id="f-per" type="number" inputMode="decimal" min="0.1" step="0.1" value={perServing} onChange={(e) => setPerServing(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="f-times">
              하루 횟수 <span className="optional">선택</span>
            </label>
            <input id="f-times" type="number" inputMode="numeric" min="1" max="24" value={times} onChange={(e) => setTimes(e.target.value)} />
          </div>
        </div>
        {pets.length > 0 && (
          <div className="field">
            <span className="label">
              먹는 반려동물 <span className="optional">여러 마리 선택 가능</span>
            </span>
            <div className="seg" role="group" aria-label="먹는 반려동물">
              {pets.map((p) => (
                <button key={p.id} type="button" aria-pressed={petIds.includes(p.id)} onClick={() => toggle(p.id)}>
                  {p.name}
                </button>
              ))}
            </div>
          </div>
        )}
        <p className="muted small" style={{ margin: 0 }}>
          남은 양·1회 급여량·하루 횟수를 넣으면 소진 예상일을 계산해 재구매 알림을 드려요.
        </p>
        {error && <p className="error" role="alert">{error}</p>}
        <div className="row">
          {food && (
            <button type="button" className="btn" style={{ flex: 1 }} onClick={remove} disabled={busy}>
              삭제
            </button>
          )}
          <button type="submit" className="btn primary" style={{ flex: 1 }} disabled={busy}>
            저장
          </button>
        </div>
      </form>
    </div>
  );
}

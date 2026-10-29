"use client";

import { useState } from "react";
import BackLink from "@/components/BackLink";
import FoodSheet from "@/components/FoodSheet";
import { api, errorMessage } from "@/lib/api";
import { dDay, shortDate, useToday } from "@/lib/date";
import { FOOD_TYPE_LABEL, grams, type Food } from "@/lib/food";
import type { Pet } from "@/lib/pets";
import { useAuthed } from "@/lib/useAuthed";

export default function FoodPage() {
  const today = useToday();
  const [version, setVersion] = useState(0);
  const foods = useAuthed<Food[]>("/api/foods", version);
  const pets = useAuthed<Pet[]>("/api/pets");
  const [editing, setEditing] = useState<Food | "new" | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const petName = (id: number) => pets.data?.find((p) => p.id === id)?.name;

  async function feed(f: Food) {
    setError(null);
    try {
      const updated = await api<Food>(`/api/foods/${f.id}/feed`, { method: "POST" });
      foods.setData((list) => list?.map((x) => (x.id === f.id ? updated : x)) ?? null);
      setNotice(`${f.name} 1회 급여 · 남은 양 ${grams(updated.remainingG)}`);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <BackLink />
      <div className="row between">
        <h1>먹이</h1>
        <button className="btn primary sm" onClick={() => setEditing("new")}>
          + 등록
        </button>
      </div>
      {(foods.error ?? error) && <p className="error" role="alert">{foods.error ?? error}</p>}
      {notice && <p className="notice" role="status">{notice}</p>}
      {!foods.data && !foods.error && <p className="muted">불러오는 중…</p>}
      {foods.data?.length === 0 && (
        <div className="card" style={{ alignItems: "center", textAlign: "center" }}>
          <p className="muted" style={{ margin: 0 }}>등록된 사료·간식이 없어요</p>
          <button className="btn primary" onClick={() => setEditing("new")}>
            첫 먹이 등록하기
          </button>
        </div>
      )}
      {foods.data?.map((f) => {
        const ratio = f.remainingG !== null && f.totalG ? Math.min(1, f.remainingG / f.totalG) : null;
        const eaters = f.petIds.map(petName).filter(Boolean).join(", ");
        return (
          <div key={f.id} className="card">
            <div className="row between">
              <h2>{f.name}</h2>
              <span className="chip">{FOOD_TYPE_LABEL[f.type]}</span>
            </div>
            {ratio !== null && (
              <div className="meter" role="img" aria-label={`남은 양 ${Math.round(ratio * 100)}%`}>
                <i style={{ width: `${ratio * 100}%` }} />
              </div>
            )}
            <div className="row wrap small">
              <span>
                남은 양 <strong>{grams(f.remainingG)}</strong>
                {f.totalG !== null && <span className="muted"> / {grams(f.totalG)}</span>}
              </span>
              <span className="muted">·</span>
              <span>
                유통기한 {shortDate(f.expiryDate)} <span className="muted">{dDay(f.expiryDate, today)}</span>
              </span>
            </div>
            <div className="row wrap small muted">
              {f.runoutDate ? (
                <span>
                  하루 {grams(f.dailyUseG)} · 소진 예상 {shortDate(f.runoutDate)} ({dDay(f.runoutDate, today)})
                </span>
              ) : (
                <span>급여량을 넣으면 소진 예상일을 계산해요</span>
              )}
              {eaters && <span>· {eaters}</span>}
            </div>
            <div className="row">
              <button className="btn sm" style={{ flex: 1 }} onClick={() => setEditing(f)}>
                수정
              </button>
              <button className="btn sm primary" style={{ flex: 1 }} onClick={() => feed(f)} disabled={!f.perServingG || f.remainingG === null}>
                1회 급여
              </button>
            </div>
          </div>
        );
      })}

      {editing && (
        <FoodSheet
          food={editing === "new" ? null : editing}
          pets={pets.data ?? []}
          onClose={() => setEditing(null)}
          onSaved={(msg) => {
            setEditing(null);
            setNotice(msg);
            setVersion((v) => v + 1);
          }}
        />
      )}
    </>
  );
}

"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import type { TankFish } from "@/lib/records";
import PetSprite from "./PetSprite";

export default function FishSection({ petId }: { petId: number }) {
  const [fish, setFish] = useState<TankFish[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    () => api<TankFish[]>(`/api/pets/${petId}/fish`).then(setFish).catch((e) => setError(errorMessage(e))),
    [petId],
  );
  useEffect(() => {
    load();
  }, [load]);

  async function add(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const name = String(new FormData(form).get("name") ?? "").trim();
    if (!name) return;
    setError(null);
    try {
      await api(`/api/pets/${petId}/fish`, { method: "POST", body: JSON.stringify({ name }) });
      form.reset();
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(id: number) {
    try {
      await api(`/api/pets/${petId}/fish/${id}`, { method: "DELETE" });
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="card">
      <h2>어항 속 물고기 {fish && <span className="muted small">{fish.length}마리</span>}</h2>
      {fish?.length === 0 && <span className="muted">아직 물고기가 없어요</span>}
      {fish?.map((f) => (
        <div key={f.id} className="list-item">
          <PetSprite category="FISH" subCategory={null} size={26} />
          <span className="grow">
            {f.name}
            {f.species && <span className="muted small"> · {f.species}</span>}
          </span>
          <button className="btn sm" onClick={() => remove(f.id)} aria-label={`${f.name} 빼기`}>
            빼기
          </button>
        </div>
      ))}
      <form className="row" onSubmit={add} noValidate>
        <div className="field" style={{ flex: 1 }}>
          <input name="name" maxLength={30} placeholder="물고기 이름" aria-label="물고기 이름" />
        </div>
        <button className="btn sm" type="submit">
          추가
        </button>
      </form>
      {error && <p className="error" role="alert">{error}</p>}
    </div>
  );
}

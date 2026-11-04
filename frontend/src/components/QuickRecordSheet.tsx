"use client";

import { useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { shortDate, todayLocal } from "@/lib/date";
import type { Pet } from "@/lib/pets";
import { CONDITION_LABEL, CONDITIONS, FEED_LABEL, FEEDS, type Condition, type FeedStatus } from "@/lib/records";
import Seg from "./Seg";

interface Entry {
  meal: Condition;
  stool: Condition;
  fed: FeedStatus;
}

const NORMAL: Entry = { meal: "NORMAL", stool: "NORMAL", fed: "DONE" };

/** 빠른 기록·일괄 기록: 오늘 모든 반려동물의 식사·배변(어항은 먹이 급여)을 한 번에 저장합니다. */
export default function QuickRecordSheet({ pets, onClose, onSaved }: { pets: Pet[]; onClose: () => void; onSaved: (msg: string) => void }) {
  const [entries, setEntries] = useState<Record<number, Entry>>(() =>
    Object.fromEntries(pets.map((p) => [p.id, { ...NORMAL }])),
  );
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [date] = useState(todayLocal);

  const update = (id: number, patch: Partial<Entry>) => setEntries((e) => ({ ...e, [id]: { ...e[id], ...patch } }));

  async function save(all: Record<number, Entry>) {
    setBusy(true);
    setError(null);
    try {
      await api("/api/health-records/batch", {
        method: "POST",
        body: JSON.stringify({
          date,
          items: pets.map((p) =>
            p.managementType === "TANK"
              ? { petId: p.id, fed: all[p.id].fed }
              : { petId: p.id, meal: all[p.id].meal, stool: all[p.id].stool },
          ),
        }),
      });
      onSaved(`${pets.length}마리 오늘 기록 완료`);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="sheet-backdrop" onClick={onClose}>
      <div className="sheet" role="dialog" aria-modal="true" aria-label="빠른 기록" onClick={(e) => e.stopPropagation()}>
        <div className="row between">
          <h2>빠른 기록 · {shortDate(date)}</h2>
          <button className="btn sm" onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </div>
        {pets.map((p) => (
          <div key={p.id} className="card">
            <strong>{p.name}</strong>
            {p.managementType === "TANK" ? (
              <div className="field">
                <span className="label">먹이 급여</span>
                <Seg ariaLabel={`${p.name} 먹이 급여`} options={FEEDS} labels={FEED_LABEL} value={entries[p.id].fed} onChange={(fed) => update(p.id, { fed })} />
              </div>
            ) : (
              <>
                <div className="field">
                  <span className="label">식사</span>
                  <Seg ariaLabel={`${p.name} 식사`} options={CONDITIONS} labels={CONDITION_LABEL} value={entries[p.id].meal} onChange={(meal) => update(p.id, { meal })} />
                </div>
                <div className="field">
                  <span className="label">배변</span>
                  <Seg ariaLabel={`${p.name} 배변`} options={CONDITIONS} labels={CONDITION_LABEL} value={entries[p.id].stool} onChange={(stool) => update(p.id, { stool })} />
                </div>
              </>
            )}
          </div>
        ))}
        {error && <p className="error" role="alert">{error}</p>}
        <div className="row">
          <button
            className="btn"
            style={{ flex: 1 }}
            disabled={busy}
            onClick={() => save(Object.fromEntries(pets.map((p) => [p.id, { ...NORMAL }])))}
          >
            모두 정상으로 기록
          </button>
          <button className="btn primary" style={{ flex: 1 }} disabled={busy} onClick={() => save(entries)}>
            저장
          </button>
        </div>
      </div>
    </div>
  );
}

"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { shortDate, useToday } from "@/lib/date";
import type { SpecialItem } from "@/lib/records";

const PLACEHOLDER: Record<string, string> = {
  WATER_QUALITY: "예) pH 7.0",
  WATER_TEMP: "예) 25℃",
  WALK: "예) 30분",
  RESPIRATORY: "예) 이상 없음",
  BEAK: "예) 길이 정상",
};

export default function SpecialTab({ petId }: { petId: number }) {
  const today = useToday();
  const [items, setItems] = useState<SpecialItem[] | null>(null);
  const [open, setOpen] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    () => api<SpecialItem[]>(`/api/pets/${petId}/special-items`).then(setItems).catch((e) => setError(errorMessage(e))),
    [petId],
  );
  useEffect(() => {
    load();
  }, [load]);

  async function onSubmit(e: FormEvent<HTMLFormElement>, item: string) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setError(null);
    try {
      await api(`/api/pets/${petId}/special-records`, {
        method: "POST",
        body: JSON.stringify({
          item,
          recordDate: String(f.get("recordDate") || today),
          value: String(f.get("value") ?? "").trim() || null,
        }),
      });
      setOpen(null);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <>
      <div className="card">
        <h2>특수 항목</h2>
        {!items && <span className="muted">불러오는 중…</span>}
        {items?.map((it, i) => (
          <div key={it.item} style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {i > 0 && <div className="divider" />}
            <div className="list-item">
              <div className="grow">
                <div className="row">
                  <strong>{it.label}</strong>
                  {it.userDefined && <span className="chip">기본</span>}
                </div>
                <span className="muted small">
                  {it.latest ? `최근 ${shortDate(it.latest.recordDate)}${it.latest.value ? ` · ${it.latest.value}` : ""}` : "기록 없음"}
                </span>
              </div>
              <button className="btn sm" aria-expanded={open === it.item} onClick={() => setOpen(open === it.item ? null : it.item)}>
                기록
              </button>
            </div>
            {open === it.item && (
              <form className="row" onSubmit={(e) => onSubmit(e, it.item)} noValidate style={{ alignItems: "flex-end" }}>
                <div className="field" style={{ flex: "0 0 140px" }}>
                  <label htmlFor={`d-${it.item}`}>날짜</label>
                  <input id={`d-${it.item}`} name="recordDate" type="date" defaultValue={today} max={today || undefined} />
                </div>
                <div className="field" style={{ flex: 1, minWidth: 0 }}>
                  <label htmlFor={`v-${it.item}`}>
                    내용 <span className="optional">선택</span>
                  </label>
                  <input id={`v-${it.item}`} name="value" maxLength={100} placeholder={PLACEHOLDER[it.item] ?? "비우면 완료로 기록"} />
                </div>
                <button className="btn primary sm" type="submit">
                  저장
                </button>
              </form>
            )}
          </div>
        ))}
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <p className="muted small" style={{ margin: 0 }}>
        기본 = 사용자가 지정한 항목. 나머지는 출시 전 수의사·사육 커뮤니티 검증이 필요한 제안 항목입니다.
      </p>
    </>
  );
}

"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { shortDate, useToday } from "@/lib/date";
import type { Pet } from "@/lib/pets";
import { CONDITION_LABEL, CONDITIONS, FEED_LABEL, FEEDS, type Condition, type FeedStatus, type HealthRecord } from "@/lib/records";
import Seg from "./Seg";

export default function HealthTab({ pet }: { pet: Pet }) {
  const tank = pet.managementType === "TANK";
  const today = useToday();
  const [records, setRecords] = useState<HealthRecord[] | null>(null);
  const [meal, setMeal] = useState<Condition | null>("NORMAL");
  const [stool, setStool] = useState<Condition | null>("NORMAL");
  const [fed, setFed] = useState<FeedStatus | null>("DONE");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const load = useCallback(
    () => api<HealthRecord[]>(`/api/pets/${pet.id}/health-records`).then(setRecords).catch((e) => setError(errorMessage(e))),
    [pet.id],
  );
  useEffect(() => {
    load();
  }, [load]);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const f = new FormData(form);
    const date = String(f.get("date") || today);
    const weight = String(f.get("weightKg") ?? "").trim();
    const memo = String(f.get("memo") ?? "").trim();
    const body = tank
      ? { fed, memo: memo || null }
      : { weightKg: weight ? Number(weight) : null, meal, stool, memo: memo || null };
    setError(null);
    try {
      await api(`/api/pets/${pet.id}/health-records/${date}`, { method: "PUT", body: JSON.stringify(body) });
      setSaved(`${shortDate(date)} 기록을 저장했어요. 같은 날짜에 다시 저장하면 덮어씁니다.`);
      form.reset();
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <>
      <form className="card" onSubmit={onSubmit} noValidate>
        <h2>{tank ? "급여 기록" : "건강 기록"}</h2>
        <div className="field">
          <label htmlFor="date">날짜</label>
          <input id="date" name="date" type="date" defaultValue={today} max={today || undefined} key={today} />
        </div>
        {tank ? (
          <div className="field">
            <span className="label">먹이 급여</span>
            <Seg ariaLabel="먹이 급여" options={FEEDS} labels={FEED_LABEL} value={fed} onChange={setFed} />
          </div>
        ) : (
          <>
            <div className="field">
              <label htmlFor="weightKg">
                체중 (kg) <span className="optional">선택</span>
              </label>
              <input id="weightKg" name="weightKg" type="number" min={0} step={0.001} inputMode="decimal" placeholder="숫자 입력" />
            </div>
            <div className="field">
              <span className="label">식사</span>
              <Seg ariaLabel="식사" options={CONDITIONS} labels={CONDITION_LABEL} value={meal} onChange={setMeal} />
            </div>
            <div className="field">
              <span className="label">배변</span>
              <Seg ariaLabel="배변" options={CONDITIONS} labels={CONDITION_LABEL} value={stool} onChange={setStool} />
            </div>
          </>
        )}
        <div className="field">
          <label htmlFor="memo">
            메모 <span className="optional">선택</span>
          </label>
          <textarea id="memo" name="memo" maxLength={500} placeholder="특이사항" />
        </div>
        {error && <p className="error" role="alert">{error}</p>}
        {saved && <p className="notice" role="status">{saved}</p>}
        <button className="btn primary block" type="submit">
          저장
        </button>
      </form>

      <div className="card">
        <h2>최근 30일</h2>
        {!records && <span className="muted">불러오는 중…</span>}
        {records && records.length === 0 && <span className="muted">아직 기록이 없어요</span>}
        {records?.map((r, i) => (
          <div key={r.date}>
            {i > 0 && <div className="divider" />}
            <div className="list-item" style={{ paddingTop: i > 0 ? 10 : 0 }}>
              <span className="date">{shortDate(r.date)}</span>
              <span className="grow">
                {tank
                  ? `먹이 급여 ${r.fed ? FEED_LABEL[r.fed] : "-"}`
                  : `식사 ${r.meal ? CONDITION_LABEL[r.meal] : "-"} · 배변 ${r.stool ? CONDITION_LABEL[r.stool] : "-"}${r.weightKg ? ` · ${r.weightKg}kg` : ""}`}
                {r.memo && (
                  <>
                    <br />
                    <span className="muted small">{r.memo}</span>
                  </>
                )}
              </span>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}

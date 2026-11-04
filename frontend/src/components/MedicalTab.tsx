"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { dDay, shortDate, useToday } from "@/lib/date";
import { isTreatment, MEDICAL_KINDS, MEDICAL_LABEL, type MedicalKind, type MedicalRecord } from "@/lib/records";
import Seg from "./Seg";

export default function MedicalTab({ petId }: { petId: number }) {
  const today = useToday();
  const [records, setRecords] = useState<MedicalRecord[] | null>(null);
  const [kind, setKind] = useState<MedicalKind | null>("VISIT");
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  const load = useCallback(
    () => api<MedicalRecord[]>(`/api/pets/${petId}/medical-records`).then(setRecords).catch((e) => setError(errorMessage(e))),
    [petId],
  );
  useEffect(() => {
    load();
  }, [load]);

  const next = records
    ?.filter((r) => r.nextDate && today && r.nextDate >= today)
    .sort((a, b) => (a.nextDate! < b.nextDate! ? -1 : 1))[0];

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const text = (k: string) => String(f.get(k) ?? "").trim() || null;
    setError(null);
    try {
      await api(`/api/pets/${petId}/medical-records`, {
        method: "POST",
        body: JSON.stringify({
          kind,
          recordDate: text("recordDate") ?? today,
          hospital: text("hospital"),
          content: text("content") ?? "",
          nextDate: text("nextDate"),
        }),
      });
      setAdding(false);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(id: number) {
    try {
      await api(`/api/pets/${petId}/medical-records/${id}`, { method: "DELETE" });
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <>
      {next && (
        <div className="card">
          <div className="row">
            <span className={`chip ${isTreatment(next.kind) ? "treat" : "med"}`}>다음 일정</span>
            <strong className="grow">
              {shortDate(next.nextDate!)} {next.content}
            </strong>
            <span className="date">{dDay(next.nextDate!, today)}</span>
          </div>
        </div>
      )}

      <div className="card">
        <h2>의료 기록</h2>
        {!records && <span className="muted">불러오는 중…</span>}
        {records && records.length === 0 && <span className="muted">아직 의료 기록이 없어요</span>}
        {records?.map((r) => (
          <div key={r.id} className="list-item">
            <span className={`chip ${isTreatment(r.kind) ? "treat" : "med"}`}>{MEDICAL_LABEL[r.kind]}</span>
            <span className="grow">
              {r.content}
              <br />
              <span className="muted small">
                {shortDate(r.recordDate)}
                {r.hospital && ` · ${r.hospital}`}
                {r.nextDate && ` · 다음 ${shortDate(r.nextDate)}`}
              </span>
            </span>
            <button className="btn sm" onClick={() => remove(r.id)} aria-label={`${r.content} 삭제`}>
              삭제
            </button>
          </div>
        ))}
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      {adding ? (
        <form className="card" onSubmit={onSubmit} noValidate>
          <h2>의료 기록 추가</h2>
          <div className="field">
            <span className="label">종류</span>
            <Seg ariaLabel="종류" options={MEDICAL_KINDS} labels={MEDICAL_LABEL} value={kind} onChange={setKind} />
          </div>
          <div className="field">
            <label htmlFor="recordDate">날짜</label>
            <input id="recordDate" name="recordDate" type="date" defaultValue={today} max={today || undefined} key={today} />
          </div>
          <div className="field">
            <label htmlFor="content">내용</label>
            <input id="content" name="content" maxLength={100} placeholder="진료 내용, 백신·약 이름 등" />
          </div>
          <div className="field">
            <label htmlFor="hospital">
              병원명 <span className="optional">선택</span>
            </label>
            <input id="hospital" name="hospital" maxLength={50} />
          </div>
          <div className="field">
            <label htmlFor="nextDate">
              다음 예정일 <span className="optional">선택</span>
            </label>
            <input id="nextDate" name="nextDate" type="date" min={today || undefined} />
          </div>
          <p className="muted small" style={{ margin: 0 }}>
            다음 예정일은 달력의 주요 이벤트와 알림에 쓰입니다 (3단계).
          </p>
          <div className="row">
            <button type="button" className="btn" style={{ flex: 1 }} onClick={() => setAdding(false)}>
              취소
            </button>
            <button type="submit" className="btn primary" style={{ flex: 1 }}>
              저장
            </button>
          </div>
        </form>
      ) : (
        <button className="btn primary block" onClick={() => setAdding(true)}>
          + 의료 기록 추가
        </button>
      )}
    </>
  );
}

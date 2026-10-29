"use client";

import { useState, type CSSProperties } from "react";
import BackLink from "@/components/BackLink";
import EventRow from "@/components/EventRow";
import { shortDate, useToday } from "@/lib/date";
import { EVENT_CLASS, EVENT_LABEL, EVENT_ORDER, type CalendarDayDetail, type CalendarMonth } from "@/lib/events";
import type { Pet } from "@/lib/pets";
import { CONDITION_LABEL, FEED_LABEL, MEDICAL_LABEL } from "@/lib/records";
import { useAuthed } from "@/lib/useAuthed";

const DOW = ["일", "월", "화", "수", "목", "금", "토"];

function monthOf(today: string, offset: number): string {
  const [y, m] = today.split("-").map(Number);
  const d = new Date(y, m - 1 + offset, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

export default function CalendarPage() {
  const today = useToday();
  const [offset, setOffset] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const month = today ? monthOf(today, offset) : null;
  const cal = useAuthed<CalendarMonth>(month ? `/api/calendar?month=${month}` : null);
  const day = useAuthed<CalendarDayDetail>(selected ? `/api/calendar/days/${selected}` : null);
  const pets = useAuthed<Pet[]>("/api/pets");
  const petName = (id: number) => pets.data?.find((p) => p.id === id)?.name ?? "";

  if (!month) return <p className="muted">불러오는 중…</p>;

  const [y, m] = month.split("-").map(Number);
  const first = new Date(y, m - 1, 1).getDay();
  const last = new Date(y, m, 0).getDate();
  const byDate = new Map((cal.data?.month === month ? cal.data.days : []).map((d) => [d.date, d]));
  const detail = day.data?.date === selected ? day.data : null;

  return (
    <>
      <BackLink />
      <div className="cal-head">
        <button className="btn sm" onClick={() => setOffset(offset - 1)} aria-label="이전 달">
          ‹
        </button>
        <h1>
          {y}년 {m}월
        </h1>
        <button className="btn sm" onClick={() => setOffset(offset + 1)} aria-label="다음 달">
          ›
        </button>
      </div>
      {cal.error && <p className="error" role="alert">{cal.error}</p>}

      <div className="card">
        <div className="cal-grid">
          {DOW.map((d) => (
            <span key={d} className="dow">
              {d}
            </span>
          ))}
          {Array.from({ length: first }, (_, i) => (
            <span key={`b${i}`} className="day blank" />
          ))}
          {Array.from({ length: last }, (_, i) => {
            const date = `${month}-${String(i + 1).padStart(2, "0")}`;
            const info = byDate.get(date);
            const top = EVENT_ORDER.find((t) => info?.eventTypes.includes(t));
            const cls = ["day", top ? "ev" : "", date === today ? "today" : ""].join(" ");
            return (
              <button
                key={date}
                className={cls}
                style={top ? ({ "--c": `var(--${EVENT_CLASS[top]})` } as CSSProperties) : undefined}
                aria-pressed={selected === date}
                aria-label={`${m}월 ${i + 1}일${top ? `, ${info!.eventTypes.map((t) => EVENT_LABEL[t]).join(", ")}` : ""}${info?.recordCount ? `, 기록 ${info.recordCount}개` : ""}`}
                onClick={() => setSelected(date)}
              >
                {i + 1}
                <span className="dots">
                  {Array.from({ length: Math.min(info?.recordCount ?? 0, 3) }, (_, k) => (
                    <i key={k} />
                  ))}
                </span>
              </button>
            );
          })}
        </div>
        <div className="legend">
          {EVENT_ORDER.map((t) => (
            <span key={t} className={`chip ${EVENT_CLASS[t]}`}>
              {EVENT_LABEL[t]}
            </span>
          ))}
          <span className="chip muted">• 기록</span>
        </div>
      </div>

      {selected && (
        <div className="card">
          <h2>{shortDate(selected)}</h2>
          {!detail && !day.error && <span className="muted">불러오는 중…</span>}
          {day.error && <p className="error">{day.error}</p>}
          {detail && (
            <>
              {detail.events.map((e) => (
                <EventRow key={`${e.type}-${e.medicalRecordId}-${e.foodId}`} event={e} />
              ))}
              {detail.health.map((h) => (
                <div key={`h${h.petId}`} className="list-item">
                  <span className="chip muted">건강</span>
                  <span className="grow">
                    {petName(h.petId)}{" "}
                    {h.fed
                      ? `먹이 ${FEED_LABEL[h.fed]}`
                      : [h.meal && `식사 ${CONDITION_LABEL[h.meal]}`, h.stool && `배변 ${CONDITION_LABEL[h.stool]}`, h.weightKg !== null && `${h.weightKg}kg`]
                          .filter(Boolean)
                          .join(" · ")}
                  </span>
                </div>
              ))}
              {detail.medical.map((r) => (
                <div key={`m${r.id}`} className="list-item">
                  <span className="chip muted">{MEDICAL_LABEL[r.kind]}</span>
                  <span className="grow">
                    {petName(r.petId)} {r.content}
                  </span>
                </div>
              ))}
              {detail.special.map((r) => (
                <div key={`s${r.id}`} className="list-item">
                  <span className="chip muted">{r.label}</span>
                  <span className="grow">
                    {petName(r.petId)} {r.value ?? ""}
                  </span>
                </div>
              ))}
              {detail.events.length + detail.health.length + detail.medical.length + detail.special.length === 0 && (
                <span className="muted">이날은 기록이 없어요</span>
              )}
            </>
          )}
        </div>
      )}
    </>
  );
}

"use client";

import BackLink from "@/components/BackLink";
import EventRow from "@/components/EventRow";
import type { PetEvent } from "@/lib/events";
import { useAuthed } from "@/lib/useAuthed";

export default function EventsPage() {
  const alerts = useAuthed<PetEvent[]>("/api/events/alerts");
  const upcoming = useAuthed<PetEvent[]>("/api/events");
  const alertKeys = new Set((alerts.data ?? []).map(key));
  const rest = (upcoming.data ?? []).filter((e) => !alertKeys.has(key(e)) && e.daysUntil > 0);
  const error = alerts.error ?? upcoming.error;

  return (
    <>
      <BackLink />
      <h1>주요 이벤트</h1>
      {error && <p className="error" role="alert">{error}</p>}

      <h2>알림 대상</h2>
      <div className="card">
        {!alerts.data && !error && <span className="muted">불러오는 중…</span>}
        {alerts.data?.length === 0 && <span className="muted">지금 챙길 일이 없어요</span>}
        {alerts.data?.map((e) => <EventRow key={key(e)} event={e} />)}
      </div>

      <h2>
        다가오는 이벤트 <span className="muted small" style={{ fontFamily: "var(--f-body)" }}>30일 이내</span>
      </h2>
      <div className="card">
        {upcoming.data && rest.length === 0 && <span className="muted">예정된 일정이 없어요</span>}
        {rest.map((e) => <EventRow key={key(e)} event={e} />)}
      </div>
      <p className="muted small" style={{ margin: 0 }}>
        알림 대상은 설정의 알림 시점에 따라 정해집니다. 항목을 누르면 해당 화면으로 이동합니다.
      </p>
    </>
  );
}

function key(e: PetEvent): string {
  return `${e.type}-${e.medicalRecordId ?? ""}-${e.foodId ?? ""}-${e.date}`;
}

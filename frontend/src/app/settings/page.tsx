"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import BackLink from "@/components/BackLink";
import Seg from "@/components/Seg";
import { api, errorMessage, logout } from "@/lib/api";
import { applyTheme, THEME_LABEL, type Settings, type Theme } from "@/lib/settings";
import { useAuthed } from "@/lib/useAuthed";

const NOTICES: { key: keyof Omit<Settings, "theme">; label: string }[] = [
  { key: "medicalNoticeDays", label: "의료 일정" },
  { key: "treatmentNoticeDays", label: "치료 정보" },
  { key: "expiryNoticeDays", label: "유통기한 임박" },
  { key: "rebuyNoticeDays", label: "재구매 필요" },
];

export default function SettingsPage() {
  const router = useRouter();
  const loaded = useAuthed<Settings>("/api/users/me/settings");
  const [edited, setForm] = useState<Settings | null>(null);
  const form = edited ?? loaded.data;
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function save(next: Settings) {
    setBusy(true);
    setError(null);
    try {
      const saved = await api<Settings>("/api/users/me/settings", { method: "PUT", body: JSON.stringify(next) });
      setForm(saved);
      applyTheme(saved.theme);
      setNotice("저장했어요");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <>
      <BackLink />
      <h1>설정</h1>
      {(loaded.error ?? error) && <p className="error" role="alert">{loaded.error ?? error}</p>}
      {notice && <p className="notice" role="status">{notice}</p>}
      {!form && !loaded.error && <p className="muted">불러오는 중…</p>}
      {form && (
        <>
          <div className="card">
            <h2>색상 테마</h2>
            <Seg
              options={["PASTEL", "MONOCHROME"] as const}
              value={form.theme}
              onChange={(theme: Theme) => save({ ...form, theme })}
              labels={THEME_LABEL}
              ariaLabel="색상 테마"
            />
          </div>
          <form
            className="card"
            onSubmit={(e) => {
              e.preventDefault();
              save(form);
            }}
          >
            <h2>알림 시점</h2>
            <p className="muted small" style={{ margin: 0 }}>
              이벤트 며칠 전부터 주요 이벤트에 알림으로 표시할지 정해요. 0은 당일입니다.
            </p>
            {NOTICES.map(({ key, label }) => (
              <div key={key} className="row between">
                <label htmlFor={key} style={{ fontWeight: 700 }}>
                  {label}
                </label>
                <span className="row">
                  <input
                    id={key}
                    type="number"
                    inputMode="numeric"
                    min={0}
                    max={30}
                    value={form[key]}
                    onChange={(e) => setForm({ ...form, [key]: Number(e.target.value) })}
                    style={{ width: 64, border: "1px solid var(--line)", borderRadius: 10, padding: "8px 10px", background: "var(--surface)" }}
                  />
                  <span className="muted small">일 전</span>
                </span>
              </div>
            ))}
            <button type="submit" className="btn primary block" disabled={busy}>
              알림 시점 저장
            </button>
          </form>
        </>
      )}
      <button className="link" onClick={onLogout}>
        로그아웃
      </button>
    </>
  );
}

"use client";

import { useEffect, useState } from "react";
import { errorMessage } from "@/lib/api";
import { disablePush, enablePush, pushState, type PushState } from "@/lib/push";

const STATE_TEXT: Record<PushState, string> = {
  on: "매일 오전 9시, 챙길 일이 있으면 알려 드려요.",
  off: "알림을 켜면 매일 오전 9시에 챙길 일을 알려 드려요.",
  denied: "브라우저에서 알림이 차단되어 있어요. 브라우저 설정에서 이 사이트의 알림을 허용해 주세요.",
  unsupported: "이 브라우저는 알림을 지원하지 않아요. iPhone은 Safari에서 ‘홈 화면에 추가’한 앱으로 열면 사용할 수 있어요.",
};

export default function PushCard() {
  const [state, setState] = useState<PushState | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let alive = true;
    pushState()
      .then((s) => alive && setState(s))
      .catch(() => alive && setState("unsupported"));
    return () => {
      alive = false;
    };
  }, []);

  async function toggle() {
    setBusy(true);
    setError(null);
    try {
      setState(state === "on" ? await disablePush() : await enablePush());
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card">
      <div className="row between">
        <h2>알림 받기</h2>
        {(state === "on" || state === "off") && (
          <button className={`btn sm${state === "on" ? "" : " primary"}`} onClick={toggle} disabled={busy}>
            {state === "on" ? "끄기" : "켜기"}
          </button>
        )}
      </div>
      <p className="muted small" style={{ margin: 0 }}>
        {state ? STATE_TEXT[state] : "확인 중…"}
      </p>
      {error && <p className="error" role="alert">{error}</p>}
    </div>
  );
}

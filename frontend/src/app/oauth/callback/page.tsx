"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, errorMessage, tokenStore, type TokenResponse } from "@/lib/api";
import { consumeState, redirectUri } from "@/lib/oauth";

export default function OAuthCallbackPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    const provider = consumeState(params.get("state"));

    const run = async () => {
      if (!code || !provider) {
        throw new Error("로그인 요청을 확인할 수 없습니다. 처음부터 다시 시도해 주세요.");
      }
      const tokens = await api<TokenResponse>(`/api/auth/oauth/${provider}`, {
        method: "POST",
        body: JSON.stringify({ code, redirectUri: redirectUri() }),
      });
      tokenStore.save(tokens);
      router.replace("/");
    };
    run().catch((e) => setError(e instanceof Error && !("code" in e) ? e.message : errorMessage(e)));
  }, [router]);

  return error ? (
    <div className="card" style={{ marginTop: 40 }}>
      <h2>로그인하지 못했어요</h2>
      <p className="error" role="alert">{error}</p>
      <Link className="btn primary block" href="/login">
        로그인 화면으로
      </Link>
    </div>
  ) : (
    <p className="muted">로그인하는 중…</p>
  );
}

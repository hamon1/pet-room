"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, errorMessage, tokenStore } from "@/lib/api";
import PetSprite from "@/components/PetSprite";

type Mode = "login" | "signup";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<Mode>("login");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const body = {
      email: String(form.get("email") ?? ""),
      password: String(form.get("password") ?? ""),
      ...(mode === "signup" ? { nickname: String(form.get("nickname") ?? "") } : {}),
    };
    setBusy(true);
    setError(null);
    try {
      const res = await api<{ accessToken: string }>(`/api/auth/${mode}`, {
        method: "POST",
        body: JSON.stringify(body),
      });
      tokenStore.set(res.accessToken);
      router.replace("/pets");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="row" style={{ justifyContent: "center", paddingTop: 40 }}>
        <PetSprite category="MAMMAL" subCategory="DOG" size={88} />
      </div>
      <h1 style={{ textAlign: "center" }}>pet-room</h1>
      <p className="muted" style={{ textAlign: "center", margin: 0 }}>
        우리 아이 돌봄을 게임처럼
      </p>

      <form className="card" onSubmit={onSubmit} noValidate>
        <div className="field">
          <label htmlFor="email">이메일</label>
          <input id="email" name="email" type="email" autoComplete="email" placeholder="email@example.com" required />
        </div>
        <div className="field">
          <label htmlFor="password">비밀번호</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            placeholder={mode === "signup" ? "8자 이상" : "비밀번호"}
            required
          />
        </div>
        {mode === "signup" && (
          <div className="field">
            <label htmlFor="nickname">닉네임</label>
            <input id="nickname" name="nickname" maxLength={20} placeholder="20자 이내" required />
          </div>
        )}
        {error && <p className="error" role="alert">{error}</p>}
        <button className="btn primary block" type="submit" disabled={busy}>
          {mode === "login" ? "이메일로 로그인" : "가입하기"}
        </button>
      </form>

      <button
        className="link"
        style={{ alignSelf: "center" }}
        onClick={() => {
          setMode(mode === "login" ? "signup" : "login");
          setError(null);
        }}
      >
        {mode === "login" ? "계정이 없나요? 회원가입" : "이미 계정이 있나요? 로그인"}
      </button>
      <p className="muted small" style={{ textAlign: "center", margin: 0 }}>
        카카오·구글 로그인은 2단계에서 추가됩니다.
      </p>
    </>
  );
}

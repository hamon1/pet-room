"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError, errorMessage } from "./api";

/** 로그인이 필요한 화면의 데이터 불러오기. 401이면 로그인 화면으로 보냅니다. */
export function useAuthed<T>(path: string | null, version = 0) {
  const router = useRouter();
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!path) return;
    let alive = true;
    api<T>(path)
      .then((d) => {
        if (!alive) return;
        setData(d);
        setError(null);
      })
      .catch((e) => {
        if (!alive) return;
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
        else setError(errorMessage(e));
      });
    return () => {
      alive = false;
    };
  }, [path, version, router]);

  return { data, error, setData };
}

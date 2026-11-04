"use client";

import { useEffect } from "react";
import { applyTheme, cachedTheme } from "@/lib/settings";

/** 기기에 기억된 테마를 바로 적용합니다. 서버 설정은 방 화면·설정 화면에서 다시 맞춥니다. */
export default function ThemeSync() {
  useEffect(() => {
    applyTheme(cachedTheme());
  }, []);
  return null;
}

"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { tokenStore } from "@/lib/api";

export default function Home() {
  const router = useRouter();
  useEffect(() => {
    router.replace(tokenStore.get() ? "/pets" : "/login");
  }, [router]);
  return <p className="muted">불러오는 중…</p>;
}

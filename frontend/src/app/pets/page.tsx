"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError, errorMessage, tokenStore } from "@/lib/api";
import { ageLabel, CATEGORY_LABEL, kindLabel, type Pet } from "@/lib/pets";
import PetSprite from "@/components/PetSprite";

export default function PetsPage() {
  const router = useRouter();
  const [pets, setPets] = useState<Pet[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    api<Pet[]>("/api/pets")
      .then((list) => alive && setPets(list))
      .catch((e) => {
        if (!alive) return;
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
        else setError(errorMessage(e));
      });
    return () => {
      alive = false;
    };
  }, [router]);

  function logout() {
    tokenStore.clear();
    router.replace("/login");
  }

  return (
    <>
      <div className="row between">
        <h1>반려동물 목록</h1>
        <Link className="btn primary" href="/pets/new">
          + 등록
        </Link>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {!pets && !error && <p className="muted">불러오는 중…</p>}

      {pets && pets.length === 0 && (
        <div className="card" style={{ alignItems: "center", textAlign: "center" }}>
          <h2>아직 반려동물이 없어요</h2>
          <p className="muted" style={{ margin: 0 }}>첫 친구를 방에 데려와 볼까요?</p>
          <Link className="btn primary block" href="/pets/new">
            첫 반려동물 등록하기
          </Link>
        </div>
      )}

      {pets && pets.length > 0 && (
        <div className="grid-2">
          {pets.map((p) => (
            <article key={p.id} className="card" style={{ alignItems: "center", textAlign: "center" }}>
              <PetSprite category={p.category} subCategory={p.subCategory} size={64} />
              <h2>{p.name}</h2>
              <div className="row wrap" style={{ justifyContent: "center" }}>
                <span className="chip">{kindLabel(p)}</span>
                {ageLabel(p) && <span className="chip">{ageLabel(p)}</span>}
              </div>
              <span className="muted small">{p.species ?? CATEGORY_LABEL[p.category]}</span>
            </article>
          ))}
        </div>
      )}

      <p className="muted small" style={{ margin: 0 }}>
        메인 방 화면, 기록, 달력은 다음 단계에서 이어집니다.
      </p>
      <button className="link" onClick={logout}>
        로그아웃
      </button>
    </>
  );
}

"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError, errorMessage } from "@/lib/api";
import { ageLabel, CATEGORY_LABEL, kindLabel, type Pet } from "@/lib/pets";
import PetSprite from "@/components/PetSprite";
import QuickRecordSheet from "@/components/QuickRecordSheet";
import BackLink from "@/components/BackLink";

export default function PetsPage() {
  const router = useRouter();
  const [pets, setPets] = useState<Pet[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [quickOpen, setQuickOpen] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

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

  return (
    <>
      <BackLink />
      <div className="row between">
        <h1>반려동물 목록</h1>
        <Link className="btn primary" href="/pets/new">
          + 등록
        </Link>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {notice && <p className="notice" role="status">{notice}</p>}
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
        <>
          <button className="btn block" onClick={() => setQuickOpen(true)}>
            빠른 기록 · 오늘 모두 한 번에
          </button>
          <div className="grid-2">
            {pets.map((p) => (
              <Link key={p.id} href={`/pets/${p.id}`} className="card" style={{ alignItems: "center", textAlign: "center" }}>
                <PetSprite category={p.category} subCategory={p.subCategory} size={64} />
                <h2>{p.name}</h2>
                <div className="row wrap" style={{ justifyContent: "center" }}>
                  <span className="chip">{kindLabel(p)}</span>
                  {ageLabel(p) && <span className="chip">{ageLabel(p)}</span>}
                </div>
                <span className="muted small">{p.species ?? CATEGORY_LABEL[p.category]}</span>
              </Link>
            ))}
          </div>
        </>
      )}


      {quickOpen && pets && (
        <QuickRecordSheet
          pets={pets}
          onClose={() => setQuickOpen(false)}
          onSaved={(msg) => {
            setQuickOpen(false);
            setNotice(msg);
          }}
        />
      )}
    </>
  );
}

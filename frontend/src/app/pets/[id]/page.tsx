"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { api, ApiError, errorMessage } from "@/lib/api";
import { ageLabel, CATEGORY_LABEL, kindLabel, type Pet } from "@/lib/pets";
import PetSprite from "@/components/PetSprite";
import HealthTab from "@/components/HealthTab";
import MedicalTab from "@/components/MedicalTab";
import SpecialTab from "@/components/SpecialTab";
import FishSection from "@/components/FishSection";

type Tab = "health" | "medical" | "special";

/** 주소의 id는 실행 시점에만 알 수 있어, 사전 렌더링에서는 로딩 문구를 보여주고 브라우저에서 채웁니다. */
export default function PetDetailPage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <PetDetail />
    </Suspense>
  );
}

function PetDetail() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [pet, setPet] = useState<Pet | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<Tab>("health");

  useEffect(() => {
    let alive = true;
    api<Pet>(`/api/pets/${id}`)
      .then((p) => alive && setPet(p))
      .catch((e) => {
        if (!alive) return;
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
        else setError(errorMessage(e));
      });
    return () => {
      alive = false;
    };
  }, [id, router]);

  if (error) {
    return (
      <>
        <Link className="link" href="/pets">
          ‹ 목록
        </Link>
        <p className="error" role="alert">{error}</p>
      </>
    );
  }
  if (!pet) return <p className="muted">불러오는 중…</p>;

  const tank = pet.managementType === "TANK";
  const tabs: [Tab, string][] = [
    ["health", tank ? "급여 기록" : "건강 상태"],
    ["medical", "의료 정보"],
    ["special", "특수 항목"],
  ];

  return (
    <>
      <Link className="link" href="/pets">
        ‹ 목록
      </Link>
      <div className="card" style={{ alignItems: "center", textAlign: "center" }}>
        <PetSprite category={pet.category} subCategory={pet.subCategory} size={88} />
        <h1>{pet.name}</h1>
        <div className="row wrap" style={{ justifyContent: "center" }}>
          <span className="chip">
            {CATEGORY_LABEL[pet.category]} · {kindLabel(pet)}
          </span>
          {ageLabel(pet) && <span className="chip muted">{ageLabel(pet)}</span>}
        </div>
        {pet.species && <span className="muted small">{pet.species}</span>}
        {pet.traits && <span className="small">{pet.traits}</span>}
      </div>

      <div className="tabs" role="tablist">
        {tabs.map(([key, label]) => (
          <button key={key} role="tab" aria-selected={tab === key} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
      </div>

      {tab === "health" && <HealthTab pet={pet} />}
      {tab === "medical" && <MedicalTab petId={pet.id} />}
      {tab === "special" && (
        <>
          <SpecialTab petId={pet.id} />
          {tank && <FishSection petId={pet.id} />}
        </>
      )}
    </>
  );
}

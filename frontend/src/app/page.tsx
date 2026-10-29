"use client";

import { useCallback, useEffect, useRef, useState, type CSSProperties } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError, errorMessage, tokenStore } from "@/lib/api";
import type { PetEvent } from "@/lib/events";
import type { Pet } from "@/lib/pets";
import type { TankFish } from "@/lib/records";
import { applyTheme, type Settings } from "@/lib/settings";
import PetSprite from "@/components/PetSprite";
import QuickRecordSheet from "@/components/QuickRecordSheet";

interface Spot {
  x: number;
  y: number;
  flip: boolean;
  moving: boolean;
  hop: boolean;
}

interface Menu {
  pet: Pet;
  left: number;
  top: number;
}

const isTank = (p: Pet) => p.managementType === "TANK";
const isBowl = (p: Pet) => p.category === "FISH" && !isTank(p);

/** 시작 위치는 순서대로 고르게 흩어 놓고, 이후 무작위로 돌아다닙니다. */
function initialSpots(pets: Pet[]): Record<number, Spot> {
  return Object.fromEntries(
    pets.map((p, i) => [p.id, { x: 15 + ((i * 37) % 70), y: 30 + ((i * 23) % 50), flip: false, moving: false, hop: false }]),
  );
}

export default function RoomPage() {
  const router = useRouter();
  const roomRef = useRef<HTMLDivElement>(null);
  const [pets, setPets] = useState<Pet[] | null>(null);
  const [fish, setFish] = useState<TankFish[]>([]);
  const [alerts, setAlerts] = useState<PetEvent[]>([]);
  const [spots, setSpots] = useState<Record<number, Spot>>({});
  const [menu, setMenu] = useState<Menu | null>(null);
  const [quick, setQuick] = useState<Pet | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!tokenStore.get()) {
      router.replace("/login");
      return;
    }
    let alive = true;
    (async () => {
      try {
        const [list, events, settings] = await Promise.all([
          api<Pet[]>("/api/pets"),
          api<PetEvent[]>("/api/events/alerts"),
          api<Settings>("/api/users/me/settings"),
        ]);
        if (!alive) return;
        applyTheme(settings.theme);
        setPets(list);
        setAlerts(events);
        setSpots(initialSpots(list.filter((p) => p.category !== "FISH")));
        const tank = list.find(isTank);
        if (tank) {
          const f = await api<TankFish[]>(`/api/pets/${tank.id}/fish`);
          if (alive) setFish(f);
        }
      } catch (e) {
        if (!alive) return;
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
        else setError(errorMessage(e));
      }
    })();
    return () => {
      alive = false;
    };
  }, [router]);

  // 돌아다니기: 메뉴가 열려 있거나 동작 줄이기 설정이면 멈춥니다.
  const menuOpen = menu !== null || quick !== null;
  useEffect(() => {
    if (!pets || menuOpen) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const timers: number[] = [];
    const step = () => {
      setSpots((prev) => {
        const next = { ...prev };
        for (const id of Object.keys(prev).map(Number)) {
          if (Math.random() >= 0.5) continue;
          const x = 8 + Math.random() * 84;
          const y = 15 + Math.random() * 80;
          next[id] = { x, y, flip: x < prev[id].x, moving: true, hop: false };
          timers.push(
            window.setTimeout(() => {
              const hop = Math.random() < 0.45;
              setSpots((s) => (s[id] ? { ...s, [id]: { ...s[id], moving: false, hop } } : s));
              if (hop) timers.push(window.setTimeout(() => setSpots((s) => (s[id] ? { ...s, [id]: { ...s[id], hop: false } } : s)), 700));
            }, 2400),
          );
        }
        return next;
      });
    };
    const first = window.setTimeout(step, 300);
    const interval = window.setInterval(step, 3400);
    return () => {
      window.clearTimeout(first);
      window.clearInterval(interval);
      timers.forEach((t) => window.clearTimeout(t));
    };
  }, [pets, menuOpen]);

  const hasAlert = useCallback((id: number) => alerts.some((e) => e.petIds.includes(id)), [alerts]);

  function openMenu(pet: Pet, el: HTMLElement) {
    const room = roomRef.current;
    if (!room) return;
    const r = el.getBoundingClientRect();
    const rr = room.getBoundingClientRect();
    setMenu({
      pet,
      left: Math.min(Math.max(8, r.left - rr.left + r.width / 2 - 84), rr.width - 176),
      top: Math.max(70, r.top - rr.top - 150),
    });
  }

  function showToast(msg: string) {
    setToast(msg);
    window.setTimeout(() => setToast(null), 2200);
  }

  const walkers = pets?.filter((p) => p.category !== "FISH") ?? [];
  const tank = pets?.find(isTank);
  const bowls = pets?.filter(isBowl).slice(0, 2) ?? [];

  return (
    <div className="room" ref={roomRef}>
      <div className="wall" />
      <div className="baseboard" />
      <div className="floor" />
      <div className="window">
        <div className="cloud" />
      </div>
      <div className="shelf" />
      {bowls.length === 0 && (
        <>
          <div className="pot" />
          <div className="leaf" />
        </>
      )}
      {bowls.map((b, i) => (
        <Link
          key={b.id}
          href={`/pets/${b.id}`}
          className="bowl"
          style={{ right: `${8 + i * 18}%`, top: "calc(27% - 46px)" }}
          aria-label={`${b.name} 상세 보기`}
        >
          <span className="swim" style={{ "--d": "5s" } as CSSProperties}>
            <PetSprite category="FISH" subCategory={null} size={16} />
          </span>
        </Link>
      ))}
      <div className="rug" />

      {tank && (
        <Link href={`/pets/${tank.id}`} className="tank-wrap" aria-label={`${tank.name} 상세 보기`}>
          <div className="tank">
            <i className="weed" style={{ left: "10%", height: "34%" }} />
            <i className="weed" style={{ right: "12%", height: "44%" }} />
            <i className="sand" />
            {(fish.length ? fish : [null]).slice(0, 4).map((f, i) => (
              <span
                key={f?.id ?? "empty"}
                className="swim"
                style={{ top: `${16 + i * 16}%`, "--d": `${7 + i * 1.7}s`, animationDelay: `-${i * 2.1}s` } as CSSProperties}
              >
                <PetSprite category="FISH" subCategory={null} size={22 - i * 2} />
              </span>
            ))}
            <i className="bubble-dot" style={{ left: "40%", bottom: "12%" }} />
            <i className="bubble-dot" style={{ left: "62%", bottom: "16%", animationDelay: "-1.4s" }} />
          </div>
          <div className="stand" />
          <span className="nametag" style={{ position: "absolute", left: "50%", bottom: -24, transform: "translateX(-50%)" }}>
            {tank.name}
          </span>
          {hasAlert(tank.id) && (
            <span className="alert-mark" style={{ top: -28 }}>
              !
            </span>
          )}
        </Link>
      )}

      <div className="floorzone">
        {walkers.map((p) => {
          const s = spots[p.id];
          if (!s) return null;
          return (
            <button
              key={p.id}
              className={`walker${s.moving ? " moving" : ""}${s.hop ? " hop" : ""}${s.flip ? " flip" : ""}`}
              style={{ left: `${s.x}%`, top: `${s.y}%`, zIndex: Math.round(s.y) }}
              onClick={(e) => openMenu(p, e.currentTarget)}
              aria-label={`${p.name} 메뉴 열기`}
            >
              {hasAlert(p.id) && <span className="alert-mark">!</span>}
              <span className="bob">
                <PetSprite category={p.category} subCategory={p.subCategory} size={p.category === "BIRD" ? 44 : 58} />
              </span>
              <span className="nametag">{p.name}</span>
            </button>
          );
        })}
      </div>

      <div className="hud">
        <div className="room-title">우리 집</div>
        <div className="row">
          <Link className="hud-btn" href="/food">
            <span className="ico" aria-hidden="true">🥣</span>먹이
          </Link>
          <Link className="hud-btn" href="/settings">
            <span className="ico" aria-hidden="true">⚙️</span>설정
          </Link>
        </div>
      </div>

      {error && (
        <div className="card empty-card">
          <p className="error" role="alert">{error}</p>
        </div>
      )}
      {!pets && !error && (
        <div className="card empty-card">
          <p className="muted" style={{ margin: 0 }}>불러오는 중…</p>
        </div>
      )}
      {pets && pets.length === 0 && (
        <div className="card empty-card">
          <h2>아직 반려동물이 없어요</h2>
          <p className="muted" style={{ margin: 0 }}>첫 친구를 방에 데려와 볼까요?</p>
          <Link className="btn primary block" href="/pets/new">
            첫 반려동물 등록하기
          </Link>
        </div>
      )}

      {menu && (
        <div className="popover" role="menu" style={{ left: menu.left, top: menu.top }}>
          <div className="row between" style={{ paddingLeft: 6 }}>
            <h2>{menu.pet.name}</h2>
            <button className="btn sm" onClick={() => setMenu(null)} aria-label="닫기">
              ✕
            </button>
          </div>
          <button
            className="item"
            role="menuitem"
            autoFocus
            onClick={() => {
              setQuick(menu.pet);
              setMenu(null);
            }}
          >
            빠른 기록
          </button>
          <Link className="item" role="menuitem" href={`/pets/${menu.pet.id}`}>
            상세 보기
          </Link>
        </div>
      )}

      {toast && (
        <div className="toast" role="status">
          {toast}
        </div>
      )}

      <nav className="main-panel" aria-label="메인 메뉴">
        <Link className="main-btn" href="/pets">
          <span className="ico" aria-hidden="true">📋</span>반려동물 목록
        </Link>
        <Link className="main-btn" href="/calendar">
          <span className="ico" aria-hidden="true">📅</span>달력
        </Link>
        <Link className="main-btn" href="/events">
          <span className="ico" aria-hidden="true">🔔</span>주요 이벤트
          {alerts.length > 0 && <span className="badge">{alerts.length}</span>}
        </Link>
      </nav>

      {quick && (
        <QuickRecordSheet
          pets={[quick]}
          onClose={() => setQuick(null)}
          onSaved={(msg) => {
            setQuick(null);
            showToast(msg.replace("1마리", quick.name));
          }}
        />
      )}
    </div>
  );
}

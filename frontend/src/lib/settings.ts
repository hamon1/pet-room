export type Theme = "PASTEL" | "MONOCHROME";

export interface Settings {
  medicalNoticeDays: number;
  treatmentNoticeDays: number;
  expiryNoticeDays: number;
  rebuyNoticeDays: number;
  theme: Theme;
}

export const THEME_LABEL: Record<Theme, string> = { PASTEL: "파스텔", MONOCHROME: "블랙 앤 화이트" };

const THEME_KEY = "pet-room.theme";

/** body 클래스로 테마를 적용하고, 다음 접속 때 깜빡임이 없도록 기기에 기억합니다. */
export function applyTheme(theme: Theme) {
  document.body.classList.toggle("bw", theme === "MONOCHROME");
  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch {
    /* 저장소를 쓸 수 없으면 이번 화면에만 적용 */
  }
}

export function cachedTheme(): Theme {
  try {
    return localStorage.getItem(THEME_KEY) === "MONOCHROME" ? "MONOCHROME" : "PASTEL";
  } catch {
    return "PASTEL";
  }
}

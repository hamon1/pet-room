import { defineConfig } from "@playwright/test";

/**
 * 실제 백엔드(:8080)·DB와 프론트엔드(:3000)를 띄운 상태에서 실행하는 전체 흐름 테스트.
 * CI의 e2e 작업이 서버를 먼저 띄웁니다. 로컬: README "E2E 테스트" 참고.
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  retries: 0,
  reporter: process.env.CI ? [["github"], ["list"]] : "list",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    locale: "ko-KR",
    timezoneId: "Asia/Seoul",
    viewport: { width: 390, height: 844 },
    // 돌아다니는 애니메이션을 끄고 클릭 대상이 움직이지 않게 합니다.
    contextOptions: { reducedMotion: "reduce" },
    trace: "retain-on-failure",
  },
});

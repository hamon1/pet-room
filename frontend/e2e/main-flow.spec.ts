import { expect, test, type Page } from "@playwright/test";

const API = process.env.E2E_API_URL ?? "http://localhost:8080";

/** 한국 시간 기준 오늘에서 n일 뒤 (YYYY-MM-DD) */
function kstDate(n = 0): string {
  const d = new Date(Date.now() + 9 * 3600_000 + n * 86400_000);
  return d.toISOString().slice(0, 10);
}

async function token(page: Page): Promise<string> {
  return (await page.evaluate(() => localStorage.getItem("pet-room.token"))) ?? "";
}

test("가입부터 방·먹이·이벤트·달력·설정까지 실제 서버로 동작한다", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("response", (r) => {
    if (r.url().startsWith(API) && r.status() >= 500) errors.push(`${r.status()} ${r.url()}`);
  });

  // 1. 회원가입 → 빈 방
  await page.goto("/login");
  await page.getByRole("button", { name: "계정이 없나요? 회원가입" }).click();
  await page.getByLabel("이메일").fill(`e2e-${Date.now()}@example.com`);
  await page.getByLabel("비밀번호").fill("password123");
  await page.getByLabel("닉네임").fill("테스터");
  await page.getByRole("button", { name: "가입하기" }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByText("아직 반려동물이 없어요")).toBeVisible();

  // 2. 반려동물 등록 (기본 선택: 포유류·강아지) → 방에 나타남
  await page.getByRole("link", { name: "첫 반려동물 등록하기" }).click();
  await page.getByLabel("이름").fill("코코");
  await page.getByRole("button", { name: "등록하기" }).click();
  await expect(page).toHaveURL(/\/$/);
  const coco = page.getByRole("button", { name: "코코 메뉴 열기" });
  await expect(coco).toBeVisible();

  // 3. 펫 메뉴 → 빠른 기록
  await coco.click();
  await page.getByRole("menuitem", { name: "빠른 기록" }).click();
  await page.getByRole("button", { name: "모두 정상으로 기록" }).click();
  await expect(page.getByRole("status")).toHaveText("코코 오늘 기록 완료");

  // 4. 의료 기록(다음 예정일 내일)은 API로 추가
  const auth = { Authorization: `Bearer ${await token(page)}` };
  const pets = await (await page.request.get(`${API}/api/pets`, { headers: auth })).json();
  const petId: number = pets[0].id;
  const medical = await page.request.post(`${API}/api/pets/${petId}/medical-records`, {
    headers: auth,
    data: { kind: "VACCINATION", recordDate: kstDate(0), content: "종합백신", nextDate: kstDate(1) },
  });
  expect(medical.status()).toBe(201);

  // 5. 먹이 등록 → 1회 급여
  await page.getByRole("link", { name: "먹이" }).click();
  await page.getByRole("button", { name: "+ 등록" }).click();
  const sheet = page.getByRole("dialog", { name: "먹이 등록" });
  await sheet.getByLabel("제품명").fill("사료 A");
  await sheet.getByLabel("유통기한").fill(kstDate(2));
  await sheet.getByLabel("남은 양 (g)").fill("300");
  await sheet.getByLabel("1회 급여량 (g)").fill("100");
  await sheet.getByLabel("하루 횟수").fill("1");
  await sheet.getByRole("button", { name: "코코" }).click();
  await sheet.getByRole("button", { name: "저장" }).click();
  await expect(page.getByRole("heading", { name: "사료 A" })).toBeVisible();
  await page.getByRole("button", { name: "1회 급여" }).click();
  await expect(page.getByRole("status")).toContainText("남은 양 200g");

  // 6. 방: 알림 3건 (의료 D-1, 유통기한 D-2, 재구매 D-2)
  await page.getByRole("link", { name: "← 방" }).click();
  await expect(page.locator(".badge")).toHaveText("3");

  // 7. 주요 이벤트
  await page.getByRole("link", { name: /주요 이벤트/ }).click();
  for (const title of ["코코 접종 · 종합백신", "사료 A 유통기한", "사료 A 재구매"]) {
    await expect(page.getByText(title)).toBeVisible();
  }

  // 8. 달력: 오늘 칸에 기록 2개(건강·의료), 누르면 그날 기록
  await page.goto("/calendar");
  const day = Number(kstDate(0).slice(8));
  await page.getByRole("button", { name: new RegExp(`^\\d+월 ${day}일.*기록 2개`) }).click();
  await expect(page.getByText("코코 식사 정상 · 배변 정상")).toBeVisible();
  await expect(page.getByText("코코 종합백신")).toBeVisible();

  // 9. 설정: 테마 → 블랙 앤 화이트, 알림 시점 0일 → 알림 없음
  await page.goto("/settings");
  await Promise.all([
    page.waitForResponse((r) => r.url().endsWith("/api/users/me/settings") && r.request().method() === "PUT" && r.ok()),
    page.getByRole("button", { name: "블랙 앤 화이트" }).click(),
  ]);
  await expect(page.locator("body")).toHaveClass(/bw/);
  for (const label of ["의료 일정", "치료 정보", "유통기한 임박", "재구매 필요"]) {
    await page.getByLabel(label).fill("0");
  }
  await Promise.all([
    page.waitForResponse((r) => r.url().endsWith("/api/users/me/settings") && r.request().method() === "PUT" && r.ok()),
    page.getByRole("button", { name: "알림 시점 저장" }).click(),
  ]);
  await page.goto("/");
  await expect(page.getByRole("button", { name: "코코 메뉴 열기" })).toBeVisible();
  await expect(page.locator(".badge")).toHaveCount(0);
  await expect(page.locator("body")).toHaveClass(/bw/);

  // 10. 로그아웃
  await page.goto("/settings");
  await page.getByRole("button", { name: "로그아웃" }).click();
  await expect(page).toHaveURL(/\/login$/);

  expect(errors).toEqual([]);
});

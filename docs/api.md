# API 명세

- 기본 경로: `/api`
- 형식: JSON, 날짜는 `YYYY-MM-DD`, 시각은 ISO-8601 (UTC)
- 인증: `Authorization: Bearer <accessToken>` (`/api/auth/**`, `/actuator/health` 제외)
- 상태 표시: ✅ 구현 완료 · ⏳ 예정

## 오류 응답

[RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) 형식을 씁니다.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값을 확인해 주세요.",
  "code": "INVALID_INPUT",
  "errors": [{ "field": "name", "message": "이름을 입력해 주세요." }]
}
```

| status | code | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 필수값 누락, 형식 오류 |
| 400 | `INVALID_PET_CLASSIFICATION` | 분류·세부 분류·관리 방식 조합이 맞지 않음 |
| 401 | `UNAUTHORIZED` | 토큰 없음·만료, 로그인 실패 |
| 404 | `PET_NOT_FOUND` | 없거나 내 반려동물이 아님 |
| 409 | `EMAIL_ALREADY_EXISTS` | 이미 가입된 이메일 |

## 인증

### ✅ POST /api/auth/signup
```json
{ "email": "me@example.com", "password": "password123", "nickname": "하랑" }
```
- 비밀번호 8~64자, 닉네임 1~20자
- `201 Created` → `{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 86400 }`

### ✅ POST /api/auth/login
```json
{ "email": "me@example.com", "password": "password123" }
```
- `200 OK` → 회원가입과 같은 토큰 응답
- 이메일이 없거나 비밀번호가 틀려도 같은 `401`을 돌려줍니다 (가입 여부 노출 방지).

### ⏳ POST /api/auth/oauth/{provider}
카카오·구글 로그인 (2단계)

## 사용자

### ✅ GET /api/users/me
`200 OK` → `{ "id": 1, "email": "me@example.com", "nickname": "하랑" }`

## 반려동물

### ✅ POST /api/pets
```json
{
  "name": "코코",
  "category": "MAMMAL",
  "subCategory": "DOG",
  "managementType": "INDIVIDUAL",
  "species": "말티즈",
  "birthDate": "2023-05-01",
  "ageYears": null,
  "traits": "산책을 좋아함"
}
```
| 필드 | 필수 | 설명 |
| --- | --- | --- |
| name | O | 1~30자 |
| category | O | `MAMMAL`, `BIRD`, `FISH` |
| subCategory | 포유류·조류 | `DOG`, `CAT` / `FINCH`, `PARROT`. 어류는 비워 둠 |
| managementType | O | `INDIVIDUAL`, 어류만 `TANK` 가능 |
| species | | 상세 종명, 30자 이내 |
| birthDate | | 출생일(어항은 설치일). 오늘 이후 불가 |
| ageYears | | 나이(년, 0.5 단위 가능). `birthDate`와 동시에 보내면 400 |
| traits | | 특징, 500자 이내 |

`201 Created`, `Location: /api/pets/{id}` → 반려동물 응답

### ✅ GET /api/pets
내 반려동물 목록 (등록순) → `[반려동물 응답]`

### ✅ GET /api/pets/{id}

### ✅ PUT /api/pets/{id}
등록과 같은 본문으로 전체 수정

### ✅ DELETE /api/pets/{id}
`204 No Content`

반려동물 응답:
```json
{
  "id": 1,
  "name": "코코",
  "category": "MAMMAL",
  "subCategory": "DOG",
  "managementType": "INDIVIDUAL",
  "species": "말티즈",
  "birthDate": "2023-05-01",
  "birthDateEstimated": false,
  "traits": "산책을 좋아함",
  "createdAt": "2026-10-29T08:00:00Z"
}
```

## 예정 (⏳)

| 단계 | 엔드포인트 | 설명 |
| --- | --- | --- |
| 2 | `GET/PUT /api/pets/{id}/health-records/{date}` | 날짜별 건강 기록 (같은 날 다시 저장하면 덮어씀) |
| 2 | `POST /api/health-records/batch` | 빠른 기록·일괄 기록 |
| 2 | `GET/POST /api/pets/{id}/medical-records` | 의료 기록 |
| 2 | `GET/POST /api/pets/{id}/special-records` | 특수 항목 기록 |
| 2 | `GET/POST/DELETE /api/pets/{id}/fish` | 어항 속 물고기 |
| 3 | `GET/POST/PATCH/DELETE /api/foods` | 먹이 재고, 급여 차감, 남은 양 수정 |
| 3 | `GET /api/events?from=&to=` | 주요 이벤트(계산값) |
| 3 | `GET /api/calendar?month=YYYY-MM` | 날짜별 이벤트·기록 요약 |
| 3 | `GET/PUT /api/users/me/settings` | 알림 시점, 색상 테마 |
| 4 | `POST /api/push-subscriptions` | 웹 푸시 구독 |

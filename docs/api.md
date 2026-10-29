# API 명세

- 기본 경로: `/api`
- 형식: JSON, 날짜는 `YYYY-MM-DD`, 시각은 ISO-8601 (UTC)
- 인증: `Authorization: Bearer <accessToken>` (`/api/auth/**`, `/actuator/health` 제외)
- 날짜 규칙("오늘 이후 불가" 등)은 **한국 시간** 기준입니다.
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
| 400 | `INVALID_INPUT` | 필수값 누락, 형식 오류, 없는 값(enum) |
| 400 | `INVALID_PET_CLASSIFICATION` | 분류·세부 분류·관리 방식 조합이 맞지 않음 |
| 400 | `INVALID_RECORD` | 오늘 이후 날짜, 빈 기록, 어항·개체에 맞지 않는 항목 |
| 400 | `INVALID_SPECIAL_ITEM` | 분류에 없는 특수 항목 |
| 400 | `NOT_A_TANK` | 어항 단위가 아닌 반려동물에 물고기 추가 |
| 400 | `OAUTH_PROVIDER_DISABLED` | 앱 키가 설정되지 않은 소셜 로그인 |
| 400 | `INVALID_REDIRECT_URI` | 허용되지 않은 소셜 로그인 콜백 주소 |
| 401 | `UNAUTHORIZED` | 액세스 토큰 없음·만료·위조 |
| 401 | `INVALID_CREDENTIALS` | 이메일 또는 비밀번호 불일치 |
| 401 | `INVALID_REFRESH_TOKEN` | 리프레시 토큰 없음·만료·이미 사용됨 |
| 401 | `OAUTH_FAILED` | 소셜 로그인 제공자에서 실패 (인가 코드 만료 등) |
| 404 | `PET_NOT_FOUND` | 없거나 내 반려동물이 아님 |
| 404 | `HEALTH_RECORD_NOT_FOUND` · `MEDICAL_RECORD_NOT_FOUND` · `SPECIAL_RECORD_NOT_FOUND` · `FISH_NOT_FOUND` | 해당 기록 없음 |
| 409 | `EMAIL_ALREADY_EXISTS` | 이미 가입된 이메일 |

## 인증

토큰 응답 (가입·로그인·갱신·소셜 로그인 공통):

```json
{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 1800, "refreshToken": "...", "refreshExpiresIn": 1209600 }
```

- 액세스 토큰 30분, 리프레시 토큰 14일 (환경변수 `JWT_EXPIRES_IN`, `JWT_REFRESH_EXPIRES_IN`)

### ✅ POST /api/auth/signup
```json
{ "email": "me@example.com", "password": "password123", "nickname": "하랑" }
```
- 비밀번호 8~64자, 닉네임 1~20자, 이메일은 대소문자를 구분하지 않음
- `201 Created` → 토큰 응답

### ✅ POST /api/auth/login
```json
{ "email": "me@example.com", "password": "password123" }
```
- `200 OK` → 토큰 응답
- 이메일이 없거나 비밀번호가 틀려도 같은 `401`을 돌려줍니다 (가입 여부 노출 방지).

### ✅ POST /api/auth/refresh
```json
{ "refreshToken": "..." }
```
- `200 OK` → 새 토큰 응답. 보낸 리프레시 토큰은 더 이상 쓸 수 없습니다(회전).
- 이미 쓴 토큰이 다시 오면 탈취로 보고 그 사용자의 리프레시 토큰을 모두 무효화한 뒤 `401`.

### ✅ POST /api/auth/logout
본문은 refresh와 같음. `204 No Content` (없는 토큰이어도 204)

### ✅ POST /api/auth/oauth/{provider}
`provider`: `kakao`, `google`
```json
{ "code": "제공자가 준 인가 코드", "redirectUri": "http://localhost:3000/oauth/callback" }
```
- `redirectUri`는 서버 설정 `OAUTH_ALLOWED_REDIRECT_URIS`에 있어야 합니다.
- 처음 들어온 계정은 새 사용자로 가입됩니다. 같은 이메일의 기존 계정과 **자동으로 합치지 않습니다.**
- 설정 방법: [setup-oauth.md](setup-oauth.md)

## 사용자

### ✅ GET /api/users/me
`200 OK` → `{ "id": 1, "email": "me@example.com", "nickname": "하랑" }` (소셜 사용자는 `email`이 없을 수 있음)

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

### ✅ GET /api/pets · GET /api/pets/{id} · PUT /api/pets/{id} · DELETE /api/pets/{id}
목록(등록순), 조회, 전체 수정(등록과 같은 본문), 삭제(`204`, 딸린 기록도 함께 삭제)

반려동물 응답:
```json
{
  "id": 1, "name": "코코", "category": "MAMMAL", "subCategory": "DOG", "managementType": "INDIVIDUAL",
  "species": "말티즈", "birthDate": "2023-05-01", "birthDateEstimated": false, "traits": "산책을 좋아함",
  "createdAt": "2026-10-29T08:00:00Z"
}
```

## 건강 기록

반려동물 한 마리당 날짜별로 하나입니다. 개체는 체중·식사·배변, 어항은 먹이 급여를 기록하고 메모는 둘 다 가능합니다.

| 필드 | 값 |
| --- | --- |
| weightKg | 0.001~200, 소수점 셋째 자리까지 (개체만) |
| meal, stool | `NORMAL`(정상), `LOW`(적음), `ABNORMAL`(이상) (개체만) |
| fed | `DONE`(완료), `SKIPPED`(안 함) (어항만) |
| memo | 500자 이내 |

### ✅ PUT /api/pets/{petId}/health-records/{date}
해당 날짜 기록을 **전체 덮어쓰기**합니다(보내지 않은 값은 비움). 오늘 이후 날짜, 빈 기록은 400.
```json
{ "weightKg": 5.2, "meal": "NORMAL", "stool": "NORMAL", "memo": "산책 후 컨디션 좋음" }
```
`200 OK` → `{ "petId": 1, "date": "2026-10-29", "weightKg": 5.2, "meal": "NORMAL", "stool": "NORMAL", "fed": null, "memo": "..." }`

### ✅ GET /api/pets/{petId}/health-records?from=&to=
기간 조회(최신순). 기본 최근 30일, 최대 1년.

### ✅ DELETE /api/pets/{petId}/health-records/{date}

### ✅ POST /api/health-records/batch
빠른 기록·일괄 기록. 보낸 값만 바꾸고 **기존 체중·메모는 유지**합니다. 하나라도 내 반려동물이 아니면 전체를 저장하지 않고 404.
```json
{ "date": "2026-10-29", "items": [
  { "petId": 1, "meal": "NORMAL", "stool": "NORMAL" },
  { "petId": 4, "fed": "DONE" }
] }
```
`200 OK` → 저장된 건강 기록 배열 (보낸 순서)

## 의료 기록

### ✅ POST /api/pets/{petId}/medical-records
```json
{ "kind": "VACCINATION", "recordDate": "2026-10-26", "hospital": "예시동물병원", "content": "종합백신 2차", "nextDate": "2026-11-25" }
```
- `kind`: `VISIT`(진료), `VACCINATION`(접종), `MEDICATION`(투약), `TREATMENT`(치료)
- `recordDate`는 오늘 이후 불가, `nextDate`는 `recordDate` 이후만 가능
- 투약·치료의 다음 예정일은 "치료 정보", 진료·접종은 "의료 일정" 주요 이벤트가 됩니다 (3단계).

### ✅ GET /api/pets/{petId}/medical-records · PUT, DELETE /api/pets/{petId}/medical-records/{id}

## 특수 항목

| item | 이름 | 분류 | 구분 |
| --- | --- | --- | --- |
| `BEAK` | 부리 관리 | 조류 | 사용자 지정 |
| `RESPIRATORY` | 호흡기 관리 | 조류 | 사용자 지정 |
| `WATER_QUALITY` | 어항 수질 관리 | 어류 | 사용자 지정 |
| `NAIL` | 발톱 관리 | 포유류·조류 | 제안 |
| `COAT_SKIN` · `DENTAL` · `WALK` | 털·피부 관리, 치아 관리, 산책 기록 | 포유류 | 제안 |
| `MOLT` | 깃털·털갈이 | 조류 | 제안 |
| `WATER_TEMP` · `WATER_CHANGE` · `FILTER_CLEANING` | 수온, 환수 기록, 여과기 청소 | 어류 | 제안 |

제안 항목은 출시 전 수의사·사육 커뮤니티 검증이 필요합니다.

### ✅ GET /api/pets/{petId}/special-items
이 반려동물의 항목과 가장 최근 기록 → `[{ "item": "BEAK", "label": "부리 관리", "userDefined": true, "latest": { ... } }]`

### ✅ POST /api/pets/{petId}/special-records
```json
{ "item": "BEAK", "recordDate": "2026-10-29", "value": "길이 정상" }
```
`201 Created`. 분류에 없는 항목은 400.

### ✅ GET /api/pets/{petId}/special-records?item= · DELETE /api/pets/{petId}/special-records/{id}

## 어항 속 물고기

### ✅ GET, POST /api/pets/{petId}/fish · DELETE /api/pets/{petId}/fish/{fishId}
```json
{ "name": "구피 1", "species": null }
```
어항 단위(`TANK`) 반려동물만 가능. 종명을 비우면 어항의 종을 씁니다.

## 예정 (⏳)

| 단계 | 엔드포인트 | 설명 |
| --- | --- | --- |
| 3 | `GET/POST/PATCH/DELETE /api/foods` | 먹이 재고, 급여 차감, 남은 양 수정 |
| 3 | `GET /api/events?from=&to=` | 주요 이벤트(계산값) |
| 3 | `GET /api/calendar?month=YYYY-MM` | 날짜별 이벤트·기록 요약 |
| 3 | `GET/PUT /api/users/me/settings` | 알림 시점, 색상 테마 |
| 4 | `POST /api/push-subscriptions` | 웹 푸시 구독 |

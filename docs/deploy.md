# 배포 가이드

> 상태: 배포 준비 완료(Docker 이미지, 운영 설정), **호스팅 서비스는 미정**입니다. 서비스를 정하면 이 문서에 실제 절차와 CD를 추가합니다.

## 구성

| 구성 요소 | 이미지 / 실행 | 포트 |
| --- | --- | --- |
| 백엔드 | `backend/Dockerfile` (Java 21 JRE, `prod` 프로필) | 8080 |
| 프론트엔드 | `frontend/Dockerfile` (Next.js standalone) | 3000 |
| DB | PostgreSQL 16 (관리형 DB 권장) | 5432 |

HTTPS는 호스팅 서비스나 앞단 프록시가 처리합니다. 백엔드는 `X-Forwarded-*` 헤더를 읽습니다(`server.forward-headers-strategy: framework`).

## 백엔드 환경변수

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `DB_URL` | ✅ | 예: `jdbc:postgresql://host:5432/petroom?sslmode=require` |
| `DB_USERNAME`, `DB_PASSWORD` | ✅ | |
| `JWT_SECRET` | ✅ | 32바이트 이상 무작위 값 (`openssl rand -base64 48`). 로컬 기본값이면 시작하지 않음 |
| `CORS_ALLOWED_ORIGINS` | ✅ | 프론트엔드 주소, 예: `https://pet-room.example.com` |
| `OAUTH_ALLOWED_REDIRECT_URIS` | 소셜 로그인 시 | 예: `https://pet-room.example.com/oauth/callback` |
| `KAKAO_CLIENT_ID` 등 | 선택 | 없으면 해당 소셜 로그인 비활성 ([setup-oauth.md](setup-oauth.md)) |
| `PUSH_VAPID_PUBLIC_KEY`, `PUSH_VAPID_PRIVATE_KEY`, `PUSH_VAPID_SUBJECT` | 선택 | 없으면 웹 푸시 비활성. 아래 키 생성 참고 |
| `PUSH_CRON` | 선택 | 매일 알림 시각, 기본 `0 0 9 * * *` (한국 시간) |

## 프론트엔드 빌드 인자

`NEXT_PUBLIC_*` 값은 빌드할 때 화면 코드에 들어갑니다. 값을 바꾸면 이미지를 다시 빌드해야 합니다.

| 이름 | 설명 |
| --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | 백엔드 주소, 예: `https://api.pet-room.example.com` |
| `NEXT_PUBLIC_KAKAO_CLIENT_ID`, `NEXT_PUBLIC_GOOGLE_CLIENT_ID` | 선택, 소셜 로그인 버튼 활성화 |

## 웹 푸시 키 생성

```bash
npx web-push generate-vapid-keys --json
```

- `publicKey` → `PUSH_VAPID_PUBLIC_KEY`, `privateKey` → `PUSH_VAPID_PRIVATE_KEY`
- `PUSH_VAPID_SUBJECT`: 푸시 서비스가 연락할 운영자 주소, 예: `mailto:본인이메일`
- 키를 바꾸면 기존 구독은 모두 무효가 되므로, 한 번 정하면 유지합니다.
- 웹 푸시는 HTTPS에서만 동작합니다. iPhone은 Safari에서 "홈 화면에 추가"한 앱으로 열어야 알림을 받을 수 있습니다.

## 이미지 빌드·실행 (예시)

```bash
docker build -t pet-room-backend backend
docker build -t pet-room-frontend --build-arg NEXT_PUBLIC_API_BASE_URL=https://api.example.com frontend

docker run -d -p 8080:8080 --env-file backend.env pet-room-backend
docker run -d -p 3000:3000 pet-room-frontend
```

CI(`Docker images` 작업)가 매 PR마다 두 이미지를 빌드하고, 프론트엔드 이미지 실행과 "설정 없이 백엔드 실행 시 중단"을 확인합니다.

## 운영 확인

- 상태 확인: `GET /actuator/health` (호스팅 서비스의 헬스체크로 지정)
- 로그: `prod` 프로필은 JSON 한 줄(ECS) 형식. 요청마다 `requestId`가 붙고, 응답 헤더 `X-Request-Id`와 같은 값입니다. 사용자가 오류 화면의 요청 ID를 알려 주면 그 값으로 로그를 찾습니다.
- 외부 오류 모니터링 서비스 연동은 보류 (서비스 가입 필요)

# ERD

MVP 전체 테이블 설계입니다. 단계별로 Flyway 마이그레이션을 추가합니다.

| 마이그레이션 | 단계 | 테이블 |
| --- | --- | --- |
| V1 | 1 | `users`, `pets` |
| V2 | 2 | `health_records`, `medical_records`, `special_records`, `tank_fish` |
| V3 | 2 | `refresh_tokens`, `social_accounts` (소셜 사용자를 위해 `users.email`, `password_hash` nullable) |
| 예정 | 3 | `foods`, `food_pets`, `user_settings` |

```mermaid
erDiagram
  users ||--o{ pets : owns
  users ||--|| user_settings : has
  users ||--o{ foods : owns
  users ||--o{ refresh_tokens : has
  users ||--o{ social_accounts : "signs in with"
  pets ||--o{ tank_fish : contains
  pets ||--o{ health_records : has
  pets ||--o{ medical_records : has
  pets ||--o{ special_records : has
  foods ||--o{ food_pets : "eaten by"
  pets ||--o{ food_pets : eats

  users {
    bigint id PK
    varchar email UK "소셜 사용자는 없을 수 있음"
    varchar password_hash "소셜 사용자는 없음"
    varchar nickname
    timestamptz created_at
  }
  pets {
    bigint id PK
    bigint owner_id FK
    varchar name
    varchar category "MAMMAL, BIRD, FISH"
    varchar sub_category "DOG, CAT, FINCH, PARROT (어류는 없음)"
    varchar management_type "INDIVIDUAL, TANK"
    varchar species "상세 종명"
    date birth_date
    boolean birth_date_estimated "나이로 입력한 경우 true"
    varchar traits "특징"
    timestamptz archived_at "기록 보관 모드 (확장)"
    timestamptz created_at
    timestamptz updated_at
  }
  refresh_tokens {
    bigint id PK
    bigint user_id FK
    varchar token_hash UK "SHA-256, 원문은 저장하지 않음"
    timestamptz expires_at
    timestamptz revoked_at
  }
  social_accounts {
    bigint id PK
    bigint user_id FK
    varchar provider "KAKAO, GOOGLE"
    varchar provider_user_id "provider와 함께 유일"
  }
  tank_fish {
    bigint id PK
    bigint pet_id FK "management_type = TANK"
    varchar name
    varchar species
  }
  health_records {
    bigint id PK
    bigint pet_id FK
    date record_date "pet_id + record_date 유일"
    numeric weight_kg
    varchar meal "NORMAL, LOW, ABNORMAL"
    varchar stool "NORMAL, LOW, ABNORMAL"
    varchar fed "어항: DONE, SKIPPED"
    varchar memo
  }
  medical_records {
    bigint id PK
    bigint pet_id FK
    varchar kind "VISIT, VACCINATION, MEDICATION, TREATMENT"
    date record_date
    varchar hospital
    varchar content
    date next_date "주요 이벤트 계산에 사용"
  }
  special_records {
    bigint id PK
    bigint pet_id FK
    varchar item_key "BEAK, RESPIRATORY, WATER_QUALITY ... (docs/api.md 특수 항목 표)"
    date record_date
    varchar value
  }
  foods {
    bigint id PK
    bigint owner_id FK
    varchar name
    varchar type "FEED, TREAT"
    date expiry_date
    date opened_date
    numeric total_g
    numeric remaining_g
    numeric per_serving_g
    int times_per_day
  }
  food_pets {
    bigint food_id PK
    bigint pet_id PK
  }
  user_settings {
    bigint user_id PK
    int medical_notice_days
    int treatment_notice_days
    int expiry_notice_days
    int rebuy_notice_days
    varchar theme "PASTEL, MONOCHROME"
  }
```

## 설계 메모

- **분류와 세부 분류의 조합 규칙**은 DB 제약 대신 서비스 계층에서 검사합니다. 분류가 늘어날 때 마이그레이션 없이 규칙만 바꾸기 위해서입니다.
  - 포유류: 세부 분류 DOG 또는 CAT, 관리 방식 INDIVIDUAL
  - 조류: 세부 분류 FINCH 또는 PARROT, 관리 방식 INDIVIDUAL
  - 어류: 세부 분류 없음, 관리 방식 INDIVIDUAL 또는 TANK
- **먹이 재고는 사용자 단위**로 두고 `food_pets`로 먹는 동물을 연결합니다. 여러 마리가 같은 사료를 먹을 때 재고가 하나로 관리됩니다.
- **소진 예상일** = 남은 양 ÷ (1회 급여량 × 하루 급여 횟수 × 먹는 동물 수). 저장하지 않고 계산합니다.
- **나이로 입력**하면 출생일을 역산해 저장하고 `birth_date_estimated = true`로 표시합니다.
- 가족 공유(확장 단계)가 들어오면 `owner_id` 대신 가구(household) 단위로 소유를 옮깁니다.

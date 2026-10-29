package com.petroom.pet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PetServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final PetService service = new PetService(mock(PetRepository.class), clock);

    private PetRequest request(LocalDate birthDate, BigDecimal ageYears) {
        return new PetRequest("  코코  ", PetCategory.MAMMAL, PetSubCategory.DOG, ManagementType.INDIVIDUAL,
                "  ", birthDate, ageYears, null);
    }

    @Test
    @DisplayName("나이 1.5살은 오늘(한국 시간) 기준 548일 전 출생으로 추정한다")
    void ageToBirthDate() {
        PetProfile profile = service.toProfile(request(null, new BigDecimal("1.5")));

        assertThat(profile.birthDate()).isEqualTo(LocalDate.of(2026, 10, 29).minusDays(548));
        assertThat(profile.birthDateEstimated()).isTrue();
    }

    @Test
    @DisplayName("이름은 앞뒤 공백을 지우고, 빈 종명은 null로 저장한다")
    void normalizes() {
        PetProfile profile = service.toProfile(request(LocalDate.of(2023, 5, 1), null));

        assertThat(profile.name()).isEqualTo("코코");
        assertThat(profile.species()).isNull();
        assertThat(profile.birthDateEstimated()).isFalse();
    }

    @Test
    @DisplayName("출생일과 나이를 함께 보내면 거절한다")
    void bothBirthAndAge() {
        assertThatThrownBy(() -> service.toProfile(request(LocalDate.of(2023, 5, 1), BigDecimal.ONE)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @ParameterizedTest(name = "{0} / {1} / {2} 허용")
    @CsvSource({
            "MAMMAL, DOG, INDIVIDUAL",
            "MAMMAL, CAT, INDIVIDUAL",
            "BIRD, FINCH, INDIVIDUAL",
            "BIRD, PARROT, INDIVIDUAL",
            "FISH, , INDIVIDUAL",
            "FISH, , TANK"
    })
    void allowedClassifications(PetCategory category, PetSubCategory sub, ManagementType type) {
        PetClassification.validate(category, sub, type);
    }

    @ParameterizedTest(name = "{0} / {1} / {2} 거절")
    @CsvSource({
            "MAMMAL, , INDIVIDUAL",
            "MAMMAL, FINCH, INDIVIDUAL",
            "BIRD, DOG, INDIVIDUAL",
            "BIRD, PARROT, TANK",
            "FISH, DOG, INDIVIDUAL"
    })
    void rejectedClassifications(PetCategory category, PetSubCategory sub, ManagementType type) {
        assertThatThrownBy(() -> PetClassification.validate(category, sub, type))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PET_CLASSIFICATION);
    }
}

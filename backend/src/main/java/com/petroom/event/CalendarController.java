package com.petroom.event;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.medical.MedicalRecordRepository;
import com.petroom.medical.MedicalRecordResponse;
import com.petroom.pet.Pet;
import com.petroom.pet.PetRepository;
import com.petroom.record.HealthRecordRepository;
import com.petroom.record.HealthRecordResponse;
import com.petroom.special.SpecialRecordRepository;
import com.petroom.special.SpecialRecordResponse;
import com.petroom.user.CurrentUser;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final EventCalculator calculator;
    private final PetRepository petRepository;
    private final HealthRecordRepository healthRepository;
    private final MedicalRecordRepository medicalRepository;
    private final SpecialRecordRepository specialRepository;

    public CalendarController(EventCalculator calculator, PetRepository petRepository,
                              HealthRecordRepository healthRepository, MedicalRecordRepository medicalRepository,
                              SpecialRecordRepository specialRepository) {
        this.calculator = calculator;
        this.petRepository = petRepository;
        this.healthRepository = healthRepository;
        this.medicalRepository = medicalRepository;
        this.specialRepository = specialRepository;
    }

    /** 월 단위 요약. month 형식: YYYY-MM */
    @GetMapping
    @Transactional(readOnly = true)
    public CalendarMonthResponse month(@AuthenticationPrincipal Jwt jwt, @RequestParam String month) {
        YearMonth ym = parseMonth(month);
        Long ownerId = CurrentUser.id(jwt);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        Map<LocalDate, Set<EventType>> types = new TreeMap<>();
        Map<LocalDate, Integer> counts = new TreeMap<>();
        for (EventResponse e : calculator.between(ownerId, from, to)) {
            types.computeIfAbsent(e.date(), d -> EnumSet.noneOf(EventType.class)).add(e.type());
        }
        Set<Long> petIds = petIds(ownerId);
        if (!petIds.isEmpty()) {
            healthRepository.findAllByPetIdInAndRecordDateBetween(petIds, from, to)
                    .forEach(r -> counts.merge(r.getRecordDate(), 1, Integer::sum));
            medicalRepository.findAllByPetIdInAndRecordDateBetween(petIds, from, to)
                    .forEach(r -> counts.merge(r.getRecordDate(), 1, Integer::sum));
            specialRepository.findAllByPetIdInAndRecordDateBetween(petIds, from, to)
                    .forEach(r -> counts.merge(r.getRecordDate(), 1, Integer::sum));
        }

        TreeMap<LocalDate, Boolean> dates = new TreeMap<>();
        types.keySet().forEach(d -> dates.put(d, true));
        counts.keySet().forEach(d -> dates.put(d, true));
        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate d : dates.keySet()) {
            List<EventType> t = new ArrayList<>(types.getOrDefault(d, EnumSet.noneOf(EventType.class)));
            days.add(new CalendarDay(d, t, counts.getOrDefault(d, 0)));
        }
        return new CalendarMonthResponse(ym.toString(), days);
    }

    /** 하루 상세: 그날의 이벤트와 모든 반려동물의 기록 */
    @GetMapping("/days/{date}")
    @Transactional(readOnly = true)
    public CalendarDayResponse day(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long ownerId = CurrentUser.id(jwt);
        List<EventResponse> events = calculator.between(ownerId, date, date);
        Set<Long> petIds = petIds(ownerId);
        if (petIds.isEmpty()) {
            return new CalendarDayResponse(date, events, List.of(), List.of(), List.of());
        }
        List<HealthRecordResponse> health = healthRepository.findAllByPetIdInAndRecordDateBetween(petIds, date, date)
                .stream().map(HealthRecordResponse::from)
                .sorted(Comparator.comparing(HealthRecordResponse::petId)).toList();
        List<MedicalRecordResponse> medical = medicalRepository.findAllByPetIdInAndRecordDateBetween(petIds, date, date)
                .stream().map(MedicalRecordResponse::from)
                .sorted(Comparator.comparing(MedicalRecordResponse::id)).toList();
        List<SpecialRecordResponse> special = specialRepository.findAllByPetIdInAndRecordDateBetween(petIds, date, date)
                .stream().map(SpecialRecordResponse::from)
                .sorted(Comparator.comparing(SpecialRecordResponse::id)).toList();
        return new CalendarDayResponse(date, events, health, medical, special);
    }

    private Set<Long> petIds(Long ownerId) {
        return petRepository.findAllByOwnerIdOrderByIdAsc(ownerId).stream()
                .map(Pet::getId).collect(Collectors.toSet());
    }

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "month는 YYYY-MM 형식으로 입력해 주세요.");
        }
    }
}

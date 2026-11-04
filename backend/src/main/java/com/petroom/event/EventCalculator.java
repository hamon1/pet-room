package com.petroom.event;

import com.petroom.food.Food;
import com.petroom.food.FoodRepository;
import com.petroom.global.config.Today;
import com.petroom.medical.MedicalKind;
import com.petroom.medical.MedicalRecord;
import com.petroom.medical.MedicalRecordRepository;
import com.petroom.pet.Pet;
import com.petroom.pet.PetRepository;
import com.petroom.settings.SettingsService;
import com.petroom.settings.UserSettings;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 의료 기록의 다음 예정일과 먹이 재고에서 주요 이벤트를 계산합니다. */
@Component
public class EventCalculator {

    private final PetRepository petRepository;
    private final MedicalRecordRepository medicalRepository;
    private final FoodRepository foodRepository;
    private final SettingsService settingsService;
    private final Today today;

    public EventCalculator(PetRepository petRepository, MedicalRecordRepository medicalRepository,
                           FoodRepository foodRepository, SettingsService settingsService, Today today) {
        this.petRepository = petRepository;
        this.medicalRepository = medicalRepository;
        this.foodRepository = foodRepository;
        this.settingsService = settingsService;
        this.today = today;
    }

    /** from~to(포함) 기간에 날짜가 속한 이벤트 */
    public List<EventResponse> between(Long ownerId, LocalDate from, LocalDate to) {
        return all(ownerId, from, to).stream()
                .filter(e -> !e.date().isBefore(from) && !e.date().isAfter(to))
                .toList();
    }

    /** 지금 알려야 하는 이벤트. 유통기한·재구매는 이미 지난 것도 포함합니다. */
    public List<EventResponse> alerts(Long ownerId) {
        UserSettings s = settingsService.load(ownerId);
        LocalDate now = today.get();
        int maxNotice = Math.max(s.getMedicalNoticeDays(), s.getTreatmentNoticeDays());
        List<EventResponse> events = new ArrayList<>(medicalEvents(ownerId, now, now.plusDays(maxNotice), s, now));
        events.addAll(foodEvents(ownerId, s, now));
        return events.stream().filter(EventResponse::alert).sorted(ORDER).toList();
    }

    private List<EventResponse> all(Long ownerId, LocalDate from, LocalDate to) {
        UserSettings s = settingsService.load(ownerId);
        LocalDate now = today.get();
        List<EventResponse> events = new ArrayList<>(medicalEvents(ownerId, from, to, s, now));
        events.addAll(foodEvents(ownerId, s, now));
        events.sort(ORDER);
        return events;
    }

    private static final Comparator<EventResponse> ORDER = Comparator
            .comparing(EventResponse::date)
            .thenComparing(EventResponse::type);

    private List<EventResponse> medicalEvents(Long ownerId, LocalDate from, LocalDate to, UserSettings s,
                                              LocalDate now) {
        Map<Long, Pet> pets = petRepository.findAllByOwnerIdOrderByIdAsc(ownerId).stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));
        if (pets.isEmpty()) {
            return List.of();
        }
        return medicalRepository.findAllByPetIdInAndNextDateBetween(pets.keySet(), from, to).stream()
                .map(r -> {
                    EventType type = r.getKind().isTreatment() ? EventType.TREATMENT : EventType.MEDICAL;
                    int notice = type == EventType.TREATMENT ? s.getTreatmentNoticeDays() : s.getMedicalNoticeDays();
                    long days = ChronoUnit.DAYS.between(now, r.getNextDate());
                    boolean alert = days >= 0 && days <= notice;
                    return new EventResponse(type, r.getNextDate(), days, alert,
                            medicalTitle(pets.get(r.getPetId()), r), List.of(r.getPetId()), r.getId(), null);
                })
                .toList();
    }

    private List<EventResponse> foodEvents(Long ownerId, UserSettings s, LocalDate now) {
        List<EventResponse> events = new ArrayList<>();
        for (Food f : foodRepository.findAllByOwnerIdOrderByExpiryDateAscIdAsc(ownerId)) {
            List<Long> petIds = f.getPetIds().stream().sorted().toList();
            long untilExpiry = ChronoUnit.DAYS.between(now, f.getExpiryDate());
            events.add(new EventResponse(EventType.EXPIRY, f.getExpiryDate(), untilExpiry,
                    untilExpiry <= s.getExpiryNoticeDays(), f.getName() + " 유통기한", petIds, null, f.getId()));
            LocalDate runout = f.runoutDate(now);
            if (runout != null) {
                long untilRunout = ChronoUnit.DAYS.between(now, runout);
                events.add(new EventResponse(EventType.REBUY, runout, untilRunout,
                        untilRunout <= s.getRebuyNoticeDays(), f.getName() + " 재구매", petIds, null, f.getId()));
            }
        }
        return events;
    }

    private static String medicalTitle(Pet pet, MedicalRecord r) {
        String name = pet == null ? "" : pet.getName() + " ";
        String content = r.getContent() == null || r.getContent().isBlank() ? "" : " · " + r.getContent();
        return name + kindLabel(r.getKind()) + content;
    }

    private static String kindLabel(MedicalKind kind) {
        return switch (kind) {
            case VISIT -> "진료";
            case VACCINATION -> "접종";
            case MEDICATION -> "투약";
            case TREATMENT -> "치료";
        };
    }
}

package com.petroom.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final UserSettingsRepository repository;

    public SettingsService(UserSettingsRepository repository) {
        this.repository = repository;
    }

    /** 아직 설정을 바꾼 적이 없으면 저장하지 않고 기본값을 돌려줍니다. */
    @Transactional(readOnly = true)
    public UserSettings load(Long userId) {
        return repository.findById(userId).orElseGet(() -> UserSettings.defaults(userId));
    }

    @Transactional
    public SettingsResponse update(Long userId, SettingsRequest request) {
        UserSettings settings = repository.findById(userId).orElseGet(() -> UserSettings.defaults(userId));
        settings.update(request);
        return SettingsResponse.from(repository.save(settings));
    }
}

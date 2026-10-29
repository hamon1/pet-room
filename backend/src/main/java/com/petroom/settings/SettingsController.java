package com.petroom.settings;

import com.petroom.user.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public SettingsResponse get(@AuthenticationPrincipal Jwt jwt) {
        return SettingsResponse.from(service.load(CurrentUser.id(jwt)));
    }

    @PutMapping
    public SettingsResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SettingsRequest request) {
        return service.update(CurrentUser.id(jwt), request);
    }
}

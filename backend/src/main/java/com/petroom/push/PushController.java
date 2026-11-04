package com.petroom.push;

import com.petroom.user.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PushController {

    private final PushService service;

    public PushController(PushService service) {
        this.service = service;
    }

    /** 브라우저 구독에 쓰는 VAPID 공개키. 서버에 키가 없으면 404 PUSH_DISABLED */
    @GetMapping("/api/push/public-key")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", service.publicKey());
    }

    @PostMapping("/api/push-subscriptions")
    public ResponseEntity<Void> subscribe(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody PushSubscriptionRequest request) {
        service.subscribe(CurrentUser.id(jwt), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/push-subscriptions")
    public ResponseEntity<Void> unsubscribe(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody UnsubscribeRequest request) {
        service.unsubscribe(CurrentUser.id(jwt), request.endpoint());
        return ResponseEntity.noContent().build();
    }

    public record UnsubscribeRequest(@NotBlank String endpoint) {
    }
}

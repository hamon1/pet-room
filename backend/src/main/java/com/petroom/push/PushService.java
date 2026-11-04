package com.petroom.push;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushService {

    /**
     * 서버가 이 주소로 직접 요청을 보내므로(SSRF 위험) 알려진 브라우저 푸시 서비스만 허용합니다.
     * Chrome·Edge(FCM), Firefox(Mozilla), Safari(Apple), 구형 Edge(Windows)
     */
    static final List<String> ALLOWED_HOST_SUFFIXES = List.of(
            "fcm.googleapis.com", "push.services.mozilla.com", "push.apple.com", "notify.windows.com");

    private final PushSubscriptionRepository repository;
    private final PushProperties properties;

    public PushService(PushSubscriptionRepository repository, PushProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public String publicKey() {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.PUSH_DISABLED);
        }
        return properties.vapidPublicKey().strip();
    }

    @Transactional
    public void subscribe(Long userId, PushSubscriptionRequest request) {
        publicKey();
        validate(request);
        repository.findByEndpoint(request.endpoint())
                .ifPresentOrElse(
                        s -> s.update(userId, request.keys().p256dh(), request.keys().auth()),
                        () -> repository.save(new PushSubscription(userId, request.endpoint(),
                                request.keys().p256dh(), request.keys().auth())));
    }

    public void unsubscribe(Long userId, String endpoint) {
        repository.deleteByEndpointAndUserId(endpoint, userId);
    }

    static boolean allowedEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || uri.getUserInfo() != null) {
                return false;
            }
            String h = host.toLowerCase();
            return ALLOWED_HOST_SUFFIXES.stream().anyMatch(s -> h.equals(s) || h.endsWith("." + s));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void validate(PushSubscriptionRequest r) {
        if (!allowedEndpoint(r.endpoint())) {
            throw new BusinessException(ErrorCode.INVALID_PUSH_SUBSCRIPTION, "지원하지 않는 푸시 서비스 주소입니다.");
        }
        try {
            EcKeys.publicKey(EcKeys.b64(r.keys().p256dh()));
            if (EcKeys.b64(r.keys().auth()).length != 16) {
                throw new GeneralSecurityException("auth must be 16 bytes");
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_PUSH_SUBSCRIPTION);
        }
    }
}

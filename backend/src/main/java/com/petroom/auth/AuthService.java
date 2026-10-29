package com.petroom.auth;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.user.User;
import com.petroom.user.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.create(email, passwordEncoder.encode(request.password()), request.nickname().strip());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 이메일로 가입한 경우 유일 제약에서 걸러짐
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        return tokenService.issue(user.getId());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(User::hasPassword)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        return tokenService.issue(user.getId());
    }

    public TokenResponse refresh(RefreshRequest request) {
        return tokenService.rotate(request.refreshToken());
    }

    public void logout(RefreshRequest request) {
        tokenService.revoke(request.refreshToken());
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}

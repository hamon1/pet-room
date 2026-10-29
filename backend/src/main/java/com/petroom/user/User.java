package com.petroom.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소셜 로그인 사용자는 이메일이 없을 수 있습니다. */
    @Column(unique = true)
    private String email;

    /** 소셜 로그인 사용자는 비밀번호가 없습니다. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    private User(String email, String passwordHash, String nickname) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
    }

    public static User create(String email, String passwordHash, String nickname) {
        return new User(email, passwordHash, nickname);
    }

    public static User createSocial(String email, String nickname) {
        return new User(email, null, nickname);
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

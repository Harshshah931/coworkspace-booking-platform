package com.coworkspace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "registration_tokens", indexes = {
        @Index(name = "idx_reg_token", columnList = "token", unique = true),
        @Index(name = "idx_reg_phone", columnList = "phone")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean consumed = false;
}

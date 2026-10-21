package com.coworkspace.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@Profile("prod")
public class ProductionSecurityValidator implements InitializingBean {

    private final Environment environment;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${otp.hmac.secret}")
    private String otpHmacSecret;

    @Value("${otp.provider}")
    private String otpProvider;

    @Value("${otp.mock.expose-code:false}")
    private boolean exposeMockOtp;

    @Value("${admin.seed.email:}")
    private String adminEmail;

    @Value("${admin.seed.password:}")
    private String adminPassword;

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            throw new IllegalStateException("The dev profile cannot be active alongside the prod profile.");
        }
        requireStrongSecret(jwtSecret, "JWT_SECRET");
        requireStrongSecret(otpHmacSecret, "OTP_HMAC_SECRET");
        if (jwtSecret.equals(otpHmacSecret)) {
            throw new IllegalStateException("JWT_SECRET and OTP_HMAC_SECRET must be different in production.");
        }
        if ("MOCK".equalsIgnoreCase(otpProvider)) {
            throw new IllegalStateException("The MOCK OTP provider is available only outside production.");
        }
        if (exposeMockOtp) {
            throw new IllegalStateException("Mock OTP code exposure must be disabled in production.");
        }
        if (hasText(adminEmail) && hasText(adminPassword)) {
            requireStrongAdminPassword(adminPassword);
        }
    }

    private static void requireStrongSecret(String secret, String propertyName) {
        long distinctCharacters = secret == null ? 0 : secret.chars().distinct().count();
        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < 32
                || distinctCharacters < 16
                || secret.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalStateException(
                    propertyName + " must be a strong secret with at least 32 non-whitespace bytes in production."
            );
        }
    }

    private static void requireStrongAdminPassword(String password) {
        long distinctCharacters = password.chars().distinct().count();
        if (password.length() < 12
                || distinctCharacters < 8
                || password.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least 12 non-whitespace characters with sufficient variety when admin seeding is enabled."
            );
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

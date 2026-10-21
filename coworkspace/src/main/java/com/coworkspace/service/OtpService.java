package com.coworkspace.service;

import com.coworkspace.dto.OtpDtos.SendOtpRequest;
import com.coworkspace.dto.OtpDtos.SendOtpResponse;
import com.coworkspace.entity.*;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.exception.RateLimitExceededException;
import com.coworkspace.notification.NotificationGateway;
import com.coworkspace.notification.OtpDeliveryResult;
import com.coworkspace.repository.OtpChallengeRepository;
import com.coworkspace.repository.RegistrationTokenRepository;
import com.coworkspace.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpChallengeRepository otpChallengeRepository;
    private final RegistrationTokenRepository registrationTokenRepository;
    private final NotificationGateway notificationGateway;
    private final Clock clock;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.cooldown-seconds:60}")
    private int cooldownSeconds;

    @Value("${otp.expiration-seconds:300}")
    private int expirationSeconds;

    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${otp.hourly-limit-per-phone:3}")
    private int hourlyLimitPerPhone;

    @Value("${otp.hourly-limit-per-ip:10}")
    private int hourlyLimitPerIp;

    @Value("${otp.hmac.secret}")
    private String hmacSecret;

    @Value("${otp.provider:MOCK}")
    private String deliveryProvider;

    @Transactional
    public SendOtpResponse sendOtp(SendOtpRequest request, String clientIp) {
        String normalizedPhone = PhoneNumberUtil.normalizeAndValidate(request.getPhone());
        OtpChannel channel = request.getChannel() != null ? request.getChannel() : OtpChannel.SMS;
        OtpPurpose purpose = request.getPurpose() != null ? request.getPurpose() : OtpPurpose.AUTH;

        Instant now = clock.instant();

        // 1. Resend cooldown check
        otpChallengeRepository.findFirstByPhoneOrderByCreatedAtDesc(normalizedPhone)
                .ifPresent(latest -> {
                    long elapsedSeconds = Duration.between(latest.getCreatedAt(), now).toSeconds();
                    if (elapsedSeconds < cooldownSeconds) {
                        long remaining = cooldownSeconds - elapsedSeconds;
                        throw new RateLimitExceededException("Please wait " + remaining + " seconds before requesting another code.");
                    }
                });

        // 2. Hourly rate limit per phone
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        long phoneRequests = otpChallengeRepository.countByPhoneAndCreatedAtAfter(normalizedPhone, oneHourAgo);
        if (phoneRequests >= hourlyLimitPerPhone) {
            throw new RateLimitExceededException("Hourly OTP request limit reached for this phone number. Please try again later.");
        }

        // 3. Hourly rate limit per client IP
        if (clientIp != null && !clientIp.isBlank()) {
            long ipRequests = otpChallengeRepository.countByClientIpAndCreatedAtAfter(clientIp, oneHourAgo);
            if (ipRequests >= hourlyLimitPerIp) {
                throw new RateLimitExceededException("Too many OTP requests from your network. Please try again later.");
            }
        }

        // 4. Invalidate any previous active challenges for this phone and purpose
        otpChallengeRepository.supersedeActiveChallenges(normalizedPhone, purpose);

        // 5. Generate secure 6-digit numeric OTP and compute HMAC
        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        String otpHmac = computeHmac(otpCode);

        // 6. Save new Challenge entity
        OtpChallenge challenge = OtpChallenge.builder()
                .phone(normalizedPhone)
                .otpHmac(otpHmac)
                .purpose(purpose)
                .channel(channel)
                .clientIp(clientIp)
                .createdAt(now)
                .expiresAt(now.plus(expirationSeconds, ChronoUnit.SECONDS))
                .attempts(0)
                .consumed(false)
                .superseded(false)
                .build();
        otpChallengeRepository.save(challenge);

        // 7. Dispatch via notification gateway (mock or live)
        OtpDeliveryResult deliveryResult = notificationGateway.sendOtp(normalizedPhone, otpCode, channel, purpose);
        if (!deliveryResult.success()) {
            throw new BadRequestException(deliveryResult.errorMessage() != null ?
                    deliveryResult.errorMessage() : "Failed to deliver OTP message.");
        }

        return SendOtpResponse.builder()
                .status("OTP_SENT")
                .phone(normalizedPhone)
                .channel(channel)
                .deliveryProvider(deliveryProvider)
                .cooldownSeconds(cooldownSeconds)
                .expiresInSeconds(expirationSeconds)
                .build();
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void verifyOtp(String rawPhone, String code, OtpPurpose purpose) {
        String normalizedPhone = PhoneNumberUtil.normalizeAndValidate(rawPhone);
        Instant now = clock.instant();

        OtpChallenge challenge = otpChallengeRepository
                .findFirstByPhoneAndPurposeAndConsumedFalseAndSupersededFalseOrderByCreatedAtDesc(normalizedPhone, purpose)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

        // Acquire pessimistic write lock to prevent race conditions / parallel replay
        challenge = otpChallengeRepository.findByIdWithLock(challenge.getId())
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

        if (challenge.isConsumed() || challenge.isSuperseded() || now.isAfter(challenge.getExpiresAt())) {
            throw new BadRequestException("Invalid or expired verification code");
        }

        if (challenge.getAttempts() >= maxAttempts) {
            challenge.setSuperseded(true);
            otpChallengeRepository.save(challenge);
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new code.");
        }

        String computedHmac = computeHmac(code);
        boolean matches = MessageDigest.isEqual(
                challenge.getOtpHmac().getBytes(StandardCharsets.UTF_8),
                computedHmac.getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            int newAttempts = challenge.getAttempts() + 1;
            challenge.setAttempts(newAttempts);
            if (newAttempts >= maxAttempts) {
                challenge.setSuperseded(true);
            }
            otpChallengeRepository.save(challenge);

            int remaining = Math.max(0, maxAttempts - newAttempts);
            if (remaining == 0) {
                throw new BadRequestException("Maximum verification attempts exceeded. Please request a new code.");
            } else {
                throw new BadRequestException("Invalid verification code. " + remaining + " attempts remaining.");
            }
        }

        // Successfully verified: atomically consume
        challenge.setConsumed(true);
        otpChallengeRepository.save(challenge);
    }

    @Transactional
    public String createRegistrationToken(String normalizedPhone) {
        Instant now = clock.instant();
        String token = "reg_" + UUID.randomUUID().toString().replace("-", "") +
                       HexFormat.of().formatHex(UUID.randomUUID().toString().getBytes()).substring(0, 16);

        RegistrationToken regToken = RegistrationToken.builder()
                .token(token)
                .phone(normalizedPhone)
                .createdAt(now)
                .expiresAt(now.plus(10, ChronoUnit.MINUTES))
                .consumed(false)
                .build();

        registrationTokenRepository.save(regToken);
        return token;
    }

    @Transactional
    public RegistrationToken validateAndConsumeRegistrationToken(String token) {
        Instant now = clock.instant();

        RegistrationToken regToken = registrationTokenRepository.findByTokenWithLock(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired registration token"));

        if (regToken.isConsumed() || now.isAfter(regToken.getExpiresAt())) {
            throw new BadRequestException("Registration token has expired or has already been used");
        }

        regToken.setConsumed(true);
        return registrationTokenRepository.save(regToken);
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC for OTP verification", e);
        }
    }
}

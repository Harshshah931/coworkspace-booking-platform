package com.coworkspace.service;

import com.coworkspace.dto.OtpDtos.SendOtpRequest;
import com.coworkspace.dto.OtpDtos.SendOtpResponse;
import com.coworkspace.entity.*;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.exception.RateLimitExceededException;
import com.coworkspace.notification.MockNotificationGateway;
import com.coworkspace.repository.OtpChallengeRepository;
import com.coworkspace.repository.RegistrationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OtpServiceTest {

    static class MutableClock extends Clock {
        private Instant currentInstant = Instant.parse("2026-10-21T10:00:00Z");
        private final ZoneId zone = ZoneOffset.UTC;

        public void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        public void setInstant(Instant instant) {
            this.currentInstant = instant;
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return currentInstant; }
    }

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpChallengeRepository otpChallengeRepository;

    @Autowired
    private RegistrationTokenRepository registrationTokenRepository;

    @Autowired
    private MockNotificationGateway mockNotificationGateway;

    @Autowired
    private Clock clock;

    private MutableClock mutableClock;

    private final String testPhone = "+919876543210";
    private final String testIp = "192.168.1.100";

    @BeforeEach
    void setUp() {
        otpChallengeRepository.deleteAll();
        registrationTokenRepository.deleteAll();
        mockNotificationGateway.clear();

        mutableClock = (MutableClock) clock;
        mutableClock.setInstant(Instant.parse("2026-10-21T10:00:00Z"));

        // Ensure HMAC secret is set
        ReflectionTestUtils.setField(otpService, "hmacSecret", "test-secret-key-at-least-256-bits-long-for-hmac-sha256");
    }

    @Test
    @DisplayName("Should generate 6-digit OTP and successfully verify with valid code")
    void testSendAndVerifyOtp_Success() {
        SendOtpRequest request = new SendOtpRequest(testPhone, OtpChannel.SMS, OtpPurpose.AUTH);
        SendOtpResponse response = otpService.sendOtp(request, testIp);

        assertNotNull(response);
        assertEquals("OTP_SENT", response.getStatus());
        assertEquals(testPhone, response.getPhone());
        assertEquals(OtpChannel.SMS, response.getChannel());
        assertEquals(60, response.getCooldownSeconds());
        assertEquals(300, response.getExpiresInSeconds());

        String sentOtp = mockNotificationGateway.getLatestOtp(testPhone);
        assertNotNull(sentOtp);
        assertEquals(6, sentOtp.length());
        assertTrue(sentOtp.matches("^\\d{6}$"));

        // Verify with the dispatched code
        assertDoesNotThrow(() -> otpService.verifyOtp(testPhone, sentOtp, OtpPurpose.AUTH));

        // Once consumed, challenge cannot be reused (replay protection)
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(testPhone, sentOtp, OtpPurpose.AUTH));
        assertTrue(ex.getMessage().contains("Invalid or expired"));
    }

    @Test
    @DisplayName("Should track failed attempts and invalidate challenge after 3 failed attempts")
    void testFailedAttempts_Exhaustion() {
        SendOtpRequest request = new SendOtpRequest(testPhone, OtpChannel.SMS, OtpPurpose.AUTH);
        otpService.sendOtp(request, testIp);

        String realOtp = mockNotificationGateway.getLatestOtp(testPhone);
        String wrongOtp = realOtp.equals("111111") ? "222222" : "111111";

        // Attempt 1 fails
        BadRequestException ex1 = assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(testPhone, wrongOtp, OtpPurpose.AUTH));
        assertTrue(ex1.getMessage().contains("2 attempts remaining"));

        // Attempt 2 fails
        BadRequestException ex2 = assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(testPhone, wrongOtp, OtpPurpose.AUTH));
        assertTrue(ex2.getMessage().contains("1 attempts remaining"));

        // Attempt 3 fails -> exhausted
        BadRequestException ex3 = assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(testPhone, wrongOtp, OtpPurpose.AUTH));
        assertTrue(ex3.getMessage().contains("Maximum verification attempts exceeded"));

        // Even with the correct code, attempt is now blocked
        BadRequestException ex4 = assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(testPhone, realOtp, OtpPurpose.AUTH));
        assertTrue(ex4.getMessage().contains("Maximum verification attempts exceeded") ||
                   ex4.getMessage().contains("Invalid or expired"));
    }

    @Test
    @DisplayName("Should reject OTP verification after 5-minute expiration window")
    void testOtpExpiry_DeterministicallyWithClock() {
        SendOtpRequest request = new SendOtpRequest(testPhone, OtpChannel.SMS, OtpPurpose.AUTH);
        otpService.sendOtp(request, testIp);
        String realOtp = mockNotificationGateway.getLatestOtp(testPhone);

        // Advance clock by 5 minutes and 1 second
        mutableClock.advance(Duration.ofMinutes(5).plusSeconds(1));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(testPhone, realOtp, OtpPurpose.AUTH));
        assertTrue(ex.getMessage().contains("Invalid or expired"));
    }

    @Test
    @DisplayName("Should enforce 60-second cooldown between consecutive OTP requests")
    void testResendCooldown() {
        SendOtpRequest request = new SendOtpRequest(testPhone, OtpChannel.SMS, OtpPurpose.AUTH);
        otpService.sendOtp(request, testIp);

        // Request again after only 15 seconds
        mutableClock.advance(Duration.ofSeconds(15));
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> otpService.sendOtp(request, testIp));
        assertTrue(ex.getMessage().contains("Please wait 45 seconds before requesting another code"));

        // Advance beyond 60s total cooldown
        mutableClock.advance(Duration.ofSeconds(46));
        assertDoesNotThrow(() -> otpService.sendOtp(request, testIp));
    }

    @Test
    @DisplayName("Should enforce maximum 3 OTP requests per phone per hour")
    void testHourlyRateLimitPerPhone() {
        SendOtpRequest request = new SendOtpRequest(testPhone, OtpChannel.SMS, OtpPurpose.AUTH);

        // Request 1
        otpService.sendOtp(request, testIp);

        // Request 2 (after 61s cooldown)
        mutableClock.advance(Duration.ofSeconds(65));
        otpService.sendOtp(request, testIp);

        // Request 3 (after 61s cooldown)
        mutableClock.advance(Duration.ofSeconds(65));
        otpService.sendOtp(request, testIp);

        // Request 4 (within same hour) must be blocked
        mutableClock.advance(Duration.ofSeconds(65));
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> otpService.sendOtp(request, testIp));
        assertTrue(ex.getMessage().contains("Hourly OTP request limit reached for this phone number"));

        // Advance 1 hour -> should be allowed again
        mutableClock.advance(Duration.ofHours(1));
        assertDoesNotThrow(() -> otpService.sendOtp(request, testIp));
    }

    @Test
    @DisplayName("Should enforce hourly rate limit per IP address across multiple phones")
    void testHourlyRateLimitPerIp() {
        // Send to 10 distinct phones from the same IP (after cooldowns)
        for (int i = 1; i <= 10; i++) {
            SendOtpRequest req = new SendOtpRequest("+91900000000" + i, OtpChannel.SMS, OtpPurpose.AUTH);
            otpService.sendOtp(req, testIp);
        }

        // 11th request from the same IP should be blocked
        SendOtpRequest blockedReq = new SendOtpRequest("+919000000011", OtpChannel.SMS, OtpPurpose.AUTH);
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> otpService.sendOtp(blockedReq, testIp));
        assertTrue(ex.getMessage().contains("Too many OTP requests from your network"));
    }

    @Test
    @DisplayName("Should supersede older OTP code when a new OTP request is made")
    void testSupersedePreviousActiveChallenges() {
        SendOtpRequest request = new SendOtpRequest(testPhone, OtpChannel.SMS, OtpPurpose.AUTH);
        otpService.sendOtp(request, testIp);
        String firstOtp = mockNotificationGateway.getLatestOtp(testPhone);

        // Advance past cooldown and request new OTP
        mutableClock.advance(Duration.ofSeconds(65));
        otpService.sendOtp(request, testIp);
        String secondOtp = mockNotificationGateway.getLatestOtp(testPhone);

        assertNotEquals(firstOtp, secondOtp);

        // Attempting to verify with the first OTP should now fail because it was superseded
        assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(testPhone, firstOtp, OtpPurpose.AUTH));

        // Verifying with the second OTP succeeds
        assertDoesNotThrow(() -> otpService.verifyOtp(testPhone, secondOtp, OtpPurpose.AUTH));
    }

    @Test
    @DisplayName("Should reject and roll back when delivery provider reports failure")
    void testProviderFailureHandling() {
        mockNotificationGateway.setSimulateFailure(true);
        mockNotificationGateway.setSimulatedErrorMessage("Telecom network timeout");

        SendOtpRequest request = new SendOtpRequest(testPhone, OtpChannel.SMS, OtpPurpose.AUTH);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> otpService.sendOtp(request, testIp));
        assertEquals("Telecom network timeout", ex.getMessage());

        mockNotificationGateway.setSimulateFailure(false);
    }

    @Test
    @DisplayName("Should create single-use phone-bound registration token and prevent token reuse")
    void testRegistrationTokenLifecycle() {
        String token = otpService.createRegistrationToken(testPhone);
        assertNotNull(token);
        assertTrue(token.startsWith("reg_"));

        // First consumption succeeds
        RegistrationToken consumed = otpService.validateAndConsumeRegistrationToken(token);
        assertNotNull(consumed);
        assertEquals(testPhone, consumed.getPhone());
        assertTrue(consumed.isConsumed());

        // Second consumption fails (single-use protection)
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> otpService.validateAndConsumeRegistrationToken(token));
        assertTrue(ex.getMessage().contains("has expired or has already been used"));
    }
}

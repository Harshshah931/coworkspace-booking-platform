package com.coworkspace.controller;

import com.coworkspace.exception.ResourceNotFoundException;
import com.coworkspace.notification.MockNotificationGateway;
import com.coworkspace.repository.OtpChallengeRepository;
import com.coworkspace.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Map;

@RestController
@RequestMapping("/api/dev/otp")
@RequiredArgsConstructor
@Profile("dev")
@ConditionalOnExpression("'${otp.provider:MOCK}' == 'MOCK' and '${otp.mock.expose-code:false}' == 'true'")
public class MockOtpController {

    private final MockNotificationGateway mockNotificationGateway;
    private final OtpChallengeRepository otpChallengeRepository;
    private final Clock clock;

    @GetMapping("/latest")
    public Map<String, String> getLatestOtp(@RequestParam String phone) {
        String normalizedPhone = PhoneNumberUtil.normalizeAndValidate(phone);
        otpChallengeRepository
                .findFirstByPhoneAndConsumedFalseAndSupersededFalseOrderByCreatedAtDesc(normalizedPhone)
                .filter(latest -> clock.instant().isBefore(latest.getExpiresAt()))
                .orElseThrow(() -> new ResourceNotFoundException("No active mock OTP is available for this phone number"));
        String code = mockNotificationGateway.getLatestOtp(normalizedPhone);
        if (code == null) {
            throw new ResourceNotFoundException("No active mock OTP is available for this phone number");
        }
        return Map.of("phone", normalizedPhone, "code", code);
    }
}

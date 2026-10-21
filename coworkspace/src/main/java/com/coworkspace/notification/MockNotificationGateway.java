package com.coworkspace.notification;

import com.coworkspace.entity.OtpChannel;
import com.coworkspace.entity.OtpPurpose;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@ConditionalOnProperty(name = "otp.provider", havingValue = "MOCK", matchIfMissing = true)
public class MockNotificationGateway implements NotificationGateway {

    public record SentMessage(String recipientPhone, String otpCode, OtpChannel channel, OtpPurpose purpose) {}

    private final Map<String, String> lastSentOtpByPhone = new ConcurrentHashMap<>();
    private final List<SentMessage> sentHistory = new CopyOnWriteArrayList<>();

    @Setter
    private boolean simulateFailure = false;

    @Setter
    private String simulatedErrorMessage = "Simulated delivery provider failure";

    @Override
    public OtpDeliveryResult sendOtp(String recipientPhone, String otpCode, OtpChannel channel, OtpPurpose purpose) {
        if (simulateFailure) {
            log.warn("MockNotificationGateway simulating failure for recipient phone: {} via channel: {}", recipientPhone, channel);
            return OtpDeliveryResult.failure(simulatedErrorMessage);
        }

        lastSentOtpByPhone.put(recipientPhone, otpCode);
        sentHistory.add(new SentMessage(recipientPhone, otpCode, channel, purpose));

        String mockId = "mock-msg-" + UUID.randomUUID();
        log.info("MockNotificationGateway dispatched OTP challenge to: {} via channel: {} (mockMessageId={})",
                recipientPhone, channel, mockId);

        return OtpDeliveryResult.success(mockId);
    }

    /**
     * Test inspection helper: allows automated tests to retrieve the sent OTP for verification.
     */
    public String getLatestOtp(String recipientPhone) {
        return lastSentOtpByPhone.get(recipientPhone);
    }

    public List<SentMessage> getSentHistory() {
        return Collections.unmodifiableList(sentHistory);
    }

    public void clear() {
        lastSentOtpByPhone.clear();
        sentHistory.clear();
        simulateFailure = false;
        simulatedErrorMessage = "Simulated delivery provider failure";
    }
}

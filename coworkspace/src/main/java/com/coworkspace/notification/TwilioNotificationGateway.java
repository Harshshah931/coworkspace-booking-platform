package com.coworkspace.notification;

import com.coworkspace.entity.OtpChannel;
import com.coworkspace.entity.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "otp.provider", havingValue = "TWILIO")
public class TwilioNotificationGateway implements NotificationGateway {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String fromSmsNumber;

    @Value("${twilio.whatsapp-number:}")
    private String fromWhatsAppNumber;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public OtpDeliveryResult sendOtp(String recipientPhone, String otpCode, OtpChannel channel, OtpPurpose purpose) {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.error("Twilio credentials not configured. Please supply TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN.");
            return OtpDeliveryResult.failure("SMS/WhatsApp provider credentials missing on server");
        }

        try {
            String from = (channel == OtpChannel.WHATSAPP)
                    ? "whatsapp:" + fromWhatsAppNumber
                    : fromSmsNumber;

            String to = (channel == OtpChannel.WHATSAPP)
                    ? "whatsapp:" + recipientPhone
                    : recipientPhone;

            String messageBody = "Your CoWorkSpace verification code is: " + otpCode + ". Valid for 5 minutes. Do not share this code.";

            Map<String, String> formData = new HashMap<>();
            formData.put("From", from);
            formData.put("To", to);
            formData.put("Body", messageBody);

            String formEncoded = formData.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                              URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            String basicAuth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formEncoded))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Twilio successfully queued OTP message to {} via {}", recipientPhone, channel);
                return OtpDeliveryResult.success("twilio-ok");
            } else {
                log.warn("Twilio API returned error status: {}", response.statusCode());
                return OtpDeliveryResult.failure("Provider failed to deliver OTP. Status: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Exception while communicating with Twilio API: {}", e.getMessage());
            return OtpDeliveryResult.failure("Failed to communicate with delivery provider");
        }
    }
}

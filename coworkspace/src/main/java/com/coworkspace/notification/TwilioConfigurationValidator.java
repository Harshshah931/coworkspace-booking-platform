package com.coworkspace.notification;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "otp.provider", havingValue = "TWILIO")
public class TwilioConfigurationValidator implements InitializingBean {

    private static final Pattern ACCOUNT_SID = Pattern.compile("^AC[0-9a-fA-F]{32}$");
    private static final Pattern E164_NUMBER = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String smsSender;

    @Value("${twilio.whatsapp-number:}")
    private String whatsappSender;

    @Override
    public void afterPropertiesSet() {
        if (accountSid == null || !ACCOUNT_SID.matcher(accountSid).matches()
                || authToken == null || authToken.length() < 32
                || authToken.chars().anyMatch(Character::isWhitespace)
                || smsSender == null || !E164_NUMBER.matcher(smsSender).matches()
                || whatsappSender == null || !E164_NUMBER.matcher(whatsappSender).matches()) {
            throw new IllegalStateException(
                    "OTP_PROVIDER=TWILIO requires a valid account SID, auth token, SMS sender, and WhatsApp sender configuration."
            );
        }
    }
}

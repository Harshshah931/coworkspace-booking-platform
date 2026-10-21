package com.coworkspace.config;

import com.coworkspace.controller.MockOtpController;
import com.coworkspace.notification.TwilioConfigurationValidator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigurationValidationTest {

    @Test
    void productionRequiresStrongDistinctSigningAndOtpSecrets() {
        ProductionSecurityValidator validator = productionValidator();
        assertDoesNotThrow(validator::afterPropertiesSet);

        ReflectionTestUtils.setField(validator, "jwtSecret", "short");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);

        ReflectionTestUtils.setField(validator, "jwtSecret", "A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5p6");
        ReflectionTestUtils.setField(validator, "otpHmacSecret", "A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5p6");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void productionRejectsMockOtpAndOtpExposureButAllowsMissingAdminSeedCredentials() {
        ProductionSecurityValidator validator = productionValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5p6");
        ReflectionTestUtils.setField(validator, "otpHmacSecret", "Z9y8X7w6V5u4T3s2R1q0P9o8N7m6L5k4");
        ReflectionTestUtils.setField(validator, "adminEmail", "");
        ReflectionTestUtils.setField(validator, "adminPassword", "");
        assertDoesNotThrow(validator::afterPropertiesSet);

        ReflectionTestUtils.setField(validator, "otpProvider", "MOCK");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);

        ReflectionTestUtils.setField(validator, "otpProvider", "TWILIO");
        ReflectionTestUtils.setField(validator, "exposeMockOtp", true);
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void productionCannotBeCombinedWithDevelopmentProfile() {
        ProductionSecurityValidator validator = productionValidator("prod", "dev");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void configuredProductionAdminSeedMustUseStrongPassword() {
        ProductionSecurityValidator validator = productionValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5p6");
        ReflectionTestUtils.setField(validator, "otpHmacSecret", "Z9y8X7w6V5u4T3s2R1q0P9o8N7m6L5k4");
        ReflectionTestUtils.setField(validator, "adminEmail", "operator@example.com");
        ReflectionTestUtils.setField(validator, "adminPassword", "short");

        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void mockOtpRetrievalControllerIsDevelopmentProfileOnly() {
        Profile profile = MockOtpController.class.getAnnotation(Profile.class);
        assertTrue(profile != null && profile.value().length == 1 && "dev".equals(profile.value()[0]));
    }

    @Test
    void twilioSelectionRequiresCredentialsAndBothSenderNumbers() {
        TwilioConfigurationValidator validator = new TwilioConfigurationValidator();
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);

        ReflectionTestUtils.setField(validator, "accountSid", "AC" + "0".repeat(32));
        ReflectionTestUtils.setField(validator, "authToken", "0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(validator, "smsSender", "+14155552671");
        ReflectionTestUtils.setField(validator, "whatsappSender", "+14155552672");
        assertDoesNotThrow(validator::afterPropertiesSet);

        ReflectionTestUtils.setField(validator, "whatsappSender", "");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    private ProductionSecurityValidator productionValidator() {
        return productionValidator("prod");
    }

    private ProductionSecurityValidator productionValidator(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5p6");
        ReflectionTestUtils.setField(validator, "otpHmacSecret", "Z9y8X7w6V5u4T3s2R1q0P9o8N7m6L5k4");
        ReflectionTestUtils.setField(validator, "otpProvider", "TWILIO");
        ReflectionTestUtils.setField(validator, "exposeMockOtp", false);
        ReflectionTestUtils.setField(validator, "adminEmail", "");
        ReflectionTestUtils.setField(validator, "adminPassword", "");
        return validator;
    }
}

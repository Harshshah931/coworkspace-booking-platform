package com.coworkspace.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigurationValidationTest {

    @Test
    void productionRequiresStrongJwtSecret() {
        ProductionSecurityValidator validator = productionValidator();
        assertDoesNotThrow(validator::afterPropertiesSet);
        ReflectionTestUtils.setField(validator, "jwtSecret", "short");
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
        ReflectionTestUtils.setField(validator, "adminEmail", "operator@example.com");
        ReflectionTestUtils.setField(validator, "adminPassword", "short");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    private ProductionSecurityValidator productionValidator(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5p6");
        ReflectionTestUtils.setField(validator, "adminEmail", "");
        ReflectionTestUtils.setField(validator, "adminPassword", "");
        return validator;
    }
}

package com.coworkspace.dto;

import com.coworkspace.dto.AuthDtos.AuthResponse;
import com.coworkspace.entity.OtpChannel;
import com.coworkspace.entity.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class OtpDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendOtpRequest {
        @NotBlank(message = "Phone number is required")
        private String phone;

        private OtpChannel channel = OtpChannel.SMS;

        private OtpPurpose purpose = OtpPurpose.AUTH;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendOtpResponse {
        private String status;
        private String phone;
        private OtpChannel channel;
        private String deliveryProvider;
        private int cooldownSeconds;
        private int expiresInSeconds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyOtpRequest {
        @NotBlank(message = "Phone number is required")
        private String phone;

        @NotBlank(message = "OTP code is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit numeric code")
        private String code;

        private OtpPurpose purpose = OtpPurpose.AUTH;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyOtpResponse {
        private boolean isNewUser;
        private String phone;
        private String registrationToken;
        private AuthResponse authResponse;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneRegisterRequest {
        @NotBlank(message = "Registration token is required")
        private String registrationToken;

        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }
}

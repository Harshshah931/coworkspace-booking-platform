package com.coworkspace.notification;

public record OtpDeliveryResult(
        boolean success,
        String providerMessageId,
        String errorMessage
) {
    public static OtpDeliveryResult success(String providerMessageId) {
        return new OtpDeliveryResult(true, providerMessageId, null);
    }

    public static OtpDeliveryResult failure(String errorMessage) {
        return new OtpDeliveryResult(false, null, errorMessage);
    }
}

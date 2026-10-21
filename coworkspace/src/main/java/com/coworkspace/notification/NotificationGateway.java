package com.coworkspace.notification;

import com.coworkspace.entity.OtpChannel;
import com.coworkspace.entity.OtpPurpose;

public interface NotificationGateway {

    /**
     * Sends an OTP verification challenge to the recipient phone via the requested channel.
     *
     * @param recipientPhone normalized E.164 phone number
     * @param otpCode 6-digit one-time password
     * @param channel delivery channel (SMS or WHATSAPP)
     * @param purpose challenge purpose (AUTH or BOOKING)
     * @return delivery result with success status and error message if failed
     */
    OtpDeliveryResult sendOtp(String recipientPhone, String otpCode, OtpChannel channel, OtpPurpose purpose);
}

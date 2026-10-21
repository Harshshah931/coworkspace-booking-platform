package com.coworkspace.util;

import com.coworkspace.exception.BadRequestException;

import java.util.regex.Pattern;

public final class PhoneNumberUtil {

    // E.164 format: leading +, country code (1-3 digits), and subscriber number (total 7 to 15 digits)
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    private PhoneNumberUtil() {}

    /**
     * Normalizes a phone number into strict E.164 format and validates it.
     *
     * @param rawPhone raw user input phone string
     * @return normalized E.164 string (e.g. "+919876543210")
     * @throws BadRequestException if number is invalid or cannot be parsed as E.164
     */
    public static String normalizeAndValidate(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BadRequestException("Phone number is required");
        }

        // Remove all whitespace, hyphens, dots, parentheses
        String cleaned = rawPhone.replaceAll("[\\s\\-\\.\\(\\)]", "");

        if (cleaned.matches("^[6-9]\\d{9}$")) {
            cleaned = "+91" + cleaned;
        }

        if (!cleaned.startsWith("+")) {
            throw new BadRequestException("Enter a 10-digit Indian mobile number or an international number with country code, such as +<country code><number>");
        }

        if (!E164_PATTERN.matcher(cleaned).matches()) {
            throw new BadRequestException("Phone number is invalid. Must conform to international E.164 format (leading + followed by 7 to 15 digits)");
        }

        return cleaned;
    }
}

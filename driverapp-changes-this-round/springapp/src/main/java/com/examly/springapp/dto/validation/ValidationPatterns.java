package com.examly.springapp.dto.validation;

/**
 * Every regular expression used for validation, in one place. The Angular app uses the very same
 * patterns (see angularapp/src/app/utils/constants.ts), so a value is accepted or rejected
 * identically in the browser and on the server.
 */
public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    /** Exactly 10 digits. */
    public static final String MOBILE = "^\\d{10}$";

    /**
     * Password: 8-64 characters, no spaces, with at least one lower-case letter, one upper-case
     * letter, one digit and one special character.
     */
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,64}$";

    /** The 6 digit one-time password. */
    public static final String OTP = "^\\d{6}$";

    /** Driver licence: 5-25 letters, digits, spaces, '-' or '/'. */
    public static final String LICENSE = "^[A-Za-z0-9 \\-/]{5,25}$";

    /** Driver contact number: 10-13 digits with an optional leading '+'. */
    public static final String CONTACT = "^\\+?\\d{10,13}$";

    /** Text that contains at least one non-space character. */
    public static final String NOT_BLANK_TEXT = "(?s).*\\S.*";

    public static final String VEHICLE_TYPE = "^(Sedan|SUV|Hatchback|Truck|Van|Bike)$";
    public static final String DRIVER_STATUS = "^(Active|Inactive|On Leave)$";
    public static final String REQUEST_STATUS = "^(Pending|Approved|Rejected|Cancelled|Trip End|Closed)$";
    public static final String FEEDBACK_CATEGORY =
            "^(Driver Performance|Service Experience|Punctuality|Vehicle Condition|Other)$";
}

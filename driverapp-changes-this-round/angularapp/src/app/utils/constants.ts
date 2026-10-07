export const VEHICLE_TYPES: string[] = ['Sedan', 'SUV', 'Hatchback', 'Truck', 'Van', 'Bike'];

export const DRIVER_STATUSES: string[] = ['Active', 'Inactive', 'On Leave'];

export const REQUEST_STATUSES: string[] = ['Pending', 'Approved', 'Rejected', 'Cancelled', 'Trip End', 'Closed'];

/** Stages shown in the "Request Progression" modal (a rejected or cancelled request stops early). */
export const REQUEST_STAGES: string[] = ['Pending', 'Approved', 'Trip End', 'Closed'];

export const FEEDBACK_CATEGORIES: string[] = [
  'Driver Performance',
  'Service Experience',
  'Punctuality',
  'Vehicle Condition',
  'Other'
];

export const SENTIMENTS: string[] = ['Positive', 'Neutral', 'Negative'];

export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const MOBILE_REGEX = /^\d{10}$/;

/** The password rule. It is the same pattern the Spring Boot backend checks (ValidationPatterns.PASSWORD). */
export const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,64}$/;

/** The 6 digit one-time password sent by e-mail / SMS. */
export const OTP_REGEX = /^\d{6}$/;

/** Seconds before a new OTP may be requested (the backend enforces the same cool-down). */
export const OTP_RESEND_SECONDS = 30;

/** The password rule broken into the single checks shown as a live checklist on the sign-up forms. */
export const PASSWORD_RULES: { text: string; test: (value: string) => boolean }[] = [
  { text: '8 to 64 characters', test: (v) => v.length >= 8 && v.length <= 64 },
  { text: 'One upper-case letter (A-Z)', test: (v) => /[A-Z]/.test(v) },
  { text: 'One lower-case letter (a-z)', test: (v) => /[a-z]/.test(v) },
  { text: 'One number (0-9)', test: (v) => /\d/.test(v) },
  { text: 'One special character (e.g. @ # $ !)', test: (v) => /[^A-Za-z0-9\s]/.test(v) },
  { text: 'No spaces', test: (v) => !/\s/.test(v) }
];

package com.rhpaie.chat.service;

import java.time.LocalDate;

/**
 * Structured leave-date extraction. Null dates/duration mean "not understood"
 * (never use sentinel dates).
 */
public record DateExtractionResult(
        LocalDate startDate,
        LocalDate endDate,
        Integer durationDays,
        boolean ambiguous,
        DateExtractionSource source
) {
    public static DateExtractionResult none() {
        return new DateExtractionResult(null, null, null, false, DateExtractionSource.NONE);
    }

    public static DateExtractionResult ofRule(
            LocalDate startDate,
            LocalDate endDate,
            Integer durationDays,
            boolean ambiguous
    ) {
        return new DateExtractionResult(
                startDate,
                endDate,
                durationDays,
                ambiguous,
                DateExtractionSource.JAVA_RULE
        );
    }

    /** Recognized as date-related but too vague to commit start/end. */
    public static DateExtractionResult ofAmbiguous() {
        return new DateExtractionResult(null, null, null, true, DateExtractionSource.JAVA_RULE);
    }

    public boolean hasDates() {
        return startDate != null && endDate != null;
    }

    public boolean hasDuration() {
        return durationDays != null;
    }
}

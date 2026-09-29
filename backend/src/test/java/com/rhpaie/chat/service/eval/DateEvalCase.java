package com.rhpaie.chat.service.eval;

import java.time.LocalDate;

/**
 * One leave-date understanding case for evaluation suites.
 * Expectations describe intended HR meaning, not necessarily current parser support.
 */
public record DateEvalCase(
        String name,
        String category,
        String input,
        LocalDate expectedStartDate,
        LocalDate expectedEndDate,
        boolean shouldAskClarification,
        String origin
) {
    public DateEvalCase(
            String name,
            String category,
            String input,
            LocalDate expectedStartDate,
            LocalDate expectedEndDate,
            boolean shouldAskClarification
    ) {
        this(name, category, input, expectedStartDate, expectedEndDate, shouldAskClarification, null);
    }

    public static DateEvalCase dates(
            String name,
            String category,
            String input,
            LocalDate start,
            LocalDate end
    ) {
        return new DateEvalCase(name, category, input, start, end, false, null);
    }

    public static DateEvalCase dates(
            String name,
            String category,
            String input,
            LocalDate start,
            LocalDate end,
            String origin
    ) {
        return new DateEvalCase(name, category, input, start, end, false, origin);
    }

    public static DateEvalCase clarify(String name, String category, String input) {
        return new DateEvalCase(name, category, input, null, null, true, null);
    }

    public static DateEvalCase clarify(String name, String category, String input, String origin) {
        return new DateEvalCase(name, category, input, null, null, true, origin);
    }
}

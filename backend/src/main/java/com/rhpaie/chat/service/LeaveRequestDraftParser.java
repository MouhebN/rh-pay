package com.rhpaie.chat.service;

import com.rhpaie.chat.dto.ChatProposedAction;
import com.rhpaie.leave.entity.LeaveType;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LeaveRequestDraftParser {

    private static final Pattern ISO_RANGE = Pattern.compile(
            "(20\\d{2}-\\d{2}-\\d{2}).{0,20}?(20\\d{2}-\\d{2}-\\d{2})"
    );
    private static final Pattern ISO_SINGLE = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})");
    private static final Pattern SLASH_RANGE = Pattern.compile(
            "(\\d{1,2}/\\d{1,2}/20\\d{2}).{0,20}?(\\d{1,2}/\\d{1,2}/20\\d{2})"
    );
    private static final Pattern DU_AU = Pattern.compile(
            "du\\s+(\\d{1,2})(?:/(\\d{1,2})(?:/(20\\d{2}))?)?\\s+au\\s+(\\d{1,2})(?:/(\\d{1,2})(?:/(20\\d{2}))?)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MOTIF = Pattern.compile(
            "(?:motif|raison|parce que|pour)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    public Optional<ChatProposedAction> parse(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        LeaveType type = resolveType(message);
        LocalDate[] dates = resolveDates(message);
        if (dates == null) {
            return Optional.empty();
        }
        LocalDate start = dates[0];
        LocalDate end = dates[1];
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        String reason = resolveReason(message);
        return Optional.of(ChatProposedAction.builder()
                .type("CREATE_LEAVE_REQUEST")
                .leaveType(type.name())
                .startDate(start)
                .endDate(end)
                .reason(reason)
                .leaveRequestId(null)
                .approveLeaveRequestIds(null)
                .rejectLeaveRequestIds(null)
                .build());
    }

    private LeaveType resolveType(String message) {
        String text = normalize(message);
        if (containsAny(text, "maladie", "sick")) {
            return LeaveType.SICK_LEAVE;
        }
        if (containsAny(text, "sans solde", "unpaid", "non paye")) {
            return LeaveType.UNPAID_LEAVE;
        }
        if (containsAny(text, "maternite", "maternity")) {
            return LeaveType.MATERNITY_LEAVE;
        }
        if (containsAny(text, "autre", "other", "teletravail", "remote")) {
            return LeaveType.OTHER;
        }
        return LeaveType.PAID_LEAVE;
    }

    private LocalDate[] resolveDates(String message) {
        String raw = message.trim();
        String text = normalize(raw);
        LocalDate today = LocalDate.now();

        Matcher isoRange = ISO_RANGE.matcher(raw);
        if (isoRange.find()) {
            return new LocalDate[]{LocalDate.parse(isoRange.group(1)), LocalDate.parse(isoRange.group(2))};
        }

        Matcher slashRange = SLASH_RANGE.matcher(raw);
        if (slashRange.find()) {
            return new LocalDate[]{parseSlash(slashRange.group(1)), parseSlash(slashRange.group(2))};
        }

        Matcher duAu = DU_AU.matcher(text);
        if (duAu.find()) {
            int startDay = Integer.parseInt(duAu.group(1));
            Integer startMonth = duAu.group(2) == null ? null : Integer.parseInt(duAu.group(2));
            Integer startYear = duAu.group(3) == null ? null : Integer.parseInt(duAu.group(3));
            int endDay = Integer.parseInt(duAu.group(4));
            Integer endMonth = duAu.group(5) == null ? null : Integer.parseInt(duAu.group(5));
            Integer endYear = duAu.group(6) == null ? null : Integer.parseInt(duAu.group(6));
            int month = endMonth != null ? endMonth : (startMonth != null ? startMonth : today.getMonthValue());
            int year = endYear != null ? endYear : (startYear != null ? startYear : today.getYear());
            int startM = startMonth != null ? startMonth : month;
            int startY = startYear != null ? startYear : year;
            try {
                return new LocalDate[]{
                        LocalDate.of(startY, startM, startDay),
                        LocalDate.of(year, month, endDay)
                };
            } catch (Exception ignored) {
                // fall through
            }
        }

        Matcher isoSingle = ISO_SINGLE.matcher(raw);
        if (isoSingle.find()) {
            LocalDate day = LocalDate.parse(isoSingle.group(1));
            return new LocalDate[]{day, day};
        }

        if (containsAny(text, "demain")) {
            LocalDate day = today.plusDays(1);
            return new LocalDate[]{day, day};
        }
        if (containsAny(text, "aujourd hui", "aujourdhui")) {
            return new LocalDate[]{today, today};
        }
        if (containsAny(text, "apres demain")) {
            LocalDate day = today.plusDays(2);
            return new LocalDate[]{day, day};
        }
        if (containsAny(text, "lundi")) {
            return weekdayRange(today, java.time.DayOfWeek.MONDAY);
        }
        if (containsAny(text, "mardi")) {
            return weekdayRange(today, java.time.DayOfWeek.TUESDAY);
        }
        if (containsAny(text, "mercredi")) {
            return weekdayRange(today, java.time.DayOfWeek.WEDNESDAY);
        }
        if (containsAny(text, "jeudi")) {
            return weekdayRange(today, java.time.DayOfWeek.THURSDAY);
        }
        if (containsAny(text, "vendredi")) {
            return weekdayRange(today, java.time.DayOfWeek.FRIDAY);
        }
        return null;
    }

    private LocalDate[] weekdayRange(LocalDate today, java.time.DayOfWeek target) {
        LocalDate day = today;
        for (int i = 0; i < 8; i++) {
            if (day.getDayOfWeek() == target && !day.isBefore(today)) {
                return new LocalDate[]{day, day};
            }
            day = day.plusDays(1);
        }
        return new LocalDate[]{today, today};
    }

    private LocalDate parseSlash(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("d/M/yyyy"));
        } catch (DateTimeParseException exception) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }

    private String resolveReason(String message) {
        Matcher matcher = MOTIF.matcher(message.trim());
        if (matcher.find()) {
            String reason = matcher.group(1).trim();
            if (reason.length() > 200) {
                reason = reason.substring(0, 200);
            }
            if (!reason.isBlank()) {
                return reason;
            }
        }
        return "Demande via assistant RH";
    }

    private String normalize(String message) {
        String normalized = Normalizer.normalize(message == null ? "" : message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replace("'", " ").replace("’", " ");
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

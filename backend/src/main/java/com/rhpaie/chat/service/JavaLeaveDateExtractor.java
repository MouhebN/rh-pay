package com.rhpaie.chat.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based leave date/duration extractor (moved from LeaveDraftSlotFiller).
 * Behavior must stay identical to the pre-refactor parser.
 */
@Component
public class JavaLeaveDateExtractor implements LeaveDateExtractor {

    private static final Pattern ISO_RANGE = Pattern.compile(
            "(20\\d{2}-\\d{2}-\\d{2}).{0,20}?(20\\d{2}-\\d{2}-\\d{2})"
    );
    private static final Pattern ISO_SINGLE = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})");
    private static final Pattern SLASH_RANGE = Pattern.compile(
            "(\\d{1,2}/\\d{1,2}(?:/20\\d{2})?).{0,20}?(\\d{1,2}/\\d{1,2}(?:/20\\d{2})?)"
    );
    private static final Pattern SLASH_SINGLE = Pattern.compile(
            "(?<!\\d)(\\d{1,2})/(\\d{1,2})(?:/(20\\d{2}))?(?!\\d)"
    );
    private static final Pattern DASH_RANGE = Pattern.compile(
            "(\\d{1,2}-\\d{1,2}(?:-20\\d{2})?).{0,20}?(\\d{1,2}-\\d{1,2}(?:-20\\d{2})?)"
    );
    private static final Pattern DASH_SINGLE = Pattern.compile(
            "(?<!\\d)(\\d{1,2})-(\\d{1,2})(?:-(20\\d{2}))?(?!\\d)"
    );
    private static final String MONTH_ALT =
            "janvier|fevrier|february|mars|march|avril|april|mai|may|juin|june|"
                    + "juillet|july|aout|august|septembre|september|octobre|october|"
                    + "novembre|november|decembre|december";
    private static final Pattern DU_AU = Pattern.compile(
            "(?:du|de)\\s+(\\d{1,2})(?:/(\\d{1,2})(?:/(20\\d{2}))?)?\\s+(?:au|a|to)\\s+(\\d{1,2})(?:/(\\d{1,2})(?:/(20\\d{2}))?)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DU_AU_MONTH = Pattern.compile(
            "(?:du|de)\\s+(\\d{1,2})\\s+(?:au|a|to)\\s+(\\d{1,2})\\s+(" + MONTH_ALT + ")",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DE_MONTH_A_MONTH = Pattern.compile(
            "(?:du|de)\\s+(\\d{1,2})\\s+(" + MONTH_ALT + ")(?:\\s+(20\\d{2}))?"
                    + "\\s+(?:au|a|to)\\s+(\\d{1,2})(?:\\s+(" + MONTH_ALT + "))?(?:\\s+(20\\d{2}))?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LE_MONTH = Pattern.compile(
            "(?:le\\s+|the\\s+)?(\\d{1,2})\\s+(" + MONTH_ALT + ")(?:\\s+(20\\d{2}))?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DAYS = Pattern.compile(
            "(\\d{1,2}|deux|trois|quatre|cinq|six|sept)\\s*(?:jours?|j\\b|days?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DAYS_ONLY = Pattern.compile("^\\s*(\\d{1,2})\\s*$");
    private static final Pattern REL_DE_A = Pattern.compile(
            "(?:^|\\s)(?:from|de|du)\\s+(.+?)\\s+(?:au|a|to)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REL_JUSQUA = Pattern.compile(
            "(.+?)\\s+jusqu(?:\\s+a|e)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    /** Relative single-day offset: "dans trois jours" / "dans 3 jours". */
    private static final Pattern DANS_JOURS = Pattern.compile(
            "\\bdans\\s+(\\d{1,2}|un|une|deux|trois|quatre|cinq|six|sept)\\s+jours?\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, Integer> FRENCH_MONTHS = frenchMonths();

    @Override
    public DateExtractionResult extract(String message, LocalDate referenceDate) {
        if (message == null || message.isBlank() || referenceDate == null) {
            return DateExtractionResult.none();
        }
        if (looksAmbiguous(message)) {
            return DateExtractionResult.ofAmbiguous();
        }
        Integer days = resolveDays(message);
        LocalDate[] dates = resolveDates(message, referenceDate);
        if (dates == null && days == null) {
            return DateExtractionResult.none();
        }
        if (dates == null) {
            return DateExtractionResult.ofRule(null, null, days, false);
        }
        return DateExtractionResult.ofRule(dates[0], dates[1], days, false);
    }

    private boolean looksAmbiguous(String message) {
        String text = normalizeEnglishDateConcepts(normalize(message));
        if (containsAny(text,
                "bientot",
                "quelques jours",
                "vers la fin du mois",
                "un de ces jours",
                "la semaine prochaine",
                "semaine prochaine",
                "next week")) {
            return true;
        }
        // Week scope alone is underspecified for leave; explicit weekday + this week is not.
        // Do not treat "this weekend" as "this week".
        return hasBareThisWeekScope(text) && parseWeekday(text) == null;
    }

    /** True for "cette semaine" / "this week", false for weekend phrases. */
    private boolean hasBareThisWeekScope(String text) {
        if (containsAny(text, "cette semaine")) {
            return true;
        }
        if (!containsAny(text, "this week")) {
            return false;
        }
        return !containsAny(text, "weekend", "week end", "week-end", "weekende");
    }

    /** Package-visible for focused unit tests. */
    LocalDate[] resolveDates(String message, LocalDate today) {
        if (message == null || message.isBlank() || today == null) {
            return null;
        }
        String raw = message.trim();
        String text = normalizeEnglishDateConcepts(normalize(raw));

        Matcher isoRange = ISO_RANGE.matcher(raw);
        if (isoRange.find()) {
            return new LocalDate[]{LocalDate.parse(isoRange.group(1)), LocalDate.parse(isoRange.group(2))};
        }

        Matcher slashRange = SLASH_RANGE.matcher(raw);
        if (slashRange.find()) {
            LocalDate start = parseFlexibleSlash(slashRange.group(1), today);
            LocalDate end = parseFlexibleSlash(slashRange.group(2), today);
            if (start != null && end != null) {
                return new LocalDate[]{start, end};
            }
        }

        Matcher dashRange = DASH_RANGE.matcher(raw);
        if (dashRange.find()) {
            LocalDate start = parseFlexibleDash(dashRange.group(1), today);
            LocalDate end = parseFlexibleDash(dashRange.group(2), today);
            if (start != null && end != null) {
                return new LocalDate[]{start, end};
            }
        }

        Matcher deMonthAMonth = DE_MONTH_A_MONTH.matcher(text);
        if (deMonthAMonth.find()) {
            Integer startMonth = FRENCH_MONTHS.get(deMonthAMonth.group(2));
            Integer endMonth = deMonthAMonth.group(5) != null
                    ? FRENCH_MONTHS.get(deMonthAMonth.group(5))
                    : startMonth;
            Integer explicitStartYear = deMonthAMonth.group(3) == null
                    ? null
                    : Integer.parseInt(deMonthAMonth.group(3));
            Integer explicitEndYear = deMonthAMonth.group(6) == null
                    ? null
                    : Integer.parseInt(deMonthAMonth.group(6));
            if (startMonth != null && endMonth != null) {
                int startDay = Integer.parseInt(deMonthAMonth.group(1));
                int endDay = Integer.parseInt(deMonthAMonth.group(4));
                int startYear = explicitStartYear != null
                        ? explicitStartYear
                        : explicitEndYear != null && endMonth.equals(startMonth)
                        ? explicitEndYear
                        : resolveYearForMonth(today, startMonth);
                int endYear = explicitEndYear != null
                        ? explicitEndYear
                        : endMonth < startMonth ? startYear + 1 : startYear;
                if (explicitEndYear == null && endMonth.equals(startMonth)) {
                    endYear = startYear;
                }
                try {
                    return new LocalDate[]{
                            LocalDate.of(startYear, startMonth, startDay),
                            LocalDate.of(endYear, endMonth, endDay)
                    };
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }

        Matcher duAuMonth = DU_AU_MONTH.matcher(text);
        if (duAuMonth.find()) {
            Integer month = FRENCH_MONTHS.get(duAuMonth.group(3));
            if (month != null) {
                int startDay = Integer.parseInt(duAuMonth.group(1));
                int endDay = Integer.parseInt(duAuMonth.group(2));
                int year = resolveYearForMonth(today, month);
                try {
                    return new LocalDate[]{
                            LocalDate.of(year, month, startDay),
                            LocalDate.of(year, month, endDay)
                    };
                } catch (Exception ignored) {
                    // fall through
                }
            }
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

        Matcher leMonth = LE_MONTH.matcher(text);
        if (leMonth.find()) {
            Integer month = FRENCH_MONTHS.get(leMonth.group(2));
            if (month != null) {
                int day = Integer.parseInt(leMonth.group(1));
                int year = leMonth.group(3) != null
                        ? Integer.parseInt(leMonth.group(3))
                        : resolveYearForMonth(today, month);
                try {
                    LocalDate date = LocalDate.of(year, month, day);
                    return new LocalDate[]{date, date};
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }

        Matcher isoSingle = ISO_SINGLE.matcher(raw);
        if (isoSingle.find()) {
            LocalDate day = LocalDate.parse(isoSingle.group(1));
            return new LocalDate[]{day, day};
        }

        Matcher slashSingle = SLASH_SINGLE.matcher(raw);
        if (slashSingle.find()) {
            LocalDate day = parseDayMonth(
                    Integer.parseInt(slashSingle.group(1)),
                    Integer.parseInt(slashSingle.group(2)),
                    slashSingle.group(3) == null ? null : Integer.parseInt(slashSingle.group(3)),
                    today
            );
            if (day != null) {
                return new LocalDate[]{day, day};
            }
        }

        Matcher dashSingle = DASH_SINGLE.matcher(raw);
        if (dashSingle.find()) {
            LocalDate day = parseDayMonth(
                    Integer.parseInt(dashSingle.group(1)),
                    Integer.parseInt(dashSingle.group(2)),
                    dashSingle.group(3) == null ? null : Integer.parseInt(dashSingle.group(3)),
                    today
            );
            if (day != null) {
                return new LocalDate[]{day, day};
            }
        }

        if (containsAny(text, "deux prochains jours", "les deux prochains jours", "2 prochains jours")) {
            return new LocalDate[]{today.plusDays(1), today.plusDays(2)};
        }

        LocalDate[] relativeRange = parseRelativeRange(text, today);
        if (relativeRange != null) {
            return relativeRange;
        }

        Matcher dansJours = DANS_JOURS.matcher(text);
        if (dansJours.find()) {
            Integer offset = parseDayCount(dansJours.group(1));
            if (offset != null && offset >= 1 && offset <= 60) {
                LocalDate day = today.plusDays(offset);
                return new LocalDate[]{day, day};
            }
        }

        LocalDate[] overridden = resolveCorrectionOrNegation(text, today);
        if (overridden != null) {
            return overridden;
        }
        if (isBareNegatedAtom(text)) {
            return null;
        }

        // Relative days — strip "apres demain" before matching standalone "demain"
        String relative = text
                .replace("apres demain", "REL_APRES")
                .replace("apresdemain", "REL_APRES");
        boolean hasApresDemain = relative.contains("REL_APRES")
                || containsAny(text, "day after tomorrow");
        boolean hasDemain = relative.matches(".*\\bdemain\\b.*") || containsAny(text, "tomorrow");
        if (hasDemain && hasApresDemain && (relative.contains(" et ") || relative.contains(" and "))) {
            return new LocalDate[]{today.plusDays(1), today.plusDays(2)};
        }
        if (hasApresDemain) {
            LocalDate day = today.plusDays(2);
            return new LocalDate[]{day, day};
        }
        if (hasDemain) {
            LocalDate day = today.plusDays(1);
            return new LocalDate[]{day, day};
        }
        if (containsAny(text, "hier", "yesterday")) {
            LocalDate day = today.minusDays(1);
            return new LocalDate[]{day, day};
        }
        if (containsAny(text, "aujourd hui", "aujourdhui", "today")) {
            return new LocalDate[]{today, today};
        }

        // Explicit weekday + "this week" beats bare week-scope (never invent Mon–Fri).
        DayOfWeek weekdayThisWeek = parseWeekday(text);
        if (weekdayThisWeek != null && hasBareThisWeekScope(text)) {
            LocalDate day = weekdayInCurrentCalendarWeek(today, weekdayThisWeek);
            return new LocalDate[]{day, day};
        }

        boolean weekendNext = containsAny(text,
                "weekend prochain", "weekend prochaine", "week end prochain", "week end prochaine",
                "week-end prochain", "week-end prochaine", "next weekend", "le weekend prochain",
                "weekende prochain", "weekende prochaine");
        boolean finSemaine = containsAny(text, "fin de semaine", "en fin de semaine", "fins de semaine");
        boolean weekend = weekendNext || finSemaine || containsAny(text,
                "ce weekend", "this weekend", "le weekend", "weekend", "week end", "week-end", "weekende");
        if (weekend) {
            return weekendRange(today, weekendNext);
        }

        // Bare "cette semaine" / "this week" → ambiguous (looksAmbiguous); do not invent a range.
        if (hasBareThisWeekScope(text)) {
            return null;
        }
        // "semaine prochaine" / "next week" → ambiguous (handled in extract); no silent Mon–Fri.
        if (containsAny(text, "semaine prochaine", "next week", "la semaine prochaine")) {
            return null;
        }

        if (containsAny(text, "lundi prochain", "next monday")) {
            return weekdayRange(today.plusDays(1), DayOfWeek.MONDAY);
        }
        if (containsAny(text, "mardi prochain", "next tuesday")) {
            return weekdayRange(today.plusDays(1), DayOfWeek.TUESDAY);
        }
        if (containsAny(text, "mercredi prochain", "next wednesday")) {
            return weekdayRange(today.plusDays(1), DayOfWeek.WEDNESDAY);
        }
        if (containsAny(text, "jeudi prochain", "next thursday")) {
            return weekdayRange(today.plusDays(1), DayOfWeek.THURSDAY);
        }
        if (containsAny(text, "vendredi prochain", "next friday")) {
            return weekdayRange(today.plusDays(1), DayOfWeek.FRIDAY);
        }
        if (containsAny(text, "lundi")) {
            return weekdayRange(today, DayOfWeek.MONDAY);
        }
        if (containsAny(text, "mardi")) {
            return weekdayRange(today, DayOfWeek.TUESDAY);
        }
        if (containsAny(text, "mercredi")) {
            return weekdayRange(today, DayOfWeek.WEDNESDAY);
        }
        if (containsAny(text, "jeudi")) {
            return weekdayRange(today, DayOfWeek.THURSDAY);
        }
        if (containsAny(text, "vendredi")) {
            return weekdayRange(today, DayOfWeek.FRIDAY);
        }
        if (containsAny(text, "samedi")) {
            return weekdayRange(today, DayOfWeek.SATURDAY);
        }
        if (containsAny(text, "dimanche")) {
            return weekdayRange(today, DayOfWeek.SUNDAY);
        }
        return null;
    }

    Integer resolveDays(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String raw = message.trim();
        String softened = normalize(raw);
        // "dans N jours" is a relative calendar date, not a leave duration count.
        if (DANS_JOURS.matcher(softened).find()) {
            return null;
        }
        Matcher days = DAYS.matcher(raw);
        if (days.find()) {
            Integer value = parseDayCount(days.group(1));
            return value != null && value >= 1 && value <= 60 ? value : null;
        }
        String text = softened;
        days = DAYS.matcher(text);
        if (days.find()) {
            Integer value = parseDayCount(days.group(1));
            return value != null && value >= 1 && value <= 60 ? value : null;
        }
        if (containsAny(text, "une journee", "un jour", "1 jour", "one day")) {
            return 1;
        }
        if (DAYS_ONLY.matcher(raw).matches() && raw.trim().length() <= 2) {
            int value = Integer.parseInt(raw.trim());
            return value >= 1 && value <= 60 ? value : null;
        }
        return null;
    }

    private Integer parseDayCount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = raw.toLowerCase();
        return switch (token) {
            case "un", "une" -> 1;
            case "deux" -> 2;
            case "trois" -> 3;
            case "quatre" -> 4;
            case "cinq" -> 5;
            case "six" -> 6;
            case "sept" -> 7;
            default -> {
                try {
                    yield Integer.parseInt(token);
                } catch (NumberFormatException exception) {
                    yield null;
                }
            }
        };
    }

    /**
     * Explicit correction or negation with a replacement date.
     * Supported tails only: "non plutôt Y", "plutôt Y", "finalement Y",
     * "je voulais dire Y", "X, non Y", "pas X mais Y", "pas X, Y"
     * where X and Y are a single date atom (demain, après-demain, weekday, aujourd'hui, hier).
     * Returns null when the phrase is not one of those constructions.
     */
    private LocalDate[] resolveCorrectionOrNegation(String text, LocalDate today) {
        String[] markers = {"je voulais dire", "non plutot", "plutot", "finalement"};
        for (String marker : markers) {
            int index = text.indexOf(marker);
            if (index < 0) {
                continue;
            }
            LocalDate day = atomDate(text.substring(index + marker.length()), today);
            if (day != null) {
                return new LocalDate[]{day, day};
            }
        }
        Matcher nonFix = Pattern.compile("(.+)\\bnon\\s+(.+)$").matcher(text);
        if (nonFix.find() && containsDateAtom(nonFix.group(1))) {
            LocalDate day = atomDate(nonFix.group(2), today);
            if (day != null) {
                return new LocalDate[]{day, day};
            }
        }
        Matcher pasMais = Pattern.compile("\\bpas\\s+(.+?)\\s+mais\\s+(.+)$").matcher(text);
        if (pasMais.find() && isDateAtom(pasMais.group(1))) {
            LocalDate day = atomDate(pasMais.group(2), today);
            if (day != null) {
                return new LocalDate[]{day, day};
            }
        }
        // Commas are removed by TextNormalize.soften, so "pas lundi, mardi" arrives as "pas lundi mardi".
        Matcher pasPair = Pattern.compile("\\bpas\\s+(\\S+)\\s+(\\S+)\\s*$").matcher(text);
        if (pasPair.find() && !"mais".equals(pasPair.group(2)) && isDateAtom(pasPair.group(1))) {
            LocalDate day = atomDate(pasPair.group(2), today);
            if (day != null) {
                return new LocalDate[]{day, day};
            }
        }
        return null;
    }

    /** "pas lundi" / "non lundi" with no replacement must not keep the negated day. */
    private boolean isBareNegatedAtom(String text) {
        return text.matches("^(?:pas|non)\\s+(?:"
                + "apres[\\s-]?demain|demain|aujourdhui|aujourd hui|hier|"
                + "lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\\s*$");
    }

    private boolean containsDateAtom(String fragment) {
        if (fragment == null) {
            return false;
        }
        return fragment.matches(".*\\b(?:apres[\\s-]?demain|demain|aujourdhui|aujourd hui|hier|"
                + "lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\\b.*");
    }

    private boolean isDateAtom(String fragment) {
        return atomDate(fragment, LocalDate.of(2026, 1, 1)) != null || isAtomShape(fragment);
    }

    private boolean isAtomShape(String fragment) {
        String token = atomToken(fragment);
        return token != null && token.matches("(?:apres[\\s-]?demain|demain|aujourdhui|aujourd hui|hier|"
                + "lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)");
    }

    private LocalDate atomDate(String fragment, LocalDate today) {
        String token = atomToken(fragment);
        if (token == null) {
            return null;
        }
        return resolveAnchor(token, today, today);
    }

    private String atomToken(String fragment) {
        if (fragment == null) {
            return null;
        }
        String token = fragment.trim().replaceAll("[,.!;:?]+$", "").replaceAll("^[,.!;:?]+", "").trim();
        if (!token.matches("(?:apres[\\s-]?demain|demain|aujourdhui|aujourd hui|hier|"
                + "lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)")) {
            return null;
        }
        return token;
    }

    private LocalDate[] parseRelativeRange(String text, LocalDate today) {
        Matcher jusqua = REL_JUSQUA.matcher(text);
        if (jusqua.find()) {
            LocalDate start = resolveAnchor(jusqua.group(1), today, today);
            if (start != null) {
                LocalDate end = resolveAnchor(jusqua.group(2), today, start);
                if (end != null) {
                    return ordered(start, end);
                }
            }
        }
        Matcher deA = REL_DE_A.matcher(text);
        if (deA.find()) {
            String left = deA.group(1).trim();
            String right = deA.group(2).trim();
            // Skip pure numeric/month forms already handled above.
            if (left.matches("\\d.*") || right.matches("\\d.*")) {
                return null;
            }
            LocalDate start = resolveAnchor(left, today, today);
            if (start != null) {
                LocalDate end = resolveAnchor(right, today, start);
                if (end != null) {
                    return ordered(start, end);
                }
            }
        }
        return null;
    }

    private LocalDate resolveAnchor(String fragment, LocalDate today, LocalDate weekdayFromDate) {
        if (fragment == null || fragment.isBlank()) {
            return null;
        }
        String f = normalize(fragment).trim();
        String relative = f.replace("apres demain", "REL_APRES").replace("apresdemain", "REL_APRES");
        if (relative.contains("REL_APRES") || containsAny(f, "day after tomorrow")) {
            return today.plusDays(2);
        }
        if (relative.matches(".*\\bdemain\\b.*") || containsAny(f, "tomorrow")) {
            return today.plusDays(1);
        }
        if (containsAny(f, "hier", "yesterday")) {
            return today.minusDays(1);
        }
        if (containsAny(f, "aujourd hui", "aujourdhui", "today")) {
            return today;
        }
        DayOfWeek dow = parseWeekday(f);
        if (dow != null) {
            boolean next = containsAny(f, "prochain", "next");
            LocalDate from = next ? weekdayFromDate.plusDays(1) : weekdayFromDate;
            return weekdayFrom(from, dow);
        }
        return null;
    }

    private DayOfWeek parseWeekday(String text) {
        if (containsAny(text, "lundi", "monday")) {
            return DayOfWeek.MONDAY;
        }
        if (containsAny(text, "mardi", "tuesday")) {
            return DayOfWeek.TUESDAY;
        }
        if (containsAny(text, "mercredi", "wednesday")) {
            return DayOfWeek.WEDNESDAY;
        }
        if (containsAny(text, "jeudi", "thursday")) {
            return DayOfWeek.THURSDAY;
        }
        if (containsAny(text, "vendredi", "friday")) {
            return DayOfWeek.FRIDAY;
        }
        if (containsAny(text, "samedi", "saturday")) {
            return DayOfWeek.SATURDAY;
        }
        if (containsAny(text, "dimanche", "sunday")) {
            return DayOfWeek.SUNDAY;
        }
        return null;
    }

    private LocalDate[] ordered(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            return new LocalDate[]{end, start};
        }
        return new LocalDate[]{start, end};
    }

    private LocalDate[] weekendRange(LocalDate today, boolean forceNext) {
        LocalDate saturday;
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY) {
            saturday = forceNext ? today.plusDays(7) : today;
        } else if (dow == DayOfWeek.SUNDAY) {
            saturday = forceNext ? today.plusDays(6) : today.minusDays(1);
        } else {
            int daysUntilSaturday = DayOfWeek.SATURDAY.getValue() - dow.getValue();
            saturday = today.plusDays(daysUntilSaturday);
        }
        return new LocalDate[]{saturday, saturday.plusDays(1)};
    }

    private LocalDate weekdayInCurrentCalendarWeek(LocalDate today, DayOfWeek target) {
        LocalDate monday = today;
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.minusDays(1);
        }
        return weekdayFrom(monday, target);
    }

    private LocalDate weekdayFrom(LocalDate from, DayOfWeek target) {
        LocalDate day = from;
        for (int i = 0; i < 8; i++) {
            if (day.getDayOfWeek() == target) {
                return day;
            }
            day = day.plusDays(1);
        }
        return from;
    }

    private LocalDate[] weekdayRange(LocalDate from, DayOfWeek target) {
        LocalDate day = weekdayFrom(from, target);
        return new LocalDate[]{day, day};
    }

    private int resolveYearForMonth(LocalDate today, int month) {
        LocalDate endOfMonth = LocalDate.of(today.getYear(), month, 1).plusMonths(1).minusDays(1);
        if (endOfMonth.isBefore(today)) {
            return today.getYear() + 1;
        }
        return today.getYear();
    }

    private LocalDate parseSlash(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("d/M/yyyy"));
        } catch (DateTimeParseException exception) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }

    private LocalDate parseFlexibleSlash(String value, LocalDate today) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split("/");
        if (parts.length == 3) {
            try {
                return parseSlash(value);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        if (parts.length == 2) {
            return parseDayMonth(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), null, today);
        }
        return null;
    }

    private LocalDate parseFlexibleDash(String value, LocalDate today) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split("-");
        if (parts.length == 3) {
            return parseDayMonth(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    today
            );
        }
        if (parts.length == 2) {
            return parseDayMonth(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), null, today);
        }
        return null;
    }

    private LocalDate parseDayMonth(int day, int month, Integer year, LocalDate today) {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            return null;
        }
        int y = year != null ? year : resolveYearForMonth(today, month);
        try {
            return LocalDate.of(y, month, day);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalize(String message) {
        return TextNormalize.soften(message);
    }

    /**
     * Map high-frequency English date vocabulary onto existing French concepts
     * so relative-range and anchor resolvers stay single-path.
     */
    private String normalizeEnglishDateConcepts(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String t = text;
        // Longer phrases first
        t = t.replace("day after tomorrow", "apres demain");
        t = t.replace("the day after", "apres demain");
        t = t.replace("le jour d apres", "apres demain");
        t = t.replace("jour d apres", "apres demain");
        t = replaceWord(t, "tomorrow", "demain");
        t = replaceWord(t, "monday", "lundi");
        t = replaceWord(t, "tuesday", "mardi");
        t = replaceWord(t, "wednesday", "mercredi");
        t = replaceWord(t, "thursday", "jeudi");
        t = replaceWord(t, "friday", "vendredi");
        t = replaceWord(t, "saturday", "samedi");
        t = replaceWord(t, "sunday", "dimanche");
        return t;
    }

    private static String replaceWord(String text, String from, String to) {
        return text.replaceAll("\\b" + Pattern.quote(from) + "\\b", to);
    }

    private boolean containsAny(String text, String... needles) {
        return TextNormalize.containsAny(text, needles);
    }

    private static Map<String, Integer> frenchMonths() {
        Map<String, Integer> months = new LinkedHashMap<>();
        months.put("janvier", 1);
        months.put("fevrier", 2);
        months.put("february", 2);
        months.put("mars", 3);
        months.put("march", 3);
        months.put("avril", 4);
        months.put("april", 4);
        months.put("mai", 5);
        months.put("may", 5);
        months.put("juin", 6);
        months.put("june", 6);
        months.put("juillet", 7);
        months.put("july", 7);
        months.put("aout", 8);
        months.put("august", 8);
        months.put("septembre", 9);
        months.put("september", 9);
        months.put("octobre", 10);
        months.put("october", 10);
        months.put("novembre", 11);
        months.put("november", 11);
        months.put("decembre", 12);
        months.put("december", 12);
        return months;
    }
}

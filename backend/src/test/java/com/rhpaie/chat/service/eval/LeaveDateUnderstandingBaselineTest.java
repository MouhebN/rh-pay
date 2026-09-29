package com.rhpaie.chat.service.eval;

import com.rhpaie.chat.entity.ChatActionDraft;
import com.rhpaie.chat.service.JavaLeaveDateExtractor;
import com.rhpaie.chat.service.LeaveDraftSlotFiller;
import com.rhpaie.user.entity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step-1 leave-date NL baseline.
 * <p>
 * Fixed clock: {@link LeaveDateUnderstandingCases#REF} (2026-09-16).
 * Production parser is exercised as-is via {@link LeaveDraftSlotFiller#merge}.
 * Full-dataset scoring never fails the Maven suite; a small must-pass subset guards regressions.
 */
class LeaveDateUnderstandingBaselineTest {

    private LeaveDraftSlotFiller filler;

    @BeforeEach
    void setUp() {
        filler = new LeaveDraftSlotFiller(new JavaLeaveDateExtractor());
    }

    static Stream<DateEvalCase> mustPassCases() {
        // Curated subset already aligned with current LeaveDraftSlotFiller support
        // (reference clock 2026-09-16). Gaps stay in the full baseline report only.
        return LeaveDateUnderstandingCases.all().stream()
                .filter(c -> switch (c.name()) {
                    case "simple_demain",
                         "simple_apres_demain_hyphen",
                         "simple_apres_demain_space",
                         "simple_apres_demain_accent_space",
                         "simple_le_17_septembre",
                         "simple_17_septembre",
                         "simple_17_slash_09",
                         "simple_17_dash_09",
                         "simple_lundi_prochain",
                         "simple_vendredi_prochain",
                         "natural_demain_et_apres_demain",
                         "natural_demain_et_apres_demain_plain",
                         "natural_je_veux_prendre_demain_et_apres",
                         "natural_de_demain_a_apres_demain",
                         "natural_deux_prochains_jours",
                         "natural_demain_pour_deux_jours",
                         "natural_a_partir_demain_pendant_3",
                         "natural_demain_jusqua_vendredi",
                         "natural_de_lundi_a_mercredi",
                         "natural_du_17_au_19_septembre",
                         "natural_du_17_slash_au_19_slash",
                         "typo_demain_et_apres_deamin",
                         "typo_demian",
                         "typo_apre_demain",
                         "typo_vendredi_prochin",
                         "typo_je_veu_prendre_demain",
                         "typo_demainn",
                         "typo_septembr",
                         "mixed_tomorrow",
                         "mixed_next_friday",
                         "mixed_je_veux_off_tomorrow",
                         "ambiguous_bientot",
                         "ambiguous_quelques_jours",
                         "ambiguous_un_de_ces_jours",
                         "ambiguous_bientot_des_conges",
                         "ambiguous_la_semaine_prochaine",
                         "ambiguous_vers_fin_mois" -> true;
                    default -> false;
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mustPassCases")
    @DisplayName("must-pass leave-date regressions (fixed clock 2026-09-16)")
    void mustPass_currentSupportedBehavior(DateEvalCase testCase) {
        CaseResult result = evaluate(testCase);
        assertTrue(result.correct(), () -> failureDetail(testCase, result));
    }

    @Test
    @DisplayName("print leave-date understanding baseline (does not fail on gaps)")
    void printBaselineReport() {
        List<DateEvalCase> cases = LeaveDateUnderstandingCases.all();
        List<CaseResult> results = new ArrayList<>(cases.size());
        for (DateEvalCase testCase : cases) {
            results.add(evaluate(testCase));
        }

        int total = results.size();
        int correct = (int) results.stream().filter(CaseResult::correct).count();
        int incorrect = total - correct;
        double accuracy = total == 0 ? 0.0 : (100.0 * correct / total);

        Map<String, List<CaseResult>> byCategory = results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.testCase().category(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        StringBuilder report = new StringBuilder();
        report.append('\n');
        report.append("============================================================\n");
        report.append("Date understanding baseline (REF=").append(LeaveDateUnderstandingCases.REF).append(")\n");
        report.append("============================================================\n");
        report.append(String.format("Total: %d%n", total));
        report.append(String.format("Correct: %d%n", correct));
        report.append(String.format("Unsupported/incorrect: %d%n", incorrect));
        report.append(String.format("Accuracy: %.1f%%%n", accuracy));
        report.append("------------------------------------------------------------\n");
        report.append("By category:\n");
        for (Map.Entry<String, List<CaseResult>> entry : byCategory.entrySet()) {
            int catTotal = entry.getValue().size();
            int catOk = (int) entry.getValue().stream().filter(CaseResult::correct).count();
            report.append(String.format(
                    "  %-12s %2d/%2d (%.0f%%)%n",
                    entry.getKey(),
                    catOk,
                    catTotal,
                    catTotal == 0 ? 0.0 : 100.0 * catOk / catTotal
            ));
        }
        report.append("------------------------------------------------------------\n");
        report.append("Incorrect / unsupported:\n");
        for (CaseResult result : results) {
            if (result.correct()) {
                continue;
            }
            report.append("  - [").append(result.testCase().category()).append("] ")
                    .append(result.testCase().name())
                    .append(" | input=\"").append(result.testCase().input()).append('"')
                    .append(" | got=");
            if (result.gotStart() == null && result.gotEnd() == null) {
                report.append("none/clarification");
            } else {
                report.append(result.gotStart()).append("→").append(result.gotEnd());
            }
            report.append(" | expected=");
            if (result.testCase().shouldAskClarification()) {
                report.append("clarification");
            } else {
                report.append(result.testCase().expectedStartDate())
                        .append("→")
                        .append(result.testCase().expectedEndDate());
            }
            report.append('\n');
        }
        report.append("============================================================\n");
        System.out.println(report);
        try {
            java.nio.file.Path out = java.nio.file.Path.of("target", "leave-date-understanding-baseline.txt");
            java.nio.file.Files.createDirectories(out.getParent());
            java.nio.file.Files.writeString(out, report.toString());
        } catch (Exception exception) {
            // Report file is best-effort; console output remains the source of truth in CI logs.
        }

        // Suite stays green: this test only freezes visibility of the baseline.
        assertFalse(cases.isEmpty(), "evaluation dataset must not be empty");
        assertEquals(LeaveDateUnderstandingCases.REF, LocalDate.of(2026, 9, 16));
    }

    private CaseResult evaluate(DateEvalCase testCase) {
        ChatActionDraft draft = emptyDraft();
        filler.merge(draft, testCase.input(), LeaveDateUnderstandingCases.REF);
        LocalDate gotStart = draft.getStartDate();
        LocalDate gotEnd = draft.getEndDate();

        boolean correct;
        if (testCase.shouldAskClarification()) {
            // Ambiguous success = no confident date extraction (do not invent dates).
            correct = gotStart == null && gotEnd == null;
        } else {
            correct = testCase.expectedStartDate().equals(gotStart)
                    && testCase.expectedEndDate().equals(gotEnd);
        }
        return new CaseResult(testCase, gotStart, gotEnd, correct);
    }

    private static String failureDetail(DateEvalCase testCase, CaseResult result) {
        return "case=" + testCase.name()
                + " input=\"" + testCase.input() + '"'
                + " expected="
                + (testCase.shouldAskClarification()
                ? "clarification"
                : testCase.expectedStartDate() + "→" + testCase.expectedEndDate())
                + " got=" + result.gotStart() + "→" + result.gotEnd();
    }

    private static ChatActionDraft emptyDraft() {
        return ChatActionDraft.builder()
                .user(UserAccount.builder().id(1L).username("eval").build())
                .actionType("CREATE_LEAVE_REQUEST")
                .build();
    }

    private record CaseResult(
            DateEvalCase testCase,
            LocalDate gotStart,
            LocalDate gotEnd,
            boolean correct
    ) {
    }
}

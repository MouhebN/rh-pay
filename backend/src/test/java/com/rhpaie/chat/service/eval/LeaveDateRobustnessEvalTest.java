package com.rhpaie.chat.service.eval;

import com.rhpaie.chat.entity.ChatActionDraft;
import com.rhpaie.chat.service.JavaLeaveDateExtractor;
import com.rhpaie.chat.service.LeaveDraftSlotFiller;
import com.rhpaie.user.entity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * External robustness evaluation (MASSIVE-adapted), split into DEV and HOLDOUT.
 * <p>
 * Does not fail the Maven suite on gaps — reports accuracy and false-confident counts.
 * Production parser must not be tuned against individual HOLDOUT failures.
 * Fixed clock: {@link LeaveDateUnderstandingCases#REF} (2026-09-16).
 */
class LeaveDateRobustnessEvalTest {

    static final LocalDate REF = LeaveDateUnderstandingCases.REF;

    private LeaveDraftSlotFiller filler;

    @BeforeEach
    void setUp() {
        filler = new LeaveDraftSlotFiller(new JavaLeaveDateExtractor());
    }

    @Test
    @DisplayName("existing regression baseline is 46/46 after explicit correction and negation")
    void existingRegressionBaseline() {
        EvalReport report = score("Existing regression", LeaveDateUnderstandingCases.all());
        writeReport("target/leave-date-existing-regression.txt", report);
        System.out.println(report.text());
        assertEquals(46, report.total());
        assertEquals(46, report.correct(), "correction and negation cases are now part of the regression lock");
        assertEquals(REF, LocalDate.of(2026, 9, 16));
    }

    @Test
    @DisplayName("print DEV robustness report (does not fail on gaps)")
    void printDevRobustnessReport() {
        List<DateEvalCase> cases = LeaveDateRobustnessCaseLoader.load("leave-date-eval/robustness-dev.json");
        EvalReport report = score("New DEV robustness", cases);
        writeReport("target/leave-date-robustness-dev.txt", report);
        System.out.println(report.text());
        assertFalse(cases.isEmpty());
    }

    @Test
    @DisplayName("print HOLDOUT robustness report (do not tune production on these)")
    void printHoldoutRobustnessReport() {
        List<DateEvalCase> cases = LeaveDateRobustnessCaseLoader.load("leave-date-eval/robustness-holdout.json");
        EvalReport report = score("New HOLDOUT", cases);
        writeReport("target/leave-date-robustness-holdout.txt", report);
        System.out.println(report.text());
        assertFalse(cases.isEmpty());
    }

    @Test
    @DisplayName("combined DEV+HOLDOUT summary with false-confident metric")
    void printCombinedSummary() {
        List<DateEvalCase> existing = LeaveDateUnderstandingCases.all();
        List<DateEvalCase> dev = LeaveDateRobustnessCaseLoader.load("leave-date-eval/robustness-dev.json");
        List<DateEvalCase> holdout = LeaveDateRobustnessCaseLoader.load("leave-date-eval/robustness-holdout.json");

        EvalReport existingReport = score("Existing regression", existing);
        EvalReport devReport = score("New DEV", dev);
        EvalReport holdoutReport = score("New HOLDOUT", holdout);

        List<DateEvalCase> newAll = new ArrayList<>(dev.size() + holdout.size());
        newAll.addAll(dev);
        newAll.addAll(holdout);
        EvalReport newAllReport = score("New DEV+HOLDOUT", newAll);

        StringBuilder summary = new StringBuilder();
        summary.append('\n');
        summary.append("============================================================\n");
        summary.append("Leave-date robustness summary (REF=").append(REF).append(")\n");
        summary.append("============================================================\n");
        summary.append(String.format(
                "Existing regression:%n%d/%d = %.1f%%%n%n",
                existingReport.correct(), existingReport.total(), existingReport.accuracyPct()
        ));
        summary.append(String.format(
                "New DEV:%n%d/%d = %.1f%%%n%n",
                devReport.correct(), devReport.total(), devReport.accuracyPct()
        ));
        summary.append(String.format(
                "New HOLDOUT:%n%d/%d = %.1f%%%n%n",
                holdoutReport.correct(), holdoutReport.total(), holdoutReport.accuracyPct()
        ));
        summary.append(String.format(
                "False-confident (new cases):%n%d/%d%n%n",
                newAllReport.falseConfident(), newAllReport.total()
        ));
        summary.append("DEV categories:\n").append(devReport.categoryBlock());
        summary.append("HOLDOUT categories:\n").append(holdoutReport.categoryBlock());
        summary.append("Main failures (HOLDOUT):\n");
        for (CaseResult result : holdoutReport.results()) {
            if (!result.correct()) {
                summary.append("  - [").append(result.testCase().category()).append("] ")
                        .append(result.testCase().name())
                        .append(" | input=\"").append(result.testCase().input()).append('"')
                        .append(" | got=").append(formatGot(result))
                        .append(" | expected=").append(formatExpected(result.testCase()))
                        .append('\n');
            }
        }
        summary.append("============================================================\n");

        System.out.println(summary);
        writeReport("target/leave-date-robustness-summary.txt", summary.toString());
        assertEquals(46, existingReport.correct());
        assertFalse(dev.isEmpty());
        assertFalse(holdout.isEmpty());
    }

    private EvalReport score(String title, List<DateEvalCase> cases) {
        List<CaseResult> results = new ArrayList<>(cases.size());
        for (DateEvalCase testCase : cases) {
            results.add(evaluate(testCase));
        }
        return EvalReport.from(title, results);
    }

    private CaseResult evaluate(DateEvalCase testCase) {
        ChatActionDraft draft = emptyDraft();
        filler.merge(draft, testCase.input(), REF);
        LocalDate gotStart = draft.getStartDate();
        LocalDate gotEnd = draft.getEndDate();

        boolean correct;
        if (testCase.shouldAskClarification()) {
            correct = gotStart == null && gotEnd == null;
        } else {
            correct = testCase.expectedStartDate().equals(gotStart)
                    && testCase.expectedEndDate().equals(gotEnd);
        }

        boolean falseConfident = false;
        if (gotStart != null || gotEnd != null) {
            if (testCase.shouldAskClarification()) {
                falseConfident = true;
            } else if (!correct) {
                falseConfident = true;
            }
        }
        return new CaseResult(testCase, gotStart, gotEnd, correct, falseConfident);
    }

    private static ChatActionDraft emptyDraft() {
        return ChatActionDraft.builder()
                .user(UserAccount.builder().id(1L).username("eval").build())
                .actionType("CREATE_LEAVE_REQUEST")
                .build();
    }

    private static void writeReport(String path, EvalReport report) {
        writeReport(path, report.text());
    }

    private static void writeReport(String path, String text) {
        try {
            Path out = Path.of(path);
            Files.createDirectories(out.getParent());
            Files.writeString(out, text);
        } catch (Exception ignored) {
            // Best-effort artifact for local inspection.
        }
    }

    private static String formatGot(CaseResult result) {
        if (result.gotStart() == null && result.gotEnd() == null) {
            return "none/clarification";
        }
        return result.gotStart() + "→" + result.gotEnd();
    }

    private static String formatExpected(DateEvalCase testCase) {
        if (testCase.shouldAskClarification()) {
            return "clarification";
        }
        return testCase.expectedStartDate() + "→" + testCase.expectedEndDate();
    }

    private record CaseResult(
            DateEvalCase testCase,
            LocalDate gotStart,
            LocalDate gotEnd,
            boolean correct,
            boolean falseConfident
    ) {
    }

    private record EvalReport(
            String title,
            List<CaseResult> results,
            int total,
            int correct,
            int incorrect,
            int falseConfident,
            String text
    ) {
        static EvalReport from(String title, List<CaseResult> results) {
            int total = results.size();
            int correct = (int) results.stream().filter(CaseResult::correct).count();
            int incorrect = total - correct;
            int falseConfident = (int) results.stream().filter(CaseResult::falseConfident).count();
            double accuracy = total == 0 ? 0.0 : 100.0 * correct / total;

            Map<String, List<CaseResult>> byCategory = results.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.testCase().category(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            StringBuilder report = new StringBuilder();
            report.append('\n');
            report.append("============================================================\n");
            report.append(title).append(" (REF=").append(REF).append(")\n");
            report.append("============================================================\n");
            report.append(String.format("Total: %d%n", total));
            report.append(String.format("Correct: %d%n", correct));
            report.append(String.format("Incorrect: %d%n", incorrect));
            report.append(String.format("Accuracy: %.1f%%%n", accuracy));
            report.append(String.format("False-confident: %d%n", falseConfident));
            report.append("------------------------------------------------------------\n");
            report.append("By category:\n");
            report.append(categoryBlock(byCategory));
            report.append("------------------------------------------------------------\n");
            report.append("Incorrect / unsupported:\n");
            for (CaseResult result : results) {
                if (result.correct()) {
                    continue;
                }
                report.append("  - [").append(result.testCase().category()).append("] ")
                        .append(result.testCase().name())
                        .append(" | input=\"").append(result.testCase().input()).append('"')
                        .append(" | got=").append(formatGot(result))
                        .append(" | expected=").append(formatExpected(result.testCase()));
                if (result.falseConfident()) {
                    report.append(" | FALSE_CONFIDENT");
                }
                report.append('\n');
            }
            report.append("============================================================\n");
            return new EvalReport(title, results, total, correct, incorrect, falseConfident, report.toString());
        }

        double accuracyPct() {
            return total == 0 ? 0.0 : 100.0 * correct / total;
        }

        String categoryBlock() {
            Map<String, List<CaseResult>> byCategory = results.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.testCase().category(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
            return categoryBlock(byCategory);
        }

        private static String categoryBlock(Map<String, List<CaseResult>> byCategory) {
            StringBuilder block = new StringBuilder();
            for (Map.Entry<String, List<CaseResult>> entry : byCategory.entrySet()) {
                int catTotal = entry.getValue().size();
                int catOk = (int) entry.getValue().stream().filter(CaseResult::correct).count();
                block.append(String.format(
                        "  %-12s %2d/%2d (%.0f%%)%n",
                        entry.getKey(),
                        catOk,
                        catTotal,
                        catTotal == 0 ? 0.0 : 100.0 * catOk / catTotal
                ));
            }
            return block.toString();
        }
    }
}

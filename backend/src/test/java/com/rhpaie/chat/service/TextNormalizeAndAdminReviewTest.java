package com.rhpaie.chat.service;

import com.rhpaie.leave.dto.LeaveRequestResponse;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextNormalizeAndAdminReviewTest {

    private final AdminLeaveReviewService reviewService = new AdminLeaveReviewService(null, null, null);
    private final KeywordIntentFallback fallback = new KeywordIntentFallback();

    @Test
    void soften_fixesAcccepterTus() {
        assertEquals("accepter tous", TextNormalize.soften("acccepter tus"));
    }

    @Test
    void soften_fixesAbscenceAndAnuller() {
        assertTrue(TextNormalize.soften("annuler mon abscence").contains("absence"));
        assertTrue(TextNormalize.soften("anuller ma demande").contains("annuler"));
    }

    @Test
    void looksLikeBatchDecision_acceptsTypoAll() {
        assertTrue(reviewService.looksLikeBatchDecision("acccepter tus"));
        assertTrue(fallback.looksLikeReviewBatch("acccepter tus"));
    }

    @Test
    void parseDecision_approveAllEnglish() {
        List<LeaveRequestResponse> pending = samplePending();
        AdminLeaveReviewService.Decision decision = reviewService.parseDecision("approve all", pending);
        assertEquals(List.of(1, 2, 3), decision.approveIndexes());
        assertTrue(decision.rejectIndexes().isEmpty());
    }

    @Test
    void parseDecision_approveByName() {
        List<LeaveRequestResponse> pending = samplePending();
        AdminLeaveReviewService.Decision decision = reviewService.parseDecision("accepte Ali", pending);
        assertEquals(List.of(2), decision.approveIndexes());
    }

    @Test
    void parseDecision_approveAllExceptIndex() {
        List<LeaveRequestResponse> pending = samplePending();
        AdminLeaveReviewService.Decision decision = reviewService.parseDecision("accepte tous sauf 3", pending);
        assertEquals(List.of(1, 2), decision.approveIndexes());
        assertTrue(decision.rejectIndexes().isEmpty());
    }

    @Test
    void looksLikeCreateLeave_synonymPhrases() {
        assertTrue(fallback.looksLikeCreateLeave("je souhaite poser un conge"));
        assertTrue(fallback.looksLikeCreateLeave("prendre un conge demain"));
        assertTrue(fallback.looksLikeCreateLeave("j aimerais un conge"));
        assertTrue(fallback.looksLikeCreateLeave("request leave"));
    }

    @Test
    void looksLikeCancelLeave_synonymPhrases() {
        assertTrue(fallback.looksLikeCancelLeave("annule ma demande"));
        assertTrue(fallback.looksLikeCancelLeave("cancel my leave"));
        assertTrue(fallback.looksLikeCancelLeave("retire ma demande"));
        assertFalse(fallback.looksLikeCreateLeave("annule ma demande"));
        assertFalse(fallback.looksLikeCreateLeave("cancel my leave"));
    }

    @Test
    void parseDecision_synonymApproveAndRejectAll() {
        List<LeaveRequestResponse> pending = samplePending();
        AdminLeaveReviewService.Decision validate = reviewService.parseDecision("valider tous", pending);
        assertEquals(List.of(1, 2, 3), validate.approveIndexes());

        AdminLeaveReviewService.Decision okPour = reviewService.parseDecision("ok pour tous", pending);
        assertEquals(List.of(1, 2, 3), okPour.approveIndexes());

        AdminLeaveReviewService.Decision decline = reviewService.parseDecision("decline all", pending);
        assertEquals(List.of(1, 2, 3), decline.rejectIndexes());
        assertTrue(decline.approveIndexes().isEmpty());

        assertTrue(fallback.looksLikeReviewBatch("valider tous"));
        assertTrue(fallback.looksLikeReviewBatch("ok pour tous"));
        assertTrue(fallback.looksLikeReviewBatch("decline all"));
    }

    @Test
    void parseDecision_frenchOrdinals() {
        List<LeaveRequestResponse> pending = samplePending();
        AdminLeaveReviewService.Decision both = reviewService.parseDecision("Accepte 1 et la deuxieme", pending);
        assertEquals(List.of(1, 2), both.approveIndexes());
        assertTrue(both.unmatchedNames().isEmpty());

        AdminLeaveReviewService.Decision second = reviewService.parseDecision("accepte la deuxieme", pending);
        assertEquals(List.of(2), second.approveIndexes());
        assertTrue(reviewService.looksLikeBatchDecision("accepte la deuxieme"));
    }

    private static List<LeaveRequestResponse> samplePending() {
        return List.of(
                leave(10L, "Samir Benali"),
                leave(11L, "Ali Mansouri"),
                leave(12L, "Nora Haddad")
        );
    }

    private static LeaveRequestResponse leave(Long id, String name) {
        return LeaveRequestResponse.builder()
                .id(id)
                .employeeId(1L)
                .employeeFullName(name)
                .type(LeaveType.PAID_LEAVE)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 2))
                .totalDays(2.0)
                .reason("test")
                .status(LeaveStatus.PENDING)
                .hasDocument(false)
                .build();
    }
}

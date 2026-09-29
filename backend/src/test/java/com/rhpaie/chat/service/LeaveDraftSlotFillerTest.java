package com.rhpaie.chat.service;

import com.rhpaie.chat.entity.ChatActionDraft;
import com.rhpaie.user.entity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaveDraftSlotFillerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15); // Tuesday

    private LeaveDraftSlotFiller filler;
    private JavaLeaveDateExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JavaLeaveDateExtractor();
        filler = new LeaveDraftSlotFiller(extractor);
    }

    @Test
    void demain_isTomorrow() {
        LocalDate[] dates = extractor.resolveDates("demain", TODAY);
        assertEquals(LocalDate.of(2026, 9, 16), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 16), dates[1]);
    }

    @Test
    void apresDemain_isPlusTwo() {
        LocalDate[] dates = extractor.resolveDates("après-demain", TODAY);
        assertEquals(LocalDate.of(2026, 9, 17), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 17), dates[1]);
    }

    @Test
    void merge_threeDaysFromDemain() {
        ChatActionDraft draft = emptyDraft();
        filler.merge(draft, "3 jours à partir de demain", TODAY);
        assertTrue(filler.isComplete(draft));
        assertEquals(LocalDate.of(2026, 9, 16), draft.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 18), draft.getEndDate());
        assertEquals(3, draft.getDays());
    }

    @Test
    void merge_daysThenDemain() {
        ChatActionDraft draft = emptyDraft();
        filler.merge(draft, "3 jours", TODAY);
        assertFalse(filler.isComplete(draft));
        filler.merge(draft, "demain", TODAY);
        assertEquals(LocalDate.of(2026, 9, 16), draft.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 18), draft.getEndDate());
    }

    @Test
    void weekendProchaine_isSatSun() {
        LocalDate[] dates = extractor.resolveDates("weekend prochaine", TODAY);
        assertEquals(LocalDate.of(2026, 9, 19), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 20), dates[1]);
    }

    @Test
    void nextWeekend_isSatSun() {
        LocalDate[] dates = extractor.resolveDates("next weekend", TODAY);
        assertEquals(LocalDate.of(2026, 9, 19), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 20), dates[1]);
    }

    @Test
    void finDeSemaine_isWeekend() {
        LocalDate[] dates = extractor.resolveDates("fin de semaine", TODAY);
        assertEquals(LocalDate.of(2026, 9, 19), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 20), dates[1]);
    }

    @Test
    void semaineProchaine_isAmbiguousNoDates() {
        assertNull(extractor.resolveDates("semaine prochaine", TODAY));
        DateExtractionResult result = extractor.extract("la semaine prochaine", TODAY);
        assertTrue(result.ambiguous());
        assertNull(result.startDate());
        assertNull(result.endDate());
    }

    @Test
    void cetteSemaine_isAmbiguousNotWorkWeek() {
        assertNull(extractor.resolveDates("cette semaine", TODAY));
        DateExtractionResult result = extractor.extract("cette semaine", TODAY);
        assertTrue(result.ambiguous());
        assertNull(result.startDate());
        assertNull(result.endDate());
    }

    @Test
    void isoRange() {
        LocalDate[] dates = extractor.resolveDates("du 2026-10-20 au 2026-10-21", TODAY);
        assertEquals(LocalDate.of(2026, 10, 20), dates[0]);
        assertEquals(LocalDate.of(2026, 10, 21), dates[1]);
    }

    @Test
    void frenchMonthRange() {
        LocalDate[] dates = extractor.resolveDates("du 10 au 12 octobre", TODAY);
        assertEquals(LocalDate.of(2026, 10, 10), dates[0]);
        assertEquals(LocalDate.of(2026, 10, 12), dates[1]);
    }

    @Test
    void frenchMonthSingle() {
        LocalDate[] dates = extractor.resolveDates("le 20 octobre", TODAY);
        assertEquals(LocalDate.of(2026, 10, 20), dates[0]);
        assertEquals(LocalDate.of(2026, 10, 20), dates[1]);
    }

    @Test
    void lundi_isNextMondayIncludingTodayWeek() {
        LocalDate[] dates = extractor.resolveDates("lundi", TODAY);
        // From Tuesday 15 → Monday 21 (next occurrence not before today)
        assertEquals(LocalDate.of(2026, 9, 21), dates[0]);
    }

    @Test
    void vendredi_thisWeek() {
        LocalDate[] dates = extractor.resolveDates("vendredi", TODAY);
        assertEquals(LocalDate.of(2026, 9, 18), dates[0]);
    }

    @Test
    void samedi_thisWeek() {
        LocalDate[] dates = extractor.resolveDates("samedi", TODAY);
        assertEquals(LocalDate.of(2026, 9, 19), dates[0]);
    }

    @Test
    void nextMonday_english() {
        LocalDate[] dates = extractor.resolveDates("next monday", TODAY);
        assertEquals(LocalDate.of(2026, 9, 21), dates[0]);
    }

    @Test
    void jeVeuxUnConge_hasNoSchedulingCue() {
        assertFalse(filler.hasSchedulingCue("Je veux un congé", TODAY));
        assertNull(extractor.resolveDates("Je veux un congé", TODAY));
    }

    @Test
    void nextQuestion_echoesDaysOnly() {
        ChatActionDraft draft = emptyDraft();
        filler.merge(draft, "3 jours", TODAY);
        String question = filler.nextQuestion(draft);
        assertTrue(question.contains("3 jour"));
        assertTrue(question.contains("quelle date"));
        assertFalse(question.contains("Combien de jours souhaitez-vous, et à partir"));
    }

    @Test
    void toProposedAction_whenComplete() {
        ChatActionDraft draft = emptyDraft();
        filler.merge(draft, "du 2026-10-20 au 2026-10-21 motif vacances", TODAY);
        assertTrue(filler.isComplete(draft));
        var action = filler.toProposedAction(draft, "proposal-1");
        assertEquals("CREATE_LEAVE_REQUEST", action.type());
        assertEquals(LocalDate.of(2026, 10, 20), action.startDate());
        assertEquals(LocalDate.of(2026, 10, 21), action.endDate());
        assertNotNull(action.reason());
    }

    @Test
    void yesterday_andHier() {
        assertEquals(LocalDate.of(2026, 9, 14), extractor.resolveDates("yesterday", TODAY)[0]);
        assertEquals(LocalDate.of(2026, 9, 14), extractor.resolveDates("hier", TODAY)[0]);
    }

    @Test
    void slashDayMonth_withoutYear() {
        LocalDate[] dates = extractor.resolveDates("10/09", TODAY);
        assertEquals(LocalDate.of(2026, 9, 10), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 10), dates[1]);
    }

    @Test
    void deEnglishMonthRange() {
        LocalDate[] dates = extractor.resolveDates("de 20 september a 22 september", TODAY);
        assertEquals(LocalDate.of(2026, 9, 20), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 22), dates[1]);
    }

    @Test
    void deFrenchMonthRange_variant() {
        LocalDate[] dates = extractor.resolveDates("de 10 septembre a 11 septembre", TODAY);
        assertEquals(LocalDate.of(2026, 9, 10), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 11), dates[1]);
    }

    @Test
    void demainEtApresDemain_isTwoDayRange() {
        LocalDate[] dates = extractor.resolveDates("demain et apres demain", TODAY);
        assertEquals(LocalDate.of(2026, 9, 16), dates[0]);
        assertEquals(LocalDate.of(2026, 9, 17), dates[1]);
    }

    @Test
    void demandesFollowUpIsNotASlotAnswer() {
        assertFalse(filler.looksLikeSlotAnswer("et mes demandes ?"));
        assertTrue(filler.looksLikeSlotAnswer("demain"));
    }

    private static ChatActionDraft emptyDraft() {
        return ChatActionDraft.builder()
                .user(UserAccount.builder().id(1L).username("ali").build())
                .actionType("CREATE_LEAVE_REQUEST")
                .build();
    }
}

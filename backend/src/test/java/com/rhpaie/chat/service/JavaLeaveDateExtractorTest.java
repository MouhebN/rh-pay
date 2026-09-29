package com.rhpaie.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLeaveDateExtractorTest {

    private static final LocalDate REF = LocalDate.of(2026, 9, 16);

    private JavaLeaveDateExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JavaLeaveDateExtractor();
    }

    @Test
    void demain() {
        DateExtractionResult result = extractor.extract("demain", REF);
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 17), result.endDate());
    }

    @Test
    void apresDemain() {
        DateExtractionResult result = extractor.extract("après-demain", REF);
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
    }

    @Test
    void supportedFrenchMonthRange() {
        DateExtractionResult result = extractor.extract("du 17 septembre au 19 septembre", REF);
        assertTrue(result.hasDates());
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 19), result.endDate());
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
    }

    @Test
    void dashDate_yearless() {
        DateExtractionResult result = extractor.extract("17-09", REF);
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 17), result.endDate());
    }

    @Test
    void dashDate_withYear() {
        DateExtractionResult result = extractor.extract("17-09-2026", REF);
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
    }

    @Test
    void dashDate_invalidDay_rejected() {
        DateExtractionResult result = extractor.extract("32-09", REF);
        assertEquals(DateExtractionSource.NONE, result.source());
        assertNull(result.startDate());
    }

    @Test
    void deDemainAApresDemain() {
        DateExtractionResult result = extractor.extract("de demain à après demain", REF);
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }

    @Test
    void demainJusquaVendredi() {
        DateExtractionResult result = extractor.extract("demain jusqu'à vendredi", REF);
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }

    @Test
    void deLundiAMercredi() {
        DateExtractionResult result = extractor.extract("de lundi à mercredi", REF);
        assertEquals(LocalDate.of(2026, 9, 21), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 23), result.endDate());
    }

    @Test
    void demainPourDeuxJours_duration() {
        DateExtractionResult result = extractor.extract("demain pour deux jours", REF);
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(2, result.durationDays());
    }

    @Test
    void aPartirDeDemainPendant3Jours() {
        DateExtractionResult result = extractor.extract("à partir de demain pendant 3 jours", REF);
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(3, result.durationDays());
    }

    @Test
    void typoDemian() {
        DateExtractionResult result = extractor.extract("demian", REF);
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
    }

    @Test
    void typoVendrediProchin() {
        DateExtractionResult result = extractor.extract("vendredi prochin", REF);
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
    }

    @Test
    void typoApreDemain() {
        DateExtractionResult result = extractor.extract("apré demain", REF);
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
    }

    @Test
    void laSemaineProchaine_ambiguous() {
        DateExtractionResult result = extractor.extract("la semaine prochaine", REF);
        assertTrue(result.ambiguous());
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
        assertNull(result.startDate());
        assertNull(result.endDate());
    }

    @Test
    void noDate_returnsNone() {
        DateExtractionResult result = extractor.extract("bonjour", REF);
        assertEquals(DateExtractionSource.NONE, result.source());
        assertFalse(result.hasDates());
        assertFalse(result.hasDuration());
    }

    @Test
    void tomorrowAndTheDayAfter() {
        DateExtractionResult result = extractor.extract("tomorrow and the day after", REF);
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }

    @Test
    void fromTomorrowToFriday() {
        DateExtractionResult result = extractor.extract("from tomorrow to friday", REF);
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }

    @Test
    void fromDemainToFriday() {
        DateExtractionResult result = extractor.extract("from demain to friday", REF);
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }

    @Test
    void fromHelloToWorld_notAValidRange() {
        DateExtractionResult result = extractor.extract("from hello to world", REF);
        assertEquals(DateExtractionSource.NONE, result.source());
        assertNull(result.startDate());
        assertNull(result.endDate());
        assertFalse(result.hasDates());
    }

    @Test
    void cetteSemaine_ambiguous() {
        DateExtractionResult result = extractor.extract("cette semaine", REF);
        assertTrue(result.ambiguous());
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
        assertNull(result.startDate());
        assertNull(result.endDate());
    }

    @Test
    void thisWeek_ambiguous() {
        DateExtractionResult result = extractor.extract("this week", REF);
        assertTrue(result.ambiguous());
        assertNull(result.startDate());
        assertNull(result.endDate());
    }

    @Test
    void vendrediDeCetteSemaine_isFridayNotWeekRange() {
        DateExtractionResult result = extractor.extract("vendredi de cette semaine", REF);
        assertFalse(result.ambiguous());
        assertEquals(DateExtractionSource.JAVA_RULE, result.source());
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }

    @Test
    void vendrediCetteSemaine_isFridayNotWeekRange() {
        DateExtractionResult result = extractor.extract("vendredi cette semaine", REF);
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }

    @Test
    void lundiCetteSemaine_isMondayOfCurrentWeekEvenIfPast() {
        DateExtractionResult result = extractor.extract("lundi cette semaine", REF);
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 14), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 14), result.endDate());
    }

    @Test
    void laSemaineProchaine_remainsAmbiguous() {
        DateExtractionResult result = extractor.extract("la semaine prochaine", REF);
        assertTrue(result.ambiguous());
        assertNull(result.startDate());
        assertNull(result.endDate());
    }

    @Test
    void dansTroisJours() {
        DateExtractionResult result = extractor.extract("dans trois jours", REF);
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 19), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 19), result.endDate());
        assertNull(result.durationDays());
    }

    @Test
    void dans3Jours() {
        DateExtractionResult result = extractor.extract("dans 3 jours", REF);
        assertEquals(LocalDate.of(2026, 9, 19), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 19), result.endDate());
        assertNull(result.durationDays());
    }

    @Test
    void correctionDemainNonPlutotVendredi() {
        DateExtractionResult result = extractor.extract("je veux demain, non plutôt vendredi", REF);
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }

    @Test
    void correctionDemainNonVendredi() {
        DateExtractionResult result = extractor.extract("demain, non vendredi", REF);
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
    }

    @Test
    void correctionFinalementVendredi() {
        DateExtractionResult result = extractor.extract("finalement vendredi", REF);
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
    }

    @Test
    void correctionJeVoulaisDireVendredi() {
        DateExtractionResult result = extractor.extract("je voulais dire vendredi", REF);
        assertEquals(LocalDate.of(2026, 9, 18), result.startDate());
    }

    @Test
    void negationPasLundiMardi() {
        DateExtractionResult result = extractor.extract("pas lundi, mardi", REF);
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 22), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 22), result.endDate());
    }

    @Test
    void negationPasLundiMaisMardi() {
        DateExtractionResult result = extractor.extract("pas lundi mais mardi", REF);
        assertEquals(LocalDate.of(2026, 9, 22), result.startDate());
    }

    @Test
    void negationNonLundiPlutotMardi() {
        DateExtractionResult result = extractor.extract("non lundi, plutôt mardi", REF);
        assertEquals(LocalDate.of(2026, 9, 22), result.startDate());
    }

    @Test
    void bareNegationDoesNotKeepRejectedDay() {
        DateExtractionResult result = extractor.extract("pas lundi", REF);
        assertNull(result.startDate());
        assertFalse(result.hasDates());
    }

    @Test
    void dashDayMonthYearUnchanged() {
        DateExtractionResult result = extractor.extract("10-05-2027", REF);
        assertEquals(LocalDate.of(2027, 5, 10), result.startDate());
        assertEquals(LocalDate.of(2027, 5, 10), result.endDate());
    }

    @Test
    void fullMonthYearRangeKeepsBothDays() {
        DateExtractionResult march = extractor.extract("du 2 mars 2027 au 3 mars 2027", REF);
        assertEquals(LocalDate.of(2027, 3, 2), march.startDate());
        assertEquals(LocalDate.of(2027, 3, 3), march.endDate());

        DateExtractionResult april = extractor.extract("Je veux un congé du 10 avril 2027 au 11 avril 2027", REF);
        assertEquals(LocalDate.of(2027, 4, 10), april.startDate());
        assertEquals(LocalDate.of(2027, 4, 11), april.endDate());
    }

    @Test
    void yearOnTheEndSideAppliesToTheSameMonth() {
        DateExtractionResult result = extractor.extract("du 2 mars au 3 mars 2027", REF);
        assertEquals(LocalDate.of(2027, 3, 2), result.startDate());
        assertEquals(LocalDate.of(2027, 3, 3), result.endDate());
    }

    @Test
    void numericDayRangeWithoutMonthStaysInTheReferenceMonth() {
        DateExtractionResult result = extractor.extract("du 10 au 12", REF);
        assertEquals(LocalDate.of(2026, 9, 10), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 12), result.endDate());
    }

    @Test
    void demainEtLeJourDApres() {
        DateExtractionResult result = extractor.extract("demain et le jour d'après", REF);
        assertFalse(result.ambiguous());
        assertEquals(LocalDate.of(2026, 9, 17), result.startDate());
        assertEquals(LocalDate.of(2026, 9, 18), result.endDate());
    }
}

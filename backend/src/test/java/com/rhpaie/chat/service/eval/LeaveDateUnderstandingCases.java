package com.rhpaie.chat.service.eval;

import java.time.LocalDate;
import java.util.List;

/**
 * Frozen leave-date NL dataset. Reference clock for all expectations: {@link #REF}.
 */
public final class LeaveDateUnderstandingCases {

    /** Fixed "today" for the entire baseline (Wednesday). */
    public static final LocalDate REF = LocalDate.of(2026, 9, 16);

    private static final LocalDate D17 = LocalDate.of(2026, 9, 17);
    private static final LocalDate D18 = LocalDate.of(2026, 9, 18);
    private static final LocalDate D19 = LocalDate.of(2026, 9, 19);
    private static final LocalDate D21 = LocalDate.of(2026, 9, 21);
    private static final LocalDate D22 = LocalDate.of(2026, 9, 22);
    private static final LocalDate D23 = LocalDate.of(2026, 9, 23);
    private static final LocalDate D25 = LocalDate.of(2026, 9, 25);

    private LeaveDateUnderstandingCases() {
    }

    public static List<DateEvalCase> all() {
        return List.of(
                // --- 1. simple ---
                DateEvalCase.dates("simple_demain", "simple", "demain", D17, D17),
                DateEvalCase.dates("simple_apres_demain_hyphen", "simple", "après-demain", D18, D18),
                DateEvalCase.dates("simple_apres_demain_space", "simple", "apres demain", D18, D18),
                DateEvalCase.dates("simple_apres_demain_accent_space", "simple", "après demain", D18, D18),
                DateEvalCase.dates("simple_le_17_septembre", "simple", "le 17 septembre", D17, D17),
                DateEvalCase.dates("simple_17_septembre", "simple", "17 septembre", D17, D17),
                DateEvalCase.dates("simple_17_slash_09", "simple", "17/09", D17, D17),
                DateEvalCase.dates("simple_17_dash_09", "simple", "17-09", D17, D17),
                DateEvalCase.dates("simple_lundi_prochain", "simple", "lundi prochain", D21, D21),
                // On Wed 16, "vendredi prochain" = upcoming Friday this week (parser + common FR reading).
                DateEvalCase.dates("simple_vendredi_prochain", "simple", "vendredi prochain", D18, D18),

                // --- 2. natural multi-date ---
                DateEvalCase.dates("natural_demain_et_apres_demain", "natural",
                        "demain et après demain", D17, D18),
                DateEvalCase.dates("natural_demain_et_apres_demain_plain", "natural",
                        "demain et apres demain", D17, D18),
                DateEvalCase.dates("natural_je_veux_prendre_demain_et_apres", "natural",
                        "je veux prendre demain et après-demain", D17, D18),
                DateEvalCase.dates("natural_de_demain_a_apres_demain", "natural",
                        "de demain à après demain", D17, D18),
                DateEvalCase.dates("natural_deux_prochains_jours", "natural",
                        "je veux les deux prochains jours", D17, D18),
                DateEvalCase.dates("natural_demain_pour_deux_jours", "natural",
                        "je prends demain pour deux jours", D17, D18),
                DateEvalCase.dates("natural_a_partir_demain_pendant_3", "natural",
                        "à partir de demain pendant 3 jours", D17, D19),
                DateEvalCase.dates("natural_demain_jusqua_vendredi", "natural",
                        "demain jusqu'à vendredi", D17, D18),
                DateEvalCase.dates("natural_de_lundi_a_mercredi", "natural",
                        "de lundi à mercredi", D21, D23),
                DateEvalCase.dates("natural_du_17_au_19_septembre", "natural",
                        "du 17 septembre au 19 septembre", D17, D19),
                DateEvalCase.dates("natural_du_17_slash_au_19_slash", "natural",
                        "du 17/09 au 19/09", D17, D19),

                // --- 3. typo / noisy ---
                DateEvalCase.dates("typo_demain_et_apres_deamin", "typo",
                        "demain et apres deamin", D17, D18),
                DateEvalCase.dates("typo_demian", "typo", "demian", D17, D17),
                DateEvalCase.dates("typo_apre_demain", "typo", "apré demain", D18, D18),
                DateEvalCase.dates("typo_vendredi_prochin", "typo", "vendredi prochin", D18, D18),
                DateEvalCase.dates("typo_je_veu_prendre_demain", "typo",
                        "je veu prendre demain", D17, D17),
                DateEvalCase.dates("typo_demainn", "typo",
                        "demainn", D17, D17),
                DateEvalCase.dates("typo_septembr", "typo", "le 17 septembr", D17, D17),

                // --- 4. mixed FR/EN ---
                DateEvalCase.dates("mixed_tomorrow", "mixed", "tomorrow", D17, D17),
                DateEvalCase.dates("mixed_tomorrow_and_day_after", "mixed",
                        "tomorrow and the day after", D17, D18),
                DateEvalCase.dates("mixed_tomorrow_jusqua_vendredi", "mixed",
                        "tomorrow jusqu'à vendredi", D17, D18),
                DateEvalCase.dates("mixed_from_tomorrow_to_friday", "mixed",
                        "from tomorrow to friday", D17, D18),
                DateEvalCase.dates("mixed_je_veux_off_tomorrow", "mixed",
                        "je veux off tomorrow", D17, D17),
                DateEvalCase.dates("mixed_from_demain_to_friday", "mixed",
                        "from demain to friday", D17, D18),
                DateEvalCase.dates("mixed_next_friday", "mixed", "next friday", D18, D18),

                // --- 5. correction / conversational ---
                DateEvalCase.dates("correction_pas_demain_mais_apres", "correction",
                        "pas demain mais après demain", D18, D18),
                DateEvalCase.dates("correction_non_plutot_vendredi", "correction",
                        "non plutôt vendredi", D18, D18),
                DateEvalCase.dates("correction_finalement_apres_demain", "correction",
                        "finalement après demain", D18, D18),
                DateEvalCase.dates("correction_demain_non_plutot_vendredi", "correction",
                        "je veux demain, non plutôt vendredi", D18, D18),
                DateEvalCase.dates("correction_pas_lundi_mardi", "correction",
                        "pas lundi, mardi", D22, D22),

                // --- 6. ambiguous → clarification / no confident date ---
                // "la semaine prochaine" is listed as ambiguous for this baseline: prefer ask, not silent week range.
                DateEvalCase.clarify("ambiguous_bientot", "ambiguous", "bientôt"),
                DateEvalCase.clarify("ambiguous_quelques_jours", "ambiguous", "quelques jours"),
                DateEvalCase.clarify("ambiguous_vers_fin_mois", "ambiguous", "vers la fin du mois"),
                DateEvalCase.clarify("ambiguous_la_semaine_prochaine", "ambiguous", "la semaine prochaine"),
                DateEvalCase.clarify("ambiguous_un_de_ces_jours", "ambiguous", "un de ces jours"),
                DateEvalCase.clarify("ambiguous_bientot_des_conges", "ambiguous", "je veux un congé bientôt")
        );
    }
}

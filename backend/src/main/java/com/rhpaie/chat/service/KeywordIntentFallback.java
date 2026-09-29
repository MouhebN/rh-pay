package com.rhpaie.chat.service;

import com.rhpaie.chat.context.ReadFollowUpResolver;
import com.rhpaie.chat.dto.IntentPrediction;
import com.rhpaie.chat.entity.ChatIntent;
import org.springframework.stereotype.Component;

@Component
public class KeywordIntentFallback {

    public IntentPrediction predict(String message) {
        String text = ReadFollowUpResolver.stripInstructionPrefix(message);
        if (text.isBlank()) {
            return new IntentPrediction(ChatIntent.OUT_OF_SCOPE.name(), 0.2);
        }

        if (TextNormalize.containsAny(text, "bitcoin", "meteo", "football", "recette", "blague", "politique")) {
            return new IntentPrediction(ChatIntent.OUT_OF_SCOPE.name(), 0.9);
        }

        // Cancel before create cues that might also match "demande".
        if (looksLikeCancelLeave(message)) {
            return new IntentPrediction(ChatIntent.CANCEL_LEAVE_REQUEST.name(), 0.92);
        }

        if (looksLikeReviewBatch(message)) {
            return new IntentPrediction(ChatIntent.REVIEW_LEAVE_BATCH.name(), 0.9);
        }

        // Write-action before FAQ "comment demander" and generic congé.
        if (looksLikeCreateLeave(message)) {
            return new IntentPrediction(ChatIntent.CREATE_LEAVE_REQUEST.name(), 0.9);
        }

        if (TextNormalize.containsAny(text, "comment demander", "comment poser", "comment creer", "comment faire une demande",
                "comment valider", "piece jointe", "justificatif", "procedure de conge", "ou demander",
                "teletravail", "travail a distance")) {
            return new IntentPrediction(ChatIntent.FAQ_LEAVE.name(), 0.82);
        }
        if (TextNormalize.containsAny(text, "comment calculer", "comment generer", "comment exporter", "comment finaliser",
                "variables de paie", "processus de paie", "etapes du calcul")) {
            return new IntentPrediction(ChatIntent.FAQ_PAYROLL.name(), 0.82);
        }

        if (TextNormalize.containsAny(text, "calendrier", "planning des conges", "planning des absences",
                "qui est en conge", "qui est absent", "absences cette semaine", "absences du mois")) {
            return new IntentPrediction(ChatIntent.LEAVE_CALENDAR.name(), 0.84);
        }
        if (TextNormalize.containsAny(text, "mes salaires", "mon salaire", "calculs de salaire", "mon net du mois",
                "historique salarial", "ma remuneration")) {
            return new IntentPrediction(ChatIntent.MY_SALARIES.name(), 0.84);
        }
        if (TextNormalize.containsAny(text, "document de sortie", "documents de sortie", "solde de tout compte",
                "attestation de travail", "certificat de travail", "offboarding", "papiers de depart")) {
            return new IntentPrediction(ChatIntent.MY_EXIT_DOCUMENTS.name(), 0.84);
        }
        if (TextNormalize.containsAny(text, "departs prevus", "sorties a venir", "qui quitte", "departs programmes",
                "suivi des sorties", "employee exits")) {
            return new IntentPrediction(ChatIntent.EXIT_SUMMARY.name(), 0.84);
        }

        if (!text.startsWith("comment") && !TextNormalize.containsAny(text, "comment ", "depose")
                && TextNormalize.containsAny(text, "en attente", "pending", "a valider", "demandes pending",
                "demandes de conge", "demande de conge", "liste des conges",
                "donne moi les demandes", "donne moi les demande",
                "montre les demandes", "montre les demande")) {
            return new IntentPrediction(ChatIntent.PENDING_LEAVES.name(), 0.82);
        }
        if (TextNormalize.containsAny(text, "expire", "expirent", "echeance", "bientot fin")) {
            return new IntentPrediction(ChatIntent.EXPIRING_CONTRACTS.name(), 0.8);
        }
        if (TextNormalize.containsAny(text, "statut paie", "paie du mois", "calculs de paie", "ou en est la paie", "payroll")) {
            return new IntentPrediction(ChatIntent.PAYROLL_STATUS.name(), 0.78);
        }

        if (TextNormalize.containsAny(text, "solde", "reste", "jours de conge", "conges restant", "combien de conge")) {
            return new IntentPrediction(ChatIntent.LEAVE_BALANCE.name(), 0.85);
        }
        if (TextNormalize.containsAny(text, "mes demandes", "mes demande", "ma demande", "statut de mes conges", "mes conges")) {
            return new IntentPrediction(ChatIntent.MY_LEAVE_REQUESTS.name(), 0.8);
        }
        if (TextNormalize.containsAny(text, "bulletin", "fiche de paie", "payslip", "mes bulletins")) {
            return new IntentPrediction(ChatIntent.MY_PAYSLIPS.name(), 0.85);
        }
        if (TextNormalize.containsAny(text, "mon contrat", "mes contrats", "contrat")) {
            return new IntentPrediction(ChatIntent.MY_CONTRACTS.name(), 0.78);
        }

        if (TextNormalize.containsAny(text, "conge", "absence")) {
            return new IntentPrediction(ChatIntent.FAQ_LEAVE.name(), 0.6);
        }
        if (TextNormalize.containsAny(text, "paie", "salaire")) {
            return new IntentPrediction(ChatIntent.FAQ_PAYROLL.name(), 0.58);
        }

        return new IntentPrediction(ChatIntent.OUT_OF_SCOPE.name(), 0.45);
    }

    public boolean looksLikeCancelLeave(String message) {
        String text = TextNormalize.soften(message);
        if (text.isBlank()) {
            return false;
        }
        if (TextNormalize.containsAny(text,
                "annule ma demande", "annuler ma demande", "annule mon conge", "annuler mon conge",
                "retire ma demande", "retirer ma demande", "supprime ma demande", "supprimer ma demande",
                "supprime ma demande en attente", "cancel my leave", "cancel my request",
                "je veux annuler", "je souhaite annuler")) {
            return true;
        }
        boolean cancelVerb = TextNormalize.containsAny(text,
                "annuler", "annule", "cancel", "retirer", "retire", "supprimer", "supprime");
        boolean target = TextNormalize.containsAny(text,
                "demande", "conge", "absence", "leave", "attente");
        return cancelVerb && target;
    }

    public boolean looksLikeCreateLeave(String message) {
        String text = TextNormalize.soften(message);
        if (text.isBlank() || looksLikeCancelLeave(message)) {
            return false;
        }
        if (TextNormalize.containsAny(text,
                "pose un conge", "poser un conge", "creer une demande de conge",
                "je veux un conge", "je veux poser", "demande un conge pour", "reserve un conge",
                "ouvre une demande de conge", "soumettre une demande de conge", "creer ma demande",
                "je veux creer une demande", "pose moi un conge", "book a leave", "create a leave request",
                "conge du ", "conge demain", "absence du ", "pose une absence",
                "i want conge", "i want a conge", "i want leave", "i want a leave", "want conge",
                "j aimerais un conge", "je souhaite poser un conge", "je souhaite un conge",
                "prendre un conge", "prendre une absence", "faire une demande de conge",
                "request leave", "ask for leave", "request a leave")) {
            return true;
        }
        boolean want = TextNormalize.containsAny(text, "je veux", "i want", "j aimerais", "je souhaite");
        boolean leave = TextNormalize.containsAny(text, "conge", "absence", "leave");
        return want && leave;
    }

    public boolean looksLikeReviewBatch(String message) {
        String text = TextNormalize.soften(message);
        if (text.isBlank()) {
            return false;
        }
        boolean cue = TextNormalize.containsAny(text,
                "accepte", "accepter", "approuve", "approve", "valide", "valider", "ok pour",
                "refuse", "refuser", "rejette", "rejeter", "reject", "decline",
                "n accepte pas", "dont accept");
        boolean quantifier = text.matches(".*\\d.*")
                || TextNormalize.containsAny(text, "tous", "toutes", "tout", " all", "premier", "dernier", "sauf");
        return cue && quantifier;
    }
}

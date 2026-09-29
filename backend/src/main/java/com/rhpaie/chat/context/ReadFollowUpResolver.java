package com.rhpaie.chat.context;

import com.rhpaie.chat.entity.ChatIntent;
import com.rhpaie.chat.service.TextNormalize;
import com.rhpaie.user.entity.Role;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic resolver for short READ follow-ups against {@link ConversationContext}.
 * Never resolves ACTION intents and never bypasses role checks.
 */
@Component
public class ReadFollowUpResolver {

    public static final String FILTER_STATUS = "status";
    public static final String FILTER_NEAREST_EXPIRING = "nearestExpiring";
    public static final String FILTER_EMPLOYEE_NAME = "employeeNameToken";

    private static final Pattern SEULEMENT_NAME = Pattern.compile(
            "\\b(?:seulement|uniquement)\\s+(?:ceux|celles|celui|celle)\\s+de\\s+([a-zA-ZÀ-ÿ\\-]{2,})",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * @param classifiedIntent intent from the classifier (may be OUT_OF_SCOPE / UNKNOWN-mapped)
     * @param strongStandalone true when caller already has a confident allowed TOOL/FAQ/ACTION
     */
    public Optional<ResolvedReadFollowUp> resolve(
            String message,
            ChatIntent classifiedIntent,
            ConversationContext context,
            Role role,
            boolean strongStandalone
    ) {
        if (strongStandalone || message == null || message.isBlank()) {
            return Optional.empty();
        }
        if (context == null || context.getDomain() == null) {
            return Optional.empty();
        }

        String text = TextNormalize.soften(message);
        ChatReadDomain domain = context.getDomain();

        if (domain == ChatReadDomain.LEAVE) {
            Optional<ResolvedReadFollowUp> leave = resolveLeave(text, context, role);
            if (leave.isPresent()) {
                return leave;
            }
        }
        if (domain == ChatReadDomain.CONTRACT) {
            Optional<ResolvedReadFollowUp> contract = resolveContract(text, role);
            if (contract.isPresent()) {
                return contract;
            }
        }
        return Optional.empty();
    }

    /**
     * Employee self-service READ with no conversation context required.
     * Does not match admin queue phrasing such as "Combien de congés en attente ?".
     */
    public Optional<ResolvedReadFollowUp> resolveEmployeeSelfRead(String message, Role role) {
        if (role != Role.EMPLOYE || !looksLikeEmployeeSelfRead(message)) {
            return Optional.empty();
        }
        String text = TextNormalize.soften(message);
        if (TextNormalize.containsAny(text, "mes contrats", "mon contrat")) {
            return Optional.of(ResolvedReadFollowUp.of(ChatIntent.MY_CONTRACTS));
        }
        if (TextNormalize.containsAny(text, "mes bulletins", "mon bulletin")) {
            return Optional.of(ResolvedReadFollowUp.of(ChatIntent.MY_PAYSLIPS));
        }
        if (TextNormalize.containsAny(text, "solde")) {
            return Optional.of(ResolvedReadFollowUp.of(ChatIntent.LEAVE_BALANCE));
        }
        boolean pending = TextNormalize.containsAny(text, "en attente", "pending", "attente");
        if (pending) {
            Map<String, String> filters = new LinkedHashMap<>();
            filters.put(FILTER_STATUS, "PENDING");
            return Optional.of(ResolvedReadFollowUp.of(ChatIntent.MY_LEAVE_REQUESTS, filters));
        }
        return Optional.of(ResolvedReadFollowUp.of(ChatIntent.MY_LEAVE_REQUESTS));
    }

    /**
     * True for employee phrases that must beat an open ACTION draft.
     * "Combien de congés en attente ?" is not a self-read (no mes / celles / ceux).
     */
    public static boolean looksLikeEmployeeSelfRead(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String text = stripInstructionPrefix(message);
        if (TextNormalize.containsAny(text,
                "mes demandes", "mes demande", "ma demande",
                "mes contrats", "mon contrat",
                "mes bulletins", "mon bulletin",
                "solde")) {
            return true;
        }
        boolean self = TextNormalize.containsAny(text, "celles", "ceux");
        boolean pending = TextNormalize.containsAny(text, "en attente", "pending", "attente");
        return self && pending;
    }

    /**
     * A complete company-queue question, with no personal marker such as
     * "mes", "celles" or "ceux". Short filters stay follow-ups.
     * Known instruction prefixes are ignored so they cannot hide the request.
     */
    public static boolean isExplicitCompanyPendingQuestion(String message) {
        if (message == null || message.isBlank() || looksLikeEmployeeSelfRead(message)) {
            return false;
        }
        String text = stripInstructionPrefix(message);
        if (text.isBlank() || looksLikeEmployeeSelfRead(text)) {
            return false;
        }
        // How-to / FAQ wording is a complete new question, not the company queue.
        if (text.startsWith("comment") || TextNormalize.containsAny(text,
                "comment demander", "comment poser", "comment creer", "comment faire",
                "comment un", "comment se", "procedure", "justificatif", "piece jointe", "depose")) {
            return false;
        }
        boolean pending = TextNormalize.containsAny(text, "en attente", "pending", "a valider");
        boolean companyScope = TextNormalize.containsAny(text, "conge", "conges", "combien", "liste", "montre", "file");
        boolean companyList = TextNormalize.containsAny(text,
                "toutes les demandes", "toutes les demande",
                "liste des conges", "liste des demandes", "liste des demande",
                "file des demandes", "file des demande", "file d attente",
                "montre les demandes", "montre les demande",
                "donne moi les demandes", "donne moi les demande");
        return (pending && companyScope) || companyList;
    }

    /**
     * Drops a short list of instruction-like prefixes. Not a general injection detector.
     */
    public static String stripInstructionPrefix(String message) {
        String text = TextNormalize.soften(message);
        if (text.isBlank()) {
            return text;
        }
        String[] prefixes = {
                "ignore les regles",
                "ignore les restrictions",
                "ignore les instructions",
                "ignore les consignes",
                "oublie les instructions",
                "oublie les regles",
                "fais comme si j etais admin",
                "fais comme si je suis admin",
                "agis comme un admin"
        };
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                text = text.substring(prefix.length()).trim();
                if (text.startsWith("et ")) {
                    text = text.substring(3).trim();
                }
                break;
            }
        }
        return text;
    }

    /**
     * True for a short filter or pronoun follow-up. A complete FAQ / TOOL / out-of-scope
     * sentence must not match, even if it mentions "demande" or "congé".
     */
    public boolean looksLikeIncompleteFollowUp(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String text = TextNormalize.soften(message);
        if (text.startsWith("comment") || TextNormalize.containsAny(text, "comment ")) {
            return false;
        }
        if (SEULEMENT_NAME.matcher(text).find()) {
            return true;
        }
        if (TextNormalize.containsAny(text, "seulement", "uniquement")
                && TextNormalize.containsAny(text, "ceux", "celles", "celui", "celle")) {
            return true;
        }
        String raw = message.trim();
        if (raw.length() <= 40 && (raw.toLowerCase().startsWith("et ") || raw.toLowerCase().startsWith("et\u00a0"))) {
            return true;
        }
        return TextNormalize.containsAny(text,
                "et mes demandes", "et mes demande",
                "et celles", "et ceux",
                "lequel expire", "qui expire", "expire bientot");
    }

    /**
     * True when the message looks like a READ filter / short follow-up rather than a new topic.
     */
    public boolean looksLikeReadFollowUpCue(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String text = TextNormalize.soften(message);
        String raw = message.trim();
        if (raw.length() <= 40 && (raw.toLowerCase().startsWith("et ") || raw.toLowerCase().startsWith("et\u00a0"))) {
            return true;
        }
        return TextNormalize.containsAny(text,
                "seulement", "uniquement", "en attente", "pending",
                "acceptees", "approuvees", "refusees", "rejetees",
                "expire bientot", "lequel expire", "qui expire", "echeance",
                "mes demandes", "et mes demandes", "demandes")
                || SEULEMENT_NAME.matcher(text).find();
    }

    private Optional<ResolvedReadFollowUp> resolveLeave(String text, ConversationContext context, Role role) {
        // Admin pending name filter (READ-only)
        if (role == Role.ADMIN_RH
                && context.getLastIntent() == ChatIntent.PENDING_LEAVES) {
            Optional<String> nameToken = extractNameFilterToken(text);
            if (nameToken.isPresent() && ChatIntent.PENDING_LEAVES.allowedFor(role)) {
                Map<String, String> filters = new LinkedHashMap<>();
                filters.put(FILTER_EMPLOYEE_NAME, nameToken.get());
                return Optional.of(ResolvedReadFollowUp.of(ChatIntent.PENDING_LEAVES, filters));
            }
        }

        if (role != Role.EMPLOYE) {
            return Optional.empty();
        }
        // A complete company-queue question is not a personal status filter.
        if (isExplicitCompanyPendingQuestion(text)) {
            return Optional.empty();
        }

        boolean asksRequests = TextNormalize.containsAny(text,
                "mes demandes", "mes demande", "ma demande", "et mes demandes", "et mes demande",
                "demandes", "demande");
        boolean statusPending = TextNormalize.containsAny(text, "en attente", "pending", "attente");
        boolean statusApproved = TextNormalize.containsAny(text, "acceptees", "approuvees", "acceptee", "approuvee");
        boolean statusRejected = TextNormalize.containsAny(text, "refusees", "rejetees", "refusee", "rejetee");

        if (statusPending || statusApproved || statusRejected) {
            Map<String, String> filters = new LinkedHashMap<>();
            if (statusPending) {
                filters.put(FILTER_STATUS, "PENDING");
            } else if (statusApproved) {
                filters.put(FILTER_STATUS, "APPROVED");
            } else {
                filters.put(FILTER_STATUS, "REJECTED");
            }
            return Optional.of(ResolvedReadFollowUp.of(ChatIntent.MY_LEAVE_REQUESTS, filters));
        }

        if (asksRequests) {
            return Optional.of(ResolvedReadFollowUp.of(ChatIntent.MY_LEAVE_REQUESTS));
        }

        return Optional.empty();
    }

    private Optional<ResolvedReadFollowUp> resolveContract(String text, Role role) {
        if (role != Role.EMPLOYE || !ChatIntent.MY_CONTRACTS.allowedFor(role)) {
            return Optional.empty();
        }
        if (TextNormalize.containsAny(text,
                "expire bientot", "lequel expire", "qui expire", "echeance", "expire", "expirent")) {
            Map<String, String> filters = new LinkedHashMap<>();
            filters.put(FILTER_NEAREST_EXPIRING, "true");
            return Optional.of(ResolvedReadFollowUp.of(ChatIntent.MY_CONTRACTS, filters));
        }
        return Optional.empty();
    }

    private Optional<String> extractNameFilterToken(String text) {
        Matcher matcher = SEULEMENT_NAME.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String token = TextNormalize.fold(matcher.group(1)).trim();
        if (token.length() < 2) {
            return Optional.empty();
        }
        if (TextNormalize.containsAny(token,
                "attente", "pending", "ceux", "celles", "demande", "conge", "accepte", "refuse")) {
            return Optional.empty();
        }
        return Optional.of(token);
    }
}

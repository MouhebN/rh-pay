package com.rhpaie.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhpaie.chat.dto.ChatProposedAction;
import com.rhpaie.chat.dto.ChatToolResult;
import com.rhpaie.chat.entity.ChatActionDraft;
import com.rhpaie.chat.repository.ChatActionDraftRepository;
import com.rhpaie.leave.dto.LeaveRequestFilter;
import com.rhpaie.leave.dto.LeaveRequestResponse;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import com.rhpaie.leave.service.LeaveRequestService;
import com.rhpaie.user.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AdminLeaveReviewService {

    public static final String ACTION_TYPE = "ADMIN_LEAVE_REVIEW";
    private static final int LIST_CAP = 15;
    private static final Pattern MOTIF = Pattern.compile(
            "(?:motif|raison|parce que|because)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private final LeaveRequestService leaveRequestService;
    private final ChatActionDraftRepository chatActionDraftRepository;
    private final ObjectMapper objectMapper;

    public ChatToolResult listPendingAndSnapshot(UserAccount admin) {
        return listPendingAndSnapshot(admin, null);
    }

    /**
     * READ-only pending list. Optional name token filters the displayed pending requests
     * without proposing approve/reject actions.
     */
    public ChatToolResult listPendingAndSnapshot(UserAccount admin, String employeeNameToken) {
        ListPending listed = loadPending(admin);
        if (listed.pending().isEmpty()) {
            return ChatToolResult.reply(
                    "enAttente=0",
                    "Il n’y a aucune demande en attente pour le moment.",
                    "/admin/leave-requests",
                    "Ouvrir les congés"
            );
        }
        List<LeaveRequestResponse> pending = listed.pending();
        if (employeeNameToken != null && !employeeNameToken.isBlank()) {
            String token = TextNormalize.fold(employeeNameToken).trim();
            List<LeaveRequestResponse> filtered = pending.stream()
                    .filter(item -> nameHasToken(item.employeeFullName(), token))
                    .toList();
            if (filtered.isEmpty()) {
                return ChatToolResult.reply(
                        "enAttente=" + pending.size() + ", filtre=" + token + ", matches=0",
                        "Aucune demande en attente ne correspond à « " + employeeNameToken + " ».",
                        "/admin/leave-requests",
                        "Ouvrir les congés"
                );
            }
            List<Long> ids = filtered.stream().map(LeaveRequestResponse::id).toList();
            saveSnapshot(admin, ids);
            return ChatToolResult.reply(
                    "enAttente=" + filtered.size() + ", filtre=" + token + ", ids=" + ids,
                    formatListReply(filtered),
                    "/admin/leave-requests",
                    "Traiter les congés"
            );
        }
        return ChatToolResult.reply(
                "enAttente=" + listed.pending().size() + ", ids=" + listed.ids(),
                formatListReply(listed.pending()),
                "/admin/leave-requests",
                "Traiter les congés"
        );
    }

    public boolean looksLikeBatchDecision(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String text = TextNormalize.soften(message);
        boolean cue = TextNormalize.containsAny(text,
                "accepte", "accepter", "approuve", "approuver", "approve", "valide", "valider", "ok pour",
                "refuse", "refuser", "rejette", "rejeter", "reject", "decline",
                "n accepte pas", "ne accepte pas", "dont accept", "do not accept");
        boolean quantifier = text.matches(".*\\d.*")
                || TextNormalize.containsAny(text, "tous", "toutes", "tout", " all", "premier", "premiers",
                "dernier", "derniers", "sauf",
                "deuxieme", "troisieme", "quatrieme", "cinquieme",
                "sixieme", "septieme", "huitieme", "neuvieme", "dixieme");
        return cue && quantifier;
    }

    public ChatToolResult proposeFromDecision(UserAccount admin, String message) {
        ListPending listed = resolveListed(admin);
        if (listed.pending().isEmpty()) {
            return ChatToolResult.reply(
                    "enAttente=0",
                    "Il n’y a aucune demande en attente à traiter.",
                    "/admin/leave-requests",
                    "Ouvrir les congés"
            );
        }

        boolean refreshed = listed.refreshed();
        String listPrefix = refreshed ? formatListReply(listed.pending()) + "\n\n" : "";

        Decision decision = parseDecision(message, listed.pending());
        if (decision.approveIndexes().isEmpty() && decision.rejectIndexes().isEmpty()) {
            return ChatToolResult.reply(
                    listPrefix
                            + "Je n’ai pas compris qui traiter. "
                            + "Vous pouvez dire « accepter tous », « refuse 3 », "
                            + "« accepte Ali », ou « accepte 1 et 2, refuseuse 4 »."
            );
        }
        if (!decision.invalidIndexes().isEmpty()) {
            return ChatToolResult.reply(
                    listPrefix
                            + "Ces numéros ne sont pas dans la liste : " + decision.invalidIndexes()
                            + " (1–" + listed.pending().size() + ")."
            );
        }
        if (!decision.unmatchedNames().isEmpty()) {
            return ChatToolResult.reply(
                    listPrefix
                            + "Je ne trouve personne correspondant à : "
                            + String.join(", ", decision.unmatchedNames())
                            + "."
            );
        }

        List<Long> approveIds = decision.approveIndexes().stream()
                .map(i -> listed.ids().get(i - 1))
                .toList();
        List<Long> rejectIds = decision.rejectIndexes().stream()
                .map(i -> listed.ids().get(i - 1))
                .toList();
        String reason = decision.rejectReason() == null || decision.rejectReason().isBlank()
                ? "Refus via assistant RH"
                : decision.rejectReason();

        StringBuilder summary = new StringBuilder(listPrefix);
        summary.append("Récapitulatif avant validation :\n");
        if (!approveIds.isEmpty()) {
            summary.append("• Approuver : ")
                    .append(describeIndexes(decision.approveIndexes(), listed.pending()))
                    .append('\n');
        }
        if (!rejectIds.isEmpty()) {
            summary.append("• Refuser : ")
                    .append(describeIndexes(decision.rejectIndexes(), listed.pending()))
                    .append(" — motif : ").append(reason)
                    .append('\n');
        }
        summary.append("\nConfirmez pour appliquer, ou annulez.");
        String proposalId = rememberDecision(admin, approveIds, rejectIds, reason);

        ChatProposedAction action = ChatProposedAction.builder()
                .type("REVIEW_LEAVE_BATCH")
                .leaveType(null)
                .startDate(null)
                .endDate(null)
                .reason(reason)
                .leaveRequestId(null)
                .approveLeaveRequestIds(approveIds)
                .rejectLeaveRequestIds(rejectIds)
                .proposalId(proposalId)
                .build();

        return ChatToolResult.propose(
                "approve=" + approveIds + ", reject=" + rejectIds,
                summary.toString(),
                "/admin/leave-requests",
                "Voir les congés",
                action
        );
    }

    private ListPending resolveListed(UserAccount admin) {
        Optional<ChatActionDraft> draftOpt = chatActionDraftRepository.findByUser_Id(admin.getId())
                .filter(draft -> ACTION_TYPE.equals(draft.getActionType()));
        List<Long> existingIds = draftOpt.map(this::readListedIds).orElse(List.of());
        if (!existingIds.isEmpty()) {
            List<LeaveRequestResponse> pending = leaveRequestService.findAll(
                            LeaveRequestFilter.builder().status(LeaveStatus.PENDING).build()
                    ).stream()
                    .filter(item -> existingIds.contains(item.id()))
                    .sorted((a, b) -> Integer.compare(existingIds.indexOf(a.id()), existingIds.indexOf(b.id())))
                    .toList();
            if (pending.size() == existingIds.size()) {
                return new ListPending(pending, existingIds, false);
            }
        }
        ListPending fresh = loadPending(admin);
        return new ListPending(fresh.pending(), fresh.ids(), true);
    }

    private ListPending loadPending(UserAccount admin) {
        List<LeaveRequestResponse> pending = leaveRequestService.findAll(
                        LeaveRequestFilter.builder().status(LeaveStatus.PENDING).build()
                ).stream()
                .limit(LIST_CAP)
                .toList();
        if (pending.isEmpty()) {
            clearDraft(admin.getId());
            return new ListPending(List.of(), List.of(), true);
        }
        List<Long> ids = pending.stream().map(LeaveRequestResponse::id).toList();
        saveSnapshot(admin, ids);
        return new ListPending(pending, ids, true);
    }

    private String formatListReply(List<LeaveRequestResponse> pending) {
        StringBuilder lines = new StringBuilder();
        lines.append(pending.size() == 1
                ? "Voici la demande en attente :\n"
                : "Voici les " + pending.size() + " demandes en attente :\n");
        for (int i = 0; i < pending.size(); i++) {
            LeaveRequestResponse item = pending.get(i);
            lines.append(i + 1)
                    .append(". ")
                    .append(nullToDash(item.employeeFullName()))
                    .append(" — ").append(typeLabel(item.type()))
                    .append(" — ").append(item.startDate()).append(" → ").append(item.endDate())
                    .append(" (n°").append(item.id()).append(")\n");
        }
        lines.append("\nDites-moi quoi faire (ex. accepter tous, refuse 3, accepte Ali…).");
        return lines.toString().trim();
    }

    private String describeIndexes(List<Integer> indexes, List<LeaveRequestResponse> pending) {
        return indexes.stream()
                .map(i -> {
                    LeaveRequestResponse item = pending.get(i - 1);
                    return i + " (" + nullToDash(item.employeeFullName()) + ")";
                })
                .collect(Collectors.joining(", "));
    }

    Decision parseDecision(String message, List<LeaveRequestResponse> pending) {
        int listSize = pending.size();
        String text = TextNormalize.soften(message);
        String reason = resolveReason(message);
        Set<Integer> approve = new LinkedHashSet<>();
        Set<Integer> reject = new LinkedHashSet<>();
        Set<Integer> invalid = new LinkedHashSet<>();
        List<String> unmatchedNames = new ArrayList<>();

        boolean approveAll = TextNormalize.containsAny(text,
                "accepte tous", "accepter tous", "accepte toutes", "accepter toutes",
                "approuve tous", "approuver tous", "approve all", "valide tous", "valider tous",
                "accepte tout", "accepter tout", "approuve tout", "ok pour tous", "ok pour toutes",
                "ok pour tout");
        boolean rejectAll = TextNormalize.containsAny(text,
                "refuse tous", "refuser tous", "refuse toutes", "refuser toutes",
                "reject all", "decline all", "rejette tous", "rejeter tous",
                "refuse tout", "refuser tout");

        Set<Integer> except = parseSaufIndexes(text, listSize, invalid);

        if (approveAll) {
            IntStream.rangeClosed(1, listSize).forEach(approve::add);
            approve.removeAll(except);
        }
        if (rejectAll) {
            IntStream.rangeClosed(1, listSize).forEach(reject::add);
            reject.removeAll(except);
        }

        // Split approve / reject clauses when both appear.
        String[] rejectSplit = text.split(
                "(?:refuse|refuser|rejette|rejeter|reject|decline|n accepte pas|ne accepte pas|dont accept|do not accept)",
                2
        );
        String approvePart = rejectSplit[0];
        String rejectPart = rejectSplit.length > 1 ? rejectSplit[1] : "";

        boolean rejectOnly = TextNormalize.containsAny(text, "refuse", "refuser", "rejette", "reject", "decline")
                && !TextNormalize.containsAny(approvePart, "accepte", "accepter", "approuve", "approuver", "approve", "valide", "valider", "ok pour");

        if (!approveAll && !rejectOnly) {
            collectIndexes(approvePart, listSize, approve, invalid);
            collectRelative(approvePart, listSize, approve);
            collectOrdinals(approvePart, listSize, approve);
            collectNames(approvePart, pending, approve, unmatchedNames);
        }
        if (!rejectAll && (rejectOnly || !rejectPart.isBlank()
                || TextNormalize.containsAny(text, "refuse", "refuser", "rejette", "reject", "decline", "n accepte pas", "dont accept"))) {
            String rejectSource = rejectOnly && rejectPart.isBlank() ? text : (rejectPart.isBlank() ? "" : rejectPart);
            if (!rejectSource.isBlank()) {
                collectIndexes(rejectSource, listSize, reject, invalid);
                collectRelative(rejectSource, listSize, reject);
                collectOrdinals(rejectSource, listSize, reject);
                collectNames(rejectSource, pending, reject, unmatchedNames);
            }
        }

        approve.removeAll(reject);
        unmatchedNames.removeIf(name -> nameMatchedSomewhere(name, pending, approve, reject));

        return new Decision(
                List.copyOf(approve),
                List.copyOf(reject),
                List.copyOf(invalid),
                List.copyOf(unmatchedNames),
                reason
        );
    }

    private Set<Integer> parseSaufIndexes(String text, int listSize, Set<Integer> invalid) {
        Set<Integer> except = new LinkedHashSet<>();
        Matcher sauf = Pattern.compile("sauf\\s+(.+)$").matcher(text);
        if (sauf.find()) {
            collectIndexes(sauf.group(1), listSize, except, invalid);
        }
        return except;
    }

    private static final String[] ORDINALS = {
            "premier", "deuxieme", "troisieme", "quatrieme", "cinquieme",
            "sixieme", "septieme", "huitieme", "neuvieme", "dixieme"
    };

    private void collectOrdinals(String part, int listSize, Set<Integer> target) {
        if (part == null || part.isBlank()) {
            return;
        }
        for (int i = 0; i < ORDINALS.length; i++) {
            if (TextNormalize.containsAny(part, ORDINALS[i])) {
                addIndex(i + 1, listSize, target, new LinkedHashSet<>());
            }
        }
    }

    private void collectRelative(String part, int listSize, Set<Integer> target) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (TextNormalize.containsAny(part, "premiers", "premier")) {
            int count = extractCountWord(part, listSize);
            for (int i = 1; i <= count; i++) {
                target.add(i);
            }
        }
        if (TextNormalize.containsAny(part, "derniers", "dernier")) {
            int count = extractCountWord(part, listSize);
            for (int i = listSize - count + 1; i <= listSize; i++) {
                if (i >= 1) {
                    target.add(i);
                }
            }
        }
    }

    private int extractCountWord(String part, int listSize) {
        Matcher digits = Pattern.compile("(\\d{1,2})\\s*(?:premiers?|derniers?)").matcher(part);
        if (digits.find()) {
            return Math.min(listSize, Integer.parseInt(digits.group(1)));
        }
        if (TextNormalize.containsAny(part, "deux")) {
            return Math.min(2, listSize);
        }
        if (TextNormalize.containsAny(part, "trois")) {
            return Math.min(3, listSize);
        }
        if (TextNormalize.containsAny(part, "quatre")) {
            return Math.min(4, listSize);
        }
        if (TextNormalize.containsAny(part, "cinq")) {
            return Math.min(5, listSize);
        }
        return Math.min(1, listSize);
    }

    private void collectNames(
            String part,
            List<LeaveRequestResponse> pending,
            Set<Integer> target,
            List<String> unmatchedNames
    ) {
        if (part == null || part.isBlank()) {
            return;
        }
        String cleaned = part
                .replaceAll("\\b(accepte|accepter|approuve|approuver|approve|valide|valider|refuse|refuser|rejette|rejeter|reject|tous|toutes|tout|all|les|la|le|de|du|des|et|sauf|motif|raison)\\b", " ")
                .replaceAll("\\d+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() < 3) {
            return;
        }
        String[] tokens = cleaned.split(" ");
        for (String token : tokens) {
            if (token.length() < 3) {
                continue;
            }
            boolean matched = false;
            for (int i = 0; i < pending.size(); i++) {
                if (nameHasToken(pending.get(i).employeeFullName(), token)) {
                    target.add(i + 1);
                    matched = true;
                }
            }
            if (!matched && !isNoiseToken(token)) {
                unmatchedNames.add(token);
            }
        }
    }

    private boolean nameMatchedSomewhere(
            String token,
            List<LeaveRequestResponse> pending,
            Set<Integer> approve,
            Set<Integer> reject
    ) {
        for (int i = 0; i < pending.size(); i++) {
            if (nameHasToken(pending.get(i).employeeFullName(), token)
                    && (approve.contains(i + 1) || reject.contains(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private boolean nameHasToken(String fullName, String token) {
        if (fullName == null || token == null || token.isBlank()) {
            return false;
        }
        String name = TextNormalize.fold(fullName);
        for (String part : name.split("\\s+")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNoiseToken(String token) {
        return TextNormalize.containsAny(token,
                "demande", "conge", "absence", "paye", "maladie", "premier", "premiere", "dernier",
                "deuxieme", "troisieme", "quatrieme", "cinquieme",
                "sixieme", "septieme", "huitieme", "neuvieme", "dixieme");
    }

    private void collectIndexes(String part, int listSize, Set<Integer> target, Set<Integer> invalid) {
        if (part == null || part.isBlank()) {
            return;
        }
        Matcher range = Pattern.compile("(\\d{1,2})\\s*[-–a]+\\s*(\\d{1,2})").matcher(part);
        while (range.find()) {
            int a = Integer.parseInt(range.group(1));
            int b = Integer.parseInt(range.group(2));
            int from = Math.min(a, b);
            int to = Math.max(a, b);
            for (int i = from; i <= to; i++) {
                addIndex(i, listSize, target, invalid);
            }
        }
        Matcher number = Pattern.compile("\\b(\\d{1,2})\\b").matcher(part);
        while (number.find()) {
            addIndex(Integer.parseInt(number.group(1)), listSize, target, invalid);
        }
    }

    private void addIndex(int index, int listSize, Set<Integer> target, Set<Integer> invalid) {
        if (index < 1 || index > listSize) {
            invalid.add(index);
        } else {
            target.add(index);
        }
    }

    private void saveSnapshot(UserAccount admin, List<Long> ids) {
        ChatActionDraft draft = chatActionDraftRepository.findByUser_Id(admin.getId())
                .orElseGet(() -> ChatActionDraft.builder()
                        .user(admin)
                        .actionType(ACTION_TYPE)
                        .build());
        draft.setActionType(ACTION_TYPE);
        draft.setLeaveType(null);
        draft.setStartDate(null);
        draft.setEndDate(null);
        draft.setDays(null);
        draft.setReason(null);
        draft.setLeaveRequestId(null);
        try {
            draft.setPayloadJson(objectMapper.writeValueAsString(Map.of("listedIds", ids)));
        } catch (JsonProcessingException exception) {
            draft.setPayloadJson("{\"listedIds\":[]}");
        }
        chatActionDraftRepository.save(draft);
    }

    private String rememberDecision(UserAccount admin, List<Long> approveIds, List<Long> rejectIds, String reason) {
        ChatActionDraft draft = chatActionDraftRepository.findByUser_Id(admin.getId())
                .orElseGet(() -> ChatActionDraft.builder()
                        .user(admin)
                        .actionType(ACTION_TYPE)
                        .build());
        draft.setActionType(ACTION_TYPE);
        Map<String, Object> payload = readPayload(draft);
        payload.put("approveIds", approveIds);
        payload.put("rejectIds", rejectIds);
        payload.put("decisionReason", reason);
        draft.setReason(reason);
        String proposalId = java.util.UUID.randomUUID().toString();
        draft.setProposalId(proposalId);
        try {
            draft.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            draft.setPayloadJson("{\"approveIds\":[],\"rejectIds\":[]}");
        }
        chatActionDraftRepository.save(draft);
        return proposalId;
    }

    private Map<String, Object> readPayload(ChatActionDraft draft) {
        if (draft.getPayloadJson() == null || draft.getPayloadJson().isBlank()) {
            return new java.util.LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(draft.getPayloadJson(), new TypeReference<>() {
            });
            return new java.util.LinkedHashMap<>(map);
        } catch (JsonProcessingException exception) {
            return new java.util.LinkedHashMap<>();
        }
    }

    private List<Long> readListedIds(ChatActionDraft draft) {
        if (draft.getPayloadJson() == null || draft.getPayloadJson().isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(draft.getPayloadJson(), new TypeReference<>() {
            });
            Object raw = map.get("listedIds");
            if (!(raw instanceof List<?> list)) {
                return List.of();
            }
            List<Long> ids = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number number) {
                    ids.add(number.longValue());
                }
            }
            return ids;
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private void clearDraft(Long userId) {
        chatActionDraftRepository.findByUser_Id(userId).ifPresent(chatActionDraftRepository::delete);
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
        return null;
    }

    private String typeLabel(LeaveType type) {
        if (type == null) {
            return "Absence";
        }
        return switch (type) {
            case PAID_LEAVE -> "Congé payé";
            case SICK_LEAVE -> "Congé maladie";
            case UNPAID_LEAVE -> "Congé sans solde";
            case MATERNITY_LEAVE -> "Congé maternité";
            case OTHER -> "Autre absence";
        };
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    record Decision(
            List<Integer> approveIndexes,
            List<Integer> rejectIndexes,
            List<Integer> invalidIndexes,
            List<String> unmatchedNames,
            String rejectReason
    ) {
    }

    private record ListPending(List<LeaveRequestResponse> pending, List<Long> ids, boolean refreshed) {
        private ListPending(List<LeaveRequestResponse> pending, List<Long> ids) {
            this(pending, ids, true);
        }
    }
}

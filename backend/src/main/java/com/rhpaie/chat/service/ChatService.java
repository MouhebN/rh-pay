package com.rhpaie.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhpaie.chat.context.ChatReadDomain;
import com.rhpaie.chat.context.ChatReadDomains;
import com.rhpaie.chat.context.ContextKey;
import com.rhpaie.chat.context.ConversationContext;
import com.rhpaie.chat.context.ConversationContextStore;
import com.rhpaie.chat.context.ReadFollowUpResolver;
import com.rhpaie.chat.context.ResolvedReadFollowUp;
import com.rhpaie.chat.dto.ChatCancelLeaveActionRequest;
import com.rhpaie.chat.dto.ChatLeaveActionRequest;
import com.rhpaie.chat.dto.ChatLeaveReviewActionRequest;
import com.rhpaie.chat.dto.ChatMessageResponse;
import com.rhpaie.chat.dto.ChatProposedAction;
import com.rhpaie.chat.dto.ChatToolResult;
import com.rhpaie.chat.dto.IntentPrediction;
import com.rhpaie.chat.entity.ChatActionDraft;
import com.rhpaie.chat.entity.ChatIntent;
import com.rhpaie.chat.entity.ChatMessage;
import com.rhpaie.chat.repository.ChatActionDraftRepository;
import com.rhpaie.chat.repository.ChatMessageRepository;
import com.rhpaie.leave.dto.LeaveRequestCreateRequest;
import com.rhpaie.leave.dto.LeaveRequestResponse;
import com.rhpaie.leave.dto.LeaveReviewRequest;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import com.rhpaie.leave.service.LeaveRequestService;
import com.rhpaie.user.entity.Role;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String OUT_OF_SCOPE_REPLY =
            "Je peux vous aider uniquement sur RH Paie : congés, contrats, bulletins et paie. Reformulez votre question dans ce cadre.";
    private static final String ROLE_REFUSAL_EMPLOYEE =
            "Cette information est réservée à l'espace administrateur RH. Je peux en revanche vous aider sur vos congés, vos contrats ou vos bulletins.";
    private static final String ROLE_REFUSAL_ADMIN =
            "Cette question concerne les données personnelles d'un employé. Connectez-vous sur un compte employé, ou demandez une synthèse (congés en attente, contrats à échéance, statut de paie).";
    private static final String ATTENDANCE_DISABLED_REPLY =
            "Le pointage n'est pas géré par l'assistant pour le moment. Utilisez le menu Pointage de l'application.";
    private static final int DRAFT_TTL_MINUTES = 30;
    private static final Pattern LEAVE_ID = Pattern.compile(
            "(?:n[°o]?\\s*|demande\\s*(?:n[°o]?)?\\s*|id\\s*|#)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BARE_LEAVE_ID = Pattern.compile("^\\s*(\\d{1,6})\\s*$");
    private static final ObjectMapper REVIEW_JSON = new ObjectMapper();

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatActionDraftRepository chatActionDraftRepository;
    private final IntentClassifierClient intentClassifierClient;
    private final ChatToolService chatToolService;
    private final RagService ragService;
    private final OllamaClient ollamaClient;
    private final ChatMemorySupport chatMemorySupport;
    private final LeaveDraftSlotFiller leaveDraftSlotFiller;
    private final LeaveRequestService leaveRequestService;
    private final KeywordIntentFallback keywordIntentFallback;
    private final AdminLeaveReviewService adminLeaveReviewService;
    private final ConversationContextStore conversationContextStore;
    private final ReadFollowUpResolver readFollowUpResolver;

    private record IntentResolution(ChatIntent intent, Map<String, String> filters) {
        static IntentResolution of(ChatIntent intent) {
            return new IntentResolution(intent, Map.of());
        }

        static IntentResolution of(ChatIntent intent, Map<String, String> filters) {
            return new IntentResolution(intent, filters == null ? Map.of() : filters);
        }
    }

    @Transactional
    public ChatMessageResponse reply(String username, String message) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        Role role = user.getRole();
        List<ChatMessage> newestFirst = chatMessageRepository.findTop3ByUser_IdOrderByCreatedAtDesc(user.getId());
        List<ChatMessage> oldestFirst = chatMemorySupport.oldestFirst(newestFirst);

        IntentResolution resolution = resolveIntent(message, newestFirst, role, user);
        ChatIntent intent = resolution.intent();
        Integer year = chatMemorySupport.resolveYear(message);
        ChatToolResult result = resolve(intent, role, username, message, year, user, resolution.filters());
        String history = intent.isFaq() ? "" : chatMemorySupport.formatHistory(oldestFirst);
        boolean skipLlm = intent == ChatIntent.OUT_OF_SCOPE
                || intent.isAction()
                || intent == ChatIntent.PENDING_LEAVES
                || (intent.isTool() && !intent.allowedFor(role))
                || (intent.isFaq() && (result.facts() == null || result.facts().isBlank()))
                || preferTemplateOnly(result.facts(), result.templateReply());
        String reply = skipLlm
                ? result.templateReply()
                : phrase(message, result.facts(), result.templateReply(), history, intent);
        if (reply.length() > 4000) {
            reply = reply.substring(0, 4000);
        }
        persist(user, role, message, reply, intent, result.link());
        updateReadContextAfterSuccess(user, role, intent, result, resolution.filters());

        return ChatMessageResponse.builder()
                .reply(reply)
                .link(result.link())
                .linkLabel(result.linkLabel())
                .intent(intent.name())
                .sourceKind(sourceKind(intent, role))
                .sourceLabel(sourceLabel(intent, role))
                .suggestions(suggestionsFor(role))
                .proposedAction(result.proposedAction())
                .draftOpen(activeDraft(user).isPresent())
                .build();
    }

    @Transactional
    public ChatMessageResponse confirmLeaveRequest(String username, ChatLeaveActionRequest request) {
        UserAccount user = requireEmployee(username);
        ChatActionDraft draft = requireLiveDraft(user, ChatIntent.CREATE_LEAVE_REQUEST.name());
        requireProposalMatch(draft, request.proposalId());
        if (draft.getStartDate() == null || draft.getEndDate() == null) {
            throw draftRejected("Le brouillon est incomplet. Reprenez la demande dans le chat.");
        }
        LeaveType draftType = draftLeaveType(draft);
        if (request.type() != draftType
                || !draft.getStartDate().equals(request.startDate())
                || !draft.getEndDate().equals(request.endDate())) {
            throw draftRejected("La confirmation ne correspond pas au brouillon.");
        }
        String reason = draft.getReason() == null || draft.getReason().isBlank()
                ? "Demande via assistant RH"
                : draft.getReason().trim();
        try {
            LeaveRequestResponse created = leaveRequestService.create(
                    LeaveRequestCreateRequest.builder()
                            .employeeId(null)
                            .type(draftType)
                            .startDate(draft.getStartDate())
                            .endDate(draft.getEndDate())
                            .reason(reason)
                            .build(),
                    username
            );
            clearDraft(user.getId());
            String reply = "Demande créée (n°" + created.id() + ") : "
                    + typeLabel(created.type()) + " du " + created.startDate()
                    + " au " + created.endDate() + " — statut " + created.status() + ".";
            persist(user, user.getRole(), "[CONFIRM_LEAVE]", reply, ChatIntent.CREATE_LEAVE_REQUEST, "/employee/my-leave-requests");
            return ChatMessageResponse.builder()
                    .reply(reply)
                    .link("/employee/my-leave-requests")
                    .linkLabel("Voir mes demandes")
                    .intent(ChatIntent.CREATE_LEAVE_REQUEST.name())
                    .sourceKind("ACTION")
                    .sourceLabel("Action : demande créée")
                    .suggestions(suggestionsFor(user.getRole()))
                    .proposedAction(null)
                    .build();
        } catch (ResponseStatusException exception) {
            String refusal = exception.getReason() == null || exception.getReason().isBlank()
                    ? "Impossible de créer la demande."
                    : exception.getReason();
            throw new ResponseStatusException(exception.getStatusCode(), "Création refusée : " + refusal);
        }
    }

    @Transactional
    public ChatMessageResponse confirmCancelLeaveRequest(String username, ChatCancelLeaveActionRequest request) {
        UserAccount user = requireEmployee(username);
        ChatActionDraft draft = requireLiveDraft(user, ChatIntent.CANCEL_LEAVE_REQUEST.name());
        requireProposalMatch(draft, request.proposalId());
        if (draft.getLeaveRequestId() == null) {
            throw draftRejected("Le brouillon est incomplet. Reprenez l'annulation dans le chat.");
        }
        if (!draft.getLeaveRequestId().equals(request.leaveRequestId())) {
            throw draftRejected("La confirmation ne correspond pas au brouillon.");
        }
        try {
            LeaveRequestResponse cancelled = leaveRequestService.cancel(draft.getLeaveRequestId(), username);
            clearDraft(user.getId());
            String reply = "Demande n°" + cancelled.id() + " annulée ("
                    + typeLabel(cancelled.type()) + " du " + cancelled.startDate()
                    + " au " + cancelled.endDate() + ").";
            persist(user, user.getRole(), "[CONFIRM_CANCEL_LEAVE]", reply, ChatIntent.CANCEL_LEAVE_REQUEST, "/employee/my-leave-requests");
            return ChatMessageResponse.builder()
                    .reply(reply)
                    .link("/employee/my-leave-requests")
                    .linkLabel("Voir mes demandes")
                    .intent(ChatIntent.CANCEL_LEAVE_REQUEST.name())
                    .sourceKind("ACTION")
                    .sourceLabel("Action : demande annulée")
                    .suggestions(suggestionsFor(user.getRole()))
                    .proposedAction(null)
                    .build();
        } catch (ResponseStatusException exception) {
            String reason = exception.getReason() == null || exception.getReason().isBlank()
                    ? "Impossible d'annuler la demande."
                    : exception.getReason();
            Long id = request.leaveRequestId();
            String lead = id == null
                    ? "Annulation refusée : "
                    : "Annulation n°" + id + " refusée : ";
            throw new ResponseStatusException(exception.getStatusCode(), lead + reason);
        }
    }

    @Transactional
    public ChatMessageResponse discardLeaveAction(String username) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        if (user.getRole() != Role.EMPLOYE && user.getRole() != Role.ADMIN_RH) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
        }
        clearDraft(user.getId());
        String reply = "Brouillon annulé. Aucune modification n'a été enregistrée.";
        ChatIntent intent = user.getRole() == Role.ADMIN_RH
                ? ChatIntent.REVIEW_LEAVE_BATCH
                : ChatIntent.CREATE_LEAVE_REQUEST;
        persist(user, user.getRole(), "[DISCARD_LEAVE_ACTION]", reply, intent, null);
        return ChatMessageResponse.builder()
                .reply(reply)
                .link(null)
                .linkLabel(null)
                .intent(intent.name())
                .sourceKind("ACTION")
                .sourceLabel("Action : brouillon annulé")
                .suggestions(suggestionsFor(user.getRole()))
                .proposedAction(null)
                .build();
    }

    public ChatMessageResponse confirmLeaveReview(String username, ChatLeaveReviewActionRequest request) {
        UserAccount user = requireAdmin(username);
        ChatActionDraft draft = requireLiveDraft(user, AdminLeaveReviewService.ACTION_TYPE);
        requireProposalMatch(draft, request.proposalId());
        ReviewProposal stored = readReviewProposal(draft);
        if (stored.approveIds().isEmpty() && stored.rejectIds().isEmpty()) {
            throw draftRejected("Le brouillon est incomplet. Reprenez la revue dans le chat.");
        }
        List<Long> clientApprove = request.approveLeaveRequestIds() == null ? List.of() : request.approveLeaveRequestIds();
        List<Long> clientReject = request.rejectLeaveRequestIds() == null ? List.of() : request.rejectLeaveRequestIds();
        if (!sameIds(clientApprove, stored.approveIds()) || !sameIds(clientReject, stored.rejectIds())) {
            throw draftRejected("La confirmation ne correspond pas au brouillon.");
        }
        List<Long> approveIds = stored.approveIds();
        List<Long> rejectIds = stored.rejectIds();
        String rejectReason = stored.reason();
        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (Long id : approveIds) {
            try {
                LeaveRequestResponse approved = leaveRequestService.approve(id, username);
                succeeded.add("Approuvée n°" + approved.id() + " (" + approved.employeeFullName() + ")");
            } catch (ResponseStatusException exception) {
                failed.add(formatReviewFailure("Approuver", id, username, exception));
            }
        }
        for (Long id : rejectIds) {
            try {
                LeaveRequestResponse rejected = leaveRequestService.reject(
                        id,
                        LeaveReviewRequest.builder().rejectionReason(rejectReason).build(),
                        username
                );
                succeeded.add("Refusée n°" + rejected.id() + " (" + rejected.employeeFullName() + ")");
            } catch (ResponseStatusException exception) {
                failed.add(formatReviewFailure("Refuser", id, username, exception));
            }
        }

        if (succeeded.isEmpty() && !failed.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucune demande traitée :\n• " + String.join("\n• ", failed)
            );
        }

        if (!succeeded.isEmpty()) {
            clearDraft(user.getId());
        }

        StringBuilder reply = new StringBuilder();
        if (!succeeded.isEmpty()) {
            reply.append("Revue appliquée :\n• ").append(String.join("\n• ", succeeded));
        }
        if (!failed.isEmpty()) {
            if (!reply.isEmpty()) {
                reply.append("\n\n");
            }
            reply.append("Échecs (non appliqués) :\n• ").append(String.join("\n• ", failed));
        }

        String replyText = reply.toString();
        persist(user, user.getRole(), "[CONFIRM_LEAVE_REVIEW]", replyText, ChatIntent.REVIEW_LEAVE_BATCH, "/admin/leave-requests");
        return ChatMessageResponse.builder()
                .reply(replyText)
                .link("/admin/leave-requests")
                .linkLabel("Voir les congés")
                .intent(ChatIntent.REVIEW_LEAVE_BATCH.name())
                .sourceKind("ACTION")
                .sourceLabel("Action : revue des demandes")
                .suggestions(suggestionsFor(user.getRole()))
                .proposedAction(null)
                .build();
    }

    private String formatReviewFailure(String verb, Long id, String username, ResponseStatusException exception) {
        String employee = "—";
        try {
            LeaveRequestResponse leave = leaveRequestService.findById(id, username);
            if (leave.employeeFullName() != null && !leave.employeeFullName().isBlank()) {
                employee = leave.employeeFullName();
            }
        } catch (RuntimeException ignored) {
            // keep placeholder if the leave is missing / inaccessible
        }
        String reason = exception.getReason() == null || exception.getReason().isBlank()
                ? "Impossible d'appliquer la revue."
                : exception.getReason();
        return verb + " n°" + id + " (" + employee + ") : " + reason;
    }

    private UserAccount requireAdmin(String username) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        if (user.getRole() != Role.ADMIN_RH) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seuls les administrateurs RH peuvent confirmer cette action.");
        }
        return user;
    }

    private UserAccount requireEmployee(String username) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        if (user.getRole() != Role.EMPLOYE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seuls les employés peuvent confirmer cette action.");
        }
        return user;
    }

    private IntentResolution resolveIntent(String message, List<ChatMessage> newestFirst, Role role, UserAccount user) {
        // Explicit action switches win over whatever draft is open.
        if (role == Role.EMPLOYE && keywordIntentFallback.looksLikeCancelLeave(message)) {
            return IntentResolution.of(ChatIntent.CANCEL_LEAVE_REQUEST);
        }
        if (role == Role.EMPLOYE && keywordIntentFallback.looksLikeCreateLeave(message)) {
            return IntentResolution.of(ChatIntent.CREATE_LEAVE_REQUEST);
        }
        if (role == Role.ADMIN_RH && adminLeaveReviewService.looksLikeBatchDecision(message)) {
            return IntentResolution.of(ChatIntent.REVIEW_LEAVE_BATCH);
        }

        // Self-service READ beats an open create/cancel draft (greeting does not set context).
        if (role == Role.EMPLOYE) {
            Optional<ResolvedReadFollowUp> selfRead = readFollowUpResolver.resolveEmployeeSelfRead(message, role);
            if (selfRead.isPresent()) {
                ResolvedReadFollowUp followUp = selfRead.get();
                return IntentResolution.of(followUp.intent(), followUp.filters());
            }
        }

        // A complete company-queue question keeps PENDING_LEAVES. The role guard refuses EMPLOYE.
        if (ReadFollowUpResolver.isExplicitCompanyPendingQuestion(message)) {
            return IntentResolution.of(ChatIntent.PENDING_LEAVES);
        }

        Optional<ChatActionDraft> openDraft = activeDraft(user);

        // READ follow-up (e.g. admin "seulement ceux de Ahmed") before open REVIEW draft steals the turn.
        if (role == Role.ADMIN_RH
                && openDraft.isPresent()
                && AdminLeaveReviewService.ACTION_TYPE.equals(openDraft.get().getActionType())
                && readFollowUpResolver.looksLikeReadFollowUpCue(message)
                && !adminLeaveReviewService.looksLikeBatchDecision(message)) {
            Optional<IntentResolution> contextual = tryReadFollowUp(message, ChatIntent.OUT_OF_SCOPE, role, user, false);
            if (contextual.isPresent()) {
                return contextual.get();
            }
        }

        if (openDraft.isPresent() && role == Role.EMPLOYE) {
            String actionType = openDraft.get().getActionType();
            if (ChatIntent.CREATE_LEAVE_REQUEST.name().equals(actionType)
                    && leaveDraftSlotFiller.looksLikeSlotAnswer(message)) {
                return IntentResolution.of(ChatIntent.CREATE_LEAVE_REQUEST);
            }
            if (ChatIntent.CANCEL_LEAVE_REQUEST.name().equals(actionType)
                    && (chatMemorySupport.looksLikeFollowUp(message) || LEAVE_ID.matcher(message).find())
                    && !keywordIntentFallback.looksLikeCreateLeave(message)) {
                return IntentResolution.of(ChatIntent.CANCEL_LEAVE_REQUEST);
            }
        }
        if (openDraft.isPresent()
                && role == Role.ADMIN_RH
                && AdminLeaveReviewService.ACTION_TYPE.equals(openDraft.get().getActionType())
                && (adminLeaveReviewService.looksLikeBatchDecision(message)
                || chatMemorySupport.looksLikeFollowUp(message))) {
            return IntentResolution.of(ChatIntent.REVIEW_LEAVE_BATCH);
        }

        ChatIntent intent = classify(message, role);
        boolean incompleteFollowUp = readFollowUpResolver.looksLikeIncompleteFollowUp(message);
        // FAQ is a complete new question even if a leave read domain is still stored.
        if (intent.isFaq()) {
            return IntentResolution.of(intent);
        }
        boolean completeStandalone = intent == ChatIntent.OUT_OF_SCOPE
                || ((intent.isTool() || intent.isAction()) && intent.allowedFor(role));
        if (completeStandalone && !incompleteFollowUp) {
            return IntentResolution.of(intent);
        }

        Optional<IntentResolution> contextual = tryReadFollowUp(message, intent, role, user, false);
        if (contextual.isPresent()) {
            return contextual.get();
        }
        return IntentResolution.of(intent);
    }

    private Optional<IntentResolution> tryReadFollowUp(
            String message,
            ChatIntent classifiedIntent,
            Role role,
            UserAccount user,
            boolean strongStandalone
    ) {
        Optional<ConversationContext> context = conversationContextStore.get(new ContextKey(user.getId()));
        Optional<ResolvedReadFollowUp> resolved = readFollowUpResolver.resolve(
                message,
                classifiedIntent,
                context.orElse(null),
                role,
                strongStandalone
        );
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        ResolvedReadFollowUp followUp = resolved.get();
        if (!followUp.intent().isTool() || !followUp.intent().allowedFor(role)) {
            return Optional.empty();
        }
        return Optional.of(IntentResolution.of(followUp.intent(), followUp.filters()));
    }

    private ChatIntent classify(String text, Role role) {
        IntentPrediction prediction = intentClassifierClient.predictForRole(text, role);
        ChatIntent intent = ChatIntent.fromLabel(prediction.intent());
        return intent == ChatIntent.UNKNOWN ? ChatIntent.OUT_OF_SCOPE : intent;
    }

    private ChatToolResult resolve(
            ChatIntent intent,
            Role role,
            String username,
            String message,
            Integer year,
            UserAccount user,
            Map<String, String> filters
    ) {
        if (intent == ChatIntent.OUT_OF_SCOPE) {
            return ChatToolResult.reply(OUT_OF_SCOPE_REPLY);
        }
        // Pointage intentionally out of the assistant for now (use app menus).
        if (intent == ChatIntent.MY_ATTENDANCE || intent == ChatIntent.FAQ_ATTENDANCE) {
            String link = role == Role.ADMIN_RH ? "/admin/attendance" : "/employee/my-attendance";
            String label = role == Role.ADMIN_RH ? "Ouvrir le pointage" : "Ouvrir mon pointage";
            return ChatToolResult.reply(ATTENDANCE_DISABLED_REPLY, ATTENDANCE_DISABLED_REPLY, link, label);
        }
        if ((intent.isTool() || intent.isAction()) && !intent.allowedFor(role)) {
            String refusal = role == Role.EMPLOYE ? ROLE_REFUSAL_EMPLOYEE : ROLE_REFUSAL_ADMIN;
            return ChatToolResult.reply(refusal);
        }
        if (intent.isAction()) {
            return resolveAction(intent, message, user, username);
        }
        if (intent == ChatIntent.PENDING_LEAVES && role == Role.ADMIN_RH) {
            String nameToken = filters == null ? null : filters.get(ReadFollowUpResolver.FILTER_EMPLOYEE_NAME);
            return adminLeaveReviewService.listPendingAndSnapshot(user, nameToken);
        }
        if (intent.isTool()) {
            return chatToolService.execute(intent, username, year, role, filters);
        }
        if (intent.isFaq()) {
            String context = ragService.retrieve(message, intent);
            String fallback = context.isBlank()
                    ? "Je n'ai pas trouvé cette procédure dans le guide RH Paie. Utilisez le menu de l'application ou reformulez."
                    : context;
            String link = faqLink(intent, role);
            String label = faqLinkLabel(intent, role);
            return ChatToolResult.reply(context, fallback, link, label);
        }
        return ChatToolResult.reply(OUT_OF_SCOPE_REPLY);
    }

    private void updateReadContextAfterSuccess(
            UserAccount user,
            Role role,
            ChatIntent intent,
            ChatToolResult result,
            Map<String, String> filters
    ) {
        if (user == null || intent == null || !intent.isTool() || !intent.allowedFor(role)) {
            return;
        }
        if (intent == ChatIntent.MY_ATTENDANCE) {
            return;
        }
        String template = result == null ? null : result.templateReply();
        if (template == null || template.isBlank()) {
            return;
        }
        if (template.equals(ROLE_REFUSAL_EMPLOYEE) || template.equals(ROLE_REFUSAL_ADMIN)
                || template.equals(ATTENDANCE_DISABLED_REPLY) || template.equals(OUT_OF_SCOPE_REPLY)) {
            return;
        }
        ChatReadDomain domain = ChatReadDomains.fromIntent(intent);
        if (domain == null) {
            return;
        }
        ConversationContext context = new ConversationContext();
        context.setDomain(domain);
        context.setLastIntent(intent);
        context.setFilters(filters == null ? Map.of() : new LinkedHashMap<>(filters));
        conversationContextStore.save(new ContextKey(user.getId()), context);
    }

    private ChatToolResult resolveAction(ChatIntent intent, String message, UserAccount user, String username) {
        if (intent == ChatIntent.CREATE_LEAVE_REQUEST) {
            return resolveCreateLeave(message, user);
        }
        if (intent == ChatIntent.CANCEL_LEAVE_REQUEST) {
            return resolveCancelLeave(message, user, username);
        }
        if (intent == ChatIntent.REVIEW_LEAVE_BATCH) {
            return adminLeaveReviewService.proposeFromDecision(user, message);
        }
        return ChatToolResult.reply(OUT_OF_SCOPE_REPLY);
    }

    private ChatToolResult resolveCreateLeave(String message, UserAccount user) {
        ChatActionDraft draft = getOrCreateDraft(user, ChatIntent.CREATE_LEAVE_REQUEST.name());
        if (!ChatIntent.CREATE_LEAVE_REQUEST.name().equals(draft.getActionType())) {
            resetDraft(draft, ChatIntent.CREATE_LEAVE_REQUEST.name());
        }
        // Fresh "je veux un congé" without dates/days must restart — don't reuse an old complete draft.
        if (keywordIntentFallback.looksLikeCreateLeave(message)
                && !leaveDraftSlotFiller.hasSchedulingCue(message)) {
            resetDraft(draft, ChatIntent.CREATE_LEAVE_REQUEST.name());
        }
        leaveDraftSlotFiller.merge(draft, message);
        chatActionDraftRepository.save(draft);

        if (!leaveDraftSlotFiller.isComplete(draft)) {
            return ChatToolResult.reply(leaveDraftSlotFiller.nextQuestion(draft));
        }

        ChatProposedAction action = leaveDraftSlotFiller.toProposedAction(draft, issueProposalId(draft));
        String facts = "type=" + action.leaveType()
                + ", debut=" + action.startDate()
                + ", fin=" + action.endDate()
                + ", motif=" + action.reason();
        String reply = "Voici la demande prête à être envoyée :\n"
                + "• Type : " + typeLabel(LeaveType.valueOf(action.leaveType())) + "\n"
                + "• Du " + action.startDate() + " au " + action.endDate() + "\n"
                + "• Motif : " + action.reason() + "\n\n"
                + "Cliquez sur Confirmer pour créer la demande (statut EN_ATTENTE), ou Annuler.";
        return ChatToolResult.propose(
                facts,
                reply,
                "/employee/leave-requests/create",
                "Ouvrir le formulaire",
                action
        );
    }

    private ChatToolResult resolveCancelLeave(String message, UserAccount user, String username) {
        List<LeaveRequestResponse> pending = leaveRequestService.findMyRequests(username).stream()
                .filter(request -> request.status() == LeaveStatus.PENDING)
                .toList();

        if (pending.isEmpty()) {
            clearDraft(user.getId());
            return ChatToolResult.reply(
                    "Vous n'avez aucune demande de congé en attente à annuler.",
                    "Vous n'avez aucune demande de congé en attente à annuler.",
                    "/employee/my-leave-requests",
                    "Voir mes demandes"
            );
        }

        ChatActionDraft draft = getOrCreateDraft(user, ChatIntent.CANCEL_LEAVE_REQUEST.name());
        if (!ChatIntent.CANCEL_LEAVE_REQUEST.name().equals(draft.getActionType())) {
            resetDraft(draft, ChatIntent.CANCEL_LEAVE_REQUEST.name());
        }

        Long requestedId = parseLeaveRequestId(message);
        LeaveRequestResponse target = null;
        if (requestedId != null) {
            target = pending.stream().filter(item -> item.id().equals(requestedId)).findFirst().orElse(null);
            if (target == null) {
                chatActionDraftRepository.save(draft);
                return ChatToolResult.reply(
                        "Je ne trouve pas de demande n°" + requestedId + " en attente. "
                                + listPending(pending)
                );
            }
        } else {
            target = matchByDates(message, pending).orElse(null);
        }

        if (target == null && pending.size() == 1) {
            target = pending.getFirst();
        }

        if (target == null) {
            draft.setLeaveRequestId(null);
            chatActionDraftRepository.save(draft);
            return ChatToolResult.reply(
                    "Plusieurs demandes sont en attente. Précisez le numéro à annuler.\n"
                            + listPending(pending)
                            + "\nExemple : « Annuler la demande " + pending.getFirst().id() + " »."
            );
        }

        draft.setLeaveRequestId(target.id());
        draft.setLeaveType(target.type().name());
        draft.setStartDate(target.startDate());
        draft.setEndDate(target.endDate());
        draft.setReason(target.reason());
        String proposalId = issueProposalId(draft);

        ChatProposedAction action = ChatProposedAction.builder()
                .type("CANCEL_LEAVE_REQUEST")
                .leaveType(target.type().name())
                .startDate(target.startDate())
                .endDate(target.endDate())
                .reason(target.reason())
                .leaveRequestId(target.id())
                .approveLeaveRequestIds(null)
                .rejectLeaveRequestIds(null)
                .proposalId(proposalId)
                .build();
        String reply = "Confirmez l'annulation de la demande n°" + target.id() + " :\n"
                + "• Type : " + typeLabel(target.type()) + "\n"
                + "• Du " + target.startDate() + " au " + target.endDate() + "\n\n"
                + "Cliquez sur Confirmer pour annuler (statut CANCELLED), ou Annuler pour garder la demande.";
        return ChatToolResult.propose(
                "cancelId=" + target.id(),
                reply,
                "/employee/my-leave-requests",
                "Voir mes demandes",
                action
        );
    }

    private String listPending(List<LeaveRequestResponse> pending) {
        return pending.stream()
                .map(item -> "• n°" + item.id() + " : " + typeLabel(item.type())
                        + " du " + item.startDate() + " au " + item.endDate())
                .collect(Collectors.joining("\n"));
    }

    private Long parseLeaveRequestId(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String raw = message.trim();
        Matcher bare = BARE_LEAVE_ID.matcher(raw);
        if (bare.matches()) {
            return Long.parseLong(bare.group(1));
        }
        Matcher matcher = LEAVE_ID.matcher(raw);
        Long found = null;
        while (matcher.find()) {
            found = Long.parseLong(matcher.group(1));
        }
        return found;
    }

    private Optional<LeaveRequestResponse> matchByDates(String message, List<LeaveRequestResponse> pending) {
        Matcher iso = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})").matcher(message == null ? "" : message);
        LocalDate first = null;
        LocalDate second = null;
        while (iso.find()) {
            LocalDate day = LocalDate.parse(iso.group(1));
            if (first == null) {
                first = day;
            } else {
                second = day;
            }
        }
        if (first == null) {
            return Optional.empty();
        }
        LocalDate start = first;
        LocalDate end = second == null ? first : second;
        return pending.stream()
                .filter(item -> item.startDate().equals(start) && item.endDate().equals(end))
                .findFirst();
    }

    public boolean hasOpenDraft(String username) {
        return userRepository.findByUsername(username)
                .map(this::activeDraft)
                .orElse(Optional.empty())
                .isPresent();
    }

    private ChatActionDraft requireLiveDraft(UserAccount user, String actionType) {
        Optional<ChatActionDraft> found = chatActionDraftRepository.findByUser_Id(user.getId());
        if (found.isEmpty()) {
            throw draftRejected("Aucune confirmation en attente. Reprenez la demande dans le chat.");
        }
        ChatActionDraft draft = found.get();
        if (draft.getUpdatedAt() != null
                && draft.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(DRAFT_TTL_MINUTES))) {
            chatActionDraftRepository.delete(draft);
            throw draftRejected("Le brouillon a expiré. Reprenez la demande dans le chat.");
        }
        if (!actionType.equals(draft.getActionType())) {
            throw draftRejected("Ce brouillon ne correspond pas à cette confirmation.");
        }
        return draft;
    }

    private void requireProposalMatch(ChatActionDraft draft, String proposalId) {
        if (proposalId == null || proposalId.isBlank()
                || draft.getProposalId() == null
                || !draft.getProposalId().equals(proposalId)) {
            throw draftRejected("Cette confirmation ne correspond pas au brouillon en cours.");
        }
    }

    private String issueProposalId(ChatActionDraft draft) {
        String proposalId = java.util.UUID.randomUUID().toString();
        draft.setProposalId(proposalId);
        chatActionDraftRepository.save(draft);
        return proposalId;
    }

    private LeaveType draftLeaveType(ChatActionDraft draft) {
        if (draft.getLeaveType() == null || draft.getLeaveType().isBlank()) {
            return LeaveType.PAID_LEAVE;
        }
        try {
            return LeaveType.valueOf(draft.getLeaveType());
        } catch (IllegalArgumentException exception) {
            throw draftRejected("Le brouillon est incomplet. Reprenez la demande dans le chat.");
        }
    }

    private ResponseStatusException draftRejected(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ReviewProposal readReviewProposal(ChatActionDraft draft) {
        if (draft.getPayloadJson() == null || draft.getPayloadJson().isBlank()) {
            return new ReviewProposal(List.of(), List.of(), "Refus via assistant RH");
        }
        try {
            Map<String, Object> payload = REVIEW_JSON.readValue(draft.getPayloadJson(), new TypeReference<>() {
            });
            String reason = payload.get("decisionReason") instanceof String text && !text.isBlank()
                    ? text
                    : "Refus via assistant RH";
            return new ReviewProposal(readIdList(payload.get("approveIds")), readIdList(payload.get("rejectIds")), reason);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return new ReviewProposal(List.of(), List.of(), "Refus via assistant RH");
        }
    }

    private List<Long> readIdList(Object raw) {
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
    }

    private boolean sameIds(List<Long> left, List<Long> right) {
        return left.size() == right.size() && new java.util.HashSet<>(left).equals(new java.util.HashSet<>(right));
    }

    private record ReviewProposal(List<Long> approveIds, List<Long> rejectIds, String reason) {
    }

    private Optional<ChatActionDraft> activeDraft(UserAccount user) {
        Optional<ChatActionDraft> draft = chatActionDraftRepository.findByUser_Id(user.getId());
        if (draft.isEmpty()) {
            return Optional.empty();
        }
        ChatActionDraft value = draft.get();
        if (value.getUpdatedAt() != null
                && value.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(DRAFT_TTL_MINUTES))) {
            // Soft-expire: keep the unique row so the next getOrCreate can reuse it
            // (delete + insert in the same TX races the unique constraint on user_id).
            return Optional.empty();
        }
        return draft;
    }

    private ChatActionDraft getOrCreateDraft(UserAccount user, String actionType) {
        Optional<ChatActionDraft> existing = chatActionDraftRepository.findByUser_Id(user.getId());
        if (existing.isPresent()) {
            ChatActionDraft draft = existing.get();
            boolean expired = draft.getUpdatedAt() != null
                    && draft.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(DRAFT_TTL_MINUTES));
            if (expired || !actionType.equals(draft.getActionType())) {
                resetDraft(draft, actionType);
            }
            return draft;
        }
        ChatActionDraft draft = ChatActionDraft.builder()
                .user(user)
                .actionType(actionType)
                .leaveType(LeaveType.PAID_LEAVE.name())
                .reason("Demande via assistant RH")
                .proposalId(java.util.UUID.randomUUID().toString())
                .build();
        return chatActionDraftRepository.save(draft);
    }

    private void resetDraft(ChatActionDraft draft, String actionType) {
        draft.setActionType(actionType);
        draft.setLeaveType(LeaveType.PAID_LEAVE.name());
        draft.setStartDate(null);
        draft.setEndDate(null);
        draft.setDays(null);
        draft.setReason("Demande via assistant RH");
        draft.setLeaveRequestId(null);
        draft.setPayloadJson(null);
        draft.setProposalId(java.util.UUID.randomUUID().toString());
    }

    private void clearDraft(Long userId) {
        chatActionDraftRepository.findByUser_Id(userId).ifPresent(chatActionDraftRepository::delete);
    }

    private String phrase(String question, String facts, String templateReply, String history, ChatIntent intent) {
        String context = (facts == null || facts.isBlank()) ? templateReply : facts;
        String generated = ollamaClient.generate(question, context, history);
        if (generated == null || generated.isBlank() || looksUngrounded(generated, templateReply)
                || (intent != null && intent.isFaq() && faqAddsForeignNumbers(generated, templateReply))) {
            return templateReply;
        }
        return generated.length() > 4000 ? generated.substring(0, 4000) : generated;
    }

    private boolean preferTemplateOnly(String facts, String templateReply) {
        String factsValue = facts == null ? "" : facts;
        String template = templateReply == null ? "" : templateReply.toLowerCase();
        return factsValue.contains(":0")
                || factsValue.contains("documentsSortie=0")
                || factsValue.contains("contrats=0")
                || factsValue.contains("absences=0")
                || template.startsWith("aucun");
    }

    private boolean looksUngrounded(String generated, String templateReply) {
        if (templateReply == null || templateReply.isBlank() || !templateReply.matches(".*\\d.*")) {
            return false;
        }
        String lower = generated.toLowerCase(Locale.ROOT);
        boolean deniesData = lower.contains("ne dispose pas")
                || lower.contains("je n'ai pas")
                || lower.contains("je n’ai pas")
                || lower.contains("aucune information")
                || lower.contains("pas d'informations")
                || lower.contains("pas d’informations")
                || lower.contains("ne possède pas")
                || lower.contains("pas disponible")
                || lower.contains("pas disponibles")
                || lower.contains("ne sont pas disponible")
                || lower.contains("n'est pas disponible")
                || lower.contains("n’est pas disponible")
                || lower.contains("données précises")
                || lower.contains("effectuer un calcul");
        if (deniesData) {
            return true;
        }
        if (templateReply.matches(".*\\d.*") && !generated.matches(".*\\d.*")) {
            return true;
        }
        // Every number in the template must survive rephrasing. A swapped "12" → "10" is ungrounded.
        Matcher templateNumbers = Pattern.compile("\\d+").matcher(templateReply);
        while (templateNumbers.find()) {
            if (!generated.contains(templateNumbers.group())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A FAQ rephrase may keep guide numbers (page size, steps) but must not
     * introduce a count, date, or balance that is not in the current guide.
     */
    private boolean faqAddsForeignNumbers(String generated, String templateReply) {
        if (generated == null || templateReply == null) {
            return false;
        }
        java.util.Set<String> allowed = new java.util.HashSet<>();
        Matcher templateNumbers = Pattern.compile("\\d+").matcher(templateReply);
        while (templateNumbers.find()) {
            allowed.add(templateNumbers.group());
        }
        Matcher generatedNumbers = Pattern.compile("\\d+").matcher(generated);
        while (generatedNumbers.find()) {
            if (!allowed.contains(generatedNumbers.group())) {
                return true;
            }
        }
        return false;
    }

    private void persist(UserAccount user, Role role, String message, String reply, ChatIntent intent, String link) {
        chatMessageRepository.save(ChatMessage.builder()
                .user(user)
                .role(role)
                .message(message)
                .reply(reply)
                .intent(intent.name())
                .link(link)
                .build());
    }

    private String faqLink(ChatIntent intent, Role role) {
        boolean admin = role == Role.ADMIN_RH;
        return switch (intent) {
            case FAQ_LEAVE -> admin ? "/admin/leave-requests" : "/employee/leave-requests/create";
            case FAQ_PAYROLL -> admin ? "/admin/payroll-calculations" : "/employee/my-payslips";
            case FAQ_ATTENDANCE -> admin ? "/admin/attendance" : "/employee/my-attendance";
            default -> admin ? "/admin/dashboard" : "/employee/dashboard";
        };
    }

    private String faqLinkLabel(ChatIntent intent, Role role) {
        boolean admin = role == Role.ADMIN_RH;
        return switch (intent) {
            case FAQ_LEAVE -> admin ? "Ouvrir les demandes de congé" : "Créer une demande de congé";
            case FAQ_PAYROLL -> admin ? "Ouvrir les calculs de paie" : "Voir mes bulletins";
            case FAQ_ATTENDANCE -> admin ? "Ouvrir le pointage" : "Ouvrir mon pointage";
            default -> "Ouvrir le tableau de bord";
        };
    }

    private String sourceKind(ChatIntent intent, Role role) {
        if (intent == ChatIntent.OUT_OF_SCOPE) {
            return "OUT_OF_SCOPE";
        }
        if ((intent.isTool() || intent.isAction()) && !intent.allowedFor(role)) {
            return "REFUSED";
        }
        if (intent.isAction()) {
            return "ACTION";
        }
        if (intent.isFaq()) {
            return "FAQ";
        }
        if (intent.isTool()) {
            return "DATA";
        }
        return "OUT_OF_SCOPE";
    }

    private String sourceLabel(ChatIntent intent, Role role) {
        if (intent == ChatIntent.OUT_OF_SCOPE) {
            return "Hors sujet";
        }
        if ((intent.isTool() || intent.isAction()) && !intent.allowedFor(role)) {
            return "Accès refusé (rôle)";
        }
        return switch (intent) {
            case LEAVE_BALANCE -> "Données : solde de congé";
            case MY_LEAVE_REQUESTS -> "Données : mes demandes";
            case MY_PAYSLIPS -> "Données : bulletins";
            case MY_CONTRACTS -> "Données : contrats";
            case MY_ATTENDANCE -> "Données : pointage";
            case LEAVE_CALENDAR -> "Données : calendrier des absences";
            case MY_SALARIES -> "Données : salaires";
            case MY_EXIT_DOCUMENTS -> "Données : documents de sortie";
            case PENDING_LEAVES -> "Données : congés en attente";
            case EXPIRING_CONTRACTS -> "Données : contrats à échéance";
            case PAYROLL_STATUS -> "Données : paie du mois";
            case EXIT_SUMMARY -> "Données : sorties / départs";
            case CREATE_LEAVE_REQUEST -> "Action : créer une demande";
            case CANCEL_LEAVE_REQUEST -> "Action : annuler une demande";
            case REVIEW_LEAVE_BATCH -> "Action : revue des demandes";
            case FAQ_LEAVE -> "Guide : congés";
            case FAQ_PAYROLL -> "Guide : paie";
            case FAQ_ATTENDANCE -> "Guide : pointage";
            default -> "Assistant RH";
        };
    }

    private String typeLabel(LeaveType type) {
        return switch (type) {
            case PAID_LEAVE -> "Congé payé";
            case SICK_LEAVE -> "Congé maladie";
            case UNPAID_LEAVE -> "Congé sans solde";
            case MATERNITY_LEAVE -> "Congé maternité";
            case OTHER -> "Autre absence";
        };
    }

    public static List<String> suggestionsFor(Role role) {
        if (role == Role.ADMIN_RH) {
            return List.of(
                    "Combien de congés en attente ?",
                    "Accepter tous",
                    "Quels contrats expirent bientôt ?",
                    "Qui est en congé cette semaine ?"
            );
        }
        return List.of(
                "Je veux un congé",
                "Annuler ma demande en attente",
                "Combien de jours de congé me reste-t-il ?",
                "Mon calendrier des absences"
        );
    }
}

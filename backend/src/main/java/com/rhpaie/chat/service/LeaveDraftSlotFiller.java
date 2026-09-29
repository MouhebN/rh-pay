package com.rhpaie.chat.service;

import com.rhpaie.chat.context.ReadFollowUpResolver;
import com.rhpaie.chat.dto.ChatProposedAction;
import com.rhpaie.chat.entity.ChatActionDraft;
import com.rhpaie.leave.entity.LeaveType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leave draft slot conversation: applies structured date extraction to {@link ChatActionDraft}
 * and fills non-date slots (type / reason). Date NL lives in {@link LeaveDateExtractor}.
 */
@Component
public class LeaveDraftSlotFiller {

    private static final Pattern MOTIF = Pattern.compile(
            "(?:motif|raison|parce que|pour)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private final LeaveDateExtractor leaveDateExtractor;

    public LeaveDraftSlotFiller(LeaveDateExtractor leaveDateExtractor) {
        this.leaveDateExtractor = leaveDateExtractor;
    }

    public void merge(ChatActionDraft draft, String message) {
        merge(draft, message, LocalDate.now());
    }

    public void merge(ChatActionDraft draft, String message, LocalDate today) {
        if (message == null || message.isBlank()) {
            return;
        }
        LeaveType type = resolveType(message);
        if (type != null) {
            draft.setLeaveType(type.name());
        } else if (draft.getLeaveType() == null || draft.getLeaveType().isBlank()) {
            draft.setLeaveType(LeaveType.PAID_LEAVE.name());
        }

        String reason = resolveReason(message);
        if (reason != null) {
            draft.setReason(reason);
        } else if (draft.getReason() == null || draft.getReason().isBlank()) {
            draft.setReason("Demande via assistant RH");
        }

        DateExtractionResult extracted = leaveDateExtractor.extract(message, today);
        Integer days = extracted.durationDays();
        if (days != null) {
            draft.setDays(days);
        }

        if (extracted.hasDates()) {
            LocalDate start = extracted.startDate();
            LocalDate end = extracted.endDate();
            boolean singleDay = start.equals(end);
            boolean rangeAlreadySized = !singleDay;
            draft.setStartDate(start);
            if (singleDay && draft.getDays() != null && draft.getDays() > 1) {
                draft.setEndDate(null);
            } else {
                draft.setEndDate(end);
                if (singleDay && days == null && draft.getDays() == null) {
                    draft.setDays(1);
                }
                if (rangeAlreadySized) {
                    draft.setDays((int) ChronoUnit.DAYS.between(start, end) + 1);
                }
            }
        }

        computeEndFromDays(draft);
        normalizeOrder(draft);
    }

    public boolean isComplete(ChatActionDraft draft) {
        return draft.getStartDate() != null && draft.getEndDate() != null;
    }

    public String nextQuestion(ChatActionDraft draft) {
        String echo = echoKnown(draft);
        if (draft.getStartDate() == null && draft.getDays() == null) {
            return echo + "Combien de jours souhaitez-vous, et à partir de quelle date ? "
                    + "(ex. « 3 jours à partir de demain », « du 10 au 12 octobre », "
                    + "« de 20 septembre a 22 septembre », « 10/09/2026 »)";
        }
        if (draft.getStartDate() == null) {
            return echo + "À partir de quelle date ? (ex. demain, lundi, weekend prochain, 2026-10-20)";
        }
        if (draft.getEndDate() == null) {
            return echo + "Combien de jours, ou jusqu'à quelle date ?";
        }
        return echo + "Il me manque encore une information pour finaliser la demande.";
    }

    public ChatProposedAction toProposedAction(ChatActionDraft draft, String proposalId) {
        computeEndFromDays(draft);
        normalizeOrder(draft);
        String leaveType = draft.getLeaveType() == null || draft.getLeaveType().isBlank()
                ? LeaveType.PAID_LEAVE.name()
                : draft.getLeaveType();
        String reason = draft.getReason() == null || draft.getReason().isBlank()
                ? "Demande via assistant RH"
                : draft.getReason();
        return ChatProposedAction.builder()
                .type("CREATE_LEAVE_REQUEST")
                .leaveType(leaveType)
                .startDate(draft.getStartDate())
                .endDate(draft.getEndDate())
                .reason(reason)
                .leaveRequestId(null)
                .approveLeaveRequestIds(null)
                .rejectLeaveRequestIds(null)
                .proposalId(proposalId)
                .build();
    }

    public boolean hasSchedulingCue(String message) {
        return hasSchedulingCue(message, LocalDate.now());
    }

    public boolean hasSchedulingCue(String message, LocalDate today) {
        if (message == null || message.isBlank()) {
            return false;
        }
        DateExtractionResult extracted = leaveDateExtractor.extract(message, today);
        return extracted.hasDuration() || extracted.hasDates();
    }

    public boolean looksLikeSlotAnswer(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (ReadFollowUpResolver.looksLikeEmployeeSelfRead(message)) {
            return false;
        }
        String text = normalize(message);
        if (containsAny(text, "annuler", "annule", "anuller", "anulle", "anuler", "anule",
                "cancel", "retirer", "supprimer")) {
            return false;
        }
        if (message.trim().length() <= 40) {
            return true;
        }
        DateExtractionResult extracted = leaveDateExtractor.extract(message, LocalDate.now());
        return extracted.hasDuration()
                || extracted.hasDates()
                || resolveType(message) != null
                || resolveReason(message) != null
                || containsAny(text, "demain", "aujourd", "lundi", "mardi", "mercredi", "jeudi", "vendredi",
                "jour", "jours", "semaine", "weekend", "week end", "week-end", "octobre", "septembre");
    }

    private String echoKnown(ChatActionDraft draft) {
        StringBuilder echo = new StringBuilder();
        if (draft.getDays() != null) {
            echo.append("Noté : ").append(draft.getDays()).append(" jour(s)");
            if (draft.getStartDate() != null) {
                echo.append(" à partir du ").append(draft.getStartDate());
            }
            echo.append(". ");
        } else if (draft.getStartDate() != null) {
            echo.append("Noté : début le ").append(draft.getStartDate()).append(". ");
        } else {
            echo.append("D'accord. ");
        }
        return echo.toString();
    }

    private void computeEndFromDays(ChatActionDraft draft) {
        if (draft.getStartDate() != null && draft.getEndDate() == null && draft.getDays() != null && draft.getDays() > 0) {
            draft.setEndDate(draft.getStartDate().plusDays(draft.getDays() - 1L));
        }
        if (draft.getStartDate() != null && draft.getEndDate() != null && draft.getDays() == null) {
            long days = ChronoUnit.DAYS.between(draft.getStartDate(), draft.getEndDate()) + 1;
            draft.setDays((int) days);
        }
    }

    private void normalizeOrder(ChatActionDraft draft) {
        if (draft.getStartDate() != null && draft.getEndDate() != null
                && draft.getEndDate().isBefore(draft.getStartDate())) {
            LocalDate tmp = draft.getStartDate();
            draft.setStartDate(draft.getEndDate());
            draft.setEndDate(tmp);
        }
    }

    private LeaveType resolveType(String message) {
        String text = normalize(message);
        if (containsAny(text, "maladie", "sick")) {
            return LeaveType.SICK_LEAVE;
        }
        if (containsAny(text, "sans solde", "unpaid", "non paye")) {
            return LeaveType.UNPAID_LEAVE;
        }
        if (containsAny(text, "maternite", "maternity")) {
            return LeaveType.MATERNITY_LEAVE;
        }
        if (containsAny(text, "teletravail", "remote") && containsAny(text, "autre", "other")) {
            return LeaveType.OTHER;
        }
        if (containsAny(text, "conge paye", "cp ", " paid")) {
            return LeaveType.PAID_LEAVE;
        }
        return null;
    }

    private String resolveReason(String message) {
        Matcher matcher = MOTIF.matcher(message.trim());
        if (matcher.find()) {
            String reason = matcher.group(1).trim();
            if (reason.length() > 200) {
                reason = reason.substring(0, 200);
            }
            if (!reason.isBlank()
                    && !normalize(reason).matches(".*(demain|aujourd|lundi|mardi|jours?|conge|weekend|octobre).*")) {
                return reason;
            }
        }
        return null;
    }

    private String normalize(String message) {
        return TextNormalize.soften(message);
    }

    private boolean containsAny(String text, String... needles) {
        return TextNormalize.containsAny(text, needles);
    }
}

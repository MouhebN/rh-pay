package com.rhpaie.chat.service;

import com.rhpaie.chat.dto.ChatGreetingResponse;
import com.rhpaie.chat.dto.GreetingFact;
import com.rhpaie.contract.dto.ContractMonitoringResponse;
import com.rhpaie.contract.service.ContractService;
import com.rhpaie.leave.dto.AdminLeaveSummaryResponse;
import com.rhpaie.leave.dto.EmployeeLeaveSummaryResponse;
import com.rhpaie.leave.service.LeaveRequestService;
import com.rhpaie.leavebalance.dto.LeaveBalanceSummaryResponse;
import com.rhpaie.leavebalance.service.LeaveBalanceService;
import com.rhpaie.user.entity.Role;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Role-aware, READ-only proactive greeting for the floating chat widget.
 * Does not mutate ConversationContext and does not call Ollama.
 */
@Service
public class ChatGreetingService {

    static final String FALLBACK_MESSAGE =
            "Bonjour, comment puis-je vous aider ?";

    private static final Logger log = LoggerFactory.getLogger(ChatGreetingService.class);

    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveRequestService leaveRequestService;
    private final ContractService contractService;

    public ChatGreetingService(
            UserRepository userRepository,
            LeaveBalanceService leaveBalanceService,
            LeaveRequestService leaveRequestService,
            ContractService contractService
    ) {
        this.userRepository = userRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.leaveRequestService = leaveRequestService;
        this.contractService = contractService;
    }

    public ChatGreetingResponse greetingFor(String username) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        Role role = user.getRole();
        if (role == Role.EMPLOYE) {
            return employeeGreeting(username);
        }
        if (role == Role.ADMIN_RH) {
            return adminGreeting();
        }
        return new ChatGreetingResponse(FALLBACK_MESSAGE, List.of(), false);
    }

    private ChatGreetingResponse employeeGreeting(String username) {
        List<GreetingFact> facts = new ArrayList<>();
        Double remaining = null;
        Long pending = null;

        try {
            LeaveBalanceSummaryResponse balance = leaveBalanceService.findMyBalance(
                    username, LocalDate.now().getYear());
            if (balance != null && balance.remainingDays() != null) {
                remaining = balance.remainingDays();
                facts.add(new GreetingFact("LEAVE_BALANCE", formatDaysValue(remaining)));
            }
        } catch (Exception exception) {
            log.warn("Chat greeting: leave balance unavailable for {}: {}", username, exception.getMessage());
        }

        try {
            EmployeeLeaveSummaryResponse summary = leaveRequestService.mySummary(username);
            if (summary != null) {
                pending = summary.pendingRequests();
                facts.add(new GreetingFact("PENDING_LEAVE_COUNT", String.valueOf(pending)));
            }
        } catch (Exception exception) {
            log.warn("Chat greeting: leave summary unavailable for {}: {}", username, exception.getMessage());
        }

        String message = phraseEmployee(remaining, pending);
        return new ChatGreetingResponse(message, List.copyOf(facts), false);
    }

    private ChatGreetingResponse adminGreeting() {
        List<GreetingFact> facts = new ArrayList<>();
        Long pendingLeaves = null;
        Long expiringContracts = null;

        try {
            AdminLeaveSummaryResponse summary = leaveRequestService.adminSummary();
            if (summary != null) {
                pendingLeaves = summary.pendingCount();
                facts.add(new GreetingFact("PENDING_LEAVE_COUNT", String.valueOf(pendingLeaves)));
            }
        } catch (Exception exception) {
            log.warn("Chat greeting: admin leave summary unavailable: {}", exception.getMessage());
        }

        try {
            ContractMonitoringResponse monitoring = contractService.monitoring();
            if (monitoring != null && monitoring.expiringSoonCount() != null) {
                expiringContracts = monitoring.expiringSoonCount();
                facts.add(new GreetingFact("EXPIRING_CONTRACT_COUNT", String.valueOf(expiringContracts)));
            }
        } catch (Exception exception) {
            log.warn("Chat greeting: contract monitoring unavailable: {}", exception.getMessage());
        }

        String message = phraseAdmin(pendingLeaves, expiringContracts);
        return new ChatGreetingResponse(message, List.copyOf(facts), false);
    }

    static String phraseEmployee(Double remainingDays, Long pendingRequests) {
        if (remainingDays == null && pendingRequests == null) {
            return FALLBACK_MESSAGE;
        }
        StringBuilder builder = new StringBuilder("Bonjour. ");
        if (remainingDays != null && pendingRequests != null) {
            builder.append("Vous disposez de ").append(formatDaysPhrase(remainingDays))
                    .append(" et vous avez ").append(formatPendingRequests(pendingRequests)).append(".");
        } else if (remainingDays != null) {
            builder.append("Vous disposez de ").append(formatDaysPhrase(remainingDays)).append(".");
        } else {
            builder.append("Vous avez ").append(formatPendingRequests(pendingRequests)).append(".");
        }
        builder.append(" Que souhaitez-vous consulter ?");
        return builder.toString();
    }

    static String phraseAdmin(Long pendingLeaves, Long expiringContracts) {
        if (pendingLeaves == null && expiringContracts == null) {
            return FALLBACK_MESSAGE;
        }
        if (isZero(pendingLeaves) && isZero(expiringContracts)) {
            return "Bonjour. Aucune demande de congé en attente et aucun contrat arrivant prochainement à échéance.";
        }
        if (pendingLeaves != null && expiringContracts != null) {
            return "Bonjour. Il y a " + formatPendingLeaves(pendingLeaves)
                    + " et " + formatExpiringContracts(expiringContracts) + ".";
        }
        if (pendingLeaves != null) {
            return "Bonjour. Il y a " + formatPendingLeaves(pendingLeaves) + ".";
        }
        return "Bonjour. Il y a " + formatExpiringContracts(expiringContracts) + ".";
    }

    private static boolean isZero(Long value) {
        return value != null && value == 0L;
    }

    private static String formatPendingRequests(long count) {
        if (count == 1L) {
            return "1 demande en attente";
        }
        return count + " demandes en attente";
    }

    private static String formatPendingLeaves(long count) {
        if (count == 1L) {
            return "1 demande de congé en attente";
        }
        return count + " demandes de congé en attente";
    }

    private static String formatExpiringContracts(long count) {
        if (count == 1L) {
            return "1 contrat qui expire prochainement";
        }
        return count + " contrats qui expirent prochainement";
    }

    private static String formatDaysPhrase(double days) {
        String value = formatDaysValue(days);
        if ("1".equals(value) || "1,0".equals(value) || "1.0".equals(value)) {
            return "1 jour de congé";
        }
        return value + " jours de congé";
    }

    private static String formatDaysValue(double days) {
        if (days == Math.rint(days)) {
            return String.valueOf((long) days);
        }
        return String.format(Locale.FRANCE, "%.1f", days);
    }
}

package com.rhpaie.chat.service;

import com.rhpaie.chat.dto.ChatGreetingResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatGreetingServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LeaveBalanceService leaveBalanceService;
    @Mock private LeaveRequestService leaveRequestService;
    @Mock private ContractService contractService;

    private ChatGreetingService service;

    @BeforeEach
    void setUp() {
        service = new ChatGreetingService(
                userRepository, leaveBalanceService, leaveRequestService, contractService);
    }

    @Test
    void employeeGreeting_balanceAndPending() {
        stubUser("alice", Role.EMPLOYE);
        when(leaveBalanceService.findMyBalance(eq("alice"), anyInt()))
                .thenReturn(LeaveBalanceSummaryResponse.builder()
                        .year(2026).remainingDays(12.0).acquiredDays(25.0).consumedDays(13.0).pendingDays(0.0)
                        .build());
        when(leaveRequestService.mySummary("alice"))
                .thenReturn(EmployeeLeaveSummaryResponse.builder()
                        .pendingRequests(1).approvedThisYear(2).rejectedThisYear(0).build());

        ChatGreetingResponse response = service.greetingFor("alice");

        assertTrue(response.message().contains("12 jours de congé"));
        assertTrue(response.message().contains("1 demande en attente"));
        assertEquals(2, response.facts().size());
    }

    @Test
    void employeeGreeting_zeroPending() {
        stubUser("alice", Role.EMPLOYE);
        when(leaveBalanceService.findMyBalance(eq("alice"), anyInt()))
                .thenReturn(LeaveBalanceSummaryResponse.builder().remainingDays(8.0).build());
        when(leaveRequestService.mySummary("alice"))
                .thenReturn(EmployeeLeaveSummaryResponse.builder()
                        .pendingRequests(0).approvedThisYear(0).rejectedThisYear(0).build());

        ChatGreetingResponse response = service.greetingFor("alice");

        assertTrue(response.message().contains("8 jours de congé"));
        assertTrue(response.message().contains("0 demandes en attente"));
    }

    @Test
    void employeeGreeting_oneServiceFails_continues() {
        stubUser("alice", Role.EMPLOYE);
        when(leaveBalanceService.findMyBalance(eq("alice"), anyInt()))
                .thenThrow(new RuntimeException("db down"));
        when(leaveRequestService.mySummary("alice"))
                .thenReturn(EmployeeLeaveSummaryResponse.builder()
                        .pendingRequests(2).approvedThisYear(0).rejectedThisYear(0).build());

        ChatGreetingResponse response = service.greetingFor("alice");

        assertTrue(response.message().contains("2 demandes en attente"));
        assertFalse(response.message().contains("jours de congé"));
        assertEquals(1, response.facts().size());
    }

    @Test
    void employeeGreeting_allFail_fallback() {
        stubUser("alice", Role.EMPLOYE);
        when(leaveBalanceService.findMyBalance(anyString(), anyInt())).thenThrow(new RuntimeException("x"));
        when(leaveRequestService.mySummary(anyString())).thenThrow(new RuntimeException("y"));

        ChatGreetingResponse response = service.greetingFor("alice");

        assertEquals(ChatGreetingService.FALLBACK_MESSAGE, response.message());
        assertTrue(response.facts().isEmpty());
    }

    @Test
    void adminGreeting_pendingAndExpiring() {
        stubUser("admin", Role.ADMIN_RH);
        when(leaveRequestService.adminSummary())
                .thenReturn(AdminLeaveSummaryResponse.builder()
                        .pendingCount(4).approvedThisMonth(1).rejectedThisMonth(0).cancelledThisMonth(0)
                        .build());
        when(contractService.monitoring())
                .thenReturn(ContractMonitoringResponse.builder()
                        .activeContractsCount(10L).expiringSoonCount(2L).expiredContractsCount(0L)
                        .missingDocumentCount(0L).actionRequiredCount(2L)
                        .build());

        ChatGreetingResponse response = service.greetingFor("admin");

        assertTrue(response.message().contains("4 demandes de congé en attente"));
        assertTrue(response.message().contains("2 contrats qui expirent prochainement"));
        verify(leaveBalanceService, never()).findMyBalance(anyString(), anyInt());
    }

    @Test
    void adminGreeting_bothZero_neutralPhrase() {
        stubUser("admin", Role.ADMIN_RH);
        when(leaveRequestService.adminSummary())
                .thenReturn(AdminLeaveSummaryResponse.builder().pendingCount(0).build());
        when(contractService.monitoring())
                .thenReturn(ContractMonitoringResponse.builder().expiringSoonCount(0L).build());

        ChatGreetingResponse response = service.greetingFor("admin");

        assertTrue(response.message().contains("Aucune demande de congé en attente"));
        assertTrue(response.message().contains("aucun contrat arrivant prochainement"));
    }

    @Test
    void adminGreeting_partialFailure() {
        stubUser("admin", Role.ADMIN_RH);
        when(leaveRequestService.adminSummary()).thenThrow(new RuntimeException("x"));
        when(contractService.monitoring())
                .thenReturn(ContractMonitoringResponse.builder().expiringSoonCount(1L).build());

        ChatGreetingResponse response = service.greetingFor("admin");

        assertTrue(response.message().contains("1 contrat qui expire prochainement"));
        assertEquals(1, response.facts().size());
    }

    @Test
    void greetingServiceDoesNotDependOnConversationContext() {
        assertTrue(Arrays.stream(ChatGreetingService.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName)
                .noneMatch(name -> name.contains("ConversationContext")));
    }

    @Test
    void phraseEmployee_pluralization() {
        assertTrue(ChatGreetingService.phraseEmployee(1.0, 1L).contains("1 jour de congé"));
        assertTrue(ChatGreetingService.phraseEmployee(1.0, 1L).contains("1 demande en attente"));
        assertTrue(ChatGreetingService.phraseEmployee(2.0, 2L).contains("2 jours de congé"));
        assertTrue(ChatGreetingService.phraseEmployee(2.0, 2L).contains("2 demandes en attente"));
    }

    private void stubUser(String username, Role role) {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(
                UserAccount.builder().id(1L).username(username).role(role).build()));
    }
}

package com.rhpaie.chat.context;

import com.rhpaie.chat.entity.ChatIntent;

/**
 * Maps successful READ intents onto a single active conversation domain.
 */
public final class ChatReadDomains {

    private ChatReadDomains() {
    }

    public static ChatReadDomain fromIntent(ChatIntent intent) {
        if (intent == null) {
            return null;
        }
        return switch (intent) {
            case LEAVE_BALANCE, MY_LEAVE_REQUESTS, LEAVE_CALENDAR, PENDING_LEAVES -> ChatReadDomain.LEAVE;
            case MY_CONTRACTS, EXPIRING_CONTRACTS -> ChatReadDomain.CONTRACT;
            case MY_PAYSLIPS -> ChatReadDomain.PAYSLIP;
            case MY_SALARIES, PAYROLL_STATUS -> ChatReadDomain.PAYROLL;
            case MY_EXIT_DOCUMENTS, EXIT_SUMMARY -> ChatReadDomain.EXIT_DOCUMENT;
            default -> null;
        };
    }
}

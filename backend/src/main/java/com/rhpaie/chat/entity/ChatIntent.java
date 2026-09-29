package com.rhpaie.chat.entity;

import com.rhpaie.user.entity.Role;

import java.util.Locale;

public enum ChatIntent {
    LEAVE_BALANCE,
    MY_LEAVE_REQUESTS,
    MY_PAYSLIPS,
    MY_CONTRACTS,
    MY_ATTENDANCE,
    LEAVE_CALENDAR,
    MY_SALARIES,
    MY_EXIT_DOCUMENTS,
    PENDING_LEAVES,
    EXPIRING_CONTRACTS,
    PAYROLL_STATUS,
    EXIT_SUMMARY,
    CREATE_LEAVE_REQUEST,
    CANCEL_LEAVE_REQUEST,
    REVIEW_LEAVE_BATCH,
    FAQ_LEAVE,
    FAQ_PAYROLL,
    FAQ_ATTENDANCE,
    OUT_OF_SCOPE,
    UNKNOWN;

    public static ChatIntent fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return UNKNOWN;
        }
        try {
            return ChatIntent.valueOf(label.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }

    public boolean isTool() {
        return switch (this) {
            case LEAVE_BALANCE, MY_LEAVE_REQUESTS, MY_PAYSLIPS, MY_CONTRACTS, MY_ATTENDANCE,
                 LEAVE_CALENDAR, MY_SALARIES, MY_EXIT_DOCUMENTS,
                 PENDING_LEAVES, EXPIRING_CONTRACTS, PAYROLL_STATUS, EXIT_SUMMARY -> true;
            default -> false;
        };
    }

    public boolean isAction() {
        return this == CREATE_LEAVE_REQUEST || this == CANCEL_LEAVE_REQUEST || this == REVIEW_LEAVE_BATCH;
    }

    public boolean isFaq() {
        return this == FAQ_LEAVE || this == FAQ_PAYROLL || this == FAQ_ATTENDANCE;
    }

    public boolean allowedFor(Role role) {
        return switch (this) {
            case LEAVE_BALANCE, MY_LEAVE_REQUESTS, MY_PAYSLIPS, MY_CONTRACTS, MY_ATTENDANCE,
                 MY_SALARIES, MY_EXIT_DOCUMENTS, CREATE_LEAVE_REQUEST, CANCEL_LEAVE_REQUEST -> role == Role.EMPLOYE;
            case PENDING_LEAVES, EXPIRING_CONTRACTS, PAYROLL_STATUS, EXIT_SUMMARY, REVIEW_LEAVE_BATCH -> role == Role.ADMIN_RH;
            default -> true;
        };
    }
}

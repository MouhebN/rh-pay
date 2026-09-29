package com.rhpaie.payrollexport.model;

import com.rhpaie.employee.entity.PaymentMethod;

import java.math.BigDecimal;

public record BankTransferRow(
        String matricule,
        String employeeFullName,
        String rib,
        String bankName,
        PaymentMethod paymentMethod,
        BigDecimal netSalary,
        Integer month,
        Integer year,
        String reference,
        String warning
) {
}

package com.rhpaie.payroll.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PayrollCalculationBulkFinalizeRequest(@NotEmpty List<Long> ids) {
}

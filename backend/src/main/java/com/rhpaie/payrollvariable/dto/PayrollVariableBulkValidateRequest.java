package com.rhpaie.payrollvariable.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PayrollVariableBulkValidateRequest(
        @NotEmpty List<Long> ids
) {
}

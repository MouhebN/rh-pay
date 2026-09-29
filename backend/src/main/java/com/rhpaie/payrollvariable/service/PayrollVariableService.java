package com.rhpaie.payrollvariable.service;

import com.rhpaie.payrollvariable.dto.PayrollVariableBulkValidateRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableFilterRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableGenerateRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableGenerationSummaryResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableSummaryResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableUpdateRequest;

import java.util.List;

public interface PayrollVariableService {

    PayrollVariableGenerationSummaryResponse generate(PayrollVariableGenerateRequest request, String username);

    List<PayrollVariableResponse> findAll(PayrollVariableFilterRequest filter);

    PayrollVariableResponse findById(Long id);

    PayrollVariableResponse update(Long id, PayrollVariableUpdateRequest request);

    PayrollVariableResponse validate(Long id, String username);

    List<PayrollVariableResponse> validateBulk(PayrollVariableBulkValidateRequest request, String username);

    PayrollVariableResponse reopen(Long id);

    PayrollVariableSummaryResponse summary(Integer month, Integer year);

    void delete(Long id);
}

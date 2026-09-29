package com.rhpaie.payroll.service;

import com.rhpaie.payroll.dto.*;

import java.util.List;

public interface PayrollCalculationService {
    PayrollCalculationGenerationSummaryResponse generate(PayrollCalculationGenerateRequest request, String username);
    List<PayrollCalculationResponse> findAll(PayrollCalculationFilterRequest filter);
    PayrollCalculationDetailsResponse findById(Long id);
    PayrollCalculationDetailsResponse finalizeCalculation(Long id, String username);
    List<PayrollCalculationResponse> finalizeBulk(PayrollCalculationBulkFinalizeRequest request, String username);
    PayrollCalculationDetailsResponse cancel(Long id);
    PayrollCalculationSummaryResponse summary(Integer month, Integer year);
    List<PayrollCalculationResponse> findMine(String username, Integer year);
    PayrollCalculationDetailsResponse findMineById(String username, Long id);
}

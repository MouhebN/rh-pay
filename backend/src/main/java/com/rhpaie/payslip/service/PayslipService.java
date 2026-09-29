package com.rhpaie.payslip.service;

import com.rhpaie.payslip.dto.*;
import org.springframework.core.io.Resource;

import java.util.List;

public interface PayslipService {
    PayslipResponse generate(PayslipGenerateRequest request, String username);
    PayslipGenerationSummaryResponse generateBulk(PayslipBulkGenerateRequest request, String username);
    List<PayslipResponse> findAll(PayslipFilterRequest filter);
    PayslipResponse findById(Long id);
    Resource download(Long id);
    PayslipResponse cancel(Long id);
    PayslipSummaryResponse summary(Integer month, Integer year);
    List<PayslipResponse> findMine(String username, Integer year);
    PayslipResponse findMineById(String username, Long id);
    Resource downloadMine(String username, Long id);
}

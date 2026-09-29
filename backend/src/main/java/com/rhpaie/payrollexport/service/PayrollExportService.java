package com.rhpaie.payrollexport.service;

import com.rhpaie.payrollexport.dto.PayrollExportPreviewResponse;
import com.rhpaie.payrollexport.dto.PayrollExportSummaryResponse;

public interface PayrollExportService {
    PayrollExportSummaryResponse summary(Integer month, Integer year);

    PayrollExportPreviewResponse preview(Integer month, Integer year);

    byte[] payrollJournal(Integer month, Integer year);

    byte[] bankTransferExcel(Integer month, Integer year);

    byte[] bankTransferCsv(Integer month, Integer year);
}

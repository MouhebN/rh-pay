package com.rhpaie.payrollexport.service;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.payroll.entity.PayrollCalculation;
import com.rhpaie.payroll.entity.PayrollCalculationStatus;
import com.rhpaie.payroll.repository.PayrollCalculationRepository;
import com.rhpaie.payrollexport.dto.PayrollExportPreviewResponse;
import com.rhpaie.payrollexport.dto.PayrollExportSummaryResponse;
import com.rhpaie.payrollexport.model.BankTransferRow;
import com.rhpaie.payrollexport.model.PayrollJournalRow;
import com.rhpaie.settings.entity.AppSettings;
import com.rhpaie.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollExportServiceImpl implements PayrollExportService {

    private final PayrollCalculationRepository calculationRepository;
    private final PayrollExcelExportService excelExportService;
    private final PayrollCsvExportService csvExportService;
    private final SettingsService settingsService;

    @Override
    @Transactional(readOnly = true)
    public PayrollExportSummaryResponse summary(Integer month, Integer year) {
        List<PayrollCalculation> calculations = finalizedCalculations(month, year);
        return PayrollExportSummaryResponse.builder()
                .month(month)
                .year(year)
                .totalEmployees((long) calculations.size())
                .totalBaseSalary(sum(calculations, PayrollCalculation::getBaseSalary))
                .totalGrossSalary(sum(calculations, PayrollCalculation::getGrossSalary))
                .totalNetSalary(sum(calculations, PayrollCalculation::getNetSalary))
                .totalDeductions(sum(calculations, PayrollCalculation::getTotalDeductions))
                .totalBonuses(sum(calculations, PayrollCalculation::getBonusAmount))
                .totalAdvances(sum(calculations, PayrollCalculation::getAdvanceAmount))
                .totalOvertimeAmount(sum(calculations, PayrollCalculation::getOvertimeAmount))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollExportPreviewResponse preview(Integer month, Integer year) {
        return PayrollExportPreviewResponse.builder()
                .month(month)
                .year(year)
                .rows(finalizedCalculations(month, year).stream()
                        .map(this::toPreviewRow)
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] payrollJournal(Integer month, Integer year) {
        AppSettings settings = settingsService.getCurrentSettings();
        return excelExportService.payrollJournal(month, year, settings.getCompanyName(), finalizedCalculations(month, year).stream()
                .map(this::toJournalRow)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] bankTransferExcel(Integer month, Integer year) {
        AppSettings settings = settingsService.getCurrentSettings();
        return excelExportService.bankTransfer(month, year, settings.getCompanyName(), finalizedCalculations(month, year).stream()
                .map(this::toBankTransferRow)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] bankTransferCsv(Integer month, Integer year) {
        return csvExportService.bankTransfer(finalizedCalculations(month, year).stream()
                .map(this::toBankTransferRow)
                .toList());
    }

    private List<PayrollCalculation> finalizedCalculations(Integer month, Integer year) {
        List<PayrollCalculation> calculations = calculationRepository.findFinalizedForExport(month, year, PayrollCalculationStatus.FINALIZED);
        if (calculations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune paie finalisée trouvée pour cette période.");
        }
        return calculations;
    }

    private PayrollExportPreviewResponse.Row toPreviewRow(PayrollCalculation calculation) {
        Employee employee = calculation.getEmployee();
        return PayrollExportPreviewResponse.Row.builder()
                .employeeFullName(fullName(employee))
                .departmentName(employee.getDepartment() == null ? null : employee.getDepartment().getName())
                .jobPositionName(employee.getJobPosition() == null ? null : employee.getJobPosition().getTitle())
                .baseSalary(calculation.getBaseSalary())
                .grossSalary(calculation.getGrossSalary())
                .totalDeductions(calculation.getTotalDeductions())
                .netSalary(calculation.getNetSalary())
                .build();
    }

    private PayrollJournalRow toJournalRow(PayrollCalculation c) {
        Employee e = c.getEmployee();
        return new PayrollJournalRow(
                e.getMatricule(),
                fullName(e),
                e.getEmail(),
                e.getDepartment() == null ? "" : e.getDepartment().getName(),
                e.getJobPosition() == null ? "" : e.getJobPosition().getTitle(),
                c.getMonth(),
                c.getYear(),
                c.getBaseSalary(),
                c.getDailyRate(),
                c.getHourlyRate(),
                c.getUnpaidAbsenceDays(),
                c.getUnpaidAbsenceDeduction(),
                c.getLateMinutes(),
                c.getLateDeduction(),
                c.getOvertimeMinutes(),
                c.getOvertimeAmount(),
                c.getBonusAmount(),
                c.getAdvanceAmount(),
                c.getDeductionAmount(),
                c.getGrossSalary(),
                c.getTotalDeductions(),
                c.getNetSalary(),
                c.getStatus()
        );
    }

    private BankTransferRow toBankTransferRow(PayrollCalculation c) {
        Employee e = c.getEmployee();
        String warning = e.getRib() == null || e.getRib().isBlank() ? "RIB manquant" : "";
        return new BankTransferRow(
                e.getMatricule(),
                fullName(e),
                e.getRib(),
                e.getBankName(),
                e.getPaymentMethod(),
                c.getNetSalary(),
                c.getMonth(),
                c.getYear(),
                "PAIE-%d-%02d-%d".formatted(c.getYear(), c.getMonth(), e.getId()),
                warning
        );
    }

    private String fullName(Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }

    private BigDecimal sum(List<PayrollCalculation> calculations, java.util.function.Function<PayrollCalculation, BigDecimal> extractor) {
        return calculations.stream()
                .map(extractor)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

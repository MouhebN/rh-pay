package com.rhpaie.payslip.service;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.notification.entity.NotificationType;
import com.rhpaie.notification.service.AppNotificationService;
import com.rhpaie.payroll.entity.PayrollCalculation;
import com.rhpaie.payroll.entity.PayrollCalculationStatus;
import com.rhpaie.payroll.repository.PayrollCalculationRepository;
import com.rhpaie.payslip.dto.*;
import com.rhpaie.payslip.entity.*;
import com.rhpaie.payslip.mapper.PayslipMapper;
import com.rhpaie.payslip.repository.PayslipRepository;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayslipServiceImpl implements PayslipService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter PDF_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PayslipRepository payslipRepository;
    private final PayrollCalculationRepository calculationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PayslipMapper payslipMapper;
    private final PayslipPdfService pdfService;
    private final FileStorageService fileStorageService;
    private final AppNotificationService notificationService;

    @Override
    @Transactional
    public PayslipResponse generate(PayslipGenerateRequest request, String username) {
        UserAccount generatedBy = findUser(username);
        PayrollCalculation calculation = calculationRepository.findById(request.payrollCalculationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll calculation not found"));
        Payslip payslip = generateForCalculation(calculation, generatedBy);
        return payslipMapper.toResponse(payslip);
    }

    @Override
    @Transactional
    public PayslipGenerationSummaryResponse generateBulk(PayslipBulkGenerateRequest request, String username) {
        UserAccount generatedBy = findUser(username);
        int generated = 0;
        int regenerated = 0;
        int skipped = 0;

        for (PayrollCalculation calculation : calculationRepository.findByMonthAndYearAndStatus(request.month(), request.year(), PayrollCalculationStatus.FINALIZED)) {
            boolean exists = payslipRepository.findByEmployeeIdAndMonthAndYear(calculation.getEmployee().getId(), calculation.getMonth(), calculation.getYear())
                    .filter(payslip -> payslip.getStatus() == PayslipStatus.GENERATED)
                    .isPresent();
            try {
                generateForCalculation(calculation, generatedBy);
                if (exists) {
                    regenerated++;
                } else {
                    generated++;
                }
            } catch (ResponseStatusException exception) {
                skipped++;
            }
        }

        return PayslipGenerationSummaryResponse.builder()
                .generatedCount(generated)
                .regeneratedCount(regenerated)
                .skippedCount(skipped)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayslipResponse> findAll(PayslipFilterRequest filter) {
        return payslipRepository.findAll(buildSpecification(filter))
                .stream()
                .map(payslipMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipResponse findById(Long id) {
        return payslipMapper.toResponse(findPayslip(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(Long id) {
        Payslip payslip = findPayslip(id);
        if (payslip.getStatus() != PayslipStatus.GENERATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payslip is cancelled");
        }
        return fileStorageService.load(payslip.getFilePath());
    }

    @Override
    @Transactional
    public PayslipResponse cancel(Long id) {
        Payslip payslip = findPayslip(id);
        payslip.setStatus(PayslipStatus.CANCELLED);
        return payslipMapper.toResponse(payslipRepository.save(payslip));
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipSummaryResponse summary(Integer month, Integer year) {
        return PayslipSummaryResponse.builder()
                .totalPayslips(payslipRepository.countByMonthAndYear(month, year))
                .generatedCount(payslipRepository.countByMonthAndYearAndStatus(month, year, PayslipStatus.GENERATED))
                .cancelledCount(payslipRepository.countByMonthAndYearAndStatus(month, year, PayslipStatus.CANCELLED))
                .totalNetSalary(payslipRepository.sumNet(month, year, PayslipStatus.GENERATED))
                .totalGrossSalary(payslipRepository.sumGross(month, year, PayslipStatus.GENERATED))
                .totalDeductions(payslipRepository.sumDeductions(month, year, PayslipStatus.GENERATED))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayslipResponse> findMine(String username, Integer year) {
        Employee employee = findEmployee(username);
        return payslipRepository.findByEmployeeIdAndStatusOrderByYearDescMonthDesc(employee.getId(), PayslipStatus.GENERATED)
                .stream()
                .filter(payslip -> year == null || payslip.getYear().equals(year))
                .map(payslipMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipResponse findMineById(String username, Long id) {
        return payslipMapper.toResponse(findOwnedPayslip(username, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadMine(String username, Long id) {
        return fileStorageService.load(findOwnedPayslip(username, id).getFilePath());
    }

    private Payslip generateForCalculation(PayrollCalculation calculation, UserAccount generatedBy) {
        if (calculation.getStatus() != PayrollCalculationStatus.FINALIZED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payslip requires a finalized payroll calculation");
        }

        byte[] pdf = pdfService.generate(calculation);
        String fileName = "payslip_%d_%d_%02d_%s.pdf".formatted(
                calculation.getEmployee().getId(),
                calculation.getYear(),
                calculation.getMonth(),
                LocalDateTime.now().format(PDF_FILE_TIMESTAMP)
        );
        Path storedPath = fileStorageService.storePayslip(fileName, pdf);
        Payslip payslip = payslipRepository
                .findByEmployeeIdAndMonthAndYear(calculation.getEmployee().getId(), calculation.getMonth(), calculation.getYear())
                .orElseGet(Payslip::new);

        payslip.setEmployee(calculation.getEmployee());
        payslip.setPayrollCalculation(calculation);
        payslip.setMonth(calculation.getMonth());
        payslip.setYear(calculation.getYear());
        payslip.setFileName(fileName);
        payslip.setFilePath(storedPath.toString());
        payslip.setContentType(PDF_CONTENT_TYPE);
        payslip.setFileSize((long) pdf.length);
        payslip.setBaseSalary(calculation.getBaseSalary());
        payslip.setGrossSalary(calculation.getGrossSalary());
        payslip.setTotalDeductions(calculation.getTotalDeductions());
        payslip.setNetSalary(calculation.getNetSalary());
        payslip.setStatus(PayslipStatus.GENERATED);
        payslip.setGeneratedBy(generatedBy);
        payslip.setGeneratedAt(LocalDateTime.now());
        Payslip saved = payslipRepository.save(payslip);
        notifyEmployee(saved);
        return saved;
    }

    private Specification<Payslip> buildSpecification(PayslipFilterRequest filter) {
        return (root, query, cb) -> {
            root.fetch("employee", JoinType.LEFT);
            root.fetch("payrollCalculation", JoinType.LEFT);
            root.fetch("generatedBy", JoinType.LEFT);
            if (query != null) {
                query.orderBy(cb.desc(root.get("year")), cb.desc(root.get("month")), cb.asc(root.get("employee").get("lastName")));
            }
            var predicates = cb.conjunction();
            if (filter.month() != null) predicates = cb.and(predicates, cb.equal(root.get("month"), filter.month()));
            if (filter.year() != null) predicates = cb.and(predicates, cb.equal(root.get("year"), filter.year()));
            if (filter.employeeId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("id"), filter.employeeId()));
            if (filter.departmentId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("department").get("id"), filter.departmentId()));
            if (filter.jobPositionId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("jobPosition").get("id"), filter.jobPositionId()));
            if (filter.status() != null) predicates = cb.and(predicates, cb.equal(root.get("status"), filter.status()));
            return predicates;
        };
    }

    private Payslip findOwnedPayslip(String username, Long id) {
        Employee employee = findEmployee(username);
        Payslip payslip = findPayslip(id);
        if (!payslip.getEmployee().getId().equals(employee.getId()) || payslip.getStatus() != PayslipStatus.GENERATED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez consulter que vos propres bulletins générés.");
        }
        return payslip;
    }

    private Payslip findPayslip(Long id) {
        return payslipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payslip not found"));
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Employee findEmployee(String username) {
        return employeeRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No employee profile is linked to the current account"));
    }

    private void notifyEmployee(Payslip payslip) {
        UserAccount user = payslip.getEmployee().getUserAccount();
        if (user != null) {
            notificationService.notifyUsername(
                    user.getUsername(),
                    "Bulletin disponible",
                    "Votre bulletin de paie " + "%02d/%d".formatted(payslip.getMonth(), payslip.getYear()) + " est disponible.",
                    NotificationType.SUCCESS,
                    "/employee/my-payslips"
            );
        }
    }
}

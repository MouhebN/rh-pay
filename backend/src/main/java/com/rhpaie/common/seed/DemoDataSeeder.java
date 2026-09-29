package com.rhpaie.common.seed;

import com.rhpaie.attendance.entity.AttendanceRecord;
import com.rhpaie.attendance.entity.AttendanceSource;
import com.rhpaie.attendance.entity.AttendanceStatus;
import com.rhpaie.attendance.repository.AttendanceRepository;
import com.rhpaie.contract.entity.Contract;
import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.entity.ContractType;
import com.rhpaie.contract.repository.ContractRepository;
import com.rhpaie.employee.entity.Department;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.employee.entity.JobPosition;
import com.rhpaie.employee.entity.PaymentMethod;
import com.rhpaie.employee.repository.DepartmentRepository;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.employee.repository.JobPositionRepository;
import com.rhpaie.employeeexit.entity.EmployeeExit;
import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.entity.ExitType;
import com.rhpaie.employeeexit.repository.EmployeeExitRepository;
import com.rhpaie.leave.entity.LeaveRequest;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import com.rhpaie.leave.repository.LeaveRequestRepository;
import com.rhpaie.leavebalance.entity.LeaveBalance;
import com.rhpaie.leavebalance.repository.LeaveBalanceRepository;
import com.rhpaie.payroll.entity.PayrollCalculation;
import com.rhpaie.payroll.entity.PayrollCalculationStatus;
import com.rhpaie.payroll.repository.PayrollCalculationRepository;
import com.rhpaie.payrollvariable.entity.PayrollVariable;
import com.rhpaie.payrollvariable.entity.PayrollVariableStatus;
import com.rhpaie.payrollvariable.repository.PayrollVariableRepository;
import com.rhpaie.payslip.dto.PayslipGenerateRequest;
import com.rhpaie.payslip.repository.PayslipRepository;
import com.rhpaie.payslip.service.PayslipService;
import com.rhpaie.settings.entity.AppSettings;
import com.rhpaie.settings.repository.AppSettingsRepository;
import com.rhpaie.user.entity.Role;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private static final String EMPLOYEE_PASSWORD = "employee123";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final BigDecimal MONTHLY_REFERENCE_HOURS = BigDecimal.valueOf(173.33);
    private static final BigDecimal OVERTIME_MULTIPLIER = BigDecimal.valueOf(1.25);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final JobPositionRepository jobPositionRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollVariableRepository payrollVariableRepository;
    private final PayrollCalculationRepository payrollCalculationRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeExitRepository employeeExitRepository;
    private final AppSettingsRepository appSettingsRepository;
    private final PayslipService payslipService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        UserAccount admin = user("admin", "admin@rhpaiepro.com", ADMIN_PASSWORD, Role.ADMIN_RH);
        UserAccount aliUser = user("ali", "ali.bensalah@rhpaiepro.com", EMPLOYEE_PASSWORD, Role.EMPLOYE);
        UserAccount nourUser = user("nour", "nour.trabelsi@rhpaiepro.com", EMPLOYEE_PASSWORD, Role.EMPLOYE);
        UserAccount samiUser = user("sami", "sami.bouazizi@rhpaiepro.com", EMPLOYEE_PASSWORD, Role.EMPLOYE);

        Map<String, Department> departments = seedDepartments();
        Map<String, JobPosition> positions = seedJobPositions(departments);
        Map<String, Employee> employees = seedEmployees(departments, positions, aliUser, nourUser, samiUser, today);

        seedSettings(admin);
        seedContracts(admin, employees, today);
        seedLeaveBalances(employees, currentMonth.getYear());
        seedLeaveRequests(admin, employees, today, currentMonth);
        seedAttendance(employees, today, currentMonth);
        seedPayrollVariables(admin, employees, currentMonth);
        seedPayrollCalculations(admin, employees, currentMonth);
        seedPayslips(currentMonth);
        seedEmployeeExits(admin, employees, today);
    }

    private UserAccount user(String username, String email, String password, Role role) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseGet(UserAccount::new);
        user.setUsername(username);
        user.setEmail(email);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setRole(role);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        return userRepository.save(user);
    }

    private Map<String, Department> seedDepartments() {
        return Map.of(
                "rh", department("Ressources Humaines", "Gestion administrative et RH"),
                "dev", department("Développement", "Équipe de développement logiciel"),
                "finance", department("Finance", "Gestion financière et comptable"),
                "support", department("Support", "Support interne et assistance")
        );
    }

    private Department department(String name, String description) {
        Department department = departmentRepository.findByName(name).orElseGet(Department::new);
        department.setName(name);
        department.setDescription(description);
        return departmentRepository.save(department);
    }

    private Map<String, JobPosition> seedJobPositions(Map<String, Department> departments) {
        return Map.of(
                "responsable-rh", jobPosition("Responsable RH", departments.get("rh"), "Pilotage RH", 2400),
                "charge-rh", jobPosition("Chargé RH", departments.get("rh"), "Gestion administrative RH", 1600),
                "fullstack", jobPosition("Développeur Full Stack", departments.get("dev"), "Développement web complet", 1800),
                "backend", jobPosition("Développeur Backend", departments.get("dev"), "APIs et intégrations", 2000),
                "comptable", jobPosition("Comptable", departments.get("finance"), "Comptabilité générale", 1500),
                "responsable-paie", jobPosition("Responsable Paie", departments.get("finance"), "Préparation et contrôle paie", 2200),
                "support-tech", jobPosition("Technicien Support", departments.get("support"), "Assistance utilisateur", 1200)
        );
    }

    private JobPosition jobPosition(String title, Department department, String description, int baseSalary) {
        JobPosition position = jobPositionRepository.findByTitle(title).orElseGet(JobPosition::new);
        position.setTitle(title);
        position.setDepartment(department);
        position.setDescription(description);
        position.setBaseSalary(money(baseSalary));
        return jobPositionRepository.save(position);
    }

    private Map<String, Employee> seedEmployees(
            Map<String, Department> departments,
            Map<String, JobPosition> positions,
            UserAccount aliUser,
            UserAccount nourUser,
            UserAccount samiUser,
            LocalDate today
    ) {
        return Map.of(
                "ali", employee("EMP-2026-001", "Ali", "Ben Salah", "ali.bensalah@rhpaiepro.com", "22111222",
                        departments.get("dev"), positions.get("fullstack"), 1800, EmploymentStatus.ACTIVE, aliUser,
                        "BIAT", "TN590000000000000001", PaymentMethod.BANK_TRANSFER, today.minusMonths(7)),
                "nour", employee("EMP-2026-002", "Nour", "Trabelsi", "nour.trabelsi@rhpaiepro.com", "23111333",
                        departments.get("rh"), positions.get("charge-rh"), 1600, EmploymentStatus.ACTIVE, nourUser,
                        "Attijari Bank", "TN590000000000000002", PaymentMethod.BANK_TRANSFER, today.minusMonths(5)),
                "sami", employee("EMP-2026-003", "Sami", "Bouazizi", "sami.bouazizi@rhpaiepro.com", "24111444",
                        departments.get("finance"), positions.get("comptable"), 1500, EmploymentStatus.ACTIVE, samiUser,
                        "STB", "TN590000000000000003", PaymentMethod.BANK_TRANSFER, today.minusMonths(4)),
                "mariem", employee("EMP-2026-004", "Mariem", "Gharbi", "mariem.gharbi@rhpaiepro.com", "25111555",
                        departments.get("support"), positions.get("support-tech"), 1200, EmploymentStatus.ACTIVE, null,
                        null, null, null, today.minusMonths(1)),
                "yassine", employee("EMP-2026-005", "Yassine", "Khelifi", "yassine.khelifi@rhpaiepro.com", "26111666",
                        departments.get("dev"), positions.get("backend"), 2000, EmploymentStatus.ACTIVE, null,
                        "Amen Bank", "TN590000000000000005", PaymentMethod.BANK_TRANSFER, today.minusMonths(11)),
                "ines", employee("EMP-2026-006", "Ines", "Jaziri", "ines.jaziri@rhpaiepro.com", "27111777",
                        departments.get("finance"), positions.get("responsable-paie"), 2200, EmploymentStatus.LEFT, null,
                        "UIB", "TN590000000000000006", PaymentMethod.BANK_TRANSFER, today.minusYears(1))
        );
    }

    private Employee employee(
            String matricule,
            String firstName,
            String lastName,
            String email,
            String phone,
            Department department,
            JobPosition jobPosition,
            int baseSalary,
            EmploymentStatus status,
            UserAccount userAccount,
            String bankName,
            String rib,
            PaymentMethod paymentMethod,
            LocalDate hireDate
    ) {
        Employee employee = employeeRepository.findByMatricule(matricule).orElseGet(Employee::new);
        employee.setMatricule(matricule);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setBirthDate(LocalDate.of(1993, 4, 12));
        employee.setHireDate(hireDate);
        employee.setDepartment(department);
        employee.setJobPosition(jobPosition);
        employee.setBaseSalary(money(baseSalary));
        employee.setStatus(status);
        employee.setUserAccount(userAccount);
        employee.setBankName(bankName);
        employee.setRib(rib);
        employee.setPaymentMethod(paymentMethod);
        return employeeRepository.save(employee);
    }

    private void seedSettings(UserAccount admin) {
        AppSettings settings = appSettingsRepository.findFirstByOrderByIdAsc().orElseGet(AppSettings::new);
        settings.setCompanyName("RH PaiePro");
        settings.setCompanyAddress("Tunis, Tunisie");
        settings.setCompanyEmail("contact@rhpaiepro.com");
        settings.setCompanyPhone("+216 70 000 000");
        settings.setWorkStartTime(LocalTime.of(9, 0));
        settings.setWorkEndTime(LocalTime.of(17, 0));
        settings.setExpectedDailyHours(BigDecimal.valueOf(8));
        settings.setMonthlyReferenceHours(MONTHLY_REFERENCE_HOURS);
        settings.setDefaultAnnualLeaveDays(BigDecimal.valueOf(24));
        settings.setAllowNegativeLeaveBalance(false);
        settings.setOvertimeMultiplier(OVERTIME_MULTIPLIER);
        settings.setDeductLateMinutesFromSalary(false);
        settings.setCurrency("TND");
        settings.setUpdatedBy(admin);
        appSettingsRepository.save(settings);
    }

    private void seedContracts(UserAccount admin, Map<String, Employee> employees, LocalDate today) {
        contract(admin, employees.get("ali"), ContractType.CDI, ContractStatus.ACTIVE, LocalDate.of(2026, 1, 1), null,
                null, 1800, "CTR-2026-ALI-001", "Contrat CDI actif.");
        contract(admin, employees.get("nour"), ContractType.CDD, ContractStatus.ACTIVE, LocalDate.of(2026, 2, 1), today.plusDays(20),
                null, 1600, "CTR-2026-NOUR-001", "CDD proche de l'échéance.");
        contract(admin, employees.get("sami"), ContractType.CDI, ContractStatus.ACTIVE, LocalDate.of(2026, 3, 1), null,
                null, 1500, "CTR-2026-SAMI-001", "Contrat CDI actif.");
        contract(admin, employees.get("mariem"), ContractType.STAGE, ContractStatus.ACTIVE, LocalDate.of(2026, 6, 1), today.plusDays(10),
                today.plusDays(5), 1200, "CTR-2026-MARIEM-001", "Stage sans document signé pour la démonstration.");
        contract(admin, employees.get("yassine"), ContractType.CDD, ContractStatus.EXPIRED, LocalDate.of(2025, 8, 1), today.minusDays(5),
                null, 2000, "CTR-2025-YASSINE-001", "Contrat expiré nécessitant une action RH.");
        Contract ines = contract(admin, employees.get("ines"), ContractType.CDI, ContractStatus.TERMINATED, LocalDate.of(2025, 1, 1), null,
                null, 2200, "CTR-2025-INES-001", "Contrat terminé.");
        ines.setTerminationDate(today.minusDays(10));
        ines.setTerminationReason("Fin de contrat");
        ines.setTerminatedBy(admin);
        contractRepository.save(ines);
    }

    private Contract contract(
            UserAccount admin,
            Employee employee,
            ContractType type,
            ContractStatus status,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate trialPeriodEndDate,
            int salary,
            String reference,
            String notes
    ) {
        Contract contract = contractRepository.findByEmployeeIdOrderByStartDateDesc(employee.getId())
                .stream()
                .filter(item -> reference.equals(item.getContractReference()))
                .findFirst()
                .orElseGet(Contract::new);
        contract.setEmployee(employee);
        contract.setType(type);
        contract.setStatus(status);
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setTrialPeriodEndDate(trialPeriodEndDate);
        contract.setSalaryAtSignature(money(salary));
        contract.setContractReference(reference);
        contract.setNotes(notes);
        contract.setCreatedBy(admin);
        contract.setUpdatedBy(admin);
        return contractRepository.save(contract);
    }

    private void seedLeaveBalances(Map<String, Employee> employees, int year) {
        leaveBalance(employees.get("ali"), year, 24, 5, 2);
        leaveBalance(employees.get("nour"), year, 24, 8, 0);
        leaveBalance(employees.get("sami"), year, 24, 2, 1);
        leaveBalance(employees.get("mariem"), year, 12, 1, 0);
        leaveBalance(employees.get("yassine"), year, 24, 12, 0);
    }

    private void leaveBalance(Employee employee, int year, double acquired, double consumed, double pending) {
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employee.getId(), year).orElseGet(LeaveBalance::new);
        balance.setEmployee(employee);
        balance.setYear(year);
        balance.setAcquiredDays(acquired);
        balance.setConsumedDays(consumed);
        balance.setPendingDays(pending);
        balance.recalculateRemainingDays();
        leaveBalanceRepository.save(balance);
    }

    private void seedLeaveRequests(UserAccount admin, Map<String, Employee> employees, LocalDate today, YearMonth currentMonth) {
        leave(employees.get("ali"), LeaveType.PAID_LEAVE, LeaveStatus.PENDING, today.plusDays(7), today.plusDays(8),
                "Congé personnel", null, null);
        leave(employees.get("ali"), LeaveType.PAID_LEAVE, LeaveStatus.PENDING, today.plusDays(80), today.plusDays(81),
                "Congé démo jury", null, null);
        leave(employees.get("nour"), LeaveType.SICK_LEAVE, LeaveStatus.APPROVED, today.minusDays(5), today.minusDays(3),
                "Congé maladie", admin, null);
        LocalDate unpaidStart = currentMonth.atDay(Math.min(10, currentMonth.lengthOfMonth()));
        leave(employees.get("sami"), LeaveType.UNPAID_LEAVE, LeaveStatus.APPROVED, unpaidStart, unpaidStart.plusDays(1),
                "Absence personnelle", admin, null);
        leave(employees.get("mariem"), LeaveType.PAID_LEAVE, LeaveStatus.REJECTED, today.plusDays(15), today.plusDays(16),
                "Congé demandé", admin, "Période non disponible");
        leave(employees.get("yassine"), LeaveType.PAID_LEAVE, LeaveStatus.APPROVED, today, today,
                "Congé planifié", admin, null);
    }

    private void leave(
            Employee employee,
            LeaveType type,
            LeaveStatus status,
            LocalDate start,
            LocalDate end,
            String reason,
            UserAccount reviewedBy,
            String rejectionReason
    ) {
        LeaveRequest request = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .stream()
                .filter(item -> item.getType() == type && item.getStartDate().equals(start) && item.getEndDate().equals(end))
                .findFirst()
                .orElseGet(LeaveRequest::new);
        request.setEmployee(employee);
        request.setType(type);
        request.setStatus(status);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setTotalDays((double) ChronoUnit.DAYS.between(start, end) + 1);
        request.setReason(reason);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(reviewedBy == null ? null : LocalDateTime.now().minusDays(1));
        request.setRejectionReason(rejectionReason);
        leaveRequestRepository.save(request);
    }

    private void seedAttendance(Map<String, Employee> employees, LocalDate today, YearMonth currentMonth) {
        attendance(employees.get("ali"), today, LocalTime.of(8, 55), null, null, 0, 0, AttendanceStatus.INCOMPLETE, AttendanceSource.WEB);
        attendance(employees.get("nour"), today, LocalTime.of(9, 20), null, null, 20, 0, AttendanceStatus.LATE, AttendanceSource.WEB);
        attendance(employees.get("sami"), today, LocalTime.of(9, 0), LocalTime.of(17, 30), BigDecimal.valueOf(8.5), 0, 30, AttendanceStatus.PRESENT, AttendanceSource.WEB);
        attendance(employees.get("yassine"), today, null, null, null, 0, 0, AttendanceStatus.ON_LEAVE, AttendanceSource.MANUAL);

        LocalDate base = currentMonth.atDay(Math.min(3, currentMonth.lengthOfMonth()));
        attendance(employees.get("ali"), base, LocalTime.of(9, 5), LocalTime.of(17, 30), BigDecimal.valueOf(8.42), 5, 25, AttendanceStatus.LATE, AttendanceSource.WEB);
        attendance(employees.get("ali"), base.plusDays(1), LocalTime.of(8, 50), LocalTime.of(17, 20), BigDecimal.valueOf(8.5), 0, 30, AttendanceStatus.PRESENT, AttendanceSource.WEB);
        attendance(employees.get("nour"), base.plusDays(2), LocalTime.of(9, 30), LocalTime.of(17, 0), BigDecimal.valueOf(7.5), 30, 0, AttendanceStatus.LATE, AttendanceSource.WEB);
        attendance(employees.get("sami"), base.plusDays(3), null, null, null, 0, 0, AttendanceStatus.ABSENT, AttendanceSource.MANUAL);
        attendance(employees.get("mariem"), base.plusDays(4), LocalTime.of(9, 0), LocalTime.of(12, 30), BigDecimal.valueOf(3.5), 0, 0, AttendanceStatus.HALF_DAY, AttendanceSource.WEB);
        attendance(employees.get("yassine"), base.plusDays(5), LocalTime.of(8, 45), LocalTime.of(18, 15), BigDecimal.valueOf(9.5), 0, 90, AttendanceStatus.PRESENT, AttendanceSource.WEB);
    }

    private void attendance(
            Employee employee,
            LocalDate date,
            LocalTime checkIn,
            LocalTime checkOut,
            BigDecimal workedHours,
            int lateMinutes,
            int overtimeMinutes,
            AttendanceStatus status,
            AttendanceSource source
    ) {
        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), date).orElseGet(AttendanceRecord::new);
        record.setEmployee(employee);
        record.setWorkDate(date);
        record.setCheckInTime(checkIn);
        record.setCheckOutTime(checkOut);
        record.setWorkedHours(workedHours);
        record.setLateMinutes(lateMinutes);
        record.setOvertimeMinutes(overtimeMinutes);
        record.setStatus(status);
        record.setSource(source);
        attendanceRepository.save(record);
    }

    private void seedPayrollVariables(UserAccount admin, Map<String, Employee> employees, YearMonth month) {
        payrollVariable(admin, employees.get("ali"), month, PayrollVariableStatus.VALIDATED, 0, 0, 0, 30, 120, 20, 0, 100, 0, 0);
        payrollVariable(admin, employees.get("nour"), month, PayrollVariableStatus.DRAFT, 0, 0, 3, 60, 0, 18, 0, 0, 100, 0);
        payrollVariable(admin, employees.get("sami"), month, PayrollVariableStatus.VALIDATED, 2, 0, 0, 15, 180, 19, 0, 150, 50, 0);
        payrollVariable(admin, employees.get("mariem"), month, PayrollVariableStatus.DRAFT, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0);
        payrollVariable(admin, employees.get("yassine"), month, PayrollVariableStatus.VALIDATED, 0, 1, 0, 0, 240, 20, 0, 200, 0, 50);
    }

    private PayrollVariable payrollVariable(
            UserAccount admin,
            Employee employee,
            YearMonth month,
            PayrollVariableStatus status,
            int unpaidAbsenceDays,
            int paidLeaveDays,
            int sickLeaveDays,
            int lateMinutes,
            int overtimeMinutes,
            int workedDays,
            int absentDays,
            int bonus,
            int advance,
            int deduction
    ) {
        PayrollVariable variable = payrollVariableRepository.findByEmployeeIdAndMonthAndYear(employee.getId(), month.getMonthValue(), month.getYear())
                .orElseGet(PayrollVariable::new);
        variable.setEmployee(employee);
        variable.setMonth(month.getMonthValue());
        variable.setYear(month.getYear());
        variable.setUnpaidAbsenceDays(money(unpaidAbsenceDays));
        variable.setPaidLeaveDays(money(paidLeaveDays));
        variable.setSickLeaveDays(money(sickLeaveDays));
        variable.setLateMinutes(lateMinutes);
        variable.setOvertimeMinutes(overtimeMinutes);
        variable.setWorkedDays(money(workedDays));
        variable.setAbsentDays(money(absentDays));
        variable.setBonusAmount(money(bonus));
        variable.setAdvanceAmount(money(advance));
        variable.setDeductionAmount(money(deduction));
        variable.setNotes("Données de démonstration pour le mois courant.");
        variable.setStatus(status);
        variable.setGeneratedBy(admin);
        variable.setGeneratedAt(LocalDateTime.now().minusHours(4));
        variable.setValidatedBy(status == PayrollVariableStatus.VALIDATED ? admin : null);
        variable.setValidatedAt(status == PayrollVariableStatus.VALIDATED ? LocalDateTime.now().minusHours(2) : null);
        return payrollVariableRepository.save(variable);
    }

    private void seedPayrollCalculations(UserAccount admin, Map<String, Employee> employees, YearMonth month) {
        calculation(admin, employees.get("ali"), month, PayrollCalculationStatus.FINALIZED);
        calculation(admin, employees.get("sami"), month, PayrollCalculationStatus.DRAFT);
        calculation(admin, employees.get("yassine"), month, PayrollCalculationStatus.FINALIZED);
    }

    private void calculation(UserAccount admin, Employee employee, YearMonth month, PayrollCalculationStatus status) {
        PayrollVariable variable = payrollVariableRepository.findByEmployeeIdAndMonthAndYear(employee.getId(), month.getMonthValue(), month.getYear())
                .orElseThrow();
        PayrollCalculation calculation = payrollCalculationRepository.findByEmployeeIdAndMonthAndYear(employee.getId(), month.getMonthValue(), month.getYear())
                .orElseGet(PayrollCalculation::new);

        BigDecimal baseSalary = employee.getBaseSalary();
        BigDecimal dailyRate = divide(baseSalary, BigDecimal.valueOf(30));
        BigDecimal hourlyRate = divide(baseSalary, MONTHLY_REFERENCE_HOURS);
        BigDecimal overtimeHours = divide(BigDecimal.valueOf(variable.getOvertimeMinutes()), BigDecimal.valueOf(60));
        BigDecimal overtimeAmount = money(overtimeHours.multiply(hourlyRate).multiply(OVERTIME_MULTIPLIER));
        BigDecimal unpaidDeduction = money(variable.getUnpaidAbsenceDays().multiply(dailyRate));
        BigDecimal lateDeduction = BigDecimal.ZERO.setScale(2);
        BigDecimal grossSalary = money(baseSalary.add(variable.getBonusAmount()).add(overtimeAmount));
        BigDecimal totalDeductions = money(unpaidDeduction.add(variable.getAdvanceAmount()).add(variable.getDeductionAmount()).add(lateDeduction));
        BigDecimal netSalary = grossSalary.subtract(totalDeductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        calculation.setEmployee(employee);
        calculation.setPayrollVariable(variable);
        calculation.setMonth(month.getMonthValue());
        calculation.setYear(month.getYear());
        calculation.setBaseSalary(baseSalary);
        calculation.setDailyRate(dailyRate);
        calculation.setHourlyRate(hourlyRate);
        calculation.setUnpaidAbsenceDays(variable.getUnpaidAbsenceDays());
        calculation.setLateMinutes(variable.getLateMinutes());
        calculation.setOvertimeMinutes(variable.getOvertimeMinutes());
        calculation.setBonusAmount(variable.getBonusAmount());
        calculation.setAdvanceAmount(variable.getAdvanceAmount());
        calculation.setDeductionAmount(variable.getDeductionAmount());
        calculation.setUnpaidAbsenceDeduction(unpaidDeduction);
        calculation.setLateDeduction(lateDeduction);
        calculation.setOvertimeAmount(overtimeAmount);
        calculation.setGrossSalary(grossSalary);
        calculation.setTotalDeductions(totalDeductions);
        calculation.setNetSalary(netSalary);
        calculation.setStatus(status);
        calculation.setCalculatedBy(admin);
        calculation.setCalculatedAt(LocalDateTime.now().minusHours(1));
        calculation.setFinalizedBy(status == PayrollCalculationStatus.FINALIZED ? admin : null);
        calculation.setFinalizedAt(status == PayrollCalculationStatus.FINALIZED ? LocalDateTime.now().minusMinutes(30) : null);
        payrollCalculationRepository.save(calculation);
    }

    private void seedPayslips(YearMonth month) {
        for (PayrollCalculation calculation : payrollCalculationRepository.findByMonthAndYearAndStatus(
                month.getMonthValue(),
                month.getYear(),
                PayrollCalculationStatus.FINALIZED
        )) {
            boolean alreadyGenerated = payslipRepository
                    .findByEmployeeIdAndMonthAndYear(calculation.getEmployee().getId(), calculation.getMonth(), calculation.getYear())
                    .isPresent();
            if (!alreadyGenerated) {
                try {
                    payslipService.generate(new PayslipGenerateRequest(calculation.getId()), "admin");
                } catch (RuntimeException ignored) {
                    // Demo data must never prevent startup. Admin can generate PDFs manually if local file storage is unavailable.
                }
            }
        }
    }

    private void seedEmployeeExits(UserAccount admin, Map<String, Employee> employees, LocalDate today) {
        employeeExit(admin, employees.get("ines"), ExitType.END_OF_CONTRACT, ExitStatus.COMPLETED, today.minusDays(10),
                "Fin de contrat", "Solde de tout compte préparé.", true, true);
        employeeExit(admin, employees.get("yassine"), ExitType.END_OF_CONTRACT, ExitStatus.SCHEDULED, today.plusDays(20),
                "Contrat arrivant à échéance", "Sortie planifiée si le contrat n'est pas renouvelé.", false, false);
    }

    private void employeeExit(
            UserAccount admin,
            Employee employee,
            ExitType type,
            ExitStatus status,
            LocalDate exitDate,
            String reason,
            String notes,
            boolean accountDisabled,
            boolean contractTerminated
    ) {
        EmployeeExit employeeExit = employeeExitRepository.findByEmployeeIdOrderByExitDateDesc(employee.getId())
                .stream()
                .filter(item -> item.getType() == type && item.getExitDate().equals(exitDate))
                .findFirst()
                .orElseGet(EmployeeExit::new);
        employeeExit.setEmployee(employee);
        employeeExit.setType(type);
        employeeExit.setStatus(status);
        employeeExit.setExitDate(exitDate);
        employeeExit.setReason(reason);
        employeeExit.setNotes(notes);
        employeeExit.setAccountDisabled(accountDisabled);
        employeeExit.setActiveContractTerminated(contractTerminated);
        employeeExit.setCreatedBy(admin);
        employeeExit.setCompletedBy(status == ExitStatus.COMPLETED ? admin : null);
        employeeExit.setCompletedAt(status == ExitStatus.COMPLETED ? LocalDateTime.now().minusDays(9) : null);
        employeeExitRepository.save(employeeExit);
    }

    private BigDecimal money(int value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(BigDecimal left, BigDecimal right) {
        return left.divide(right, 2, RoundingMode.HALF_UP);
    }
}

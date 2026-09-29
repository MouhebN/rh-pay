package com.rhpaie.employee.service;

import com.rhpaie.contract.dto.ContractCreateRequest;
import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.service.ContractService;
import com.rhpaie.employee.dto.EmployeeContractCreateRequest;
import com.rhpaie.employee.dto.EmployeeRequest;
import com.rhpaie.employee.dto.EmployeeResponse;
import com.rhpaie.employee.dto.EmployeeStatusRequest;
import com.rhpaie.employee.entity.Department;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.employee.entity.JobPosition;
import com.rhpaie.employee.mapper.EmployeeMapper;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final JobPositionService jobPositionService;
    private final UserRepository userRepository;
    private final EmployeeMapper employeeMapper;
    private final ContractService contractService;

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAll()
                .stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return employeeMapper.toResponse(findEmployeeById(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request, String username) {
        validateMatriculeAvailable(request.matricule(), null);
        validateEmailAvailable(request.email(), null);

        JobPosition jobPosition = resolveJobPosition(request.jobPositionId());

        Employee employee = Employee.builder()
                .matricule(request.matricule())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .birthDate(request.birthDate())
                .hireDate(request.hireDate())
                .department(resolveDepartment(request.departmentId()))
                .jobPosition(jobPosition)
                .baseSalary(resolveBaseSalary(request.baseSalary(), jobPosition))
                .rib(normalizeBlank(request.rib()))
                .bankName(normalizeBlank(request.bankName()))
                .paymentMethod(request.paymentMethod())
                .userAccount(resolveUserAccount(request.userAccountId()))
                .status(request.status() != null ? request.status() : EmploymentStatus.ACTIVE)
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        createInitialContractIfRequested(request, savedEmployee, username);

        return employeeMapper.toResponse(savedEmployee);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findEmployeeById(id);
        validateMatriculeAvailable(request.matricule(), id);
        validateEmailAvailable(request.email(), id);

        employee.setMatricule(request.matricule());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setBirthDate(request.birthDate());
        employee.setHireDate(request.hireDate());
        employee.setDepartment(resolveDepartment(request.departmentId()));
        JobPosition jobPosition = resolveJobPosition(request.jobPositionId());
        employee.setJobPosition(jobPosition);
        employee.setBaseSalary(resolveBaseSalary(request.baseSalary(), jobPosition));
        employee.setRib(normalizeBlank(request.rib()));
        employee.setBankName(normalizeBlank(request.bankName()));
        employee.setPaymentMethod(request.paymentMethod());
        employee.setUserAccount(resolveUserAccount(request.userAccountId()));
        employee.setStatus(request.status() != null ? request.status() : employee.getStatus());

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateStatus(Long id, EmployeeStatusRequest request) {
        Employee employee = findEmployeeById(id);
        employee.setStatus(request.status());
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        employeeRepository.delete(findEmployeeById(id));
    }

    private Employee findEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentService.findDepartmentById(departmentId);
    }

    private JobPosition resolveJobPosition(Long jobPositionId) {
        if (jobPositionId == null) {
            return null;
        }
        return jobPositionService.findJobPositionById(jobPositionId);
    }

    private UserAccount resolveUserAccount(Long userAccountId) {
        if (userAccountId == null) {
            return null;
        }
        return userRepository.findById(userAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
    }

    private BigDecimal resolveBaseSalary(BigDecimal requestedSalary, JobPosition jobPosition) {
        if (requestedSalary != null) {
            if (requestedSalary.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee base salary cannot be negative");
            }
            return requestedSalary;
        }
        return jobPosition == null ? null : jobPosition.getBaseSalary();
    }

    private void createInitialContractIfRequested(EmployeeRequest request, Employee employee, String username) {
        if (!Boolean.TRUE.equals(request.createContract()) || request.contract() == null) {
            return;
        }

        EmployeeContractCreateRequest contract = request.contract();
        BigDecimal salaryAtSignature = contract.salaryAtSignature() != null
                ? contract.salaryAtSignature()
                : employee.getBaseSalary();
        if (salaryAtSignature == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract salary is required when employee base salary is missing");
        }

        contractService.create(
                new ContractCreateRequest(
                        employee.getId(),
                        contract.type(),
                        Boolean.TRUE.equals(contract.activateImmediately()) ? ContractStatus.ACTIVE : ContractStatus.DRAFT,
                        contract.startDate(),
                        contract.endDate(),
                        contract.trialPeriodEndDate(),
                        salaryAtSignature,
                        null,
                        contract.notes()
                ),
                username
        );
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateMatriculeAvailable(String matricule, Long currentEmployeeId) {
        employeeRepository.findByMatricule(matricule)
                .filter(employee -> !employee.getId().equals(currentEmployeeId))
                .ifPresent(employee -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee matricule already exists");
                });
    }

    private void validateEmailAvailable(String email, Long currentEmployeeId) {
        employeeRepository.findByEmail(email)
                .filter(employee -> !employee.getId().equals(currentEmployeeId))
                .ifPresent(employee -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee email already exists");
                });
    }
}

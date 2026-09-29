package com.rhpaie.employeeexit.service;

import com.rhpaie.contract.dto.ContractTerminateRequest;
import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.repository.ContractRepository;
import com.rhpaie.contract.service.ContractService;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.employeeexit.dto.*;
import com.rhpaie.employeeexit.entity.EmployeeExit;
import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.entity.ExitType;
import com.rhpaie.employeeexit.mapper.EmployeeExitMapper;
import com.rhpaie.employeeexit.repository.EmployeeExitRepository;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeExitServiceImpl implements EmployeeExitService {

    private static final int DEFAULT_UPCOMING_DAYS = 30;
    private static final List<ExitStatus> ACTIVE_EXIT_STATUSES = List.of(ExitStatus.DRAFT, ExitStatus.SCHEDULED);

    private final EmployeeExitRepository employeeExitRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final ContractService contractService;
    private final EmployeeExitMapper employeeExitMapper;

    @Override
    @Transactional
    public EmployeeExitResponse create(EmployeeExitCreateRequest request, String username) {
        Employee employee = findEmployee(request.employeeId());
        validateEmployeeCanExit(employee);
        ensureNoActiveExit(employee.getId(), null);
        ExitStatus status = request.status() == null ? ExitStatus.DRAFT : request.status();
        validateEditableStatus(status);

        EmployeeExit exit = EmployeeExit.builder()
                .employee(employee)
                .type(request.type())
                .status(status)
                .exitDate(request.exitDate())
                .reason(normalizeRequired(request.reason(), "Reason is required"))
                .notes(normalizeBlank(request.notes()))
                .createdBy(findUser(username))
                .build();
        return employeeExitMapper.toResponse(employeeExitRepository.save(exit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeExitResponse> findAll(EmployeeExitFilterRequest filter) {
        return employeeExitRepository.findAll(buildSpecification(filter))
                .stream()
                .map(employeeExitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeExitResponse findById(Long id) {
        return employeeExitMapper.toResponse(findExit(id));
    }

    @Override
    @Transactional
    public EmployeeExitResponse update(Long id, EmployeeExitUpdateRequest request) {
        EmployeeExit exit = findExit(id);
        validateEditable(exit);
        validateEditableStatus(request.status());
        exit.setType(request.type());
        exit.setStatus(request.status());
        exit.setExitDate(request.exitDate());
        exit.setReason(normalizeRequired(request.reason(), "Reason is required"));
        exit.setNotes(normalizeBlank(request.notes()));
        return employeeExitMapper.toResponse(exit);
    }

    @Override
    @Transactional
    public EmployeeExitResponse complete(Long id, EmployeeExitCompleteRequest request, String username) {
        EmployeeExit exit = findExit(id);
        validateCompletable(exit);
        UserAccount admin = findUser(username);
        Employee employee = exit.getEmployee();

        boolean accountDisabled = false;
        UserAccount linkedAccount = employee.getUserAccount();
        if (linkedAccount != null && linkedAccount.isEnabled()) {
            linkedAccount.setEnabled(false);
            accountDisabled = true;
        }

        boolean contractTerminated = terminateActiveContractIfPresent(employee.getId(), exit, username);

        employee.setStatus(EmploymentStatus.LEFT);
        exit.setStatus(ExitStatus.COMPLETED);
        exit.setCompletedBy(admin);
        exit.setCompletedAt(LocalDateTime.now());
        exit.setAccountDisabled(accountDisabled);
        exit.setActiveContractTerminated(contractTerminated);
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            exit.setNotes(request.notes().trim());
        }

        return employeeExitMapper.toResponse(exit);
    }

    @Override
    @Transactional
    public EmployeeExitResponse cancel(Long id, EmployeeExitCancelRequest request, String username) {
        EmployeeExit exit = findExit(id);
        if (exit.getStatus() == ExitStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exit is already cancelled");
        }
        exit.setStatus(ExitStatus.CANCELLED);
        exit.setCancelledBy(findUser(username));
        exit.setCancelledAt(LocalDateTime.now());
        exit.setCancellationReason(normalizeRequired(request.cancellationReason(), "Cancellation reason is required"));
        return employeeExitMapper.toResponse(exit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeExitResponse> findByEmployee(Long employeeId) {
        return employeeExitRepository.findByEmployeeIdOrderByExitDateDesc(employeeId)
                .stream()
                .map(employeeExitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeExitResponse> scheduled() {
        return employeeExitRepository.findByStatusOrderByExitDateAsc(ExitStatus.SCHEDULED)
                .stream()
                .map(employeeExitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeExitResponse> upcoming(Integer days) {
        int range = days == null || days < 1 ? DEFAULT_UPCOMING_DAYS : days;
        LocalDate today = LocalDate.now();
        return employeeExitRepository.findUpcoming(ExitStatus.SCHEDULED, today, today.plusDays(range))
                .stream()
                .map(employeeExitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeExitSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        Map<String, Long> byType = Arrays.stream(ExitType.values())
                .collect(Collectors.toMap(Enum::name, employeeExitRepository::countByType, (a, b) -> a, LinkedHashMap::new));
        return EmployeeExitSummaryResponse.builder()
                .totalExits(employeeExitRepository.count())
                .draftCount(employeeExitRepository.countByStatus(ExitStatus.DRAFT))
                .scheduledCount(employeeExitRepository.countByStatus(ExitStatus.SCHEDULED))
                .completedCount(employeeExitRepository.countByStatus(ExitStatus.COMPLETED))
                .cancelledCount(employeeExitRepository.countByStatus(ExitStatus.CANCELLED))
                .upcomingCount(employeeExitRepository.countByStatusAndExitDateBetween(ExitStatus.SCHEDULED, today, today.plusDays(DEFAULT_UPCOMING_DAYS)))
                .exitsByType(byType)
                .build();
    }

    @Override
    @Transactional
    public int processDue(String username) {
        List<EmployeeExit> due = employeeExitRepository.findByStatusAndExitDateLessThanEqual(ExitStatus.SCHEDULED, LocalDate.now());
        due.forEach(exit -> complete(exit.getId(), new EmployeeExitCompleteRequest(exit.getNotes()), username));
        return due.size();
    }

    private Specification<EmployeeExit> buildSpecification(EmployeeExitFilterRequest filter) {
        return (root, query, cb) -> {
            var employeeFetch = root.fetch("employee", JoinType.LEFT);
            employeeFetch.fetch("department", JoinType.LEFT);
            employeeFetch.fetch("jobPosition", JoinType.LEFT);
            root.fetch("createdBy", JoinType.LEFT);
            root.fetch("completedBy", JoinType.LEFT);
            root.fetch("cancelledBy", JoinType.LEFT);
            if (query != null) {
                query.distinct(true);
                query.orderBy(cb.desc(root.get("exitDate")), cb.desc(root.get("id")));
            }

            var predicates = cb.conjunction();
            if (filter.employeeId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("id"), filter.employeeId()));
            if (filter.departmentId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("department").get("id"), filter.departmentId()));
            if (filter.jobPositionId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("jobPosition").get("id"), filter.jobPositionId()));
            if (filter.type() != null) predicates = cb.and(predicates, cb.equal(root.get("type"), filter.type()));
            if (filter.status() != null) predicates = cb.and(predicates, cb.equal(root.get("status"), filter.status()));
            if (filter.exitDateFrom() != null) predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("exitDate"), filter.exitDateFrom()));
            if (filter.exitDateTo() != null) predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("exitDate"), filter.exitDateTo()));
            return predicates;
        };
    }

    private boolean terminateActiveContractIfPresent(Long employeeId, EmployeeExit exit, String username) {
        return contractRepository.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("employee").get("id"), employeeId),
                        cb.equal(root.get("status"), ContractStatus.ACTIVE)
                ))
                .stream()
                .findFirst()
                .map(contract -> {
                    contractService.terminate(contract.getId(), new ContractTerminateRequest(exit.getExitDate(), exit.getReason()), username);
                    return true;
                })
                .orElse(false);
    }

    private void validateEmployeeCanExit(Employee employee) {
        if (employee.getStatus() == EmploymentStatus.LEFT || employee.getStatus() == EmploymentStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is already inactive or has left");
        }
    }

    private void ensureNoActiveExit(Long employeeId, Long currentExitId) {
        if (!employeeExitRepository.existsByEmployeeIdAndStatusIn(employeeId, ACTIVE_EXIT_STATUSES)) {
            return;
        }
        if (currentExitId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This employee already has an active exit process");
        }
    }

    private void validateEditable(EmployeeExit exit) {
        if (exit.getStatus() != ExitStatus.DRAFT && exit.getStatus() != ExitStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft or scheduled exits can be updated");
        }
    }

    private void validateCompletable(EmployeeExit exit) {
        if (exit.getStatus() != ExitStatus.DRAFT && exit.getStatus() != ExitStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft or scheduled exits can be completed");
        }
    }

    private void validateEditableStatus(ExitStatus status) {
        if (status != ExitStatus.DRAFT && status != ExitStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exit status must be draft or scheduled");
        }
    }

    private EmployeeExit findExit(Long id) {
        return employeeExitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee exit not found"));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeBlank(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }
}

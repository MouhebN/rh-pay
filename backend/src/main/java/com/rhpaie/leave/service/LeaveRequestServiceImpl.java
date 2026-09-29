package com.rhpaie.leave.service;

import com.rhpaie.common.config.FileStorageProperties;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.leave.dto.AdminLeaveSummaryResponse;
import com.rhpaie.leave.dto.AdminLeaveStatisticsResponse;
import com.rhpaie.leave.dto.EmployeeLeaveSummaryResponse;
import com.rhpaie.leave.dto.EmployeeLeaveStatisticsResponse;
import com.rhpaie.leave.dto.LeaveCalendarEventResponse;
import com.rhpaie.leave.dto.LeaveDocumentResource;
import com.rhpaie.leave.dto.LeaveRequestCreateRequest;
import com.rhpaie.leave.dto.LeaveRequestFilter;
import com.rhpaie.leave.dto.LeaveRequestResponse;
import com.rhpaie.leave.dto.LeaveReviewRequest;
import com.rhpaie.leave.dto.LeaveValidationResponse;
import com.rhpaie.leave.entity.LeaveRequest;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import com.rhpaie.leave.mapper.LeaveRequestMapper;
import com.rhpaie.leave.repository.LeaveRequestRepository;
import com.rhpaie.leavebalance.service.LeaveBalanceService;
import com.rhpaie.notification.entity.NotificationType;
import com.rhpaie.notification.service.AppNotificationService;
import com.rhpaie.user.entity.Role;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private static final List<LeaveStatus> BLOCKING_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);
    private static final long MAX_DOCUMENT_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveValidationService leaveValidationService;
    private final FileStorageProperties fileStorageProperties;
    private final AppNotificationService notificationService;

    @Override
    @Transactional
    public LeaveRequestResponse create(LeaveRequestCreateRequest request, String username) {
        UserAccount currentUser = findUser(username);
        boolean createdByAdmin = currentUser.getRole() == Role.ADMIN_RH;
        Employee employee = createdByAdmin
                ? findEmployeeForAdminRequest(request.employeeId())
                : findEmployeeForCurrentUser(username);

        validateDates(request.startDate(), request.endDate());
        validateNoOverlap(employee.getId(), request.startDate(), request.endDate());
        double totalDays = calculateTotalDays(request.startDate(), request.endDate());
        Integer balanceYear = request.startDate().getYear();

        if (request.type() == LeaveType.PAID_LEAVE) {
            if (createdByAdmin) {
                leaveBalanceService.consumeApprovedDays(employee, balanceYear, totalDays);
            } else {
                leaveBalanceService.reservePendingDays(employee, balanceYear, totalDays);
            }
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .type(request.type())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .totalDays(totalDays)
                .reason(request.reason())
                .status(createdByAdmin ? LeaveStatus.APPROVED : LeaveStatus.PENDING)
                .reviewedBy(createdByAdmin ? currentUser : null)
                .reviewedAt(createdByAdmin ? LocalDateTime.now() : null)
                .build();

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        if (createdByAdmin) {
            notifyEmployee(savedRequest, "Absence enregistrée", "Une absence a été enregistrée par le service RH.", NotificationType.INFO);
        } else {
            notificationService.notifyAdmins(
                    "Nouvelle demande d'absence",
                    employee.getFirstName() + " " + employee.getLastName() + " a envoyé une demande à valider.",
                    NotificationType.WARNING,
                    "/admin/leave-requests?status=PENDING"
            );
        }
        return leaveRequestMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> findAll(LeaveRequestFilter filter) {
        return leaveRequestRepository.findAll(buildSpecification(filter))
                .stream()
                .map(leaveRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> findMyRequests(String username) {
        Employee employee = findEmployeeForCurrentUser(username);
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .stream()
                .map(leaveRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponse findById(Long id, String username) {
        LeaveRequest leaveRequest = findLeaveRequest(id);
        UserAccount currentUser = findUser(username);
        if (currentUser.getRole() != Role.ADMIN_RH && !isOwnLeaveRequest(leaveRequest, username)) {
            throw new AccessDeniedException("You cannot access this leave request");
        }
        return leaveRequestMapper.toResponse(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveValidationResponse validate(Long id) {
        return leaveValidationService.validateBeforeApproval(findLeaveRequest(id));
    }

    @Override
    @Transactional
    public LeaveRequestResponse approve(Long id, String reviewerUsername) {
        LeaveRequest leaveRequest = findPendingLeaveRequest(id);
        LeaveValidationResponse validation = leaveValidationService.validateBeforeApproval(leaveRequest);
        if (!validation.blockingErrors().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", validation.blockingErrors()));
        }
        UserAccount reviewer = findUser(reviewerUsername);
        if (leaveRequest.getType() == LeaveType.PAID_LEAVE) {
            leaveBalanceService.approvePendingDays(
                    leaveRequest.getEmployee(),
                    leaveRequest.getStartDate().getYear(),
                    leaveRequest.getTotalDays()
            );
        }
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setReviewedBy(reviewer);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setRejectionReason(null);
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        notifyEmployee(savedRequest, "Demande approuvée", "Votre demande d'absence a été approuvée.", NotificationType.SUCCESS);
        return leaveRequestMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional
    public LeaveRequestResponse reject(Long id, LeaveReviewRequest request, String reviewerUsername) {
        if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }

        LeaveRequest leaveRequest = findPendingLeaveRequest(id);
        UserAccount reviewer = findUser(reviewerUsername);
        if (leaveRequest.getType() == LeaveType.PAID_LEAVE) {
            leaveBalanceService.releasePendingDays(
                    leaveRequest.getEmployee(),
                    leaveRequest.getStartDate().getYear(),
                    leaveRequest.getTotalDays()
            );
        }
        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setReviewedBy(reviewer);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setRejectionReason(request.rejectionReason());
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        notifyEmployee(savedRequest, "Demande refusée", "Votre demande d'absence a été refusée.", NotificationType.ERROR);
        return leaveRequestMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional
    public LeaveRequestResponse cancel(Long id, String username) {
        LeaveRequest leaveRequest = findPendingLeaveRequest(id);
        UserAccount currentUser = findUser(username);
        if (currentUser.getRole() != Role.ADMIN_RH && !isOwnLeaveRequest(leaveRequest, username)) {
            throw new AccessDeniedException("You cannot cancel this leave request");
        }
        if (leaveRequest.getType() == LeaveType.PAID_LEAVE) {
            leaveBalanceService.releasePendingDays(
                    leaveRequest.getEmployee(),
                    leaveRequest.getStartDate().getYear(),
                    leaveRequest.getTotalDays()
            );
        }
        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        if (currentUser.getRole() == Role.ADMIN_RH) {
            notifyEmployee(savedRequest, "Demande annulée", "Votre demande d'absence a été annulée par le service RH.", NotificationType.WARNING);
        } else {
            notificationService.notifyAdmins(
                    "Demande annulée",
                    savedRequest.getEmployee().getFirstName() + " " + savedRequest.getEmployee().getLastName() + " a annulé une demande d'absence.",
                    NotificationType.INFO,
                    "/admin/leave-requests"
            );
        }
        return leaveRequestMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminLeaveSummaryResponse adminSummary() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);
        return AdminLeaveSummaryResponse.builder()
                .pendingCount(leaveRequestRepository.countByStatus(LeaveStatus.PENDING))
                .approvedThisMonth(leaveRequestRepository.countByStatusAndCreatedAtBetween(LeaveStatus.APPROVED, monthStart, nextMonthStart))
                .rejectedThisMonth(leaveRequestRepository.countByStatusAndCreatedAtBetween(LeaveStatus.REJECTED, monthStart, nextMonthStart))
                .cancelledThisMonth(leaveRequestRepository.countByStatusAndCreatedAtBetween(LeaveStatus.CANCELLED, monthStart, nextMonthStart))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeLeaveSummaryResponse mySummary(String username) {
        Employee employee = findEmployeeForCurrentUser(username);
        LocalDateTime yearStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime nextYearStart = yearStart.plusYears(1);
        return EmployeeLeaveSummaryResponse.builder()
                .pendingRequests(leaveRequestRepository.countByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING))
                .approvedThisYear(leaveRequestRepository.countByEmployeeIdAndStatusAndCreatedAtBetween(employee.getId(), LeaveStatus.APPROVED, yearStart, nextYearStart))
                .rejectedThisYear(leaveRequestRepository.countByEmployeeIdAndStatusAndCreatedAtBetween(employee.getId(), LeaveStatus.REJECTED, yearStart, nextYearStart))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminLeaveStatisticsResponse adminStatistics(Integer year) {
        Integer resolvedYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        LocalDateTime nextMonthStartDateTime = monthStart.plusMonths(1).atStartOfDay();

        return AdminLeaveStatisticsResponse.builder()
                .pendingRequestsCount(leaveRequestRepository.countByStatus(LeaveStatus.PENDING))
                .approvedRequestsThisMonth(leaveRequestRepository.countByStatusAndCreatedAtBetween(LeaveStatus.APPROVED, monthStartDateTime, nextMonthStartDateTime))
                .rejectedRequestsThisMonth(leaveRequestRepository.countByStatusAndCreatedAtBetween(LeaveStatus.REJECTED, monthStartDateTime, nextMonthStartDateTime))
                .cancelledRequestsThisMonth(leaveRequestRepository.countByStatusAndCreatedAtBetween(LeaveStatus.CANCELLED, monthStartDateTime, nextMonthStartDateTime))
                .totalApprovedLeaveDaysThisMonth(leaveRequestRepository.sumDaysByStatusAndDateRange(LeaveStatus.APPROVED, monthStart, monthEnd))
                .totalUnpaidLeaveDaysThisMonth(leaveRequestRepository.sumDaysByStatusAndTypeAndDateRange(LeaveStatus.APPROVED, LeaveType.UNPAID_LEAVE, monthStart, monthEnd))
                .employeesOnLeaveToday(leaveRequestRepository.countEmployeesOnLeaveToday(LeaveStatus.APPROVED, today))
                .monthlyStats(leaveRequestRepository.findMonthlyStats(LeaveStatus.APPROVED, resolvedYear))
                .leaveTypeStats(leaveRequestRepository.findLeaveTypeStats(LeaveStatus.APPROVED, resolvedYear))
                .departmentStats(leaveRequestRepository.findDepartmentStats(LeaveStatus.APPROVED, resolvedYear))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeLeaveStatisticsResponse myStatistics(String username, Integer year) {
        Integer resolvedYear = year == null ? LocalDate.now().getYear() : year;
        Employee employee = findEmployeeForCurrentUser(username);
        LocalDateTime yearStart = LocalDate.of(resolvedYear, 1, 1).atStartOfDay();
        LocalDateTime nextYearStart = yearStart.plusYears(1);

        return EmployeeLeaveStatisticsResponse.builder()
                .myPendingRequestsCount(leaveRequestRepository.countByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING))
                .myApprovedRequestsThisYear(leaveRequestRepository.countByEmployeeIdAndStatusAndCreatedAtBetween(employee.getId(), LeaveStatus.APPROVED, yearStart, nextYearStart))
                .myRejectedRequestsThisYear(leaveRequestRepository.countByEmployeeIdAndStatusAndCreatedAtBetween(employee.getId(), LeaveStatus.REJECTED, yearStart, nextYearStart))
                .myTotalApprovedDaysThisYear(leaveRequestRepository.sumEmployeeDaysByStatusAndYear(employee.getId(), LeaveStatus.APPROVED, resolvedYear))
                .myPaidLeaveDaysThisYear(leaveRequestRepository.sumEmployeeDaysByStatusTypeAndYear(employee.getId(), LeaveStatus.APPROVED, LeaveType.PAID_LEAVE, resolvedYear))
                .myUnpaidLeaveDaysThisYear(leaveRequestRepository.sumEmployeeDaysByStatusTypeAndYear(employee.getId(), LeaveStatus.APPROVED, LeaveType.UNPAID_LEAVE, resolvedYear))
                .monthlyStats(leaveRequestRepository.findEmployeeMonthlyStats(employee.getId(), LeaveStatus.APPROVED, resolvedYear))
                .leaveTypeStats(leaveRequestRepository.findEmployeeLeaveTypeStats(employee.getId(), LeaveStatus.APPROVED, resolvedYear))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveCalendarEventResponse> adminCalendar(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId,
            LeaveStatus status,
            LeaveType type
    ) {
        validateDates(startDate, endDate);
        List<LeaveStatus> statuses = calendarStatuses(status);
        return leaveRequestRepository.findCalendarEvents(startDate, endDate, statuses, departmentId, type)
                .stream()
                .map(leaveRequestMapper::toCalendarEvent)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveCalendarEventResponse> myCalendar(String username, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        Employee employee = findEmployeeForCurrentUser(username);
        return leaveRequestRepository.findEmployeeCalendarEvents(
                        employee.getId(),
                        startDate,
                        endDate,
                        List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)
                )
                .stream()
                .map(leaveRequestMapper::toCalendarEvent)
                .toList();
    }

    @Override
    @Transactional
    public LeaveRequestResponse uploadDocument(Long id, MultipartFile file, String username) {
        LeaveRequest leaveRequest = findPendingLeaveRequest(id);
        validateCanModifyDocument(leaveRequest, username);
        validateDocument(file);

        deleteStoredDocumentIfPresent(leaveRequest);
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
        String storedFileName = leaveRequest.getId() + "-" + UUID.randomUUID() + "-" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path uploadDirectory = resolveUploadDirectory();
        Path targetPath = uploadDirectory.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store document");
        }

        leaveRequest.setDocumentFileName(originalFileName);
        leaveRequest.setDocumentContentType(file.getContentType());
        leaveRequest.setDocumentStoragePath(targetPath.toString());
        leaveRequest.setDocumentSize(file.getSize());
        leaveRequest.setDocumentUploadedAt(LocalDateTime.now());

        return leaveRequestMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveDocumentResource downloadDocument(Long id, String username) {
        LeaveRequest leaveRequest = findLeaveRequest(id);
        validateCanReadDocument(leaveRequest, username);
        if (leaveRequest.getDocumentStoragePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        try {
            Path documentPath = Paths.get(leaveRequest.getDocumentStoragePath()).normalize();
            Resource resource = new UrlResource(documentPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
            }
            return new LeaveDocumentResource(
                    resource,
                    leaveRequest.getDocumentFileName(),
                    leaveRequest.getDocumentContentType(),
                    leaveRequest.getDocumentSize()
            );
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read document");
        }
    }

    @Override
    @Transactional
    public LeaveRequestResponse deleteDocument(Long id, String username) {
        LeaveRequest leaveRequest = findPendingLeaveRequest(id);
        validateCanModifyDocument(leaveRequest, username);
        deleteStoredDocumentIfPresent(leaveRequest);
        clearDocumentMetadata(leaveRequest);
        return leaveRequestMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    private Specification<LeaveRequest> buildSpecification(LeaveRequestFilter filter) {
        return (root, query, criteriaBuilder) -> {
            root.fetch("employee", JoinType.LEFT);
            root.fetch("reviewedBy", JoinType.LEFT);
            if (query != null) {
                query.distinct(true);
            }
            var predicates = criteriaBuilder.conjunction();

            if (filter.status() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("status"), filter.status()));
            }
            if (filter.type() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("type"), filter.type()));
            }
            if (filter.employeeId() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("employee").get("id"), filter.employeeId()));
            }
            if (filter.departmentId() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("employee").get("department").get("id"), filter.departmentId()));
            }
            if (filter.startDate() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), filter.startDate()));
            }
            if (filter.endDate() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), filter.endDate()));
            }
            return predicates;
        };
    }

    private LeaveRequest findPendingLeaveRequest(Long id) {
        LeaveRequest leaveRequest = findLeaveRequest(id);
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending leave requests can be modified");
        }
        return leaveRequest;
    }

    private LeaveRequest findLeaveRequest(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
    }

    private Employee findEmployeeForAdminRequest(Long employeeId) {
        if (employeeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is required");
        }
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private Employee findEmployeeForCurrentUser(String username) {
        return employeeRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No employee profile is linked to the current account"));
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean isOwnLeaveRequest(LeaveRequest leaveRequest, String username) {
        UserAccount userAccount = leaveRequest.getEmployee().getUserAccount();
        return userAccount != null && userAccount.getUsername().equals(username);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before or equal to end date");
        }
    }

    private List<LeaveStatus> calendarStatuses(LeaveStatus status) {
        if (status != null) {
            return List.of(status);
        }
        return new ArrayList<>(List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED));
    }

    private void validateCanModifyDocument(LeaveRequest leaveRequest, String username) {
        UserAccount currentUser = findUser(username);
        if (currentUser.getRole() == Role.ADMIN_RH) {
            return;
        }
        if (!isOwnLeaveRequest(leaveRequest, username)) {
            throw new AccessDeniedException("You cannot modify this document");
        }
    }

    private void validateCanReadDocument(LeaveRequest leaveRequest, String username) {
        UserAccount currentUser = findUser(username);
        if (currentUser.getRole() == Role.ADMIN_RH) {
            return;
        }
        if (!isOwnLeaveRequest(leaveRequest, username)) {
            throw new AccessDeniedException("You cannot access this document");
        }
    }

    private void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is required");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document size must not exceed 5MB");
        }
        if (!ALLOWED_DOCUMENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, PNG, JPG and JPEG files are accepted");
        }
    }

    private Path resolveUploadDirectory() {
        return Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    private void deleteStoredDocumentIfPresent(LeaveRequest leaveRequest) {
        if (leaveRequest.getDocumentStoragePath() == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(leaveRequest.getDocumentStoragePath()).normalize());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete existing document");
        }
    }

    private void clearDocumentMetadata(LeaveRequest leaveRequest) {
        leaveRequest.setDocumentFileName(null);
        leaveRequest.setDocumentContentType(null);
        leaveRequest.setDocumentStoragePath(null);
        leaveRequest.setDocumentSize(null);
        leaveRequest.setDocumentUploadedAt(null);
    }

    private void notifyEmployee(LeaveRequest leaveRequest, String title, String message, NotificationType type) {
        UserAccount userAccount = leaveRequest.getEmployee().getUserAccount();
        if (userAccount == null) {
            return;
        }
        notificationService.notifyUsername(
                userAccount.getUsername(),
                title,
                message,
                type,
                "/employee/my-leave-requests"
        );
    }

    private void validateNoOverlap(Long employeeId, LocalDate startDate, LocalDate endDate) {
        if (leaveRequestRepository.existsOverlappingRequest(employeeId, startDate, endDate, BLOCKING_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An overlapping leave request already exists");
        }
    }

    private double calculateTotalDays(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1.0;
    }
}

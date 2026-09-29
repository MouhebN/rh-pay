package com.rhpaie.contract.service;

import com.rhpaie.common.config.FileStorageProperties;
import com.rhpaie.contract.dto.*;
import com.rhpaie.contract.entity.*;
import com.rhpaie.contract.mapper.ContractMapper;
import com.rhpaie.contract.repository.ContractRepository;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.user.entity.Role;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private static final int DEFAULT_EXPIRING_SOON_DAYS = 30;
    private static final long MAX_DOCUMENT_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ContractMapper contractMapper;
    private final FileStorageProperties fileStorageProperties;

    @Override
    @Transactional
    public ContractResponse create(ContractCreateRequest request, String username) {
        UserAccount user = findUser(username);
        Employee employee = findEmployee(request.employeeId());
        validateDates(request.type(), request.startDate(), request.endDate(), request.trialPeriodEndDate());
        ContractStatus status = request.status() == null ? ContractStatus.DRAFT : request.status();
        if (status == ContractStatus.ACTIVE) {
            ensureNoActiveContract(employee.getId());
        }

        Contract contract = Contract.builder()
                .employee(employee)
                .type(request.type())
                .status(status)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .trialPeriodEndDate(request.trialPeriodEndDate())
                .salaryAtSignature(request.salaryAtSignature())
                .contractReference(normalizeBlank(request.contractReference()))
                .notes(normalizeBlank(request.notes()))
                .createdBy(user)
                .updatedBy(user)
                .build();
        Contract saved = contractRepository.save(contract);
        if (saved.getContractReference() == null) {
            saved.setContractReference(generateReference(saved));
        }
        return contractMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> findAll(ContractFilterRequest filter) {
        return contractRepository.findAll(buildSpecification(filter))
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse findById(Long id) {
        return contractMapper.toResponse(findContract(id));
    }

    @Override
    @Transactional
    public ContractResponse update(Long id, ContractUpdateRequest request, String username) {
        Contract contract = findContract(id);
        if (contract.getStatus() == ContractStatus.TERMINATED || contract.getStatus() == ContractStatus.EXPIRED || contract.getStatus() == ContractStatus.RENEWED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit terminated, expired, or renewed contracts");
        }
        validateDates(request.type(), request.startDate(), request.endDate(), request.trialPeriodEndDate());
        ContractStatus newStatus = request.status() == null ? contract.getStatus() : request.status();
        if (newStatus == ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.ACTIVE) {
            ensureNoActiveContract(contract.getEmployee().getId());
        }
        contract.setType(request.type());
        contract.setStatus(newStatus);
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setTrialPeriodEndDate(request.trialPeriodEndDate());
        contract.setSalaryAtSignature(request.salaryAtSignature());
        contract.setContractReference(normalizeBlank(request.contractReference()) == null ? contract.getContractReference() : normalizeBlank(request.contractReference()));
        contract.setNotes(normalizeBlank(request.notes()));
        contract.setUpdatedBy(findUser(username));
        return contractMapper.toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse activate(Long id, String username) {
        Contract contract = findContract(id);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft contracts can be activated");
        }
        validateDates(contract.getType(), contract.getStartDate(), contract.getEndDate(), contract.getTrialPeriodEndDate());
        ensureNoActiveContract(contract.getEmployee().getId());
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setUpdatedBy(findUser(username));
        return contractMapper.toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse terminate(Long id, ContractTerminateRequest request, String username) {
        Contract contract = findContract(id);
        if (contract.getStatus() == ContractStatus.TERMINATED || contract.getStatus() == ContractStatus.RENEWED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract is already closed");
        }
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setTerminationDate(request.terminationDate());
        contract.setTerminationReason(normalizeBlank(request.terminationReason()));
        contract.setTerminatedBy(findUser(username));
        contract.setUpdatedBy(findUser(username));
        return contractMapper.toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse renew(Long id, ContractRenewRequest request, String username) {
        Contract oldContract = findContract(id);
        if (oldContract.getStatus() != ContractStatus.ACTIVE && oldContract.getStatus() != ContractStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only active or expired contracts can be renewed");
        }
        validateDates(oldContract.getType(), request.newStartDate(), request.newEndDate(), request.trialPeriodEndDate());
        UserAccount user = findUser(username);
        oldContract.setStatus(ContractStatus.RENEWED);
        oldContract.setUpdatedBy(user);

        Contract newContract = Contract.builder()
                .employee(oldContract.getEmployee())
                .type(oldContract.getType())
                .status(ContractStatus.ACTIVE)
                .startDate(request.newStartDate())
                .endDate(request.newEndDate())
                .trialPeriodEndDate(request.trialPeriodEndDate())
                .salaryAtSignature(request.salaryAtSignature())
                .notes(normalizeBlank(request.notes()))
                .createdBy(user)
                .updatedBy(user)
                .build();
        Contract saved = contractRepository.save(newContract);
        saved.setContractReference(generateReference(saved));
        return contractMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Contract contract = findContract(id);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft contracts can be deleted");
        }
        contractRepository.delete(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> findByEmployee(Long employeeId) {
        return contractRepository.findDetailedByEmployeeId(employeeId)
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractMonitoringResponse monitoring() {
        LocalDate today = LocalDate.now();
        long expiringSoon = contractRepository.countByStatusAndEndDateBetween(ContractStatus.ACTIVE, today, today.plusDays(DEFAULT_EXPIRING_SOON_DAYS));
        long expired = contractRepository.countByStatus(ContractStatus.EXPIRED) + contractRepository.countByStatusAndEndDateBefore(ContractStatus.ACTIVE, today);
        long missingDocuments = contractRepository.countByDocumentStoragePathIsNull();
        return ContractMonitoringResponse.builder()
                .activeContractsCount(contractRepository.countByStatus(ContractStatus.ACTIVE))
                .expiringSoonCount(expiringSoon)
                .expiredContractsCount(expired)
                .missingDocumentCount(missingDocuments)
                .actionRequiredCount(expiringSoon + expired + missingDocuments)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> expiringSoon(Integer days) {
        int range = days == null || days < 1 ? DEFAULT_EXPIRING_SOON_DAYS : days;
        LocalDate today = LocalDate.now();
        return contractRepository.findExpiringSoon(ContractStatus.ACTIVE, today, today.plusDays(range))
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> expired() {
        return contractRepository.findExpiredForMonitoring(ContractStatus.ACTIVE, ContractStatus.EXPIRED, LocalDate.now())
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> missingDocuments() {
        return contractRepository.findByDocumentStoragePathIsNullOrderByStartDateDesc()
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public int refreshExpired() {
        List<Contract> expired = contractRepository.findExpiredActiveContracts(ContractStatus.ACTIVE, LocalDate.now());
        expired.forEach(contract -> contract.setStatus(ContractStatus.EXPIRED));
        return expired.size();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSummaryResponse summary() {
        Map<String, Long> byType = Arrays.stream(ContractType.values())
                .collect(Collectors.toMap(Enum::name, contractRepository::countByType, (a, b) -> a, LinkedHashMap::new));
        LocalDate today = LocalDate.now();
        return ContractSummaryResponse.builder()
                .totalContracts(contractRepository.count())
                .activeContracts(contractRepository.countByStatus(ContractStatus.ACTIVE))
                .draftContracts(contractRepository.countByStatus(ContractStatus.DRAFT))
                .expiredContracts(contractRepository.countByStatus(ContractStatus.EXPIRED) + contractRepository.countByStatusAndEndDateBefore(ContractStatus.ACTIVE, today))
                .terminatedContracts(contractRepository.countByStatus(ContractStatus.TERMINATED))
                .expiringSoonCount(contractRepository.countByStatusAndEndDateBetween(ContractStatus.ACTIVE, today, today.plusDays(DEFAULT_EXPIRING_SOON_DAYS)))
                .contractsByType(byType)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> findMine(String username) {
        Employee employee = findEmployeeByUsername(username);
        return findByEmployee(employee.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse findMineById(String username, Long id) {
        Employee employee = findEmployeeByUsername(username);
        Contract contract = findContract(id);
        if (!contract.getEmployee().getId().equals(employee.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez consulter que vos propres contrats.");
        }
        return contractMapper.toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse uploadDocument(Long id, MultipartFile file, String username) {
        findUser(username);
        Contract contract = findContract(id);
        validateDocument(file);
        deleteStoredDocumentIfPresent(contract);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "contrat.pdf" : file.getOriginalFilename());
        String storedFileName = contract.getId() + "-" + UUID.randomUUID() + "-" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path uploadDirectory = resolveUploadDirectory();
        Path targetPath = uploadDirectory.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store contract document");
        }

        contract.setDocumentFileName(originalFileName);
        contract.setDocumentContentType(PDF_CONTENT_TYPE);
        contract.setDocumentStoragePath(targetPath.toString());
        contract.setDocumentSize(file.getSize());
        contract.setDocumentUploadedAt(LocalDateTime.now());
        contract.setUpdatedBy(findUser(username));

        return contractMapper.toResponse(contractRepository.save(contract));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDocumentResource downloadDocument(Long id, String username) {
        Contract contract = findContract(id);
        validateCanReadDocument(contract, username);
        if (contract.getDocumentStoragePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document not found");
        }

        try {
            Path documentPath = Paths.get(contract.getDocumentStoragePath()).normalize();
            Resource resource = new UrlResource(documentPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document file not found");
            }
            return new ContractDocumentResource(
                    resource,
                    contract.getDocumentFileName(),
                    contract.getDocumentContentType(),
                    contract.getDocumentSize()
            );
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read contract document");
        }
    }

    @Override
    @Transactional
    public ContractResponse deleteDocument(Long id) {
        Contract contract = findContract(id);
        deleteStoredDocumentIfPresent(contract);
        clearDocumentMetadata(contract);
        return contractMapper.toResponse(contractRepository.save(contract));
    }

    private Specification<Contract> buildSpecification(ContractFilterRequest filter) {
        return (root, query, cb) -> {
            var employeeFetch = root.fetch("employee", JoinType.LEFT);
            employeeFetch.fetch("department", JoinType.LEFT);
            employeeFetch.fetch("jobPosition", JoinType.LEFT);
            root.fetch("createdBy", JoinType.LEFT);
            root.fetch("updatedBy", JoinType.LEFT);
            root.fetch("terminatedBy", JoinType.LEFT);
            if (query != null) {
                query.distinct(true);
                query.orderBy(cb.desc(root.get("startDate")), cb.desc(root.get("id")));
            }
            var predicates = cb.conjunction();
            if (filter.employeeId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("id"), filter.employeeId()));
            if (filter.departmentId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("department").get("id"), filter.departmentId()));
            if (filter.jobPositionId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("jobPosition").get("id"), filter.jobPositionId()));
            if (filter.type() != null) predicates = cb.and(predicates, cb.equal(root.get("type"), filter.type()));
            if (filter.status() != null) predicates = cb.and(predicates, cb.equal(root.get("status"), filter.status()));
            if (filter.startDateFrom() != null) predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("startDate"), filter.startDateFrom()));
            if (filter.startDateTo() != null) predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("startDate"), filter.startDateTo()));
            if (filter.endDateFrom() != null) predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("endDate"), filter.endDateFrom()));
            if (filter.endDateTo() != null) predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("endDate"), filter.endDateTo()));
            if (filter.expiringSoonDays() != null && filter.expiringSoonDays() > 0) {
                LocalDate today = LocalDate.now();
                predicates = cb.and(predicates,
                        cb.equal(root.get("status"), ContractStatus.ACTIVE),
                        cb.isNotNull(root.get("endDate")),
                        cb.between(root.get("endDate"), today, today.plusDays(filter.expiringSoonDays())));
            }
            return predicates;
        };
    }

    private void validateDates(ContractType type, LocalDate startDate, LocalDate endDate, LocalDate trialPeriodEndDate) {
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de début doit être avant ou égale à la date de fin.");
        }
        if (type != ContractType.CDI && endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce type de contrat nécessite une date de fin.");
        }
        if (trialPeriodEndDate != null && trialPeriodEndDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période d’essai doit se terminer après la date de début.");
        }
        if (trialPeriodEndDate != null && endDate != null && trialPeriodEndDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période d’essai doit se terminer avant la date de fin.");
        }
    }

    private void ensureNoActiveContract(Long employeeId) {
        if (contractRepository.existsByEmployeeIdAndStatus(employeeId, ContractStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet employé possède déjà un contrat actif.");
        }
    }

    private String generateReference(Contract contract) {
        long sequence = contractRepository.countByEmployeeId(contract.getEmployee().getId());
        return "CTR-%d-%d-%03d".formatted(contract.getStartDate().getYear(), contract.getEmployee().getId(), sequence);
    }

    private Contract findContract(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private Employee findEmployeeByUsername(String username) {
        return employeeRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No employee profile is linked to the current account"));
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateCanReadDocument(Contract contract, String username) {
        UserAccount currentUser = findUser(username);
        if (currentUser.getRole() == Role.ADMIN_RH) {
            return;
        }
        UserAccount contractUser = contract.getEmployee().getUserAccount();
        if (contractUser == null || !contractUser.getUsername().equals(username)) {
            throw new AccessDeniedException("You cannot access this contract document");
        }
    }

    private void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is required");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document size must not exceed 5MB");
        }
        if (!PDF_CONTENT_TYPE.equals(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted");
        }
    }

    private Path resolveUploadDirectory() {
        return Paths.get(fileStorageProperties.getContractUploadDir()).toAbsolutePath().normalize();
    }

    private void deleteStoredDocumentIfPresent(Contract contract) {
        if (contract.getDocumentStoragePath() == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(contract.getDocumentStoragePath()).normalize());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete existing contract document");
        }
    }

    private void clearDocumentMetadata(Contract contract) {
        contract.setDocumentFileName(null);
        contract.setDocumentContentType(null);
        contract.setDocumentStoragePath(null);
        contract.setDocumentSize(null);
        contract.setDocumentUploadedAt(null);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

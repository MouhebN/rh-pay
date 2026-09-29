package com.rhpaie.contract.service;

import com.rhpaie.contract.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ContractService {
    ContractResponse create(ContractCreateRequest request, String username);
    List<ContractResponse> findAll(ContractFilterRequest filter);
    ContractResponse findById(Long id);
    ContractResponse update(Long id, ContractUpdateRequest request, String username);
    ContractResponse activate(Long id, String username);
    ContractResponse terminate(Long id, ContractTerminateRequest request, String username);
    ContractResponse renew(Long id, ContractRenewRequest request, String username);
    void delete(Long id);
    List<ContractResponse> findByEmployee(Long employeeId);
    ContractMonitoringResponse monitoring();
    List<ContractResponse> expiringSoon(Integer days);
    List<ContractResponse> expired();
    List<ContractResponse> missingDocuments();
    int refreshExpired();
    ContractSummaryResponse summary();
    List<ContractResponse> findMine(String username);
    ContractResponse findMineById(String username, Long id);
    ContractResponse uploadDocument(Long id, MultipartFile file, String username);
    ContractDocumentResource downloadDocument(Long id, String username);
    ContractResponse deleteDocument(Long id);
}

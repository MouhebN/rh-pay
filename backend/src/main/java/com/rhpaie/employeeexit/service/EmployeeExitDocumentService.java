package com.rhpaie.employeeexit.service;

import com.rhpaie.employeeexit.dto.EmployeeExitDocumentResource;
import com.rhpaie.employeeexit.dto.EmployeeExitDocumentResponse;
import com.rhpaie.employeeexit.entity.EmployeeExitDocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmployeeExitDocumentService {
    EmployeeExitDocumentResponse upload(Long exitId, MultipartFile file, EmployeeExitDocumentType type, Boolean visibleToEmployee, String username);
    List<EmployeeExitDocumentResponse> findByExit(Long exitId);
    EmployeeExitDocumentResource download(Long exitId, Long documentId);
    void delete(Long exitId, Long documentId);
    List<EmployeeExitDocumentResponse> findVisibleMine(String username);
    EmployeeExitDocumentResource downloadVisibleMine(String username, Long documentId);
}

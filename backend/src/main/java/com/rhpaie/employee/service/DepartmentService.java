package com.rhpaie.employee.service;

import com.rhpaie.employee.dto.DepartmentRequest;
import com.rhpaie.employee.dto.DepartmentResponse;
import com.rhpaie.employee.entity.Department;
import com.rhpaie.employee.mapper.DepartmentMapper;
import com.rhpaie.employee.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long id) {
        return departmentMapper.toResponse(findDepartmentById(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        }

        Department department = Department.builder()
                .name(request.name())
                .description(request.description())
                .build();

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findDepartmentById(id);

        if (!department.getName().equals(request.name()) && departmentRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        }

        department.setName(request.name());
        department.setDescription(request.description());

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Transactional
    public void delete(Long id) {
        departmentRepository.delete(findDepartmentById(id));
    }

    public Department findDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
    }
}

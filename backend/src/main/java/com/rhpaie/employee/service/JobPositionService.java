package com.rhpaie.employee.service;

import com.rhpaie.employee.dto.JobPositionRequest;
import com.rhpaie.employee.dto.JobPositionResponse;
import com.rhpaie.employee.entity.Department;
import com.rhpaie.employee.entity.JobPosition;
import com.rhpaie.employee.mapper.JobPositionMapper;
import com.rhpaie.employee.repository.JobPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobPositionService {

    private final JobPositionRepository jobPositionRepository;
    private final DepartmentService departmentService;
    private final JobPositionMapper jobPositionMapper;

    @Transactional(readOnly = true)
    public List<JobPositionResponse> findAll() {
        return jobPositionRepository.findAll()
                .stream()
                .map(jobPositionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobPositionResponse findById(Long id) {
        return jobPositionMapper.toResponse(findJobPositionById(id));
    }

    @Transactional
    public JobPositionResponse create(JobPositionRequest request) {
        if (jobPositionRepository.existsByTitle(request.title())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job position already exists");
        }

        JobPosition jobPosition = JobPosition.builder()
                .title(request.title())
                .description(request.description())
                .baseSalary(request.baseSalary())
                .department(resolveDepartment(request.departmentId()))
                .build();

        return jobPositionMapper.toResponse(jobPositionRepository.save(jobPosition));
    }

    @Transactional
    public JobPositionResponse update(Long id, JobPositionRequest request) {
        JobPosition jobPosition = findJobPositionById(id);

        if (!jobPosition.getTitle().equals(request.title()) && jobPositionRepository.existsByTitle(request.title())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job position already exists");
        }

        jobPosition.setTitle(request.title());
        jobPosition.setDescription(request.description());
        jobPosition.setBaseSalary(request.baseSalary());
        jobPosition.setDepartment(resolveDepartment(request.departmentId()));

        return jobPositionMapper.toResponse(jobPositionRepository.save(jobPosition));
    }

    @Transactional
    public void delete(Long id) {
        jobPositionRepository.delete(findJobPositionById(id));
    }

    public JobPosition findJobPositionById(Long id) {
        return jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job position not found"));
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentService.findDepartmentById(departmentId);
    }
}

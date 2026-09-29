package com.rhpaie.employee.mapper;

import com.rhpaie.employee.dto.JobPositionResponse;
import com.rhpaie.employee.entity.JobPosition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobPositionMapper {

    private final DepartmentMapper departmentMapper;

    public JobPositionResponse toResponse(JobPosition jobPosition) {
        if (jobPosition == null) {
            return null;
        }

        return JobPositionResponse.builder()
                .id(jobPosition.getId())
                .title(jobPosition.getTitle())
                .description(jobPosition.getDescription())
                .baseSalary(jobPosition.getBaseSalary())
                .department(departmentMapper.toResponse(jobPosition.getDepartment()))
                .createdAt(jobPosition.getCreatedAt())
                .updatedAt(jobPosition.getUpdatedAt())
                .build();
    }
}

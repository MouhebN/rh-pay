package com.rhpaie.employee.mapper;

import com.rhpaie.employee.dto.EmployeeResponse;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.user.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeMapper {

    private final DepartmentMapper departmentMapper;
    private final JobPositionMapper jobPositionMapper;

    public EmployeeResponse toResponse(Employee employee) {
        UserAccount userAccount = employee.getUserAccount();

        return EmployeeResponse.builder()
                .id(employee.getId())
                .matricule(employee.getMatricule())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .birthDate(employee.getBirthDate())
                .hireDate(employee.getHireDate())
                .department(departmentMapper.toResponse(employee.getDepartment()))
                .jobPosition(jobPositionMapper.toResponse(employee.getJobPosition()))
                .baseSalary(employee.getBaseSalary())
                .rib(employee.getRib())
                .bankName(employee.getBankName())
                .paymentMethod(employee.getPaymentMethod())
                .userAccountId(userAccount != null ? userAccount.getId() : null)
                .username(userAccount != null ? userAccount.getUsername() : null)
                .userRole(userAccount != null ? userAccount.getRole() : null)
                .status(employee.getStatus())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}

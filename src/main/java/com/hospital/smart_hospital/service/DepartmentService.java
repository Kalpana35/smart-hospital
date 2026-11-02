package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.DepartmentResponse;
import com.hospital.smart_hospital.model.Department;
import java.util.List;

public interface DepartmentService {

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentById(Long id);

    Department createDepartment(Department department);
}

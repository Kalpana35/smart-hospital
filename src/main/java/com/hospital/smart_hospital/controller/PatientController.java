package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.PatientAccountRequest;
import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.PatientResponse;
import com.hospital.smart_hospital.model.PatientStatus;
import com.hospital.smart_hospital.service.PatientService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping("/admit")
    public ResponseEntity<ApiResponse<PatientResponse>> admitPatient(@RequestBody PatientAdmissionRequest request) {
        PatientResponse patient = patientService.admitPatient(request);
        return new ResponseEntity<>(
                ApiResponse.ok("Patient admitted successfully", patient),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{id}/discharge")
    public ResponseEntity<ApiResponse<PatientResponse>> dischargePatient(@PathVariable Long id) {
        PatientResponse patient = patientService.dischargePatient(id);
        return ResponseEntity.ok(ApiResponse.ok("Patient discharged successfully", patient));
    }

    @PostMapping("/{id}/create-account")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatientAccount(
            @PathVariable Long id,
            @RequestBody PatientAccountRequest request
    ) {
        PatientResponse patient = patientService.createPatientPortalAccount(
                id,
                request != null ? request.getUsername() : null,
                request != null ? request.getPassword() : null
        );
        return ResponseEntity.ok(ApiResponse.ok("Patient portal account created successfully", patient));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PatientResponse>> getMyPatientRecord(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthenticated"));
        }
        PatientResponse patient = patientService.getMyPatientRecord(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("My patient record retrieved successfully", patient));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getPatients(
            @RequestParam(required = false) PatientStatus status
    ) {
        List<PatientResponse> patients = patientService.getPatients(status);
        return ResponseEntity.ok(ApiResponse.ok("Patients retrieved successfully", patients));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        PatientResponse patient = patientService.getPatientById(id);

        // Enforce ownership: Patients can only view their own record
        if (authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_PATIENT"))) {
            String currentUsername = authentication.getName();
            if (patient.getUsername() == null || !patient.getUsername().equalsIgnoreCase(currentUsername)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access Denied: You cannot view records of other patients."));
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("Patient retrieved successfully", patient));
    }

    @GetMapping("/by-code/{patientCode}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientByCode(@PathVariable String patientCode) {
        PatientResponse patient = patientService.getPatientByCode(patientCode);
        return ResponseEntity.ok(ApiResponse.ok("Patient retrieved successfully", patient));
    }
}


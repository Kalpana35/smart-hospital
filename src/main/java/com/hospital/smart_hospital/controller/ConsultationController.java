package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.ConsultationRequest;
import com.hospital.smart_hospital.dto.ConsultationResponse;
import com.hospital.smart_hospital.service.ConsultationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ConsultationResponse>> recordConsultation(
            @RequestBody ConsultationRequest request,
            Authentication authentication
    ) {
        String doctorUsername = authentication != null ? authentication.getName() : "doctor";
        ConsultationResponse response = consultationService.recordConsultation(request, doctorUsername);
        return new ResponseEntity<>(
                ApiResponse.ok("Consultation and prescription recorded successfully", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<ConsultationResponse>>> getConsultationsForPatient(
            @PathVariable Long patientId,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "";
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_DOCTOR") || a.equals("ROLE_RECEPTIONIST"));

        List<ConsultationResponse> list = consultationService.getConsultationsForPatient(patientId, username, isStaff);
        return ResponseEntity.ok(ApiResponse.ok("Consultations retrieved successfully", list));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ConsultationResponse>>> getMyConsultations(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "";
        String roleName = "";
        if (authentication != null && !authentication.getAuthorities().isEmpty()) {
            roleName = authentication.getAuthorities().iterator().next().getAuthority();
        }
        List<ConsultationResponse> list = consultationService.getMyConsultations(username, roleName);
        return ResponseEntity.ok(ApiResponse.ok("Consultations retrieved successfully", list));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ConsultationResponse>>> getAllConsultations() {
        List<ConsultationResponse> list = consultationService.getAllConsultations();
        return ResponseEntity.ok(ApiResponse.ok("All consultations retrieved successfully", list));
    }
}

package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.BedAllocationHistoryResponse;
import com.hospital.smart_hospital.dto.BedRecommendationRequest;
import com.hospital.smart_hospital.dto.BedRecommendationResponse;
import com.hospital.smart_hospital.dto.PatientResponse;
import com.hospital.smart_hospital.service.BedAllocationHistoryService;
import com.hospital.smart_hospital.service.BedRecommendationService;
import com.hospital.smart_hospital.service.PatientService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/allocations")
public class BedAllocationController {

    private final BedRecommendationService bedRecommendationService;
    private final BedAllocationHistoryService bedAllocationHistoryService;
    private final PatientService patientService;

    public BedAllocationController(
            BedRecommendationService bedRecommendationService,
            BedAllocationHistoryService bedAllocationHistoryService,
            PatientService patientService
    ) {
        this.bedRecommendationService = bedRecommendationService;
        this.bedAllocationHistoryService = bedAllocationHistoryService;
        this.patientService = patientService;
    }

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<List<BedRecommendationResponse>>> recommendBeds(
            @RequestBody BedRecommendationRequest request
    ) {
        List<BedRecommendationResponse> recommendations = bedRecommendationService.recommendBeds(request);

        String message = recommendations.isEmpty()
                ? "No suitable available beds found matching criteria"
        : "Bed recommendations retrieved successfully";

        return ResponseEntity.ok(ApiResponse.ok(message, recommendations));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BedAllocationHistoryResponse>>> getAllocationHistory() {
        List<BedAllocationHistoryResponse> history = bedAllocationHistoryService.getAllAllocationHistory();
        return ResponseEntity.ok(ApiResponse.ok("Bed allocation history retrieved successfully", history));
    }

    @GetMapping("/history/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<BedAllocationHistoryResponse>>> getAllocationHistoryByPatient(@PathVariable Long patientId) {
        List<BedAllocationHistoryResponse> history = bedAllocationHistoryService.getAllocationHistoryByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.ok("Bed allocation history for patient retrieved successfully", history));
    }

    @GetMapping("/predictive-discharges")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getPredictiveDischarges() {
        List<PatientResponse> predictive = patientService.getPredictiveDischarges();
        return ResponseEntity.ok(ApiResponse.ok("Predictive discharges retrieved successfully", predictive));
    }
}


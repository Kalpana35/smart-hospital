package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.HospitalAnalyticsResponse;
import com.hospital.smart_hospital.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<HospitalAnalyticsResponse>> getAnalyticsSummary() {
        HospitalAnalyticsResponse response = analyticsService.getHospitalAnalytics();
        return ResponseEntity.ok(ApiResponse.ok("Hospital analytics retrieved successfully", response));
    }
}

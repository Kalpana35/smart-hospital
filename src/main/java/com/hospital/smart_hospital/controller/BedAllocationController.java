package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.BedRecommendationRequest;
import com.hospital.smart_hospital.dto.BedRecommendationResponse;
import com.hospital.smart_hospital.service.BedRecommendationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/allocations")
public class BedAllocationController {

    private final BedRecommendationService bedRecommendationService;

    public BedAllocationController(BedRecommendationService bedRecommendationService) {
        this.bedRecommendationService = bedRecommendationService;
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
}


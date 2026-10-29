package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.BedRecommendationRequest;
import com.hospital.smart_hospital.dto.BedRecommendationResponse;
import java.util.List;

public interface BedRecommendationService {

    List<BedRecommendationResponse> recommendBeds(BedRecommendationRequest request);
}


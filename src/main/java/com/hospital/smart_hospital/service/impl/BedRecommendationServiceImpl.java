package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.BedRecommendationRequest;
import com.hospital.smart_hospital.dto.BedRecommendationResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.service.BedRecommendationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BedRecommendationServiceImpl implements BedRecommendationService {

    private final BedRepository bedRepository;

    private static final Set<String> RESTRICTED_WARD_KEYWORDS = Set.of(
            "pediatric",
            "pediatrics",
            "neonatal",
            "nicu",
            "maternity",
            "obstetrics",
            "labor",
            "psychiatric",
            "psychiatry"
    );

    public BedRecommendationServiceImpl(BedRepository bedRepository) {
        this.bedRepository = bedRepository;
    }

    @Override
    public List<BedRecommendationResponse> recommendBeds(BedRecommendationRequest request) {
        if (request == null) {
            throw new BadRequestException("Recommendation request cannot be null.");
        }
        if (request.getRoomType() == null || request.getRoomType().trim().isEmpty()) {
            throw new BadRequestException("Requested room type is required.");
        }

        String targetRoomType = request.getRoomType().trim();
        String targetWard = request.getWardDepartment() != null ? request.getWardDepartment().trim() : "";
        boolean reqIcu = Boolean.TRUE.equals(request.getRequiresIcuMonitoring()) || targetRoomType.toUpperCase().contains("ICU");
        boolean reqIso = Boolean.TRUE.equals(request.getRequiresIsolation()) || targetRoomType.toUpperCase().contains("ISO");
        Double maxBudget = request.getPreferredMaxDailyBudget();
        boolean isEmergency = Boolean.TRUE.equals(request.getEmergency());

        // 1. Retrieve ONLY AVAILABLE beds (strictly excludes OCCUPIED, MAINTENANCE, and DIRTY)
        List<Bed> availableBeds = bedRepository.findByStatus(BedStatus.AVAILABLE);

        List<BedRecommendationResponse> recommendations = new ArrayList<>();

        for (Bed bed : availableBeds) {
            Room room = bed.getRoom();
            if (room == null) {
                continue;
            }

            // Clinical Check 1: ICU Monitoring Requirement
            if (reqIcu && !room.isHasIcuMonitoring() && !room.getRoomType().toUpperCase().contains("ICU")) {
                continue; // Cannot place an ICU patient in non-monitored bed
            }

            // Clinical Check 2: Isolation Requirement
            if (reqIso && !room.isHasIsolation() && !room.getRoomType().toUpperCase().contains("ISO")) {
                continue; // Cannot place an infectious isolation patient in general non-isolated bed
            }

            String currentWard = room.getWardDepartment() != null ? room.getWardDepartment().trim() : "";
            boolean sameRoomType = room.getRoomType() != null && room.getRoomType().equalsIgnoreCase(targetRoomType);
            boolean sameWard = !targetWard.isEmpty() && currentWard.equalsIgnoreCase(targetWard);
            boolean suitableAlternateWard = isClinicallySuitable(targetWard, currentWard);

            // Clinical Check 3: Care level compatibility
            if (!sameRoomType) {
                // Allow overflow between compatible care levels (e.g. ICU vs ICU-Stepdown, Emergency vs General)
                boolean compatibleAcuity = (reqIcu && room.isHasIcuMonitoring()) || (!reqIcu && !reqIso);
                if (!compatibleAcuity) {
                    continue;
                }
            }

            if (!sameWard && !suitableAlternateWard && !targetWard.isEmpty()) {
                continue; // Incompatible ward
            }

            // Calculate Base Clinical Match Score
            int score = 0;
            StringBuilder reasonBuilder = new StringBuilder();
            boolean isOverflow = false;

            if (sameRoomType && (sameWard || targetWard.isEmpty())) {
                score = 100;
                reasonBuilder.append("Exact match: requested room type '").append(room.getRoomType())
                        .append("' in ").append(currentWard).append(".");
            } else if (sameRoomType && suitableAlternateWard) {
                score = 80;
                isOverflow = true;
                reasonBuilder.append("Department overflow: requested room type '").append(room.getRoomType())
                        .append("' available in alternate suitable ward (").append(currentWard).append(").");
            } else if (reqIcu && room.isHasIcuMonitoring()) {
                score = 85;
                isOverflow = !sameWard && !targetWard.isEmpty();
                reasonBuilder.append("Clinical ICU match: equipped with vital monitoring in ").append(currentWard).append(".");
            } else {
                score = 70;
                isOverflow = true;
                reasonBuilder.append("Compatible clinical overflow care in ").append(currentWard).append(".");
            }

            // Emergency priority boost
            if (isEmergency) {
                score += 15;
                reasonBuilder.append(" [Emergency Priority]");
            }

            // Budget-Aware Assessment
            Double bedDailyCharge = bed.getDailyCharge();
            Boolean withinBudget = null;
            if (maxBudget != null && maxBudget > 0) {
                if (bedDailyCharge <= maxBudget) {
                    withinBudget = true;
                    score += 10; // Favorable ranking for budget-compliant beds
                    reasonBuilder.append(" Estimated charge ($").append(bedDailyCharge.intValue())
                            .append("/day) is within preferred budget ($").append(maxBudget.intValue()).append("/day).");
                } else {
                    withinBudget = false;
                    reasonBuilder.append(" Estimated charge ($").append(bedDailyCharge.intValue())
                            .append("/day) exceeds preferred budget ($").append(maxBudget.intValue())
                            .append("/day), but clinically required.");
                }
            } else {
                reasonBuilder.append(" Estimated charge: $").append(bedDailyCharge.intValue()).append("/day.");
            }

            recommendations.add(new BedRecommendationResponse(
                    bed.getId(),
                    bed.getBedNumber(),
                    room.getId(),
                    room.getRoomNumber(),
                    room.getRoomType(),
                    room.getWardDepartment(),
                    score,
                    reasonBuilder.toString(),
                    bedDailyCharge,
                    withinBudget,
                    isOverflow,
                    room.isHasIcuMonitoring(),
                    room.isHasIsolation(),
                    bed.getFeatures()
            ));
        }

        // Rank by Match Score descending, then room number and bed number ascending
        recommendations.sort(
                Comparator.comparing(BedRecommendationResponse::getMatchScore, Comparator.reverseOrder())
                        .thenComparing(BedRecommendationResponse::getRoomNumber, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(BedRecommendationResponse::getBedNumber, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        return recommendations;
    }

    private boolean isClinicallySuitable(String targetWard, String candidateWard) {
        if (candidateWard == null || candidateWard.trim().isEmpty()) {
            return false;
        }

        String targetLower = targetWard != null ? targetWard.toLowerCase().trim() : "";
        String candidateLower = candidateWard.toLowerCase().trim();

        boolean targetIsRestricted = isRestrictedWard(targetLower);
        boolean candidateIsRestricted = isRestrictedWard(candidateLower);

        if (targetIsRestricted) {
            return candidateLower.contains(targetLower) || targetLower.contains(candidateLower);
        }

        return !candidateIsRestricted;
    }

    private boolean isRestrictedWard(String ward) {
        if (ward == null || ward.isEmpty()) {
            return false;
        }
        for (String keyword : RESTRICTED_WARD_KEYWORDS) {
            if (ward.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

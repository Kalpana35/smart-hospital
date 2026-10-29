package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.BedRecommendationRequest;
import com.hospital.smart_hospital.dto.BedRecommendationResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.service.BedRecommendationService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BedRecommendationServiceImpl implements BedRecommendationService {

    private final BedRepository bedRepository;

    // Restricted wards that cannot accept general cross-ward allocation unless specifically requested
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

        // 1. Retrieve AVAILABLE beds only (ignores OCCUPIED and MAINTENANCE)
        List<Bed> availableBeds = bedRepository.findByStatus(BedStatus.AVAILABLE);

        List<BedRecommendationResponse> recommendations = new ArrayList<>();

        for (Bed bed : availableBeds) {
            Room room = bed.getRoom();
            if (room == null || room.getRoomType() == null) {
                continue;
            }

            // Must match the requested care level / room type (case-insensitive)
            if (!room.getRoomType().equalsIgnoreCase(targetRoomType)) {
                continue;
            }

            String currentWard = room.getWardDepartment() != null ? room.getWardDepartment().trim() : "";

            if (!targetWard.isEmpty() && currentWard.equalsIgnoreCase(targetWard)) {
                // Tier 1: Exact match for both roomType and wardDepartment (Score: 100)
                recommendations.add(new BedRecommendationResponse(
                        bed.getId(),
                        bed.getBedNumber(),
                        room.getId(),
                        room.getRoomNumber(),
                        room.getRoomType(),
                        room.getWardDepartment(),
                        100,
                        "Exact match: requested room type '" + room.getRoomType() + "' in " + room.getWardDepartment() + "."
                ));
            } else if (isClinicallySuitable(targetWard, currentWard)) {
                // Tier 2: Same roomType in a clinically suitable alternate ward (Score: 75)
                String reason = targetWard.isEmpty()
                        ? "Available bed matching requested room type '" + room.getRoomType() + "' in " + room.getWardDepartment() + "."
                        : "Clinical match: requested room type '" + room.getRoomType() + "' available in alternate suitable ward (" + room.getWardDepartment() + ").";

                recommendations.add(new BedRecommendationResponse(
                        bed.getId(),
                        bed.getBedNumber(),
                        room.getId(),
                        room.getRoomNumber(),
                        room.getRoomType(),
                        room.getWardDepartment(),
                        75,
                        reason
                ));
            }
        }

        // Rank Tier 1 (100) first, then Tier 2 (75), ordered by room and bed number
        recommendations.sort(
                Comparator.comparing(BedRecommendationResponse::getMatchScore, Comparator.reverseOrder())
                        .thenComparing(BedRecommendationResponse::getRoomNumber, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(BedRecommendationResponse::getBedNumber, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        return recommendations;
    }

    /**
     * Determines whether an alternate ward is clinically suitable for a patient requiring care in targetWard.
     * Prevents adult patients from being placed into restricted specialized units (e.g. Pediatrics, Maternity, NICU)
     * and vice versa.
     */
    private boolean isClinicallySuitable(String targetWard, String candidateWard) {
        if (candidateWard == null || candidateWard.trim().isEmpty()) {
            return false;
        }

        String targetLower = targetWard != null ? targetWard.toLowerCase().trim() : "";
        String candidateLower = candidateWard.toLowerCase().trim();

        boolean targetIsRestricted = isRestrictedWard(targetLower);
        boolean candidateIsRestricted = isRestrictedWard(candidateLower);

        // If target ward is restricted (e.g. Pediatrics), candidate must also be restricted to the same care group
        if (targetIsRestricted) {
            return candidateLower.contains(targetLower) || targetLower.contains(candidateLower);
        }

        // If target is general/adult, candidate must not be a specialized restricted ward
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


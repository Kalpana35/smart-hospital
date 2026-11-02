package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.CleaningStatisticsResponse;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedCleaningLog;
import com.hospital.smart_hospital.service.CleaningService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cleaning")
public class CleaningController {

    private final CleaningService cleaningService;

    public CleaningController(CleaningService cleaningService) {
        this.cleaningService = cleaningService;
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<BedCleaningLog>>> getCleaningQueue() {
        List<BedCleaningLog> queue = cleaningService.getCleaningQueue();
        return ResponseEntity.ok(ApiResponse.ok("Cleaning queue retrieved successfully", queue));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<CleaningStatisticsResponse>> getCleaningStatistics() {
        CleaningStatisticsResponse stats = cleaningService.getCleaningStatistics();
        return ResponseEntity.ok(ApiResponse.ok("Cleaning turnaround statistics retrieved successfully", stats));
    }

    @PatchMapping("/{bedId}/assign")
    public ResponseEntity<ApiResponse<BedCleaningLog>> assignCleaner(
            @PathVariable Long bedId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String cleanerName
    ) {
        String finalCleanerName = cleanerName;
        if ((finalCleanerName == null || finalCleanerName.trim().isEmpty()) && body != null) {
            finalCleanerName = body.get("cleanerName");
        }
        BedCleaningLog log = cleaningService.assignCleaner(bedId, finalCleanerName);
        return ResponseEntity.ok(ApiResponse.ok("Cleaner assigned successfully to Bed ID " + bedId, log));
    }

    @PatchMapping("/{bedId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeCleaning(
            @PathVariable Long bedId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String cleanerName,
            @RequestParam(required = false) String notes
    ) {
        String finalCleanerName = cleanerName;
        if ((finalCleanerName == null || finalCleanerName.trim().isEmpty()) && body != null) {
            finalCleanerName = body.get("cleanerName");
        }
        String finalNotes = notes;
        if ((finalNotes == null || finalNotes.trim().isEmpty()) && body != null) {
            finalNotes = body.get("notes");
        }
        Bed bed = cleaningService.completeCleaning(bedId, finalCleanerName, finalNotes);
        return ResponseEntity.ok(ApiResponse.ok("Cleaning completed and Bed '" + bed.getBedNumber() + "' is now AVAILABLE for patient admission",
                Map.of("bedId", bed.getId(), "bedNumber", bed.getBedNumber(), "status", bed.getStatus().name())));
    }
}

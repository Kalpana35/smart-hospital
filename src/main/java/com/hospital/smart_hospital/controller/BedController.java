package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.dto.BedResponse;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.service.BedService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beds")
public class BedController {

    private final BedService bedService;

    public BedController(BedService bedService) {
        this.bedService = bedService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BedResponse>> createBed(@RequestBody BedRequest request) {
        BedResponse created = bedService.createBed(request);
        return new ResponseEntity<>(
                ApiResponse.ok("Bed created successfully", created),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BedResponse>>> getBeds(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) BedStatus status
    ) {
        List<BedResponse> beds = bedService.getBeds(roomId, status);
        return ResponseEntity.ok(ApiResponse.ok("Beds retrieved successfully", beds));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BedResponse>> getBedById(@PathVariable Long id) {
        BedResponse bed = bedService.getBedById(id);
        return ResponseEntity.ok(ApiResponse.ok("Bed retrieved successfully", bed));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BedResponse>> updateBed(
            @PathVariable Long id,
            @RequestBody BedRequest request
    ) {
        BedResponse updated = bedService.updateBed(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Bed updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BedResponse>> updateBedStatus(
            @PathVariable Long id,
            @RequestParam BedStatus status
    ) {
        BedResponse updated = bedService.updateBedStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Bed status updated successfully", updated));
    }
}


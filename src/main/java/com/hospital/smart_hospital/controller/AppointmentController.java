package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.AppointmentBookingRequest;
import com.hospital.smart_hospital.dto.AppointmentResponse;
import com.hospital.smart_hospital.dto.AppointmentStatusUpdateRequest;
import com.hospital.smart_hospital.dto.DoctorOptionDto;
import com.hospital.smart_hospital.service.AppointmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            @RequestBody AppointmentBookingRequest request,
            Authentication authentication
    ) {
        AppointmentResponse appointment = appointmentService.bookAppointment(request, authentication);
        return new ResponseEntity<>(
                ApiResponse.ok("Appointment booked successfully", appointment),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyAppointments(Authentication authentication) {
        List<AppointmentResponse> list = appointmentService.getMyAppointments(authentication);
        return ResponseEntity.ok(ApiResponse.ok("Appointments retrieved successfully", list));
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<DoctorOptionDto>>> getAvailableDoctors() {
        List<DoctorOptionDto> doctors = appointmentService.getAvailableDoctors();
        return ResponseEntity.ok(ApiResponse.ok("Doctors retrieved successfully", doctors));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAllAppointments() {
        List<AppointmentResponse> list = appointmentService.getAllAppointments();
        return ResponseEntity.ok(ApiResponse.ok("All appointments retrieved successfully", list));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getDoctorAppointments(@PathVariable Long doctorId) {
        List<AppointmentResponse> list = appointmentService.getDoctorAppointments(doctorId);
        return ResponseEntity.ok(ApiResponse.ok("Doctor appointments retrieved successfully", list));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) AppointmentStatusUpdateRequest request,
            Authentication authentication
    ) {
        String reason = request != null ? request.getReason() : null;
        AppointmentResponse response = appointmentService.cancelAppointment(id, reason, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Appointment cancelled successfully", response));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<AppointmentResponse>> completeAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) AppointmentStatusUpdateRequest request,
            Authentication authentication
    ) {
        String notes = request != null ? request.getNotes() : null;
        AppointmentResponse response = appointmentService.completeAppointment(id, notes, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Appointment marked as completed", response));
    }
}

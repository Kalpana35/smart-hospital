package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.AppointmentBookingRequest;
import com.hospital.smart_hospital.dto.AppointmentResponse;
import com.hospital.smart_hospital.dto.DoctorOptionDto;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface AppointmentService {

    AppointmentResponse bookAppointment(AppointmentBookingRequest request, Authentication authentication);

    List<AppointmentResponse> getMyAppointments(Authentication authentication);

    List<AppointmentResponse> getDoctorAppointments(Long doctorId);

    List<AppointmentResponse> getAllAppointments();

    AppointmentResponse cancelAppointment(Long id, String reason, Authentication authentication);

    AppointmentResponse completeAppointment(Long id, String notes, Authentication authentication);

    List<DoctorOptionDto> getAvailableDoctors();
}

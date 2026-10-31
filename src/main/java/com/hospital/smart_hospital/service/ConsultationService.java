package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.ConsultationRequest;
import com.hospital.smart_hospital.dto.ConsultationResponse;
import java.util.List;

public interface ConsultationService {

    ConsultationResponse recordConsultation(ConsultationRequest request, String doctorUsername);

    List<ConsultationResponse> getConsultationsForPatient(Long patientId, String requestingUsername, boolean isStaff);

    List<ConsultationResponse> getMyConsultations(String username, String roleName);

    List<ConsultationResponse> getAllConsultations();
}

package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.PatientResponse;
import com.hospital.smart_hospital.model.PatientStatus;
import java.util.List;

public interface PatientService {

    PatientResponse admitPatient(PatientAdmissionRequest request);

    PatientResponse dischargePatient(Long patientId);

    PatientResponse getPatientById(Long id);

    PatientResponse getPatientByCode(String patientCode);

    List<PatientResponse> getPatients(PatientStatus status);

    PatientResponse getMyPatientRecord(String username);

    PatientResponse createPatientPortalAccount(Long patientId, String username, String password);
}


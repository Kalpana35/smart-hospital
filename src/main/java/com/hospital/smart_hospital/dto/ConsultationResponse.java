package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.ConsultationStatus;
import java.time.LocalDateTime;

public class ConsultationResponse {

    private Long id;
    private Long patientId;
    private String patientCode;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long appointmentId;
    private LocalDateTime consultationDateTime;
    private String diagnosis;
    private String treatment;
    private String prescription;
    private String notes;
    private ConsultationStatus status;
    private LocalDateTime createdAt;

    public ConsultationResponse() {
    }

    public ConsultationResponse(
            Long id,
            Long patientId,
            String patientCode,
            String patientName,
            Long doctorId,
            String doctorName,
            Long appointmentId,
            LocalDateTime consultationDateTime,
            String diagnosis,
            String treatment,
            String prescription,
            String notes,
            ConsultationStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.patientId = patientId;
        this.patientCode = patientCode;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.appointmentId = appointmentId;
        this.consultationDateTime = consultationDateTime;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.prescription = prescription;
        this.notes = notes;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDateTime getConsultationDateTime() {
        return consultationDateTime;
    }

    public void setConsultationDateTime(LocalDateTime consultationDateTime) {
        this.consultationDateTime = consultationDateTime;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ConsultationStatus getStatus() {
        return status;
    }

    public void setStatus(ConsultationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

package com.hospital.smart_hospital.dto;

import java.time.LocalDateTime;

public class AppointmentBookingRequest {

    private Long patientId;
    private Long doctorId;
    private LocalDateTime appointmentDateTime;
    private String department;
    private String reason;

    public AppointmentBookingRequest() {
    }

    public AppointmentBookingRequest(
            Long patientId,
            Long doctorId,
            LocalDateTime appointmentDateTime,
            String department,
            String reason
    ) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentDateTime = appointmentDateTime;
        this.department = department;
        this.reason = reason;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

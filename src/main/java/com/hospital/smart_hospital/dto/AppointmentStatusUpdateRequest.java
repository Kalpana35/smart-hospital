package com.hospital.smart_hospital.dto;

public class AppointmentStatusUpdateRequest {

    private String notes;
    private String reason;

    public AppointmentStatusUpdateRequest() {
    }

    public AppointmentStatusUpdateRequest(String notes, String reason) {
        this.notes = notes;
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

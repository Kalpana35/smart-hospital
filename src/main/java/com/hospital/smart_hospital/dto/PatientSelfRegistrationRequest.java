package com.hospital.smart_hospital.dto;

public class PatientSelfRegistrationRequest {

    private String username;
    private String password;
    private String fullName;
    private String patientCode;

    public PatientSelfRegistrationRequest() {
    }

    public PatientSelfRegistrationRequest(String username, String password, String fullName, String patientCode) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.patientCode = patientCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }
}

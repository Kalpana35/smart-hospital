package com.hospital.smart_hospital.dto;

public class DoctorRegistrationRequest {

    private String fullName;
    private String username;
    private String email;
    private String specialization;
    private String qualifications;
    private String medicalRegistrationNumber;
    private String password;

    public DoctorRegistrationRequest() {
    }

    public DoctorRegistrationRequest(String fullName, String username, String email, String specialization,
                                    String qualifications, String medicalRegistrationNumber, String password) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.specialization = specialization;
        this.qualifications = qualifications;
        this.medicalRegistrationNumber = medicalRegistrationNumber;
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public String getMedicalRegistrationNumber() {
        return medicalRegistrationNumber;
    }

    public void setMedicalRegistrationNumber(String medicalRegistrationNumber) {
        this.medicalRegistrationNumber = medicalRegistrationNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

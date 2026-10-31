package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.Role;

public class StaffAccountRequest {

    private String fullName;
    private String username;
    private String email;
    private Role role; // ROLE_DOCTOR or ROLE_RECEPTIONIST
    private String temporaryPassword;
    private String specialization;
    private String qualifications;
    private String medicalRegistrationNumber;

    public StaffAccountRequest() {
    }

    public StaffAccountRequest(String fullName, String username, String email, Role role, String temporaryPassword, String specialization, String qualifications, String medicalRegistrationNumber) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.role = role;
        this.temporaryPassword = temporaryPassword;
        this.specialization = specialization;
        this.qualifications = qualifications;
        this.medicalRegistrationNumber = medicalRegistrationNumber;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
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
}

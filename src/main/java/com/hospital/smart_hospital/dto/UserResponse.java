package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.DoctorApprovalStatus;
import com.hospital.smart_hospital.model.Role;
import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private String email;
    private String specialization;
    private String qualifications;
    private String medicalRegistrationNumber;
    private DoctorApprovalStatus approvalStatus;
    private boolean passwordChangeRequired;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String fullName, Role role, boolean enabled, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.approvalStatus = DoctorApprovalStatus.APPROVED;
        this.passwordChangeRequired = false;
    }

    public UserResponse(Long id, String username, String fullName, Role role, boolean enabled, LocalDateTime createdAt,
                        String email, String specialization, String qualifications, String medicalRegistrationNumber,
                        DoctorApprovalStatus approvalStatus) {
        this(id, username, fullName, role, enabled, createdAt, email, specialization, qualifications, medicalRegistrationNumber, approvalStatus, false);
    }

    public UserResponse(Long id, String username, String fullName, Role role, boolean enabled, LocalDateTime createdAt,
                        String email, String specialization, String qualifications, String medicalRegistrationNumber,
                        DoctorApprovalStatus approvalStatus, boolean passwordChangeRequired) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.email = email;
        this.specialization = specialization;
        this.qualifications = qualifications;
        this.medicalRegistrationNumber = medicalRegistrationNumber;
        this.approvalStatus = approvalStatus != null ? approvalStatus : DoctorApprovalStatus.APPROVED;
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public DoctorApprovalStatus getApprovalStatus() {
        return approvalStatus != null ? approvalStatus : DoctorApprovalStatus.APPROVED;
    }

    public void setApprovalStatus(DoctorApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }
}


package com.hospital.smart_hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    private DoctorApprovalStatus approvalStatus = DoctorApprovalStatus.APPROVED;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "specialization", length = 100)
    private String specialization;

    @Column(name = "qualifications", length = 200)
    private String qualifications;

    @Column(name = "medical_registration_number", length = 100)
    private String medicalRegistrationNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {
    }

    public User(String username, String password, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.enabled = true;
        this.approvalStatus = DoctorApprovalStatus.APPROVED;
        this.createdAt = LocalDateTime.now();
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

    public DoctorApprovalStatus getApprovalStatus() {
        return approvalStatus != null ? approvalStatus : DoctorApprovalStatus.APPROVED;
    }

    public void setApprovalStatus(DoctorApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


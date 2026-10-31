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
@Table(name = "bed_allocation_history")
public class BedAllocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bed_id", nullable = false)
    private Long bedId;

    @Column(name = "bed_number", nullable = false, length = 50)
    private String bedNumber;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(name = "ward_department", length = 100)
    private String wardDepartment;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_code", nullable = false, length = 50)
    private String patientCode;

    @Column(name = "patient_name", nullable = false, length = 150)
    private String patientName;

    @Column(name = "allocated_by", nullable = false, length = 100)
    private String allocatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", nullable = false, length = 30)
    private AllocationType allocationType = AllocationType.RECOMMENDED;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "allocated_at", nullable = false)
    private LocalDateTime allocatedAt = LocalDateTime.now();

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public BedAllocationHistory() {
    }

    public BedAllocationHistory(
            Long bedId,
            String bedNumber,
            Long roomId,
            String roomNumber,
            String wardDepartment,
            Long patientId,
            String patientCode,
            String patientName,
            String allocatedBy,
            AllocationType allocationType,
            String overrideReason
    ) {
        this.bedId = bedId;
        this.bedNumber = bedNumber;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.wardDepartment = wardDepartment;
        this.patientId = patientId;
        this.patientCode = patientCode;
        this.patientName = patientName;
        this.allocatedBy = allocatedBy;
        this.allocationType = allocationType != null ? allocationType : AllocationType.RECOMMENDED;
        this.overrideReason = overrideReason;
        this.allocatedAt = LocalDateTime.now();
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public String getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(String bedNumber) {
        this.bedNumber = bedNumber;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getWardDepartment() {
        return wardDepartment;
    }

    public void setWardDepartment(String wardDepartment) {
        this.wardDepartment = wardDepartment;
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

    public String getAllocatedBy() {
        return allocatedBy;
    }

    public void setAllocatedBy(String allocatedBy) {
        this.allocatedBy = allocatedBy;
    }

    public AllocationType getAllocationType() {
        return allocationType;
    }

    public void setAllocationType(AllocationType allocationType) {
        this.allocationType = allocationType;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

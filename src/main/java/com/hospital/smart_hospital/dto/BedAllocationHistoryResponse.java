package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.AllocationType;
import java.time.LocalDateTime;

public class BedAllocationHistoryResponse {

    private Long id;
    private Long bedId;
    private String bedNumber;
    private Long roomId;
    private String roomNumber;
    private String wardDepartment;
    private Long patientId;
    private String patientCode;
    private String patientName;
    private String allocatedBy;
    private AllocationType allocationType;
    private String overrideReason;
    private LocalDateTime allocatedAt;
    private LocalDateTime releasedAt;
    private boolean active;

    public BedAllocationHistoryResponse() {
    }

    public BedAllocationHistoryResponse(
            Long id,
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
            String overrideReason,
            LocalDateTime allocatedAt,
            LocalDateTime releasedAt,
            boolean active
    ) {
        this.id = id;
        this.bedId = bedId;
        this.bedNumber = bedNumber;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.wardDepartment = wardDepartment;
        this.patientId = patientId;
        this.patientCode = patientCode;
        this.patientName = patientName;
        this.allocatedBy = allocatedBy;
        this.allocationType = allocationType;
        this.overrideReason = overrideReason;
        this.allocatedAt = allocatedAt;
        this.releasedAt = releasedAt;
        this.active = active;
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

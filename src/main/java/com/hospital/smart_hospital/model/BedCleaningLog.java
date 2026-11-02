package com.hospital.smart_hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "bed_cleaning_logs")
public class BedCleaningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bed_id", nullable = false)
    private Long bedId;

    @Column(name = "bed_number", nullable = false, length = 50)
    private String bedNumber;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_number", length = 50)
    private String roomNumber;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(name = "dirty_at", nullable = false)
    private LocalDateTime dirtyAt = LocalDateTime.now();

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "cleaned_at")
    private LocalDateTime cleanedAt;

    @Column(name = "duration_minutes")
    private Long durationMinutes;

    @Column(name = "priority", nullable = false)
    private Integer priority = 3; // 1 = Critical, 2 = High, 3 = Medium, 4 = Low

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DIRTY"; // DIRTY, IN_PROGRESS, COMPLETED

    @Column(name = "notes", length = 255)
    private String notes;

    public BedCleaningLog() {
    }

    public BedCleaningLog(Long bedId, String bedNumber, Long roomId, String roomNumber, String departmentName, Integer priority) {
        this.bedId = bedId;
        this.bedNumber = bedNumber;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.departmentName = departmentName;
        this.priority = priority != null ? priority : 3;
        this.dirtyAt = LocalDateTime.now();
        this.status = "DIRTY";
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public LocalDateTime getDirtyAt() {
        return dirtyAt;
    }

    public void setDirtyAt(LocalDateTime dirtyAt) {
        this.dirtyAt = dirtyAt;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getCleanedAt() {
        return cleanedAt;
    }

    public void setCleanedAt(LocalDateTime cleanedAt) {
        this.cleanedAt = cleanedAt;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

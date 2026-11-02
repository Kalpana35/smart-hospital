package com.hospital.smart_hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "beds",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_room_bed_number",
            columnNames = {"room_id", "bed_number"}
        )
    }
)
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bed_number", nullable = false, length = 50)
    private String bedNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BedStatus status = BedStatus.AVAILABLE;

    @Column(name = "daily_charge")
    private Double dailyCharge;

    @Column(name = "features", length = 150)
    private String features = "STANDARD";

    @Column(name = "cleaning_priority")
    private Integer cleaningPriority; // 1 = Critical, 2 = High, 3 = Medium, 4 = Low

    @Column(name = "cleaning_assigned_to", length = 100)
    private String cleaningAssignedTo;

    @Column(name = "cleaning_assigned_at")
    private LocalDateTime cleaningAssignedAt;

    @Column(name = "dirty_since")
    private LocalDateTime dirtySince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    public Bed() {
    }

    public Bed(String bedNumber, Room room, BedStatus status) {
        this.bedNumber = bedNumber;
        this.room = room;
        this.status = status != null ? status : BedStatus.AVAILABLE;
    }

    public Bed(String bedNumber, Room room, BedStatus status, Double dailyCharge, String features) {
        this.bedNumber = bedNumber;
        this.room = room;
        this.status = status != null ? status : BedStatus.AVAILABLE;
        this.dailyCharge = dailyCharge;
        this.features = features != null ? features : "STANDARD";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(String bedNumber) {
        this.bedNumber = bedNumber;
    }

    public BedStatus getStatus() {
        return status;
    }

    public void setStatus(BedStatus status) {
        this.status = status;
    }

    public Double getDailyCharge() {
        if (dailyCharge != null) {
            return dailyCharge;
        }
        return room != null ? room.getDailyRate() : 250.0;
    }

    public void setDailyCharge(Double dailyCharge) {
        this.dailyCharge = dailyCharge;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public Integer getCleaningPriority() {
        return cleaningPriority;
    }

    public void setCleaningPriority(Integer cleaningPriority) {
        this.cleaningPriority = cleaningPriority;
    }

    public String getCleaningAssignedTo() {
        return cleaningAssignedTo;
    }

    public void setCleaningAssignedTo(String cleaningAssignedTo) {
        this.cleaningAssignedTo = cleaningAssignedTo;
    }

    public LocalDateTime getCleaningAssignedAt() {
        return cleaningAssignedAt;
    }

    public void setCleaningAssignedAt(LocalDateTime cleaningAssignedAt) {
        this.cleaningAssignedAt = cleaningAssignedAt;
    }

    public LocalDateTime getDirtySince() {
        return dirtySince;
    }

    public void setDirtySince(LocalDateTime dirtySince) {
        this.dirtySince = dirtySince;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}

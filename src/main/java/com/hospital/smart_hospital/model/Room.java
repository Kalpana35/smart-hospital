package com.hospital.smart_hospital.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true, length = 50)
    private String roomNumber;

    @Column(name = "room_type", nullable = false, length = 100)
    private String roomType;

    @Column(name = "ward_department", nullable = false, length = 150)
    private String wardDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "daily_rate")
    private Double dailyRate = 350.0;

    @Column(name = "has_icu_monitoring")
    private boolean hasIcuMonitoring = false;

    @Column(name = "has_isolation")
    private boolean hasIsolation = false;

    @JsonIgnore
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bed> beds = new ArrayList<>();

    public Room() {
    }

    public Room(String roomNumber, String roomType, String wardDepartment) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
        this.dailyRate = defaultRateForType(roomType);
    }

    public Room(String roomNumber, String roomType, String wardDepartment, Department department, Double dailyRate, boolean hasIcuMonitoring, boolean hasIsolation) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
        this.department = department;
        this.dailyRate = dailyRate != null ? dailyRate : defaultRateForType(roomType);
        this.hasIcuMonitoring = hasIcuMonitoring;
        this.hasIsolation = hasIsolation;
    }

    private static Double defaultRateForType(String roomType) {
        if (roomType == null) return 350.0;
        String t = roomType.toUpperCase();
        if (t.contains("ICU")) return 1200.0;
        if (t.contains("ISO") || t.contains("ISOLATION")) return 850.0;
        if (t.contains("SURG")) return 600.0;
        if (t.contains("EMERG")) return 500.0;
        return 250.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getWardDepartment() {
        return wardDepartment;
    }

    public void setWardDepartment(String wardDepartment) {
        this.wardDepartment = wardDepartment;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Double getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(Double dailyRate) {
        this.dailyRate = dailyRate;
    }

    public boolean isHasIcuMonitoring() {
        return hasIcuMonitoring;
    }

    public void setHasIcuMonitoring(boolean hasIcuMonitoring) {
        this.hasIcuMonitoring = hasIcuMonitoring;
    }

    public boolean isHasIsolation() {
        return hasIsolation;
    }

    public void setHasIsolation(boolean hasIsolation) {
        this.hasIsolation = hasIsolation;
    }

    public List<Bed> getBeds() {
        return beds;
    }

    public void setBeds(List<Bed> beds) {
        this.beds = beds;
    }
}

package com.hospital.smart_hospital.dto;

public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private String headOfDepartment;
    private int roomCount;
    private int totalBeds;
    private int availableBeds;
    private int occupiedBeds;
    private int dirtyBeds;

    public DepartmentResponse() {
    }

    public DepartmentResponse(
            Long id,
            String name,
            String code,
            String description,
            String headOfDepartment,
            int roomCount,
            int totalBeds,
            int availableBeds,
            int occupiedBeds,
            int dirtyBeds
    ) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.headOfDepartment = headOfDepartment;
        this.roomCount = roomCount;
        this.totalBeds = totalBeds;
        this.availableBeds = availableBeds;
        this.occupiedBeds = occupiedBeds;
        this.dirtyBeds = dirtyBeds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHeadOfDepartment() {
        return headOfDepartment;
    }

    public void setHeadOfDepartment(String headOfDepartment) {
        this.headOfDepartment = headOfDepartment;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(int roomCount) {
        this.roomCount = roomCount;
    }

    public int getTotalBeds() {
        return totalBeds;
    }

    public void setTotalBeds(int totalBeds) {
        this.totalBeds = totalBeds;
    }

    public int getAvailableBeds() {
        return availableBeds;
    }

    public void setAvailableBeds(int availableBeds) {
        this.availableBeds = availableBeds;
    }

    public int getOccupiedBeds() {
        return occupiedBeds;
    }

    public void setOccupiedBeds(int occupiedBeds) {
        this.occupiedBeds = occupiedBeds;
    }

    public int getDirtyBeds() {
        return dirtyBeds;
    }

    public void setDirtyBeds(int dirtyBeds) {
        this.dirtyBeds = dirtyBeds;
    }
}

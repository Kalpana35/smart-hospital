package com.hospital.smart_hospital.dto;

public class RoomTypeAnalyticsDto {

    private String roomType;
    private int totalBeds;
    private int availableBeds;
    private int occupiedBeds;
    private int maintenanceBeds;
    private double occupancyRate;

    public RoomTypeAnalyticsDto() {
    }

    public RoomTypeAnalyticsDto(
            String roomType,
            int totalBeds,
            int availableBeds,
            int occupiedBeds,
            int maintenanceBeds,
            double occupancyRate
    ) {
        this.roomType = roomType;
        this.totalBeds = totalBeds;
        this.availableBeds = availableBeds;
        this.occupiedBeds = occupiedBeds;
        this.maintenanceBeds = maintenanceBeds;
        this.occupancyRate = occupancyRate;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
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

    public int getMaintenanceBeds() {
        return maintenanceBeds;
    }

    public void setMaintenanceBeds(int maintenanceBeds) {
        this.maintenanceBeds = maintenanceBeds;
    }

    public double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }
}

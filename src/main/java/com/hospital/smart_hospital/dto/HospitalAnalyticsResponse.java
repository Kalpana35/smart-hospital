package com.hospital.smart_hospital.dto;

import java.util.List;

public class HospitalAnalyticsResponse {

    private int totalRooms;
    private int totalBeds;
    private int availableBeds;
    private int occupiedBeds;
    private int maintenanceBeds;
    private int activePatients;
    private int dischargedPatients;
    private double overallOccupancyRate;
    private double averageLengthOfStayHours;
    private List<WardAnalyticsDto> wardBreakdown;
    private List<RoomTypeAnalyticsDto> roomTypeBreakdown;

    public HospitalAnalyticsResponse() {
    }

    public HospitalAnalyticsResponse(
            int totalRooms,
            int totalBeds,
            int availableBeds,
            int occupiedBeds,
            int maintenanceBeds,
            int activePatients,
            int dischargedPatients,
            double overallOccupancyRate,
            double averageLengthOfStayHours,
            List<WardAnalyticsDto> wardBreakdown,
            List<RoomTypeAnalyticsDto> roomTypeBreakdown
    ) {
        this.totalRooms = totalRooms;
        this.totalBeds = totalBeds;
        this.availableBeds = availableBeds;
        this.occupiedBeds = occupiedBeds;
        this.maintenanceBeds = maintenanceBeds;
        this.activePatients = activePatients;
        this.dischargedPatients = dischargedPatients;
        this.overallOccupancyRate = overallOccupancyRate;
        this.averageLengthOfStayHours = averageLengthOfStayHours;
        this.wardBreakdown = wardBreakdown;
        this.roomTypeBreakdown = roomTypeBreakdown;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
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

    public int getActivePatients() {
        return activePatients;
    }

    public void setActivePatients(int activePatients) {
        this.activePatients = activePatients;
    }

    public int getDischargedPatients() {
        return dischargedPatients;
    }

    public void setDischargedPatients(int dischargedPatients) {
        this.dischargedPatients = dischargedPatients;
    }

    public double getOverallOccupancyRate() {
        return overallOccupancyRate;
    }

    public void setOverallOccupancyRate(double overallOccupancyRate) {
        this.overallOccupancyRate = overallOccupancyRate;
    }

    public double getAverageLengthOfStayHours() {
        return averageLengthOfStayHours;
    }

    public void setAverageLengthOfStayHours(double averageLengthOfStayHours) {
        this.averageLengthOfStayHours = averageLengthOfStayHours;
    }

    public List<WardAnalyticsDto> getWardBreakdown() {
        return wardBreakdown;
    }

    public void setWardBreakdown(List<WardAnalyticsDto> wardBreakdown) {
        this.wardBreakdown = wardBreakdown;
    }

    public List<RoomTypeAnalyticsDto> getRoomTypeBreakdown() {
        return roomTypeBreakdown;
    }

    public void setRoomTypeBreakdown(List<RoomTypeAnalyticsDto> roomTypeBreakdown) {
        this.roomTypeBreakdown = roomTypeBreakdown;
    }
}

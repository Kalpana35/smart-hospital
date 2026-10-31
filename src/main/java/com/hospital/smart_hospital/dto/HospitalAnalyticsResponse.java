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
    private int totalDoctors;
    private int totalReceptionists;
    private int totalAppointments;
    private int scheduledAppointments;
    private int completedAppointments;
    private int cancelledAppointments;
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
        this(totalRooms, totalBeds, availableBeds, occupiedBeds, maintenanceBeds, activePatients, dischargedPatients,
                overallOccupancyRate, averageLengthOfStayHours, 0, 0, 0, 0, 0, 0, wardBreakdown, roomTypeBreakdown);
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
            int totalDoctors,
            int totalReceptionists,
            int totalAppointments,
            int scheduledAppointments,
            int completedAppointments,
            int cancelledAppointments,
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
        this.totalDoctors = totalDoctors;
        this.totalReceptionists = totalReceptionists;
        this.totalAppointments = totalAppointments;
        this.scheduledAppointments = scheduledAppointments;
        this.completedAppointments = completedAppointments;
        this.cancelledAppointments = cancelledAppointments;
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

    public int getTotalDoctors() {
        return totalDoctors;
    }

    public void setTotalDoctors(int totalDoctors) {
        this.totalDoctors = totalDoctors;
    }

    public int getTotalReceptionists() {
        return totalReceptionists;
    }

    public void setTotalReceptionists(int totalReceptionists) {
        this.totalReceptionists = totalReceptionists;
    }

    public int getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(int totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public int getScheduledAppointments() {
        return scheduledAppointments;
    }

    public void setScheduledAppointments(int scheduledAppointments) {
        this.scheduledAppointments = scheduledAppointments;
    }

    public int getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(int completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public int getCancelledAppointments() {
        return cancelledAppointments;
    }

    public void setCancelledAppointments(int cancelledAppointments) {
        this.cancelledAppointments = cancelledAppointments;
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

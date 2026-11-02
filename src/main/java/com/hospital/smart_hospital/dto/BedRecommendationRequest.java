package com.hospital.smart_hospital.dto;

public class BedRecommendationRequest {

    private String roomType;
    private String wardDepartment;
    private String acuityLevel;
    private Boolean requiresIcuMonitoring;
    private Boolean requiresIsolation;
    private Double preferredMaxDailyBudget;
    private Boolean emergency;

    public BedRecommendationRequest() {
    }

    public BedRecommendationRequest(String roomType, String wardDepartment) {
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
    }

    public BedRecommendationRequest(
            String roomType,
            String wardDepartment,
            String acuityLevel,
            Boolean requiresIcuMonitoring,
            Boolean requiresIsolation,
            Double preferredMaxDailyBudget,
            Boolean emergency
    ) {
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
        this.acuityLevel = acuityLevel;
        this.requiresIcuMonitoring = requiresIcuMonitoring;
        this.requiresIsolation = requiresIsolation;
        this.preferredMaxDailyBudget = preferredMaxDailyBudget;
        this.emergency = emergency;
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

    public String getAcuityLevel() {
        return acuityLevel;
    }

    public void setAcuityLevel(String acuityLevel) {
        this.acuityLevel = acuityLevel;
    }

    public Boolean getRequiresIcuMonitoring() {
        return requiresIcuMonitoring;
    }

    public void setRequiresIcuMonitoring(Boolean requiresIcuMonitoring) {
        this.requiresIcuMonitoring = requiresIcuMonitoring;
    }

    public Boolean getRequiresIsolation() {
        return requiresIsolation;
    }

    public void setRequiresIsolation(Boolean requiresIsolation) {
        this.requiresIsolation = requiresIsolation;
    }

    public Double getPreferredMaxDailyBudget() {
        return preferredMaxDailyBudget;
    }

    public void setPreferredMaxDailyBudget(Double preferredMaxDailyBudget) {
        this.preferredMaxDailyBudget = preferredMaxDailyBudget;
    }

    public Boolean getEmergency() {
        return emergency;
    }

    public void setEmergency(Boolean emergency) {
        this.emergency = emergency;
    }
}

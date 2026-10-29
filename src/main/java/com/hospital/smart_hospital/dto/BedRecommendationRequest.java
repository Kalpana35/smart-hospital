package com.hospital.smart_hospital.dto;

public class BedRecommendationRequest {

    private String roomType;
    private String wardDepartment;

    public BedRecommendationRequest() {
    }

    public BedRecommendationRequest(String roomType, String wardDepartment) {
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
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
}


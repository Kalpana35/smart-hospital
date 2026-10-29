package com.hospital.smart_hospital.dto;

public class BedRecommendationResponse {

    private Long bedId;
    private String bedNumber;
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private String wardDepartment;
    private Integer matchScore;
    private String matchReason;

    public BedRecommendationResponse() {
    }

    public BedRecommendationResponse(
            Long bedId,
            String bedNumber,
            Long roomId,
            String roomNumber,
            String roomType,
            String wardDepartment,
            Integer matchScore,
            String matchReason
    ) {
        this.bedId = bedId;
        this.bedNumber = bedNumber;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
        this.matchScore = matchScore;
        this.matchReason = matchReason;
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

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }
}


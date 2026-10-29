package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.BedStatus;

public class BedRequest {

    private String bedNumber;
    private Long roomId;
    private BedStatus status;

    public BedRequest() {
    }

    public BedRequest(String bedNumber, Long roomId, BedStatus status) {
        this.bedNumber = bedNumber;
        this.roomId = roomId;
        this.status = status;
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

    public BedStatus getStatus() {
        return status;
    }

    public void setStatus(BedStatus status) {
        this.status = status;
    }
}


package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.BedStatus;

public class BedResponse {

    private Long id;
    private String bedNumber;
    private BedStatus status;
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private String wardDepartment;

    public BedResponse() {
    }

    public BedResponse(Long id, String bedNumber, BedStatus status, Long roomId, String roomNumber, String wardDepartment) {
        this(id, bedNumber, status, roomId, roomNumber, null, wardDepartment);
    }

    public BedResponse(Long id, String bedNumber, BedStatus status, Long roomId, String roomNumber, String roomType, String wardDepartment) {
        this.id = id;
        this.bedNumber = bedNumber;
        this.status = status;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
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
}


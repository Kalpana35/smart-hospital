package com.hospital.smart_hospital.dto;

public class RoomRequest {

    private String roomNumber;
    private String roomType;
    private String wardDepartment;

    public RoomRequest() {
    }

    public RoomRequest(String roomNumber, String roomType, String wardDepartment) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
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


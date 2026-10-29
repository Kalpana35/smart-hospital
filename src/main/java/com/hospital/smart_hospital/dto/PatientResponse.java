package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.PatientStatus;
import java.time.LocalDateTime;

public class PatientResponse {

    private Long id;
    private String patientCode;
    private String name;
    private Integer age;
    private String gender;
    private String diagnosis;
    private PatientStatus status;
    private LocalDateTime admissionDate;
    private LocalDateTime dischargeDate;

    // Bed and room details
    private Long bedId;
    private String bedNumber;
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private String wardDepartment;

    // Linked user account details
    private Long userId;
    private String username;

    public PatientResponse() {
    }

    public PatientResponse(
            Long id,
            String patientCode,
            String name,
            Integer age,
            String gender,
            String diagnosis,
            PatientStatus status,
            LocalDateTime admissionDate,
            LocalDateTime dischargeDate,
            Long bedId,
            String bedNumber,
            Long roomId,
            String roomNumber,
            String wardDepartment
    ) {
        this(id, patientCode, name, age, gender, diagnosis, status, admissionDate, dischargeDate, bedId, bedNumber, roomId, roomNumber, null, wardDepartment);
    }

    public PatientResponse(
            Long id,
            String patientCode,
            String name,
            Integer age,
            String gender,
            String diagnosis,
            PatientStatus status,
            LocalDateTime admissionDate,
            LocalDateTime dischargeDate,
            Long bedId,
            String bedNumber,
            Long roomId,
            String roomNumber,
            String roomType,
            String wardDepartment
    ) {
        this(id, patientCode, name, age, gender, diagnosis, status, admissionDate, dischargeDate, bedId, bedNumber, roomId, roomNumber, roomType, wardDepartment, null, null);
    }

    public PatientResponse(
            Long id,
            String patientCode,
            String name,
            Integer age,
            String gender,
            String diagnosis,
            PatientStatus status,
            LocalDateTime admissionDate,
            LocalDateTime dischargeDate,
            Long bedId,
            String bedNumber,
            Long roomId,
            String roomNumber,
            String roomType,
            String wardDepartment,
            Long userId,
            String username
    ) {
        this.id = id;
        this.patientCode = patientCode;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.diagnosis = diagnosis;
        this.status = status;
        this.admissionDate = admissionDate;
        this.dischargeDate = dischargeDate;
        this.bedId = bedId;
        this.bedNumber = bedNumber;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.wardDepartment = wardDepartment;
        this.userId = userId;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }

    public LocalDateTime getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDateTime admissionDate) {
        this.admissionDate = admissionDate;
    }

    public LocalDateTime getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(LocalDateTime dischargeDate) {
        this.dischargeDate = dischargeDate;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}


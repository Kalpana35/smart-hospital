package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.AllocationType;

public class PatientAdmissionRequest {

    private String patientCode;
    private String name;
    private Integer age;
    private String gender;
    private String diagnosis;
    private Long bedId;

    // Optional fields for provisioning portal account during admission
    private Boolean createPortalAccount;
    private String portalUsername;
    private String portalPassword;

    // Allocation tracking & override reason
    private AllocationType allocationType = AllocationType.RECOMMENDED;
    private String overrideReason;
    private java.time.LocalDateTime expectedDischargeDate;

    public PatientAdmissionRequest() {
    }

    public PatientAdmissionRequest(String patientCode, String name, Integer age, String gender, String diagnosis, Long bedId) {
        this(patientCode, name, age, gender, diagnosis, bedId, false, null, null, AllocationType.RECOMMENDED, null);
    }

    public PatientAdmissionRequest(String patientCode, String name, Integer age, String gender, String diagnosis, Long bedId, java.time.LocalDateTime expectedDischargeDate) {
        this(patientCode, name, age, gender, diagnosis, bedId);
        this.expectedDischargeDate = expectedDischargeDate;
    }

    public PatientAdmissionRequest(String patientCode, String name, Integer age, String gender, String diagnosis, Long bedId, Boolean createPortalAccount, String portalUsername, String portalPassword) {
        this(patientCode, name, age, gender, diagnosis, bedId, createPortalAccount, portalUsername, portalPassword, AllocationType.RECOMMENDED, null);
    }

    public PatientAdmissionRequest(String patientCode, String name, Integer age, String gender, String diagnosis, Long bedId, Boolean createPortalAccount, String portalUsername, String portalPassword, AllocationType allocationType, String overrideReason) {
        this.patientCode = patientCode;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.diagnosis = diagnosis;
        this.bedId = bedId;
        this.createPortalAccount = createPortalAccount;
        this.portalUsername = portalUsername;
        this.portalPassword = portalPassword;
        this.allocationType = allocationType != null ? allocationType : AllocationType.RECOMMENDED;
        this.overrideReason = overrideReason;
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

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public Boolean getCreatePortalAccount() {
        return createPortalAccount;
    }

    public void setCreatePortalAccount(Boolean createPortalAccount) {
        this.createPortalAccount = createPortalAccount;
    }

    public String getPortalUsername() {
        return portalUsername;
    }

    public void setPortalUsername(String portalUsername) {
        this.portalUsername = portalUsername;
    }

    public String getPortalPassword() {
        return portalPassword;
    }

    public void setPortalPassword(String portalPassword) {
        this.portalPassword = portalPassword;
    }

    public AllocationType getAllocationType() {
        return allocationType;
    }

    public void setAllocationType(AllocationType allocationType) {
        this.allocationType = allocationType;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public java.time.LocalDateTime getExpectedDischargeDate() {
        return expectedDischargeDate;
    }

    public void setExpectedDischargeDate(java.time.LocalDateTime expectedDischargeDate) {
        this.expectedDischargeDate = expectedDischargeDate;
    }
}


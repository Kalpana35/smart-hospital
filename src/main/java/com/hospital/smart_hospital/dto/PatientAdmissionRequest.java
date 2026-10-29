package com.hospital.smart_hospital.dto;

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

    public PatientAdmissionRequest() {
    }

    public PatientAdmissionRequest(String patientCode, String name, Integer age, String gender, String diagnosis, Long bedId) {
        this(patientCode, name, age, gender, diagnosis, bedId, false, null, null);
    }

    public PatientAdmissionRequest(String patientCode, String name, Integer age, String gender, String diagnosis, Long bedId, Boolean createPortalAccount, String portalUsername, String portalPassword) {
        this.patientCode = patientCode;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.diagnosis = diagnosis;
        this.bedId = bedId;
        this.createPortalAccount = createPortalAccount;
        this.portalUsername = portalUsername;
        this.portalPassword = portalPassword;
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
}


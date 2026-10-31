package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.BedAllocationHistoryResponse;
import com.hospital.smart_hospital.model.AllocationType;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedAllocationHistory;
import com.hospital.smart_hospital.model.Patient;
import java.util.List;

public interface BedAllocationHistoryService {

    List<BedAllocationHistoryResponse> getAllAllocationHistory();

    List<BedAllocationHistoryResponse> getAllocationHistoryByPatientId(Long patientId);

    BedAllocationHistory recordAllocation(Bed bed, Patient patient, String allocatedBy, AllocationType type, String overrideReason);

    void releaseAllocation(Long patientId);
}

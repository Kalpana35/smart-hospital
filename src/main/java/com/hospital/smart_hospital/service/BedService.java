package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.dto.BedResponse;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import java.util.List;

public interface BedService {

    BedResponse createBed(BedRequest request);

    BedResponse getBedById(Long id);

    List<BedResponse> getBeds(Long roomId, BedStatus status);

    List<BedResponse> getBedsByRoomId(Long roomId);

    BedResponse updateBed(Long id, BedRequest request);

    BedResponse updateBedStatus(Long id, BedStatus status);

    Bed getBedEntity(Long id);
}


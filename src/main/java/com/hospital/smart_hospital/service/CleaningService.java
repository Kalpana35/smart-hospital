package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.CleaningStatisticsResponse;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedCleaningLog;
import java.util.List;

public interface CleaningService {

    List<BedCleaningLog> getCleaningQueue();

    BedCleaningLog assignCleaner(Long bedId, String cleanerName);

    Bed completeCleaning(Long bedId, String cleanerName, String inspectionNotes);

    CleaningStatisticsResponse getCleaningStatistics();
}

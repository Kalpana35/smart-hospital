package com.hospital.smart_hospital.dto;

import com.hospital.smart_hospital.model.BedCleaningLog;
import java.util.List;

public class CleaningStatisticsResponse {

    private int totalDirtyBeds;
    private int inProgressCount;
    private int completedToday;
    private double averageTurnaroundMinutes;
    private List<BedCleaningLog> queue;
    private List<BedCleaningLog> recentLogs;

    public CleaningStatisticsResponse() {
    }

    public CleaningStatisticsResponse(
            int totalDirtyBeds,
            int inProgressCount,
            int completedToday,
            double averageTurnaroundMinutes,
            List<BedCleaningLog> queue,
            List<BedCleaningLog> recentLogs
    ) {
        this.totalDirtyBeds = totalDirtyBeds;
        this.inProgressCount = inProgressCount;
        this.completedToday = completedToday;
        this.averageTurnaroundMinutes = averageTurnaroundMinutes;
        this.queue = queue;
        this.recentLogs = recentLogs;
    }

    public int getTotalDirtyBeds() {
        return totalDirtyBeds;
    }

    public void setTotalDirtyBeds(int totalDirtyBeds) {
        this.totalDirtyBeds = totalDirtyBeds;
    }

    public int getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(int inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public int getCompletedToday() {
        return completedToday;
    }

    public void setCompletedToday(int completedToday) {
        this.completedToday = completedToday;
    }

    public double getAverageTurnaroundMinutes() {
        return averageTurnaroundMinutes;
    }

    public void setAverageTurnaroundMinutes(double averageTurnaroundMinutes) {
        this.averageTurnaroundMinutes = averageTurnaroundMinutes;
    }

    public List<BedCleaningLog> getQueue() {
        return queue;
    }

    public void setQueue(List<BedCleaningLog> queue) {
        this.queue = queue;
    }

    public List<BedCleaningLog> getRecentLogs() {
        return recentLogs;
    }

    public void setRecentLogs(List<BedCleaningLog> recentLogs) {
        this.recentLogs = recentLogs;
    }
}

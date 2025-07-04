package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminDashboardActivities {
    private Long adminId;
    private Long agenceId;
    private LocalDateTime lastUpdate;
    private Long activitiesCount;
    private Long unreadNotifications;
    private Long urgentNotifications;
    private List<ActivitySummary> activities;
    private Map<String, Long> stats;
}
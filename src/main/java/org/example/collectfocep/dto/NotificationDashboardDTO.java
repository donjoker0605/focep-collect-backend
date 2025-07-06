package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDashboardDTO {
    private NotificationStatsDTO stats;
    private List<AdminNotificationDTO> recentNotifications;
    private List<AdminNotificationDTO> criticalNotifications;
    private LocalDateTime lastUpdate;
}
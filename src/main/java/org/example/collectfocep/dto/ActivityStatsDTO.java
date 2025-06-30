package org.example.collectfocep.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatsDTO {

    private Long totalActivities;
    private Map<String, Long> activitiesByType;
    private Long errorCount;
    private Map<String, Long> topActions;
    private Integer periodDays;
}
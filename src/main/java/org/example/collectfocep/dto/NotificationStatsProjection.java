package org.example.collectfocep.dto;

import java.time.LocalDateTime;

public interface NotificationStatsProjection {
    Long getTotal();
    Long getNonLues();
    Long getCritiques();
    Long getCritiquesNonLues();
    LocalDateTime getDerniere();
}

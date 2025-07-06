package org.example.collectfocep.dto;

import java.time.LocalDateTime;

public interface NotificationDashboardProjection {
    Long getId();
    String getType();
    String getPriority();
    String getTitle();
    String getMessage();
    LocalDateTime getDateCreation();
    Boolean getLu();
    Integer getGroupedCount();
    String getCollecteurNom();
    String getAgenceNom();
}

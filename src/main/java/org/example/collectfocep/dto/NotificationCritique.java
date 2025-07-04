package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;

@Data
@Builder
public class NotificationCritique {
    private NotificationType type;
    private Priority priority;
    private String title;
    private String message;
    private String data;
}

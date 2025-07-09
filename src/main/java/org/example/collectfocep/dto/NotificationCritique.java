package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCritique {
    private NotificationType type;
    private Priority priority;
    private String title;
    private String message;
    private String data;

    public boolean isCritical() {
        return Priority.CRITIQUE.equals(this.priority);
    }
}


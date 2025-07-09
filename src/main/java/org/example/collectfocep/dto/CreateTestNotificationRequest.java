package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
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
public class CreateTestNotificationRequest {
    @NotNull
    private NotificationType type;
    @NotNull
    private Priority priority;
    private String title;
    private String message;
    private Long collecteurId;
}

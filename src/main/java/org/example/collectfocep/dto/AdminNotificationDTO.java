package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;


import java.time.LocalDateTime;

@Data
@Builder
public class AdminNotificationDTO {
    private Long id;
    private Long adminId;
    private Long collecteurId;
    private String collecteurNom;
    private Long agenceId;
    private String agenceNom;
    private NotificationType type;
    private Priority priority;
    private String title;
    private String message;
    private String data;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLecture;
    private Boolean lu;
    private Integer groupedCount;
    private Boolean emailSent;
    private Long minutesSinceCreation;
}
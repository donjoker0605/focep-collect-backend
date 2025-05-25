package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String titre;
    private String message;
    private String type;
    private Boolean lu;
    private LocalDateTime dateCreation;
    private Map<String, Object> metadata;
}

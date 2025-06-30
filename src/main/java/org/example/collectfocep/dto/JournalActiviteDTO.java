package org.example.collectfocep.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalActiviteDTO {

    private Long id;
    private Long userId;
    private String username;
    private String userType;
    private String action;
    private String actionDisplayName;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private String userAgent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private Long agenceId;
    private String description;
    private Boolean success;
    private Long duration;
}
package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {
    private Long userId;
    private String userType;
    private String username;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private String userAgent;
    private Long agenceId;
    private Boolean success;
    private String errorMessage;
    private Long durationMs;
}
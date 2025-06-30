package org.example.collectfocep.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSearchCriteria {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private String username;
    private List<String> actions;
    private String entityType;
    private Long entityId;
    private Boolean errorsOnly;
    private String ipAddress;

    // Pour la pagination avec @Builder.Default
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "timestamp";

    @Builder.Default
    private String sortDirection = "DESC";
}
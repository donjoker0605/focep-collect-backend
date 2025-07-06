package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDTO {
    private Long total;
    private Long nonLues;
    private Long critiques;
    private Long critiquesNonLues;
    private java.time.LocalDateTime derniereNotification;
}

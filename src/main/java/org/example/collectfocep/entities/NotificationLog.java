package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "collecteur_id")
    private Long collecteurId;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATED, READ, GROUPED, EMAIL_SENT

    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "details", columnDefinition = "JSON")
    private String details;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // =====================================
    // FACTORY METHODS
    // =====================================

    public static NotificationLog created(Long adminId, Long collecteurId, Long notificationId, String details) {
        return NotificationLog.builder()
                .adminId(adminId)
                .collecteurId(collecteurId)
                .action("CREATED")
                .notificationId(notificationId)
                .details(details)
                .build();
    }

    public static NotificationLog read(Long adminId, Long notificationId, String ipAddress, String userAgent) {
        return NotificationLog.builder()
                .adminId(adminId)
                .action("READ")
                .notificationId(notificationId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }

    public static NotificationLog emailSent(Long adminId, Long notificationId, String details) {
        return NotificationLog.builder()
                .adminId(adminId)
                .action("EMAIL_SENT")
                .notificationId(notificationId)
                .details(details)
                .build();
    }
}

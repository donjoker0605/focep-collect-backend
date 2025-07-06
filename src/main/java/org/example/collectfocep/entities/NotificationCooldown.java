package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_cooldown")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCooldown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collecteur_id", nullable = false)
    private Long collecteurId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    public boolean isInCooldown(int cooldownMinutes) {
        LocalDateTime cooldownExpiry = this.lastSentAt.plusMinutes(cooldownMinutes);
        return LocalDateTime.now().isBefore(cooldownExpiry);
    }

    public long getRemainingCooldownMinutes(int totalCooldownMinutes) {
        LocalDateTime cooldownExpiry = this.lastSentAt.plusMinutes(totalCooldownMinutes);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(cooldownExpiry)) {
            return 0;
        }

        return java.time.Duration.between(now, cooldownExpiry).toMinutes();
    }

    public static NotificationCooldown create(Long collecteurId, String notificationType) {
        return NotificationCooldown.builder()
                .collecteurId(collecteurId)
                .notificationType(notificationType)
                .lastSentAt(LocalDateTime.now())
                .build();
    }
}

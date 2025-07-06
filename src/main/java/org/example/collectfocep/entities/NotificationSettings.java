package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "threshold_value", precision = 15, scale = 2)
    private BigDecimal thresholdValue;

    @Column(name = "cooldown_minutes")
    @Builder.Default
    private Integer cooldownMinutes = 60;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    public boolean shouldNotify() {
        return Boolean.TRUE.equals(this.enabled);
    }

    public boolean shouldSendEmail() {
        return Boolean.TRUE.equals(this.emailEnabled) && shouldNotify();
    }

    public boolean exceedsThreshold(BigDecimal value) {
        if (this.thresholdValue == null || value == null) {
            return false;
        }
        return value.compareTo(this.thresholdValue) > 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

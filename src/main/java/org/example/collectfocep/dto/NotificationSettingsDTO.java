package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsDTO {
    @NotNull
    private String type;
    @NotNull
    private Boolean enabled;
    @NotNull
    private Boolean emailEnabled;
    private BigDecimal thresholdValue;
    private Integer cooldownMinutes;

    // Getters pour AdminNotificationService
    public String getType() { return type; }
    public Boolean getEnabled() { return enabled; }
    public Boolean getEmailEnabled() { return emailEnabled; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public Integer getCooldownMinutes() { return cooldownMinutes; }
}
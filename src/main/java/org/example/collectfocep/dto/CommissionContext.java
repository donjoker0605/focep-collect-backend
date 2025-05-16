package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * Contexte immutable pour les calculs de commission
 * Contient toutes les informations nécessaires pour un calcul
 */
@Value
@Builder
@AllArgsConstructor
public class CommissionContext {
    Long collecteurId;
    LocalDate startDate;
    LocalDate endDate;
    CommissionRules rules;
    boolean includeInactiveClients;

    public static CommissionContext of(Long collecteurId, LocalDate start, LocalDate end) {
        return CommissionContext.builder()
                .collecteurId(collecteurId)
                .startDate(start)
                .endDate(end)
                .rules(CommissionRules.defaultRules())
                .includeInactiveClients(false)
                .build();
    }

    public static CommissionContext withRules(Long collecteurId, LocalDate start, LocalDate end, CommissionRules rules) {
        return CommissionContext.builder()
                .collecteurId(collecteurId)
                .startDate(start)
                .endDate(end)
                .rules(rules)
                .includeInactiveClients(false)
                .build();
    }

    public boolean isValidPeriod() {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }

    // Getters pour la compatibilité
    public Long getCollecteurId() { return collecteurId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public CommissionRules getRules() { return rules; }
}
package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Résultat d'un calcul de commission pour un client
 * Immutable et thread-safe
 */
@Value
@Builder
@AllArgsConstructor
public class CommissionCalculation {
    Long clientId;
    String clientName;
    String numeroCompte;

    @NonNull
    BigDecimal montantCollecte;

    @NonNull
    BigDecimal commissionBase;

    @NonNull
    BigDecimal tva;

    @NonNull
    BigDecimal commissionNet;

    String typeCommission;
    BigDecimal valeurParametre; // Valeur utilisée pour le calcul (%, montant fixe, etc.)

    LocalDateTime calculatedAt;

    // Métadonnées
    Long parameterId;
    String scope; // CLIENT, COLLECTEUR, AGENCE

    public BigDecimal getCommissionTotal() {
        return commissionBase.add(tva);
    }

    public static CommissionCalculation create(Long clientId, String clientName, String numeroCompte,
                                               BigDecimal montantCollecte, BigDecimal commission,
                                               BigDecimal tva, String type, Long parameterId) {
        return CommissionCalculation.builder()
                .clientId(clientId)
                .clientName(clientName)
                .numeroCompte(numeroCompte)
                .montantCollecte(montantCollecte)
                .commissionBase(commission)
                .tva(tva)
                .commissionNet(commission.subtract(tva))
                .typeCommission(type)
                .calculatedAt(LocalDateTime.now())
                .parameterId(parameterId)
                .build();
    }

    // Méthodes d'accès supplémentaires pour la compatibilité
    public Long getClientId() { return clientId; }
    public BigDecimal getCommissionBase() { return commissionBase; }
    public BigDecimal getTva() { return tva; }
    public String getTypeCommission() { return typeCommission; }
    public BigDecimal getValeurParametre() { return valeurParametre; }
    public Long getParameterId() { return parameterId; }
}
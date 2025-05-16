package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Règles de calcul de commission
 * Contient les taux et paramètres métier
 */
@Value
@Builder
@AllArgsConstructor
public class CommissionRules {
    @Builder.Default
    BigDecimal tvaRate = BigDecimal.valueOf(0.1925); // 19.25%

    @Builder.Default
    BigDecimal emfRate = BigDecimal.valueOf(0.30); // 30% pour EMF

    @Builder.Default
    BigDecimal collecteurRate = BigDecimal.valueOf(0.70); // 70% pour collecteur

    @Builder.Default
    BigDecimal nouveauCollecteurMontant = BigDecimal.valueOf(40000); // Montant fixe nouveaux

    @Builder.Default
    int nouveauCollecteurDureeMois = 3; // 3 mois pour être considéré comme nouveau

    @Builder.Default
    BigDecimal plafondCommissionFixe = BigDecimal.valueOf(1000000); // Plafond sécurité

    public static CommissionRules defaultRules() {
        return CommissionRules.builder().build();
    }

    public static CommissionRules customRules(BigDecimal tvaRate, BigDecimal emfRate) {
        return CommissionRules.builder()
                .tvaRate(tvaRate)
                .emfRate(emfRate)
                .collecteurRate(BigDecimal.ONE.subtract(emfRate))
                .build();
    }

    public boolean isNouveauCollecteur(int ancienneteEnMois) {
        return ancienneteEnMois <= nouveauCollecteurDureeMois;
    }

    // Getters pour la compatibilité
    public BigDecimal getTvaRate() { return tvaRate; }
    public BigDecimal getEmfRate() { return emfRate; }
    public BigDecimal getCollecteurRate() { return collecteurRate; }
    public BigDecimal getNouveauCollecteurMontant() { return nouveauCollecteurMontant; }
}
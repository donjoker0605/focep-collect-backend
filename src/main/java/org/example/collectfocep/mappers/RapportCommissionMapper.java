package org.example.collectfocep.mappers;

import org.example.collectfocep.entities.Commission;
import org.example.collectfocep.entities.RapportCommission;
import org.example.collectfocep.dto.CommissionClientDTO;
import org.example.collectfocep.dto.RapportCommissionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Utilise seulement les propriétés qui existent réellement dans les DTOs
 */
@Mapper(componentModel = "spring", imports = {LocalDateTime.class, BigDecimal.class})
public interface RapportCommissionMapper {

    // ================================
    // MAPPING RAPPORT PRINCIPAL CORRIGÉ
    // ================================

    /**
     * RapportCommission -> RapportCommissionDTO
     * Mapping selon les vraies propriétés du DTO
     */
    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "dateDebut", target = "dateDebut")
    @Mapping(source = "dateFin", target = "dateFin")
    @Mapping(source = "commissions", target = "commissionsClients")
    @Mapping(source = "totalCommissions", target = "totalCommissions")
    @Mapping(source = "totalTVA", target = "totalTVA")
    @Mapping(source = "remunerationCollecteur", target = "remunerationCollecteur")
    @Mapping(source = "partEMF", target = "partEMF")
    @Mapping(source = "tvaSurPartEMF", target = "tvaSurPartEMF")
    RapportCommissionDTO toDTO(RapportCommission rapport);

    // ================================
    // MAPPING COMMISSION CLIENT CORRIGÉ
    // ================================

    /**
     * Commission -> CommissionClientDTO
     * Mapping selon les vraies propriétés du CommissionClientDTO
     */
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.nom", target = "nomClient")
    @Mapping(source = "compte.numeroCompte", target = "numeroCompte")
    @Mapping(source = "type", target = "typeCommission")
    @Mapping(target = "dateCalcul", expression = "java(LocalDateTime.now())")

    // Utiliser les méthodes sécurisées pour les montants
    @Mapping(expression = "java(safeToDouble(commission.getMontant()))", target = "montantCollecte")
    @Mapping(expression = "java(safeToDouble(commission.getMontant()))", target = "montantCommission")
    @Mapping(expression = "java(safeToDouble(commission.getTva()))", target = "montantTVA")
    CommissionClientDTO toClientDTO(Commission commission);

    // ================================
    // MÉTHODES UTILITAIRES SÉCURISÉES
    // ================================

    /**
     * Conversion BigDecimal -> double sécurisée avec validation
     */
    default double safeToDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }

        // Vérifier que la valeur est dans une plage raisonnable pour double
        if (value.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0) {
            throw new ArithmeticException("Valeur trop grande pour conversion en double: " + value);
        }

        return value.doubleValue();
    }

    /**
     * Conversion BigDecimal -> double sécurisée avec valeur par défaut
     */
    default double safeToDoubleWithDefault(BigDecimal value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            return safeToDouble(value);
        } catch (ArithmeticException e) {
            // Log l'erreur et retourner la valeur par défaut
            System.err.println("Erreur conversion BigDecimal->double: " + e.getMessage());
            return defaultValue;
        }
    }

    // ================================
    // CALCULS AGRÉGÉS POUR RAPPORT
    // ================================

    /**
     * Calcule le total des commissions pour un rapport
     */
    default double calculateTotalCommissions(RapportCommission rapport) {
        if (rapport == null || rapport.getCommissions() == null) {
            return 0.0;
        }

        BigDecimal total = rapport.getCommissions().stream()
                .map(Commission::getMontant)
                .filter(montant -> montant != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return safeToDouble(total);
    }

    /**
     * Calcule le total de la TVA pour un rapport
     */
    default double calculateTotalTVA(RapportCommission rapport) {
        if (rapport == null || rapport.getCommissions() == null) {
            return 0.0;
        }

        BigDecimal total = rapport.getCommissions().stream()
                .map(Commission::getTva)
                .filter(tva -> tva != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return safeToDouble(total);
    }

    /**
     * Calcule le total net pour un rapport
     */
    default double calculateTotalNet(RapportCommission rapport) {
        if (rapport == null || rapport.getCommissions() == null) {
            return 0.0;
        }

        BigDecimal total = rapport.getCommissions().stream()
                .map(Commission::getMontantNet)
                .filter(net -> net != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return safeToDouble(total);
    }

    // ================================
    // MÉTHODES ALTERNATIVES AVEC BIGDECIMAL
    // ================================

    /**
     * Version alternative qui retourne BigDecimal (pour calculs internes)
     */
    default BigDecimal calculateTotalCommissionsBD(RapportCommission rapport) {
        if (rapport == null || rapport.getCommissions() == null) {
            return BigDecimal.ZERO;
        }

        return rapport.getCommissions().stream()
                .map(Commission::getMontant)
                .filter(montant -> montant != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Version alternative qui retourne BigDecimal pour TVA
     */
    default BigDecimal calculateTotalTVABD(RapportCommission rapport) {
        if (rapport == null || rapport.getCommissions() == null) {
            return BigDecimal.ZERO;
        }

        return rapport.getCommissions().stream()
                .map(Commission::getTva)
                .filter(tva -> tva != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ================================
    // MÉTHODES DE VALIDATION
    // ================================

    /**
     * Valide qu'une commission peut être mappée
     */
    default boolean isValidCommission(Commission commission) {
        return commission != null &&
                commission.getClient() != null &&
                commission.getMontant() != null;
    }

    /**
     * Valide qu'un rapport peut être mappé
     */
    default boolean isValidRapport(RapportCommission rapport) {
        return rapport != null &&
                rapport.getCollecteur() != null &&
                rapport.getDateDebut() != null &&
                rapport.getDateFin() != null;
    }

    // ================================
    // MÉTHODES DEPRECATED (à supprimer progressivement)
    // ================================

    /**
     * @deprecated Utiliser safeToDouble() à la place
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default double mapMontantCollecte(Commission commission) {
        return safeToDouble(commission != null ? commission.getMontant() : null);
    }

    /**
     * @deprecated Utiliser safeToDouble() à la place
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default double mapMontantCommission(Commission commission) {
        return safeToDouble(commission != null ? commission.getMontant() : null);
    }

    /**
     * @deprecated Utiliser safeToDouble() à la place
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default double mapMontantTVA(Commission commission) {
        return safeToDouble(commission != null ? commission.getTva() : null);
    }

    /**
     * @deprecated Utiliser safeToDouble() à la place
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default double mapMontantNet(Commission commission) {
        return safeToDouble(commission != null ? commission.getMontantNet() : null);
    }

    /**
     * @deprecated Utiliser safeToDouble() à la place
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default double mapMontantTotal(Commission commission) {
        return safeToDouble(commission != null ? commission.getMontantTotal() : null);
    }
}
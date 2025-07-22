package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionCalculation;
import org.example.collectfocep.dto.CommissionContext;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Moteur de calcul des commissions optimisé
 * Requête SQL robuste + types BigDecimal + API Spring Boot moderne
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CommissionCalculationEngine {

    private final JdbcTemplate jdbcTemplate;
    private final CommissionParameterRepository commissionParameterRepository;
    private final CommissionRepository commissionRepository;
    private final MouvementRepository mouvementRepository;
    private final ClientRepository clientRepository;

    // Configuration des types de mouvements d'épargne
    @Value("${commission.epargne.mouvements:EPARGNE,DEPOT_EPARGNE,VERSEMENT_EPARGNE}")
    private String[] epargneMouvementTypes;

    @Value("${commission.epargne.libelles:épargne,Épargne,EPARGNE,versement,Versement}")
    private String[] epargneLibelles;

    /**
     * Calcule les commissions en batch avec requête SQL robuste
     */
    @Transactional(readOnly = true)
    public List<CommissionCalculation> calculateBatch(CommissionContext context) {
        log.info("Calcul batch commissions - Collecteur: {}, Période: {} à {}",
                context.getCollecteurId(), context.getStartDate(), context.getEndDate());

        // Requête SQL robuste avec types de mouvements
        String sql = """
            SELECT 
                c.id as client_id,
                CONCAT(c.nom, ' ', c.prenom) as client_name,
                cpt.numero_compte as numero_compte,
                COALESCE(SUM(CASE 
                    WHEN m.sens = 'CREDIT' 
                    AND m.compte_destination = cc.id 
                    AND (
                        m.type_mouvement IN ('EPARGNE', 'DEPOT_EPARGNE', 'VERSEMENT_EPARGNE')
                        OR m.categorie_operation IN ('DEPOT_EPARGNE', 'VERSEMENT_EPARGNE')
                        OR (m.libelle ILIKE '%épargne%' OR m.libelle ILIKE '%versement%')
                    )
                    THEN m.montant 
                    ELSE 0 
                END), 0) as montant_collecte,
                cp.id as param_id,
                cp.type as param_type,
                cp.valeur as param_valeur
            FROM clients c
            JOIN compte_client cc ON cc.id_client = c.id
            JOIN comptes cpt ON cpt.id = cc.id
            LEFT JOIN mouvements m ON m.compte_destination = cc.id 
                AND m.date_operation BETWEEN ? AND ?
                AND m.statut = 'VALIDE'
            LEFT JOIN commission_parameter cp ON (
                (cp.client_id = c.id AND cp.client_id IS NOT NULL) OR 
                (cp.collecteur_id = c.id_collecteur AND cp.client_id IS NULL) OR 
                (cp.agence_id = c.id_agence AND cp.client_id IS NULL AND cp.collecteur_id IS NULL)
            )
            WHERE c.id_collecteur = ?
                AND c.valide = true
                AND (cp.is_active = true OR cp.is_active IS NULL)
                AND (cp.valid_from IS NULL OR cp.valid_from <= ?)
                AND (cp.valid_to IS NULL OR cp.valid_to >= ?)
            GROUP BY c.id, c.nom, c.prenom, cpt.numero_compte, cp.id, cp.type, cp.valeur
            ORDER BY c.nom, c.prenom
            """;

        // Paramètres de la requête
        Object[] params = {
                java.sql.Timestamp.valueOf(context.getStartDate().atStartOfDay()),
                java.sql.Timestamp.valueOf(context.getEndDate().atTime(23, 59, 59)),
                context.getCollecteurId(),
                context.getEndDate(),
                context.getStartDate()
        };

        // Utiliser API moderne Spring Boot (non dépréciée)
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToCommissionCalculation(rs, rowNum, context), params);
    }

    /**
     * Mapping avec types BigDecimal + context passé en paramètre
     */
    private CommissionCalculation mapRowToCommissionCalculation(ResultSet rs, int rowNum, CommissionContext context)
            throws SQLException {

        Long clientId = rs.getLong("client_id");
        String clientName = rs.getString("client_name");
        String numeroCompte = rs.getString("numero_compte");
        BigDecimal montantCollecte = rs.getBigDecimal("montant_collecte");

        Long paramId = rs.getObject("param_id") != null ? rs.getLong("param_id") : null;
        String paramType = rs.getString("param_type");
        BigDecimal paramValeur = rs.getBigDecimal("param_valeur");

        // Calcul de la commission
        BigDecimal commissionBase = calculateCommissionAmount(
                montantCollecte, paramType, paramValeur, paramId);

        // Calcul TVA avec les règles du contexte
        BigDecimal tva = commissionBase.multiply(context.getRules().getTvaRate())
                .setScale(2, RoundingMode.HALF_UP);

        return CommissionCalculation.builder()
                .clientId(clientId)
                .clientName(clientName)
                .numeroCompte(numeroCompte)
                .montantCollecte(montantCollecte)
                .commissionBase(commissionBase)
                .tva(tva)
                .commissionNet(commissionBase.subtract(tva))
                .typeCommission(paramType)
                .valeurParametre(paramValeur)
                .calculatedAt(LocalDateTime.now())
                .parameterId(paramId)
                .scope(determineScope(paramId))
                .build();
    }

    /**
     * Calcul commission avec BigDecimal
     */
    private BigDecimal calculateCommissionAmount(BigDecimal montantCollecte,
                                                 String paramType,
                                                 BigDecimal paramValeur,
                                                 Long paramId) {
        if (montantCollecte == null || montantCollecte.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (paramType == null || paramValeur == null) {
            log.warn("Paramètre de commission manquant pour calcul - type: {}, valeur: {}",
                    paramType, paramValeur);
            return BigDecimal.ZERO;
        }

        try {
            return switch (CommissionType.valueOf(paramType)) {
                case FIXED -> paramValeur.setScale(2, RoundingMode.HALF_UP);

                case PERCENTAGE -> montantCollecte
                        .multiply(paramValeur)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                case TIER -> calculateTierCommission(montantCollecte, paramId);
            };
        } catch (Exception e) {
            log.error("Erreur calcul commission - type: {}, montant: {}, valeur: {}",
                    paramType, montantCollecte, paramValeur, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calcul commission par paliers optimisé
     */
    private BigDecimal calculateTierCommission(BigDecimal montant, Long paramId) {
        if (paramId == null) return BigDecimal.ZERO;

        Optional<CommissionParameter> paramOpt = commissionParameterRepository
                .findByIdWithTiers(paramId);

        if (paramOpt.isEmpty() || paramOpt.get().getTiers() == null) {
            log.warn("Paramètre TIER sans paliers - ID: {}", paramId);
            return BigDecimal.ZERO;
        }

        CommissionTier applicableTier = paramOpt.get().findApplicableTier(montant);
        if (applicableTier == null) {
            log.warn("Aucun palier applicable pour montant {} - paramId: {}", montant, paramId);
            return BigDecimal.ZERO;
        }

        return montant.multiply(BigDecimal.valueOf(applicableTier.getTaux()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Détermine le scope d'un paramètre
     */
    private String determineScope(Long paramId) {
        if (paramId == null) return "DEFAULT";

        return commissionParameterRepository.findById(paramId)
                .map(CommissionParameter::getScope)
                .orElse("UNKNOWN");
    }

    /**
     * Vérification existence commissions pour période
     */
    @Transactional(readOnly = true)
    public boolean hasExistingCommissions(Long collecteurId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT COUNT(c.id) FROM commission c
            WHERE c.collecteur_id = ?
            AND DATE(c.date_calcul) BETWEEN ? AND ?
            """;

        Long count = jdbcTemplate.queryForObject(sql, Long.class,
                collecteurId, startDate, endDate);

        return count != null && count > 0;
    }

    /**
     * Persistance des calculs avec gestion BigDecimal
     */
    @Transactional
    public void persistCalculations(List<CommissionCalculation> calculations,
                                    CommissionContext context) {
        log.info("Persistance de {} calculs de commissions", calculations.size());

        for (CommissionCalculation calc : calculations) {
            try {
                Commission commission = Commission.builder()
                        .client(clientRepository.getReferenceById(calc.getClientId()))
                        .collecteur(clientRepository.getReferenceById(calc.getClientId()).getCollecteur())
                        .montant(calc.getCommissionBase())
                        .tva(calc.getTva())
                        .type(calc.getTypeCommission())
                        .valeur(calc.getValeurParametre())
                        .dateCalcul(calc.getCalculatedAt())
                        .commissionParameter(calc.getParameterId() != null ?
                                commissionParameterRepository.getReferenceById(calc.getParameterId()) : null)
                        .build();

                commissionRepository.save(commission);

            } catch (Exception e) {
                log.error("Erreur persistance commission client {}: {}",
                        calc.getClientId(), e.getMessage(), e);
                // Continue avec les autres calculs
            }
        }
    }

    /**
     * ✅ CORRECTION : Méthode alternative robuste avec API moderne
     */
    @Transactional(readOnly = true)
    public List<CommissionCalculation> calculateBatchFallback(CommissionContext context) {
        log.info("Calcul batch fallback - utilisation libellés épargne");

        // Construction dynamique des conditions LIKE
        StringBuilder libelleConditions = new StringBuilder();
        for (int i = 0; i < epargneLibelles.length; i++) {
            if (i > 0) libelleConditions.append(" OR ");
            libelleConditions.append("m.libelle ILIKE '%").append(epargneLibelles[i]).append("%'");
        }

        String sql = """
            SELECT c.id as client_id,
                   CONCAT(c.nom, ' ', c.prenom) as client_name,
                   cpt.numero_compte as numero_compte,
                   COALESCE(SUM(CASE 
                       WHEN m.sens = 'CREDIT' 
                       AND m.compte_destination = cc.id 
                       AND (%s)
                       THEN m.montant ELSE 0 END), 0) as montant_collecte
            FROM clients c
            JOIN compte_client cc ON cc.id_client = c.id
            JOIN comptes cpt ON cpt.id = cc.id
            LEFT JOIN mouvements m ON m.compte_destination = cc.id 
                AND m.date_operation BETWEEN ? AND ?
            WHERE c.id_collecteur = ?
            GROUP BY c.id, c.nom, c.prenom, cpt.numero_compte
            """.formatted(libelleConditions.toString());

        // ✅ CORRECTION : API moderne avec lambda et context
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> mapRowToCommissionCalculationSimple(rs, rowNum, context),
                java.sql.Timestamp.valueOf(context.getStartDate().atStartOfDay()),
                java.sql.Timestamp.valueOf(context.getEndDate().atTime(23, 59, 59)),
                context.getCollecteurId());
    }

    /**
     * ✅ CORRECTION : Mapping simplifié pour fallback avec context
     */
    private CommissionCalculation mapRowToCommissionCalculationSimple(ResultSet rs, int rowNum, CommissionContext context)
            throws SQLException {

        // Version simplifiée sans paramètres avancés
        BigDecimal montant = rs.getBigDecimal("montant_collecte");
        BigDecimal commission = montant.multiply(BigDecimal.valueOf(0.05)); // 5% par défaut
        BigDecimal tva = commission.multiply(context.getRules().getTvaRate());

        return CommissionCalculation.builder()
                .clientId(rs.getLong("client_id"))
                .clientName(rs.getString("client_name"))
                .numeroCompte(rs.getString("numero_compte"))
                .montantCollecte(montant)
                .commissionBase(commission)
                .tva(tva)
                .commissionNet(commission.subtract(tva))
                .typeCommission("PERCENTAGE")
                .calculatedAt(LocalDateTime.now())
                .scope("DEFAULT")
                .build();
    }
}
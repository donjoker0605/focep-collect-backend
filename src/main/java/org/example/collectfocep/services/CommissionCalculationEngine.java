package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionCalculation;
import org.example.collectfocep.dto.CommissionContext;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
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
 * Utilise des requêtes SQL optimisées pour éviter les N+1 problems
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

    /**
     * Calcule les commissions en batch pour tous les clients d'un collecteur
     */
    @Transactional(readOnly = true)
    public List<CommissionCalculation> calculateBatch(CommissionContext context) {
        log.info("Calcul batch commissions - Collecteur: {}, Période: {} à {}",
                context.getCollecteurId(), context.getStartDate(), context.getEndDate());

        // Requête SQL optimisée avec agrégations
        String sql = """
            SELECT 
                c.id as client_id,
                CONCAT(c.nom, ' ', c.prenom) as client_name,
                cc.numero_compte as numero_compte,
                COALESCE(SUM(CASE 
                    WHEN m.sens = 'CREDIT' AND m.compte_destination = cc.id 
                    THEN m.montant 
                    ELSE 0 
                END), 0) as montant_collecte,
                cp.id as param_id,
                cp.type as param_type,
                cp.valeur as param_valeur
            FROM clients c
            JOIN compte_client cc ON cc.id_client = c.id
            LEFT JOIN mouvements m ON m.compte_destination = cc.id 
                AND m.date_operation BETWEEN ? AND ?
                AND m.libelle LIKE '%épargne%'
            LEFT JOIN commission_parameters cp ON (
                cp.client_id = c.id OR 
                cp.collecteur_id = c.id_collecteur OR 
                cp.agence_id = c.id_agence
            )
            WHERE c.id_collecteur = ?
                AND c.valide = true
                AND cp.is_active = true
                AND (cp.valid_from IS NULL OR cp.valid_from <= ?)
                AND (cp.valid_to IS NULL OR cp.valid_to >= ?)
            GROUP BY c.id, cc.numero_compte, cp.id, cp.type, cp.valeur
            ORDER BY c.id
            """;

        return jdbcTemplate.query(sql, this::mapToCalculation,
                context.getStartDate().atStartOfDay(),
                context.getEndDate().atTime(23, 59, 59),
                context.getCollecteurId(),
                context.getStartDate(),
                context.getEndDate());
    }

    /**
     * Vérifie si des commissions existent déjà pour la période
     */
    @Transactional(readOnly = true)
    public boolean hasExistingCommissions(Long collecteurId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT COUNT(*) 
            FROM commissions com
            JOIN clients c ON c.id = com.client_id
            WHERE c.id_collecteur = ?
                AND com.date_calcul BETWEEN ? AND ?
            """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                collecteurId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));

        return count != null && count > 0;
    }

    /**
     * Persiste les calculs de commissions
     */
    @Transactional
    public void persistCalculations(List<CommissionCalculation> calculations, CommissionContext context) {
        log.info("Persistance de {} calculs de commissions", calculations.size());

        for (CommissionCalculation calc : calculations) {
            Commission commission = Commission.builder()
                    .client(clientRepository.getReferenceById(calc.getClientId()))
                    .collecteur(clientRepository.getReferenceById(calc.getClientId()).getCollecteur())
                    .montant(calc.getCommissionBase().doubleValue())
                    .tva(calc.getTva().doubleValue())
                    .type(calc.getTypeCommission())
                    .valeur(calc.getValeurParametre() != null ? calc.getValeurParametre().doubleValue() : 0)
                    .dateCalcul(LocalDateTime.now())
                    .build();

            // Associer le paramètre de commission utilisé
            if (calc.getParameterId() != null) {
                commissionParameterRepository.findById(calc.getParameterId())
                        .ifPresent(commission::setCommissionParameter);
            }

            commissionRepository.save(commission);
        }
    }

    /**
     * Calcule les paramètres de commission pour un client
     */
    @Transactional(readOnly = true)
    public Optional<CommissionParameter> getCommissionParameters(Client client) {
        log.debug("Recherche paramètres commission pour client: {}", client.getId());

        // Recherche par priorité: Client > Collecteur > Agence
        return commissionParameterRepository.findByClient(client)
                .or(() -> commissionParameterRepository.findByCollecteur(client.getCollecteur()))
                .or(() -> commissionParameterRepository.findByAgence(client.getAgence()));
    }

    private CommissionCalculation mapToCalculation(ResultSet rs, int rowNum) throws SQLException {
        Long clientId = rs.getLong("client_id");
        String clientName = rs.getString("client_name");
        String numeroCompte = rs.getString("numero_compte");
        BigDecimal montantCollecte = rs.getBigDecimal("montant_collecte");
        Long paramId = rs.getLong("param_id");
        String paramType = rs.getString("param_type");
        BigDecimal paramValeur = rs.getBigDecimal("param_valeur");

        // Si pas de collecte, pas de commission
        if (montantCollecte.compareTo(BigDecimal.ZERO) == 0) {
            return CommissionCalculation.create(
                    clientId, clientName, numeroCompte, montantCollecte,
                    BigDecimal.ZERO, BigDecimal.ZERO, "NONE", null
            );
        }

        // Calcul de la commission selon le type
        BigDecimal commission = calculateCommissionByType(paramType, paramValeur, montantCollecte, paramId);
        BigDecimal tva = commission.multiply(BigDecimal.valueOf(0.1925))
                .setScale(2, RoundingMode.HALF_UP);

        return CommissionCalculation.create(
                clientId, clientName, numeroCompte, montantCollecte,
                commission, tva, paramType, paramId
        );
    }

    private BigDecimal calculateCommissionByType(String type, BigDecimal valeur, BigDecimal montant, Long paramId) {
        if (type == null || valeur == null) {
            return BigDecimal.ZERO;
        }

        return switch (type.toUpperCase()) {
            case "FIXED" -> valeur.setScale(2, RoundingMode.HALF_UP);
            case "PERCENTAGE" -> montant.multiply(valeur)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case "TIER" -> calculateCommissionByTier(montant, paramId);
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calculateCommissionByTier(BigDecimal montant, Long paramId) {
        if (paramId == null) return BigDecimal.ZERO;

        String sql = """
            SELECT taux 
            FROM commission_tiers 
            WHERE commission_parameter_id = ? 
                AND ? BETWEEN montant_min AND montant_max
            ORDER BY montant_min
            LIMIT 1
            """;

        try {
            Double taux = jdbcTemplate.queryForObject(sql, Double.class, paramId, montant);
            if (taux != null) {
                return montant.multiply(BigDecimal.valueOf(taux))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("Erreur calcul commission par palier pour montant {} et param {}: {}",
                    montant, paramId, e.getMessage());
        }

        return BigDecimal.ZERO;
    }
}
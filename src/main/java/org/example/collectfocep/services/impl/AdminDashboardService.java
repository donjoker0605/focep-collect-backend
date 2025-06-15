package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AgenceRepository agenceRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final MouvementRepository mouvementRepository;
    private final CommissionRepository commissionRepository;
    private final SecurityService securityService;

    /**
     * Récupérer les statistiques du dashboard admin
     * MISE À JOUR: Calcule les totaux d'épargne/retrait de TOUS les collecteurs de l'agence
     */
    public AdminDashboardDTO getAdminDashboardStats(Long agenceId) {
        log.info("📊 Calcul des statistiques dashboard pour l'agence: {}", agenceId);

        // Vérifier que l'agence existe
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // Statistiques collecteurs
        Long totalCollecteurs = collecteurRepository.countByAgenceId(agenceId);
        Long collecteursActifs = collecteurRepository.countByAgenceIdAndActiveTrue(agenceId);
        Long collecteursInactifs = totalCollecteurs - collecteursActifs;

        // Statistiques clients
        Long totalClients = clientRepository.countByAgenceId(agenceId);
        Long clientsActifs = clientRepository.countByAgenceIdAndValideTrue(agenceId);
        Long clientsInactifs = totalClients - clientsActifs;

        // MISE À JOUR: Calculer les totaux d'épargne et retrait pour TOUS les collecteurs de l'agence
        // Ces montants représentent le total des opérations effectuées par tous les collecteurs
        Double totalEpargne = mouvementRepository.sumByAgenceIdAndSens(agenceId, "EPARGNE");
        Double totalRetrait = mouvementRepository.sumByAgenceIdAndSens(agenceId, "RETRAIT");

        // Solde net (épargne - retrait)
        Double soldeNet = (totalEpargne != null ? totalEpargne : 0.0) -
                (totalRetrait != null ? totalRetrait : 0.0);

        // Commissions en attente (non rapportées)
        Long commissionsEnAttente = commissionRepository.countByAgenceIdAndRapportIsNull(agenceId);
        Double totalCommissions = commissionRepository.sumByAgenceId(agenceId);

        // Calculer les taux
        Double tauxCollecteursActifs = totalCollecteurs > 0 ?
                (collecteursActifs.doubleValue() / totalCollecteurs.doubleValue()) * 100 : 0.0;
        Double tauxClientsActifs = totalClients > 0 ?
                (clientsActifs.doubleValue() / totalClients.doubleValue()) * 100 : 0.0;

        // Construire le DTO
        AdminDashboardDTO dashboard = AdminDashboardDTO.builder()
                .periode("Agence " + agenceId)
                .lastUpdate(LocalDateTime.now())

                // Collecteurs
                .totalCollecteurs(totalCollecteurs != null ? totalCollecteurs : 0L)
                .collecteursActifs(collecteursActifs != null ? collecteursActifs : 0L)
                .collecteursInactifs(collecteursInactifs)
                .tauxCollecteursActifs(tauxCollecteursActifs)

                // Clients
                .totalClients(totalClients != null ? totalClients : 0L)
                .clientsActifs(clientsActifs != null ? clientsActifs : 0L)
                .clientsInactifs(clientsInactifs)
                .tauxClientsActifs(tauxClientsActifs)

                // Finances - TOTAUX DE TOUS LES COLLECTEURS
                .totalEpargne(totalEpargne != null ? totalEpargne : 0.0)
                .totalRetrait(totalRetrait != null ? totalRetrait : 0.0)
                .soldeNet(soldeNet)

                // Commissions
                .commissionsEnAttente(commissionsEnAttente != null ? commissionsEnAttente : 0L)
                .totalCommissions(totalCommissions != null ? totalCommissions : 0.0)

                // Agence
                .agencesActives(1L) // Une agence active (celle de l'admin)

                .build();

        log.info("✅ Statistiques calculées - Épargne totale: {}, Retrait total: {}",
                totalEpargne, totalRetrait);

        return dashboard;
    }

    /**
     * Récupérer les statistiques détaillées par collecteur
     * Utile pour avoir une vue détaillée de chaque collecteur
     */
    public AdminDashboardDTO getDetailedCollecteurStats(Long agenceId, Long collecteurId) {
        log.info("📊 Calcul des statistiques détaillées pour le collecteur: {}", collecteurId);

        // Vérifier l'accès au collecteur
        if (!securityService.hasPermissionForCollecteur(collecteurId)) {
            throw new RuntimeException("Accès non autorisé au collecteur");
        }

        // Statistiques spécifiques au collecteur
        Long totalClients = clientRepository.countByCollecteurId(collecteurId);
        Long clientsActifs = clientRepository.countByCollecteurIdAndValideTrue(collecteurId);

        // Montants pour ce collecteur spécifique
        Double epargneCollecteur = mouvementRepository.sumByCollecteurIdAndSens(collecteurId, "EPARGNE");
        Double retraitCollecteur = mouvementRepository.sumByCollecteurIdAndSens(collecteurId, "RETRAIT");

        Double soldeNet = (epargneCollecteur != null ? epargneCollecteur : 0.0) -
                (retraitCollecteur != null ? retraitCollecteur : 0.0);

        // Commissions du collecteur
        Long commissionsEnAttente = commissionRepository.countByCollecteurIdAndRapportIsNull(collecteurId);
        Double totalCommissions = commissionRepository.sumByCollecteurId(collecteurId);

        return AdminDashboardDTO.builder()
                .periode("Collecteur " + collecteurId)
                .lastUpdate(LocalDateTime.now())
                .totalClients(totalClients != null ? totalClients : 0L)
                .clientsActifs(clientsActifs != null ? clientsActifs : 0L)
                .clientsInactifs((totalClients != null ? totalClients : 0L) - (clientsActifs != null ? clientsActifs : 0L))
                .totalEpargne(epargneCollecteur != null ? epargneCollecteur : 0.0)
                .totalRetrait(retraitCollecteur != null ? retraitCollecteur : 0.0)
                .soldeNet(soldeNet)
                .commissionsEnAttente(commissionsEnAttente != null ? commissionsEnAttente : 0L)
                .totalCommissions(totalCommissions != null ? totalCommissions : 0.0)
                .build();
    }
}
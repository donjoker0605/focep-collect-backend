package org.example.collectfocep.services;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.CompteNotFoundException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class CompteTransferService {
    private final CompteRepository compteRepository;
    private final CompteClientRepository compteClientRepository;
    private final ClientRepository clientRepository;
    private final CollecteurRepository collecteurRepository;
    private final MouvementRepository mouvementRepository;
    private final MouvementServiceImpl mouvementServiceImpl;
    private final CompteLiaisonRepository compteLiaisonRepository;
    private final TransfertCompteRepository transfertCompteRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public CompteTransferService(
            CompteRepository compteRepository,
            CompteClientRepository compteClientRepository,
            ClientRepository clientRepository,
            CollecteurRepository collecteurRepository,
            MouvementRepository mouvementRepository,
            MouvementServiceImpl mouvementServiceImpl,
            CompteLiaisonRepository compteLiaisonRepository,
            TransfertCompteRepository transfertCompteRepository,
            AuditLogRepository auditLogRepository) {
        this.compteRepository = compteRepository;
        this.compteClientRepository = compteClientRepository;
        this.clientRepository = clientRepository;
        this.collecteurRepository = collecteurRepository;
        this.mouvementRepository = mouvementRepository;
        this.mouvementServiceImpl = mouvementServiceImpl;
        this.compteLiaisonRepository = compteLiaisonRepository;
        this.transfertCompteRepository = transfertCompteRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Transfère un ou plusieurs comptes clients d'un collecteur à un autre.
     * Gère la création des mouvements comptables appropriés, y compris quand
     * les collecteurs appartiennent à des agences différentes.
     *
     * @param sourceCollecteurId ID du collecteur source
     * @param targetCollecteurId ID du collecteur cible
     * @param clientIds Liste des IDs des clients à transférer
     * @return Nombre de comptes transférés avec succès
     */
    public int transferComptes(Long sourceCollecteurId, Long targetCollecteurId, List<Long> clientIds) {
        log.info("Début du transfert de {} comptes du collecteur {} vers le collecteur {}",
                clientIds.size(), sourceCollecteurId, targetCollecteurId);

        Collecteur sourceCollecteur = collecteurRepository.findById(sourceCollecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur source non trouvé"));

        Collecteur targetCollecteur = collecteurRepository.findById(targetCollecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur cible non trouvé"));

        boolean isSameAgence = sourceCollecteur.getAgence().getId().equals(targetCollecteur.getAgence().getId());
        int successCount = 0;

        for (Long clientId : clientIds) {
            try {
                // Verrouiller le client pour éviter les modifications concurrentes
                Client client = clientRepository.findByIdForUpdate(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + clientId));

                // Vérifier que le client appartient bien au collecteur source
                if (!sourceCollecteurId.equals(client.getCollecteur().getId())) {
                    log.warn("Client {} n'appartient pas au collecteur source {}", clientId, sourceCollecteurId);
                    continue;
                }

                CompteClient compteClient = compteClientRepository.findByClient(client)
                        .orElseThrow(() -> new CompteNotFoundException("Compte client non trouvé pour le client: " + clientId));

                // Transférer le client vers le nouveau collecteur
                client.setCollecteur(targetCollecteur);
                clientRepository.save(client);

                // Si les agences sont différentes, gérer le transfert inter-agences
                if (!isSameAgence) {
                    handleInterAgencyTransfer(compteClient, sourceCollecteur, targetCollecteur);
                }

                successCount++;
                log.info("Transfert réussi du compte client {} du collecteur {} vers {}",
                        clientId, sourceCollecteurId, targetCollecteurId);

            } catch (Exception e) {
                log.error("Erreur lors du transfert du client {}: {}", clientId, e.getMessage(), e);
            }
        }

        log.info("Fin du transfert: {} comptes sur {} transférés avec succès",
                successCount, clientIds.size());

        return successCount;
    }

    /**
     * Gère le transfert d'un compte entre deux agences différentes.
     * Crée les mouvements comptables nécessaires pour maintenir l'équilibre des comptes.
     */
    private void handleInterAgencyTransfer(
            CompteClient compteClient,
            Collecteur sourceCollecteur,
            Collecteur targetCollecteur) {

        log.info("Transfert inter-agences du compte {} de l'agence {} vers l'agence {}",
                compteClient.getId(),
                sourceCollecteur.getAgence().getId(),
                targetCollecteur.getAgence().getId());

        double solde = compteClient.getSolde();
        double commissions = getPendingCommissions(compteClient.getId(), sourceCollecteur.getId());
        double soldeToTransfer = solde - commissions;

        if (soldeToTransfer <= 0) {
            log.info("Aucun solde à transférer après déduction des commissions");
            return;
        }

        // 1. Obtenir les comptes de liaison des deux agences
        CompteLiaison sourceAgenceLiaison = compteLiaisonRepository
                .findByAgenceAndTypeCompte(sourceCollecteur.getAgence(), "LIAISON")
                .orElseThrow(() -> new CompteNotFoundException("Compte liaison agence source non trouvé"));

        CompteLiaison targetAgenceLiaison = compteLiaisonRepository
                .findByAgenceAndTypeCompte(targetCollecteur.getAgence(), "LIAISON")
                .orElseThrow(() -> new CompteNotFoundException("Compte liaison agence cible non trouvé"));

        // 2. Créer un mouvement de transfert entre les comptes de liaison
        Mouvement transferMouvement = Mouvement.builder()
                .compteSource(sourceAgenceLiaison)
                .compteDestination(targetAgenceLiaison)
                .montant(soldeToTransfer)
                .sens("TRANSFERT")
                .libelle("Transfert inter-agences du compte client " + compteClient.getClient().getId())
                .dateOperation(LocalDateTime.now())
                .build();

        mouvementServiceImpl.effectuerMouvement(transferMouvement);

        log.info("Transfert inter-agences effectué: {} FCFA transférés de l'agence {} vers {}",
                soldeToTransfer,
                sourceCollecteur.getAgence().getId(),
                targetCollecteur.getAgence().getId());
    }

    /**
     * Calcule les commissions en attente pour un compte client
     */
    public double getPendingCommissions(Long compteId, Long collecteurId) {
        LocalDateTime dateLimit = LocalDateTime.now().minusDays(30);

        try {
            return mouvementRepository.calculatePendingCommissions(compteId, collecteurId, dateLimit);
        } catch (Exception e) {
            log.error("Erreur lors du calcul des commissions en attente pour compte {} et collecteur {}: {}",
                    compteId, collecteurId, e.getMessage());
            return 0.0; // Valeur par défaut en cas d'erreur
        }
    }
    /**
     * Récupère les détails d'un transfert spécifique à partir de l'historique des transferts.
     *
     * @param transferId L'identifiant du transfert
     * @return Un DTO contenant les détails complets du transfert
     * @throws ResourceNotFoundException si le transfert n'est pas trouvé
     */
    @Transactional(readOnly = true)
    public TransferDetailDTO getTransferDetails(Long transferId) {
        log.info("Récupération des détails du transfert: {}", transferId);

        // Récupérer l'enregistrement de transfert principal
        TransfertCompte transfert = transfertCompteRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfert non trouvé avec l'ID: " + transferId));

        // Initialiser le DTO de réponse
        TransferDetailDTO detailDTO = new TransferDetailDTO();
        detailDTO.setTransferId(transfert.getId());
        detailDTO.setDateTransfert(transfert.getDateTransfert());
        detailDTO.setNombreComptes(transfert.getNombreComptes());
        detailDTO.setMontantTotal(transfert.getMontantTotal());
        detailDTO.setMontantCommissions(transfert.getMontantCommissions());

        // Récupérer les informations des collecteurs
        Collecteur sourceCollecteur = collecteurRepository.findById(transfert.getSourceCollecteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur source non trouvé"));
        Collecteur targetCollecteur = collecteurRepository.findById(transfert.getTargetCollecteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur cible non trouvé"));

        detailDTO.setSourceCollecteurId(sourceCollecteur.getId());
        detailDTO.setSourceCollecteurNom(sourceCollecteur.getNom() + " " + sourceCollecteur.getPrenom());
        detailDTO.setTargetCollecteurId(targetCollecteur.getId());
        detailDTO.setTargetCollecteurNom(targetCollecteur.getNom() + " " + targetCollecteur.getPrenom());

        // Déterminer si c'est un transfert inter-agences
        boolean interAgenceTransfert = !sourceCollecteur.getAgence().getId().equals(targetCollecteur.getAgence().getId());
        detailDTO.setInterAgenceTransfert(interAgenceTransfert);

        // Récupérer les clients transférés
        List<Client> clientsTransferes = clientRepository.findByTransfertId(transferId);
        detailDTO.setClientsTransferes(clientsTransferes.stream()
                .map(client -> new ClientTransfereDTO(
                        client.getId(),
                        client.getNom(),
                        client.getPrenom(),
                        client.getNumeroCni()
                ))
                .collect(Collectors.toList()));

        // Récupérer tous les mouvements générés lors du transfert
        List<Mouvement> mouvements = mouvementRepository.findByTransfertId(transferId);

        // Mapper les mouvements en DTOs
        List<TransferMovementDTO> mouvementDTOs = mouvements.stream()
                .map(mouvement -> {
                    TransferMovementDTO dto = new TransferMovementDTO();
                    dto.setMouvementId(mouvement.getId());
                    dto.setLibelle(mouvement.getLibelle());
                    dto.setSens(mouvement.getSens());
                    dto.setMontant(mouvement.getMontant());

                    // Récupérer les informations des comptes de manière sécurisée
                    if (mouvement.getCompteSource() != null) {
                        dto.setCompteSource(formatCompteInfo(mouvement.getCompteSource()));
                    }

                    if (mouvement.getCompteDestination() != null) {
                        dto.setCompteDestination(formatCompteInfo(mouvement.getCompteDestination()));
                    }

                    dto.setDateOperation(mouvement.getDateOperation());
                    return dto;
                })
                .collect(Collectors.toList());

        detailDTO.setMouvements(mouvementDTOs);

        // Récupérer les événements spécifiques du transfert (audit trail)
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId("TRANSFER", transferId);
        List<TransferEventDTO> events = auditLogs.stream()
                .map(log -> new TransferEventDTO(
                        log.getTimestamp(),
                        log.getAction(),
                        log.getUsername(),
                        log.getDetails()
                ))
                .sorted(Comparator.comparing(TransferEventDTO::getTimestamp))
                .collect(Collectors.toList());

        detailDTO.setEvents(events);

        // Pour les transferts inter-agences, ajouter des informations supplémentaires
        if (interAgenceTransfert) {
            // Récupérer les informations des comptes de liaison
            CompteLiaison sourceAgenceLiaison = compteLiaisonRepository
                    .findByAgenceId(sourceCollecteur.getAgence().getId())
                    .orElse(null);

            CompteLiaison targetAgenceLiaison = compteLiaisonRepository
                    .findByAgenceId(targetCollecteur.getAgence().getId())
                    .orElse(null);

            if (sourceAgenceLiaison != null && targetAgenceLiaison != null) {
                InterAgencyTransferDTO interAgencyInfo = new InterAgencyTransferDTO();
                interAgencyInfo.setSourceAgenceId(sourceCollecteur.getAgence().getId());
                interAgencyInfo.setSourceAgenceNom(sourceCollecteur.getAgence().getNomAgence());
                interAgencyInfo.setTargetAgenceId(targetCollecteur.getAgence().getId());
                interAgencyInfo.setTargetAgenceNom(targetCollecteur.getAgence().getNomAgence());
                interAgencyInfo.setSourceLiaisonCompte(formatCompteInfo(sourceAgenceLiaison));
                interAgencyInfo.setTargetLiaisonCompte(formatCompteInfo(targetAgenceLiaison));

                // Trouver le mouvement spécifique entre les comptes de liaison
                Optional<Mouvement> liaisonMouvement = mouvements.stream()
                        .filter(m -> m.getSens().equals("TRANSFERT") &&
                                m.getCompteSource().getId().equals(sourceAgenceLiaison.getId()) &&
                                m.getCompteDestination().getId().equals(targetAgenceLiaison.getId()))
                        .findFirst();

                liaisonMouvement.ifPresent(m ->
                        interAgencyInfo.setMontantTransfere(m.getMontant()));

                detailDTO.setInterAgencyInfo(interAgencyInfo);
            }
        }

        // Calculer des statistiques supplémentaires
        if (!mouvements.isEmpty()) {
            DoubleSummaryStatistics stats = mouvements.stream()
                    .mapToDouble(Mouvement::getMontant)
                    .summaryStatistics();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("montantMoyen", stats.getAverage());
            statistics.put("montantMaximum", stats.getMax());
            statistics.put("montantMinimum", stats.getMin());
            statistics.put("nombreMouvements", stats.getCount());

            detailDTO.setStatistics(statistics);
        }

        // Journaliser la consultation des détails du transfert
        log.info("Détails du transfert {} récupérés avec succès. {} mouvements trouvés.",
                transferId, mouvementDTOs.size());

        return detailDTO;
    }

    /**
     * Formate les informations d'un compte de manière sécurisée et lisible
     */
    private String formatCompteInfo(Compte compte) {
        if (compte == null) return null;

        StringBuilder info = new StringBuilder();
        info.append(compte.getNumeroCompte())
                .append(" (")
                .append(compte.getTypeCompte());

        // Ajouter des informations spécifiques selon le type de compte
        if (compte instanceof CompteClient) {
            Client client = ((CompteClient) compte).getClient();
            if (client != null) {
                info.append(" - Client: ")
                        .append(client.getNom())
                        .append(" ")
                        .append(client.getPrenom());
            }
        } else if (compte instanceof CompteCollecteur) {
            Collecteur collecteur = ((CompteCollecteur) compte).getCollecteur();
            if (collecteur != null) {
                info.append(" - Collecteur: ")
                        .append(collecteur.getNom())
                        .append(" ")
                        .append(collecteur.getPrenom());
            }
        } else if (compte instanceof CompteLiaison) {
            Agence agence = ((CompteLiaison) compte).getAgence();
            if (agence != null) {
                info.append(" - Agence: ")
                        .append(agence.getNomAgence());
            }
        }

        info.append(")");
        return info.toString();
    }
}
package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.services.interfaces.JournalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VersementCollecteurService {

    private final VersementCollecteurRepository versementRepository;
    private final CollecteurRepository collecteurRepository;
    private final JournalService journalService;
    private final CompteService compteService;
    private final CompteServiceRepository compteServiceRepository;
    private final CompteManquantRepository compteManquantRepository;
    private final CompteAttenteRepository compteAttenteRepository;
    private final CompteRemunerationRepository compteRemunerationRepository;
    private final MouvementRepository mouvementRepository;
    private final SecurityService securityService;

    /**
     * 📋 Obtenir un aperçu avant clôture
     */
    @Transactional(readOnly = true)
    public ClotureJournalPreviewDTO getCloturePreview(Long collecteurId, LocalDate date) {
        log.info("📋 Génération aperçu clôture pour collecteur {} à la date {}", collecteurId, date);

        try {
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

            // Récupérer ou créer le journal
            Journal journal = null;
            boolean journalExiste = false;
            try {
                journal = journalService.getOrCreateJournalDuJour(collecteurId, date);
                journalExiste = true;
            } catch (Exception e) {
                log.warn("Aucun journal trouvé pour la date {}", date);
            }

            // Récupérer le solde du compte service
            Double soldeCompteService = 0.0;
            try {
                CompteServiceEntity compteService = compteServiceRepository.findFirstByCollecteur(collecteur)
                        .orElse(null);
                if (compteService != null) {
                    soldeCompteService = compteService.getSolde();
                }
            } catch (Exception e) {
                log.warn("Erreur récupération compte service: {}", e.getMessage());
            }

            // Récupérer les opérations du jour
            List<ClotureJournalPreviewDTO.OperationJournalierDTO> operations = List.of();
            Integer nombreOperations = 0;
            Double totalEpargne = 0.0;
            Double totalRetraits = 0.0;

            if (journal != null) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);

                List<Mouvement> mouvements = mouvementRepository.findByCollecteurAndDay(
                        collecteurId, startOfDay, endOfDay);

                operations = mouvements.stream()
                        .map(m -> ClotureJournalPreviewDTO.OperationJournalierDTO.builder()
                                .id(m.getId())
                                .type(m.getTypeMouvement())
                                .montant(m.getMontant())
                                .clientNom(m.getClient() != null ? m.getClient().getNom() : "N/A")
                                .clientPrenom(m.getClient() != null ? m.getClient().getPrenom() : "N/A")
                                .dateOperation(m.getDateOperation())
                                .build())
                        .collect(Collectors.toList());

                nombreOperations = operations.size();
                totalEpargne = mouvements.stream()
                        .filter(m -> "epargne".equals(m.getSens()) || "EPARGNE".equals(m.getTypeMouvement()))
                        .mapToDouble(Mouvement::getMontant)
                        .sum();

                totalRetraits = mouvements.stream()
                        .filter(m -> "retrait".equals(m.getSens()) || "RETRAIT".equals(m.getTypeMouvement()))
                        .mapToDouble(Mouvement::getMontant)
                        .sum();
            }

            // Récupérer les soldes des comptes annexes
            Double soldeCompteManquant = getCompteManquantSolde(collecteur);
            Double soldeCompteAttente = getCompteAttenteSolde(collecteur);

            return ClotureJournalPreviewDTO.builder()
                    .collecteurId(collecteurId)
                    .collecteurNom(collecteur.getNom() + " " + collecteur.getPrenom())
                    .date(date)
                    .journalId(journal != null ? journal.getId() : null)
                    .referenceJournal(journal != null ? journal.getReference() : null)
                    .journalExiste(journalExiste)
                    .dejaClôture(journal != null && journal.isEstCloture())
                    .soldeCompteService(soldeCompteService)
                    .totalEpargne(totalEpargne)
                    .totalRetraits(totalRetraits)
                    .soldeNet(totalEpargne - totalRetraits)
                    .nombreOperations(nombreOperations)
                    .operations(operations)
                    .soldeCompteManquant(soldeCompteManquant)
                    .soldeCompteAttente(soldeCompteAttente)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur génération aperçu clôture: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors de la génération de l'aperçu: " + e.getMessage());
        }
    }

    /**
     * 💰 Effectuer le versement et clôturer le journal
     */
    @Transactional
    public VersementCollecteurResponseDTO effectuerVersementEtCloture(VersementCollecteurRequestDTO request) {
        log.info("💰 Début versement collecteur {} pour date {} - Montant: {}",
                request.getCollecteurId(), request.getDate(), request.getMontantVerse());

        try {
            // 1. Validations
            Collecteur collecteur = collecteurRepository.findById(request.getCollecteurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // Vérifier autorisation
            if (!securityService.hasPermissionForCollecteur(request.getCollecteurId())) {
                throw new BusinessException("Accès non autorisé à ce collecteur");
            }

            // Vérifier si versement déjà effectué pour cette date
            if (versementRepository.findByCollecteurIdAndDateVersement(
                    request.getCollecteurId(), request.getDate()).isPresent()) {
                throw new BusinessException("Un versement a déjà été effectué pour cette date");
            }

            // 2. Récupérer le journal et les comptes
            Journal journal = journalService.getOrCreateJournalDuJour(
                    request.getCollecteurId(), request.getDate());

            if (journal.isEstCloture()) {
                throw new BusinessException("Le journal est déjà clôturé");
            }

            CompteServiceEntity compteService = compteServiceRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte service non trouvé"));

            Double montantCollecte = compteService.getSolde();

            // 3. Créer le versement
            VersementCollecteur versement = VersementCollecteur.builder()
                    .collecteur(collecteur)
                    .journal(journal)
                    .dateVersement(request.getDate())
                    .montantCollecte(montantCollecte)
                    .montantVerse(request.getMontantVerse())
                    .statut(VersementCollecteur.StatutVersement.VALIDE)
                    .commentaire(request.getCommentaire())
                    .creePar(securityService.getCurrentUsername())
                    .build();

            versement = versementRepository.save(versement);

            // 4. Traiter la différence (manquant ou excédent)
            if (versement.hasManquant()) {
                traiterManquant(collecteur, versement.getManquant(), journal);
                log.info("💸 Manquant de {} FCFA transféré vers compte manquant", versement.getManquant());
            } else if (versement.hasExcedent()) {
                traiterExcedent(collecteur, versement.getExcedent(), journal);
                log.info("💰 Excédent de {} FCFA transféré vers compte attente", versement.getExcedent());
            }

            // 5. Remettre le compte service à zéro
            compteService.setSolde(0.0);
            compteServiceRepository.save(compteService);

            // 6. Clôturer le journal
            journal = journalService.cloturerJournalDuJour(request.getCollecteurId(), request.getDate());

            log.info("✅ Versement et clôture effectués avec succès - ID: {}", versement.getId());

            return mapToResponseDTO(versement);

        } catch (Exception e) {
            log.error("❌ Erreur lors du versement: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors du versement: " + e.getMessage());
        }
    }

    /**
     * 💸 Traiter un manquant (transfert vers compte manquant)
     */
    private void traiterManquant(Collecteur collecteur, Double montantManquant, Journal journal) {
        log.info("💸 Traitement manquant de {} FCFA pour collecteur {}", montantManquant, collecteur.getId());

        try {
            CompteManquant compteManquant = compteManquantRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte manquant non trouvé"));

            // Augmenter le solde du compte manquant (dette du collecteur)
            compteManquant.setSolde(compteManquant.getSolde() + montantManquant);
            compteManquantRepository.save(compteManquant);

            // Créer mouvement de régularisation
            Mouvement mouvementManquant = new Mouvement();
            mouvementManquant.setMontant(montantManquant);
            mouvementManquant.setLibelle("Manquant collecteur - Versement insuffisant");
            mouvementManquant.setSens("debit");
            mouvementManquant.setTypeMouvement("MANQUANT");
            mouvementManquant.setDateOperation(LocalDateTime.now());
            mouvementManquant.setCollecteur(collecteur);
            mouvementManquant.setJournal(journal);

            mouvementRepository.save(mouvementManquant);

            log.info("✅ Manquant traité: {} FCFA ajouté au compte manquant", montantManquant);

        } catch (Exception e) {
            log.error("❌ Erreur traitement manquant: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors du traitement du manquant: " + e.getMessage());
        }
    }

    /**
     * 💰 Traiter un excédent (transfert vers compte attente)
     */
    private void traiterExcedent(Collecteur collecteur, Double montantExcedent, Journal journal) {
        log.info("💰 Traitement excédent de {} FCFA pour collecteur {}", montantExcedent, collecteur.getId());

        try {
            CompteAttente compteAttente = compteAttenteRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte attente non trouvé"));

            // Augmenter le solde du compte attente (créance du collecteur)
            compteAttente.setSolde(compteAttente.getSolde() + montantExcedent);
            compteAttenteRepository.save(compteAttente);

            // Créer mouvement de régularisation
            Mouvement mouvementExcedent = new Mouvement();
            mouvementExcedent.setMontant(montantExcedent);
            mouvementExcedent.setLibelle("Excédent collecteur - Versement supérieur");
            mouvementExcedent.setSens("credit");
            mouvementExcedent.setTypeMouvement("EXCEDENT");
            mouvementExcedent.setDateOperation(LocalDateTime.now());
            mouvementExcedent.setCollecteur(collecteur);
            mouvementExcedent.setJournal(journal);

            mouvementRepository.save(mouvementExcedent);

            log.info("✅ Excédent traité: {} FCFA ajouté au compte attente", montantExcedent);

        } catch (Exception e) {
            log.error("❌ Erreur traitement excédent: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors du traitement de l'excédent: " + e.getMessage());
        }
    }

    /**
     * 📊 Récupérer les comptes d'un collecteur
     */
    @Transactional(readOnly = true)
    public CollecteurComptesDTO getCollecteurComptes(Long collecteurId) {
        log.info("📊 Récupération comptes collecteur: {}", collecteurId);

        try {
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // Récupérer tous les comptes
            CompteServiceEntity compteService = compteServiceRepository.findFirstByCollecteur(collecteur).orElse(null);
            CompteManquant compteManquant = compteManquantRepository.findFirstByCollecteur(collecteur).orElse(null);
            CompteAttente compteAttente = compteAttenteRepository.findFirstByCollecteur(collecteur).orElse(null);
            CompteRemuneration compteRemuneration = compteRemunerationRepository.findFirstByCollecteur(collecteur).orElse(null);

            // Calculer totaux
            Double totalCreances = (compteManquant != null ? compteManquant.getSolde() : 0.0);
            Double totalAvoirs = (compteAttente != null ? compteAttente.getSolde() : 0.0) +
                    (compteRemuneration != null ? compteRemuneration.getSolde() : 0.0);

            return CollecteurComptesDTO.builder()
                    .collecteurId(collecteurId)
                    .collecteurNom(collecteur.getNom() + " " + collecteur.getPrenom())
                    .compteServiceId(compteService != null ? compteService.getId() : null)
                    .compteServiceNumero(compteService != null ? compteService.getNumeroCompte() : null)
                    .compteServiceSolde(compteService != null ? compteService.getSolde() : 0.0)
                    .compteManquantId(compteManquant != null ? compteManquant.getId() : null)
                    .compteManquantNumero(compteManquant != null ? compteManquant.getNumeroCompte() : null)
                    .compteManquantSolde(compteManquant != null ? compteManquant.getSolde() : 0.0)
                    .compteAttenteId(compteAttente != null ? compteAttente.getId() : null)
                    .compteAttenteNumero(compteAttente != null ? compteAttente.getNumeroCompte() : null)
                    .compteAttenteSolde(compteAttente != null ? compteAttente.getSolde() : 0.0)
                    .compteRemunerationId(compteRemuneration != null ? compteRemuneration.getId() : null)
                    .compteRemunerationNumero(compteRemuneration != null ? compteRemuneration.getNumeroCompte() : null)
                    .compteRemunerationSolde(compteRemuneration != null ? compteRemuneration.getSolde() : 0.0)
                    .totalCreances(totalCreances)
                    .totalAvoirs(totalAvoirs)
                    .soldeNet(totalAvoirs - totalCreances)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur récupération comptes: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors de la récupération des comptes: " + e.getMessage());
        }
    }

    // Méthodes utilitaires
    private Double getCompteManquantSolde(Collecteur collecteur) {
        return compteManquantRepository.findFirstByCollecteur(collecteur)
                .map(CompteManquant::getSolde)
                .orElse(0.0);
    }

    private Double getCompteAttenteSolde(Collecteur collecteur) {
        return compteAttenteRepository.findFirstByCollecteur(collecteur)
                .map(CompteAttente::getSolde)
                .orElse(0.0);
    }

    private VersementCollecteurResponseDTO mapToResponseDTO(VersementCollecteur versement) {
        return VersementCollecteurResponseDTO.builder()
                .id(versement.getId())
                .collecteurId(versement.getCollecteur().getId())
                .collecteurNom(versement.getCollecteur().getNom() + " " + versement.getCollecteur().getPrenom())
                .date(versement.getDateVersement())
                .montantCollecte(versement.getMontantCollecte())
                .montantVerse(versement.getMontantVerse())
                .excedent(versement.getExcedent())
                .manquant(versement.getManquant())
                .statut(versement.getStatut().name())
                .commentaire(versement.getCommentaire())
                .dateVersement(versement.getDateCreation())
                .numeroAutorisation(versement.getNumeroAutorisation())
                .journalId(versement.getJournal().getId())
                .build();
    }
}
package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ClotureJournalPreviewDTO;
import org.example.collectfocep.dto.VersementCollecteurRequestDTO;
import org.example.collectfocep.dto.VersementCollecteurResponseDTO;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.DateTimeService;
import org.example.collectfocep.services.interfaces.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 💰 Service de versement des collecteurs - VERSION FINALE
 * ✅ Correction du calcul des cas de versement (valeur absolue)
 * ✅ Utilise MouvementServiceImpl.effectuerMouvementVersement()
 * ✅ Gestion correcte des comptes service négatifs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersementCollecteurService {

    private final VersementCollecteurRepository versementRepository;
    private final CollecteurRepository collecteurRepository;
    private final CompteServiceRepository compteServiceRepository;
    private final CompteManquantRepository compteManquantRepository;
    private final CompteAgenceService compteAgenceService;
    private final JournalService journalService;
    private final MouvementRepository mouvementRepository;
    private final TraceabiliteCollecteQuotidienneRepository traceabiliteRepository;
    private final SecurityService securityService;
    private final DateTimeService dateTimeService;

    @Autowired
    private MouvementServiceImpl mouvementServiceImpl;

    /**
     * 📊 Générer un aperçu de clôture - INCHANGÉ
     */
    @Transactional(readOnly = true)
    public ClotureJournalPreviewDTO getCloturePreview(Long collecteurId, LocalDate date) {
        log.info("📊 Génération aperçu clôture - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            Journal journal = journalService.getOrCreateJournalDuJour(collecteurId, date);
            boolean journalExiste = journal != null;

            if (journalExiste && journal.isEstCloture()) {
                log.info("ℹ️ Journal déjà clôturé - Affichage en mode lecture seule: ID={}", journal.getId());
            }

            CompteServiceEntity compteService = compteServiceRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte service non trouvé"));

            CompteManquant compteManquant = compteManquantRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte manquant non trouvé"));

            LocalDateTime startOfDay = dateTimeService.toStartOfDay(date);
            LocalDateTime endOfDay = dateTimeService.toEndOfDay(date);

            List<Mouvement> mouvements = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfDay, endOfDay);

            Double totalEpargne = mouvements.stream()
                    .filter(m -> "epargne".equalsIgnoreCase(m.getTypeMouvement()) ||
                            "EPARGNE".equalsIgnoreCase(m.getSens()))
                    .mapToDouble(Mouvement::getMontant)
                    .sum();

            Double totalRetraits = mouvements.stream()
                    .filter(m -> "retrait".equalsIgnoreCase(m.getTypeMouvement()) ||
                            "RETRAIT".equalsIgnoreCase(m.getSens()))
                    .mapToDouble(Mouvement::getMontant)
                    .sum();

            List<ClotureJournalPreviewDTO.OperationJournalierDTO> operations = mouvements.stream()
                    .map(this::convertToOperationDTO)
                    .collect(Collectors.toList());

            return ClotureJournalPreviewDTO.builder()
                    .collecteurId(collecteurId)
                    .collecteurNom(collecteur.getNom() + " " + collecteur.getPrenom())
                    .date(date)
                    .journalId(journal != null ? journal.getId() : null)
                    .referenceJournal(journal != null ? journal.getReference() : null)
                    .journalExiste(journalExiste)
                    .dejaClôture(journal != null && journal.isEstCloture())
                    .soldeCompteService(compteService.getSolde())
                    .soldeCompteManquant(compteManquant.getSolde())
                    .totalEpargne(totalEpargne)
                    .totalRetraits(totalRetraits)
                    .soldeNet(totalEpargne - totalRetraits)
                    .nombreOperations(operations.size())
                    .operations(operations)
                    .soldeCompteAttente(0.0)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur génération aperçu clôture: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors de la génération de l'aperçu: " + e.getMessage());
        }
    }

    /**
     * 💰 Effectuer le versement et clôturer le journal - VERSION FINALE
     */
    @Transactional
    public VersementCollecteurResponseDTO effectuerVersementEtCloture(VersementCollecteurRequestDTO request) {
        log.info("💰 DÉBUT VERSEMENT - VERSION FINALE - Collecteur: {}, Date: {}, Montant versé: {}",
                request.getCollecteurId(), request.getDate(), request.getMontantVerse());

        try {
            validateVersementRequest(request);

            Collecteur collecteur = collecteurRepository.findById(request.getCollecteurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            Journal journal = journalService.getOrCreateJournalDuJour(
                    request.getCollecteurId(), request.getDate());

            if (journal.isEstCloture()) {
                throw new BusinessException("Le journal est déjà clôturé");
            }

            CompteServiceEntity compteService = compteServiceRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte service non trouvé"));

            CompteManquant compteManquant = compteManquantRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte manquant non trouvé"));

            CompteAgence compteAgence = compteAgenceService.ensureCompteAgenceExists(collecteur.getAgence());

            // 🔥 CORRECTION CRITIQUE : Utiliser la valeur absolue du solde du compte service
            BigDecimal montantDu = BigDecimal.valueOf(Math.abs(compteService.getSolde()));
            BigDecimal montantVerse = BigDecimal.valueOf(request.getMontantVerse());

            log.info("🔧 CORRECTION APPLIQUÉE - Montant dû: {} FCFA, Montant versé: {} FCFA",
                    montantDu, montantVerse);

            TraceabiliteCollecteQuotidienne trace = creerTraceAvantCloture(
                    journal, compteService, compteManquant, request.getDate());

            // ✅ EXÉCUTION DE LA LOGIQUE MÉTIER AVEC effectuerMouvementVersement()
            VersementCollecteur versement = executerLogiqueMtierFinale(
                    collecteur, journal, compteService, compteManquant, compteAgence,
                    montantDu, montantVerse, request);

            journal.cloturerJournal();
            journalService.saveJournal(journal);

            trace.marquerCommeClôturee();
            traceabiliteRepository.save(trace);

            log.info("✅ VERSEMENT TERMINÉ - VERSION FINALE - ID: {}, Cas: {}",
                    versement.getId(), determinerCasVersement(montantDu, montantVerse));

            return mapToResponseDTO(versement);

        } catch (Exception e) {
            log.error("❌ Erreur lors du versement version finale: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors du versement: " + e.getMessage());
        }
    }

    /**
     * 🎯 LOGIQUE MÉTIER FINALE - Utilise effectuerMouvementVersement()
     */
    private VersementCollecteur executerLogiqueMtierFinale(
            Collecteur collecteur,
            Journal journal,
            CompteServiceEntity compteService,
            CompteManquant compteManquant,
            CompteAgence compteAgence,
            BigDecimal montantDu,
            BigDecimal montantVerse,
            VersementCollecteurRequestDTO request) {

        String cas = determinerCasVersement(montantDu, montantVerse);
        log.info("🎯 Exécution logique métier FINALE - Cas détecté: {}", cas);

        VersementCollecteur versement = VersementCollecteur.builder()
                .collecteur(collecteur)
                .journal(journal)
                .dateVersement(request.getDate())
                .montantCollecte(montantDu.doubleValue())
                .montantVerse(montantVerse.doubleValue())
                .statut(VersementCollecteur.StatutVersement.VALIDE)
                .commentaire(request.getCommentaire())
                .creePar(securityService.getCurrentUsername())
                .build();

        versement = versementRepository.save(versement);

        // ✅ EXÉCUTER LA LOGIQUE AVEC effectuerMouvementVersement()
        switch (cas) {
            case "NORMAL":
                executerCasNormalFinal(compteService, compteAgence, montantVerse, journal);
                break;
            case "EXCEDENT":
                executerCasExcedentFinal(compteService, compteManquant, compteAgence,
                        montantDu, montantVerse, journal);
                break;
            case "MANQUANT":
                executerCasManquantFinal(compteService, compteManquant, compteAgence,
                        montantDu, montantVerse, journal);
                break;
            default:
                throw new BusinessException("Cas de versement non reconnu: " + cas);
        }

        return versement;
    }

    /**
     * ✅ CAS NORMAL - VERSION FINALE avec effectuerMouvementVersement()
     */
    private void executerCasNormalFinal(
            CompteServiceEntity compteService,
            CompteAgence compteAgence,
            BigDecimal montant,
            Journal journal) {

        log.info("✅ Exécution CAS NORMAL - VERSION FINALE - Montant: {} FCFA", montant);

        // ✅ UTILISER effectuerMouvementVersement() avec sens spécifique
        Mouvement mouvement = Mouvement.builder()
                .compteSource(compteService)
                .compteDestination(compteAgence)
                .montant(montant.doubleValue())
                .libelle("Versement normal - Clôture journal")
                .typeMouvement("VERSEMENT_NORMAL")
                .sens("versement_normal")  // ✅ Sens spécifique pour versement
                .dateOperation(dateTimeService.getCurrentDateTime())
                .journal(journal)
                .build();

        // ✅ UTILISER LA NOUVELLE MÉTHODE SPÉCIFIQUE
        mouvementServiceImpl.effectuerMouvementVersement(mouvement);

        log.info("✅ CAS NORMAL terminé avec effectuerMouvementVersement()");
    }

    /**
     * 📈 CAS EXCÉDENT - VERSION FINALE avec effectuerMouvementVersement()
     */
    private void executerCasExcedentFinal(
            CompteServiceEntity compteService,
            CompteManquant compteManquant,
            CompteAgence compteAgence,
            BigDecimal montantDu,
            BigDecimal montantVerse,
            Journal journal) {

        BigDecimal excedent = montantVerse.subtract(montantDu);
        log.info("📈 Exécution CAS EXCÉDENT - VERSION FINALE - Dû: {}, Versé: {}, Excédent: {}",
                montantDu, montantVerse, excedent);

        // ✅ MOUVEMENT 1: Enregistrer l'excédent dans le compte manquant
        Mouvement mouvement1 = Mouvement.builder()
                .compteSource(compteService)
                .compteDestination(compteManquant)
                .montant(excedent.doubleValue())
                .libelle("Excédent collecteur - Crédit compte manquant")
                .typeMouvement("EXCEDENT_AJUSTEMENT")
                .sens("ajustement_excedent")
                .dateOperation(dateTimeService.getCurrentDateTime())
                .journal(journal)
                .build();

        mouvementServiceImpl.effectuerMouvementVersement(mouvement1);

        // ✅ MOUVEMENT 2: Versement du montant dû à l'agence
        Mouvement mouvement2 = Mouvement.builder()
                .compteSource(compteService)
                .compteDestination(compteAgence)
                .montant(montantDu.doubleValue())
                .libelle("Versement avec excédent - Clôture journal")
                .typeMouvement("VERSEMENT_EXCEDENT")
                .sens("versement_excedent")
                .dateOperation(dateTimeService.getCurrentDateTime())
                .journal(journal)
                .build();

        mouvementServiceImpl.effectuerMouvementVersement(mouvement2);

        log.info("✅ CAS EXCÉDENT terminé avec effectuerMouvementVersement()");
    }

    /**
     * 📉 CAS MANQUANT - VERSION FINALE avec effectuerMouvementVersement()
     */
    private void executerCasManquantFinal(
            CompteServiceEntity compteService,
            CompteManquant compteManquant,
            CompteAgence compteAgence,
            BigDecimal montantDu,
            BigDecimal montantVerse,
            Journal journal) {

        BigDecimal manquant = montantDu.subtract(montantVerse);
        log.info("📉 Exécution CAS MANQUANT - VERSION FINALE - Dû: {}, Versé: {}, Manquant: {}",
                montantDu, montantVerse, manquant);

        // ✅ MOUVEMENT 1: Enregistrer le manquant (dette) dans le compte manquant
        Mouvement mouvement1 = Mouvement.builder()
                .compteSource(compteManquant)
                .compteDestination(compteService)
                .montant(manquant.doubleValue())
                .libelle("Manquant collecteur - Débit compte manquant")
                .typeMouvement("MANQUANT_DETTE")
                .sens("ajustement_manquant")
                .dateOperation(dateTimeService.getCurrentDateTime())
                .journal(journal)
                .build();

        mouvementServiceImpl.effectuerMouvementVersement(mouvement1);

        // ✅ MOUVEMENT 2: Versement du montant effectif à l'agence
        Mouvement mouvement2 = Mouvement.builder()
                .compteSource(compteService)
                .compteDestination(compteAgence)
                .montant(montantVerse.doubleValue())
                .libelle("Versement partiel avec manquant - Clôture journal")
                .typeMouvement("VERSEMENT_MANQUANT")
                .sens("versement_manquant")
                .dateOperation(dateTimeService.getCurrentDateTime())
                .journal(journal)
                .build();

        mouvementServiceImpl.effectuerMouvementVersement(mouvement2);

        log.info("✅ CAS MANQUANT terminé avec effectuerMouvementVersement()");
    }

    /**
     * ✅ MÉTHODE CORRIGÉE : Détermine le cas de versement avec logique correcte
     */
    private String determinerCasVersement(BigDecimal montantDu, BigDecimal montantVerse) {
        int comparison = montantVerse.compareTo(montantDu);

        log.info("🔍 Détermination du cas - Montant dû: {}, Montant versé: {}, Comparaison: {}",
                montantDu, montantVerse, comparison);

        if (comparison == 0) {
            log.info("✅ CAS DÉTECTÉ: NORMAL");
            return "NORMAL";
        } else if (comparison > 0) {
            log.info("✅ CAS DÉTECTÉ: EXCEDENT");
            return "EXCEDENT";
        } else {
            log.info("✅ CAS DÉTECTÉ: MANQUANT");
            return "MANQUANT";
        }
    }

    // === MÉTHODES UTILITAIRES INCHANGÉES ===

    private TraceabiliteCollecteQuotidienne creerTraceAvantCloture(
            Journal journal, CompteServiceEntity compteService, CompteManquant compteManquant, LocalDate date) {

        LocalDateTime startOfDay = dateTimeService.toStartOfDay(date);
        LocalDateTime endOfDay = dateTimeService.toEndOfDay(date);

        List<Mouvement> mouvements = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                journal.getCollecteur().getId(), startOfDay, endOfDay);

        BigDecimal totalEpargne = mouvements.stream()
                .filter(m -> "epargne".equalsIgnoreCase(m.getTypeMouvement()) ||
                        "EPARGNE".equalsIgnoreCase(m.getSens()))
                .map(m -> BigDecimal.valueOf(m.getMontant()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRetraits = mouvements.stream()
                .filter(m -> "retrait".equalsIgnoreCase(m.getTypeMouvement()) ||
                        "RETRAIT".equalsIgnoreCase(m.getSens()))
                .map(m -> BigDecimal.valueOf(m.getMontant()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TraceabiliteCollecteQuotidienne trace = TraceabiliteCollecteQuotidienne.creerDepuisJournal(
                journal,
                BigDecimal.valueOf(compteService.getSolde()),
                BigDecimal.valueOf(compteManquant.getSolde()),
                totalEpargne,
                totalRetraits,
                mouvements.size(),
                (int) mouvements.stream().map(m -> m.getClient()).filter(c -> c != null).distinct().count(),
                securityService.getCurrentUsername()
        );

        return traceabiliteRepository.save(trace);
    }

    private void validateVersementRequest(VersementCollecteurRequestDTO request) {
        if (request.getCollecteurId() == null) {
            throw new BusinessException("L'ID du collecteur est obligatoire");
        }
        if (request.getDate() == null) {
            throw new BusinessException("La date est obligatoire");
        }
        if (request.getMontantVerse() == null || request.getMontantVerse() < 0) {
            throw new BusinessException("Le montant versé doit être positif");
        }
        if (versementRepository.findByCollecteurIdAndDateVersement(
                request.getCollecteurId(), request.getDate()).isPresent()) {
            throw new BusinessException("Un versement a déjà été effectué pour cette date");
        }
    }

    private ClotureJournalPreviewDTO.OperationJournalierDTO convertToOperationDTO(Mouvement mouvement) {
        return ClotureJournalPreviewDTO.OperationJournalierDTO.builder()
                .id(mouvement.getId())
                .type(mouvement.getTypeMouvement())
                .montant(mouvement.getMontant())
                .clientNom(mouvement.getClient() != null ? mouvement.getClient().getNom() : "N/A")
                .clientPrenom(mouvement.getClient() != null ? mouvement.getClient().getPrenom() : "N/A")
                .dateOperation(mouvement.getDateOperation())
                .build();
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
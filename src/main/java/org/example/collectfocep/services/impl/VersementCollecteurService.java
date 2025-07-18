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
import org.example.collectfocep.services.impl.CompteAgenceService;
import org.example.collectfocep.services.impl.DateTimeServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 💰 Service de versement des collecteurs - VERSION CORRIGÉE
 * Utilise uniquement les classes et méthodes qui existent réellement
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

    /**
     * 📊 Générer un aperçu de clôture - CORRIGÉ
     */
    @Transactional(readOnly = true)
    public ClotureJournalPreviewDTO getCloturePreview(Long collecteurId, LocalDate date) {
        log.info("📊 Génération aperçu clôture - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            // 1. VALIDATIONS
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // 2. RÉCUPÉRATION DES DONNÉES
            Journal journal = journalService.getOrCreateJournalDuJour(collecteurId, date);
            boolean journalExiste = journal != null;

            if (journalExiste && journal.isEstCloture()) {
                throw new BusinessException("Le journal est déjà clôturé");
            }

            CompteServiceEntity compteService = compteServiceRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte service non trouvé"));

            CompteManquant compteManquant = compteManquantRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte manquant non trouvé"));

            // 3. CALCUL DES STATISTIQUES DU JOUR
            LocalDateTime startOfDay = dateTimeService.toStartOfDay(date);
            LocalDateTime endOfDay = dateTimeService.toEndOfDay(date);

            List<Mouvement> mouvements = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfDay, endOfDay);

            Double totalEpargne = mouvements.stream()
                    .filter(m -> "epargne".equalsIgnoreCase(m.getTypeMouvement()))
                    .mapToDouble(Mouvement::getMontant)
                    .sum();

            Double totalRetraits = mouvements.stream()
                    .filter(m -> "retrait".equalsIgnoreCase(m.getTypeMouvement()))
                    .mapToDouble(Mouvement::getMontant)
                    .sum();

            List<ClotureJournalPreviewDTO.OperationJournalierDTO> operations = mouvements.stream()
                    .map(this::convertToOperationDTO)
                    .collect(Collectors.toList());

            // 4. CONSTRUCTION DU DTO - UTILISE LA VRAIE CLASSE
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
     * 💰 Effectuer le versement et clôturer le journal - CORRIGÉ
     */
    @Transactional
    public VersementCollecteurResponseDTO effectuerVersementEtCloture(VersementCollecteurRequestDTO request) {
        log.info("💰 DÉBUT VERSEMENT - Collecteur: {}, Date: {}, Montant versé: {}",
                request.getCollecteurId(), request.getDate(), request.getMontantVerse());

        try {
            // 1. VALIDATIONS
            validateVersementRequest(request);

            // 2. RÉCUPÉRATION DES ENTITÉS
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

            // 3. CONVERSION EN BIGDECIMAL POUR PRÉCISION
            BigDecimal montantCollecte = BigDecimal.valueOf(compteService.getSolde());
            BigDecimal montantVerse = BigDecimal.valueOf(request.getMontantVerse());

            // 4. CRÉER LA TRACE AVANT MODIFICATIONS
            TraceabiliteCollecteQuotidienne trace = creerTraceAvantCloture(
                    journal, compteService, compteManquant, request.getDate());

            // 5. EXÉCUTION DE LA LOGIQUE MÉTIER
            VersementCollecteur versement = executerLogiqueMtier(
                    collecteur, journal, compteService, compteManquant, compteAgence,
                    montantCollecte, montantVerse, request);

            // 6. CLÔTURER LE JOURNAL - UTILISE LA VRAIE MÉTHODE
            journal.cloturerJournal();
            journalService.saveJournal(journal);

            // 7. FINALISER LA TRACE
            trace.marquerCommeClôturee();
            traceabiliteRepository.save(trace);

            log.info("✅ VERSEMENT TERMINÉ - ID: {}, Cas: {}",
                    versement.getId(), determinerCasVersement(montantCollecte, montantVerse));

            return mapToResponseDTO(versement);

        } catch (Exception e) {
            log.error("❌ Erreur lors du versement: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors du versement: " + e.getMessage());
        }
    }

    /**
     * 🎯 Exécute la logique métier selon les 3 cas - CORRIGÉ
     */
    private VersementCollecteur executerLogiqueMtier(
            Collecteur collecteur,
            Journal journal,
            CompteServiceEntity compteService,
            CompteManquant compteManquant,
            CompteAgence compteAgence,
            BigDecimal montantCollecte,
            BigDecimal montantVerse,
            VersementCollecteurRequestDTO request) {

        String cas = determinerCasVersement(montantCollecte, montantVerse);
        log.info("🎯 Exécution logique métier - Cas détecté: {}", cas);

        // Créer l'enregistrement de versement
        VersementCollecteur versement = VersementCollecteur.builder()
                .collecteur(collecteur)
                .journal(journal)
                .dateVersement(request.getDate())
                .montantCollecte(montantCollecte.doubleValue())
                .montantVerse(montantVerse.doubleValue())
                .statut(VersementCollecteur.StatutVersement.VALIDE)
                .commentaire(request.getCommentaire())
                .creePar(securityService.getCurrentUsername())
                .build();

        versement = versementRepository.save(versement);

        // Exécuter la logique selon le cas
        switch (cas) {
            case "NORMAL":
                executerCasNormal(compteService, compteAgence, montantVerse, journal);
                break;
            case "EXCEDENT":
                executerCasExcedent(compteService, compteManquant, compteAgence,
                        montantCollecte, montantVerse, journal);
                break;
            case "MANQUANT":
                executerCasManquant(compteService, compteManquant, compteAgence,
                        montantCollecte, montantVerse, journal);
                break;
            default:
                throw new BusinessException("Cas de versement non reconnu: " + cas);
        }

        return versement;
    }

    /**
     * ✅ CAS NORMAL - CORRIGÉ
     */
    private void executerCasNormal(CompteServiceEntity compteService, CompteAgence compteAgence,
                                   BigDecimal montant, Journal journal) {
        log.info("✅ Exécution CAS NORMAL - Montant: {} FCFA", montant);

        // Mouvement : Transfert du compte service vers compte agence
        Mouvement mouvement = creerMouvementCorrige(
                compteService, compteAgence, montant.doubleValue(),
                "Versement normal - Clôture journal", "VERSEMENT_NORMAL", journal);

        // Mise à jour des soldes
        compteService.setSolde(0.0);
        compteAgence.setSolde(compteAgence.getSolde() + montant.doubleValue());

        // Sauvegarder
        compteServiceRepository.save(compteService);
        compteAgenceService.crediterCompteAgence(compteAgence, montant.doubleValue());
        mouvementRepository.save(mouvement);

        log.info("✅ CAS NORMAL terminé - Compte service: 0, Compte agence: {}", compteAgence.getSolde());
    }

    /**
     * 📈 CAS EXCÉDENT - CORRIGÉ
     */
    private void executerCasExcedent(CompteServiceEntity compteService, CompteManquant compteManquant,
                                     CompteAgence compteAgence, BigDecimal montantCollecte,
                                     BigDecimal montantVerse, Journal journal) {
        BigDecimal constatExcedent = montantVerse.subtract(montantCollecte);
        log.info("📈 Exécution CAS EXCÉDENT - Collecté: {}, Versé: {}, Excédent: {}",
                montantCollecte, montantVerse, constatExcedent);

        // Mouvement 1: Enregistrer l'excédent
        Mouvement mouvement1 = creerMouvementCorrige(
                compteService, compteManquant, constatExcedent.doubleValue(),
                "Excédent collecteur - Ajustement", "EXCEDENT_AJUSTEMENT", journal);

        // Mouvement 2: Versement total à l'agence
        Mouvement mouvement2 = creerMouvementCorrige(
                compteService, compteAgence, montantVerse.doubleValue(),
                "Versement avec excédent - Clôture journal", "VERSEMENT_EXCEDENT", journal);

        // Mise à jour des soldes
        compteService.setSolde(0.0);
        compteManquant.setSolde(compteManquant.getSolde() + constatExcedent.doubleValue());
        compteAgence.setSolde(compteAgence.getSolde() + montantVerse.doubleValue());

        // Sauvegarder
        compteServiceRepository.save(compteService);
        compteManquantRepository.save(compteManquant);
        compteAgenceService.crediterCompteAgence(compteAgence, montantVerse.doubleValue());
        mouvementRepository.save(mouvement1);
        mouvementRepository.save(mouvement2);

        log.info("✅ CAS EXCÉDENT terminé - Service: 0, Manquant: {}, Agence: {}",
                compteManquant.getSolde(), compteAgence.getSolde());
    }

    /**
     * 📉 CAS MANQUANT - CORRIGÉ
     */
    private void executerCasManquant(CompteServiceEntity compteService, CompteManquant compteManquant,
                                     CompteAgence compteAgence, BigDecimal montantCollecte,
                                     BigDecimal montantVerse, Journal journal) {
        BigDecimal constatManquant = montantCollecte.subtract(montantVerse);
        log.info("📉 Exécution CAS MANQUANT - Collecté: {}, Versé: {}, Manquant: {}",
                montantCollecte, montantVerse, constatManquant);

        // Mouvement 1: Enregistrer le manquant (dette)
        Mouvement mouvement1 = creerMouvementCorrige(
                compteManquant, compteService, constatManquant.doubleValue(),
                "Manquant collecteur - Dette", "MANQUANT_DETTE", journal);

        // Mouvement 2: Versement effectif à l'agence
        Mouvement mouvement2 = creerMouvementCorrige(
                compteService, compteAgence, montantVerse.doubleValue(),
                "Versement avec manquant - Clôture journal", "VERSEMENT_MANQUANT", journal);

        // Mise à jour des soldes
        compteService.setSolde(0.0);
        compteManquant.setSolde(compteManquant.getSolde() - constatManquant.doubleValue());
        compteAgence.setSolde(compteAgence.getSolde() + montantVerse.doubleValue());

        // Sauvegarder
        compteServiceRepository.save(compteService);
        compteManquantRepository.save(compteManquant);
        compteAgenceService.crediterCompteAgence(compteAgence, montantVerse.doubleValue());
        mouvementRepository.save(mouvement1);
        mouvementRepository.save(mouvement2);

        log.info("✅ CAS MANQUANT terminé - Service: 0, Manquant: {}, Agence: {}",
                compteManquant.getSolde(), compteAgence.getSolde());
    }

    // === MÉTHODES UTILITAIRES CORRIGÉES ===

    /**
     * Crée un mouvement avec les vraies propriétés de l'entité Mouvement
     */
    private Mouvement creerMouvementCorrige(Compte source, Compte destination, Double montant,
                                            String description, String typeOperation, Journal journal) {
        return Mouvement.builder()
                .compteSource(source)
                .compteDestination(destination)
                .montant(montant)
                .libelle(description)
                .typeMouvement(typeOperation)
                .dateOperation(LocalDateTime.now())
                .journal(journal)
                .build();
    }

    private TraceabiliteCollecteQuotidienne creerTraceAvantCloture(
            Journal journal, CompteServiceEntity compteService, CompteManquant compteManquant, LocalDate date) {

        // Calculer les statistiques du jour
        LocalDateTime startOfDay = dateTimeService.toStartOfDay(date);
        LocalDateTime endOfDay = dateTimeService.toEndOfDay(date);

        List<Mouvement> mouvements = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                journal.getCollecteur().getId(), startOfDay, endOfDay);

        BigDecimal totalEpargne = mouvements.stream()
                .filter(m -> "epargne".equalsIgnoreCase(m.getTypeMouvement()))
                .map(m -> BigDecimal.valueOf(m.getMontant()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRetraits = mouvements.stream()
                .filter(m -> "retrait".equalsIgnoreCase(m.getTypeMouvement()))
                .map(m -> BigDecimal.valueOf(m.getMontant()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Créer la trace avec les vraies méthodes
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

    private String determinerCasVersement(BigDecimal montantCollecte, BigDecimal montantVerse) {
        int comparison = montantVerse.compareTo(montantCollecte);
        if (comparison == 0) return "NORMAL";
        if (comparison > 0) return "EXCEDENT";
        return "MANQUANT";
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
package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.DateTimeService;
import org.example.collectfocep.services.interfaces.JournalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 💰 Service de versement collecteur avec logique métier CORRIGÉE ET FINALISÉE
 *
 * PROCESSUS MÉTIER IMPLÉMENTÉ :
 * 1. Normal : montant versé = solde compte service
 * 2. Excédent : montant versé > solde compte service
 * 3. Manquant : montant versé < solde compte service
 *
 * NOUVEAUTÉS :
 * - ✅ Traçabilité via TraceabiliteCollecteQuotidienne
 * - ✅ Notifications intégrées
 * - ✅ Correction des appels JournalService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersementCollecteurService {

    private final CollecteurRepository collecteurRepository;
    private final VersementCollecteurRepository versementRepository;
    private final CompteServiceRepository compteServiceRepository;
    private final CompteManquantRepository compteManquantRepository;
    private final CompteAgenceRepository compteAgenceRepository;
    private final MouvementRepository mouvementRepository;
    private final TraceabiliteCollecteQuotidienneRepository traceabiliteRepository;
    private final JournalService journalService;
    private final SecurityService securityService;
    private final DateTimeService dateTimeService;
    private final CompteAgenceService compteAgenceService;
    private final NotificationVersementService notificationService;

    /**
     * 📋 Génère un aperçu avant clôture
     */
    @Transactional(readOnly = true)
    public ClotureJournalPreviewDTO getCloturePreview(Long collecteurId, LocalDate date) {
        log.info("📋 Génération aperçu clôture - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // Vérifier autorisation
            if (!securityService.hasPermissionForCollecteur(collecteurId)) {
                throw new BusinessException("Accès non autorisé à ce collecteur");
            }

            // 🔧 CORRECTION: Utiliser getOrCreateJournalDuJour au lieu de getJournalDuJour
            Journal journal = null;
            boolean journalExiste = false;
            try {
                journal = journalService.getOrCreateJournalDuJour(collecteurId, date);
                journalExiste = true;
            } catch (Exception e) {
                log.debug("Aucun journal trouvé pour la date: {}", date);
            }

            // Récupérer les soldes des comptes
            CompteServiceEntity compteService = compteServiceRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte service non trouvé"));

            CompteManquant compteManquant = compteManquantRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte manquant non trouvé"));

            Double soldeCompteService = compteService.getSolde();
            Double soldeCompteManquant = compteManquant.getSolde();

            // Récupérer les opérations du jour
            List<ClotureJournalPreviewDTO.OperationJournalierDTO> operations = List.of();
            Integer nombreOperations = 0;
            Double totalEpargne = 0.0;
            Double totalRetraits = 0.0;

            if (journal != null) {
                LocalDateTime startOfDay = dateTimeService.toStartOfDay(date);
                LocalDateTime endOfDay = dateTimeService.toEndOfDay(date);

                List<Mouvement> mouvements = mouvementRepository.findByCollecteurAndDay(
                        collecteurId, startOfDay, endOfDay);

                operations = mouvements.stream()
                        .map(this::convertToOperationDTO)
                        .collect(Collectors.toList());

                nombreOperations = operations.size();
                totalEpargne = mouvements.stream()
                        .filter(m -> "epargne".equalsIgnoreCase(m.getSens()) || "EPARGNE".equalsIgnoreCase(m.getTypeMouvement()))
                        .mapToDouble(Mouvement::getMontant)
                        .sum();

                totalRetraits = mouvements.stream()
                        .filter(m -> "retrait".equalsIgnoreCase(m.getSens()) || "RETRAIT".equalsIgnoreCase(m.getTypeMouvement()))
                        .mapToDouble(Mouvement::getMontant)
                        .sum();
            }

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
                    .soldeCompteAttente(0.0) // Pas utilisé dans la nouvelle logique
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur génération aperçu clôture: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors de la génération de l'aperçu: " + e.getMessage());
        }
    }

    /**
     * 💰 Effectuer le versement et clôturer le journal selon la logique métier
     */
    @Transactional
    public VersementCollecteurResponseDTO effectuerVersementEtCloture(VersementCollecteurRequestDTO request) {
        log.info("💰 DÉBUT VERSEMENT - Collecteur: {}, Date: {}, Montant versé: {}",
                request.getCollecteurId(), request.getDate(), request.getMontantVerse());

        try {
            // 1. VALIDATIONS
            Collecteur collecteur = collecteurRepository.findById(request.getCollecteurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            if (!securityService.hasPermissionForCollecteur(request.getCollecteurId())) {
                throw new BusinessException("Accès non autorisé à ce collecteur");
            }

            if (versementRepository.findByCollecteurIdAndDateVersement(
                    request.getCollecteurId(), request.getDate()).isPresent()) {
                throw new BusinessException("Un versement a déjà été effectué pour cette date");
            }

            // 2. RÉCUPÉRATION DES ENTITÉS avec CORRECTION
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

            Double montantCollecte = compteService.getSolde();
            Double montantVerse = request.getMontantVerse();

            // 🆕 3. CRÉER LA TRACE DE TRAÇABILITÉ AVANT REMISE À ZÉRO
            TraceabiliteCollecteQuotidienne trace = creerTraceAvantCloture(
                    journal, compteService, compteManquant, request.getDate());

            // 4. ANALYSE DU CAS ET EXÉCUTION DE LA LOGIQUE MÉTIER
            VersementCollecteur versement = executerLogiqueMtier(
                    collecteur, journal, compteService, compteManquant, compteAgence,
                    montantCollecte, montantVerse, request);

            // 5. CLÔTURER LE JOURNAL
            journal = journalService.cloturerJournalDuJour(request.getCollecteurId(), request.getDate());

            // 🆕 6. FINALISER LA TRACE
            trace.marquerCommeClôturee();
            traceabiliteRepository.save(trace);

            // 🆕 7. ENVOYER NOTIFICATION AU COLLECTEUR
            envoyerNotificationCollecteur(collecteur, versement);

            log.info("✅ VERSEMENT TERMINÉ - ID: {}, Cas: {}",
                    versement.getId(), determinerCasVersement(montantCollecte, montantVerse));

            return mapToResponseDTO(versement);

        } catch (Exception e) {
            log.error("❌ Erreur lors du versement: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors du versement: " + e.getMessage());
        }
    }

    /**
     * 🆕 NOUVELLE MÉTHODE: Créer la trace de traçabilité avant clôture
     */
    private TraceabiliteCollecteQuotidienne creerTraceAvantCloture(
            Journal journal,
            CompteServiceEntity compteService,
            CompteManquant compteManquant,
            LocalDate date) {

        log.info("📊 Création trace traçabilité pour journal: {}", journal.getId());

        try {
            // Calculer les totaux du jour
            LocalDateTime startOfDay = dateTimeService.toStartOfDay(date);
            LocalDateTime endOfDay = dateTimeService.toEndOfDay(date);

            List<Mouvement> mouvements = mouvementRepository.findByCollecteurAndDay(
                    journal.getCollecteur().getId(), startOfDay, endOfDay);

            Double totalEpargne = mouvements.stream()
                    .filter(m -> "epargne".equalsIgnoreCase(m.getSens()) || "EPARGNE".equalsIgnoreCase(m.getTypeMouvement()))
                    .mapToDouble(Mouvement::getMontant)
                    .sum();

            Double totalRetraits = mouvements.stream()
                    .filter(m -> "retrait".equalsIgnoreCase(m.getSens()) || "RETRAIT".equalsIgnoreCase(m.getTypeMouvement()))
                    .mapToDouble(Mouvement::getMontant)
                    .sum();

            Integer nombreOperations = mouvements.size();

            // Compter les clients uniques servis
            Integer nombreClients = (int) mouvements.stream()
                    .filter(m -> m.getClient() != null)
                    .map(m -> m.getClient().getId())
                    .distinct()
                    .count();

            // Créer la trace
            TraceabiliteCollecteQuotidienne trace = TraceabiliteCollecteQuotidienne.creerDepuisJournal(
                    journal,
                    compteService.getSolde(),
                    compteManquant.getSolde(),
                    totalEpargne,
                    totalRetraits,
                    nombreOperations,
                    nombreClients,
                    securityService.getCurrentUsername()
            );

            return traceabiliteRepository.save(trace);

        } catch (Exception e) {
            log.error("❌ Erreur création trace: {}", e.getMessage(), e);
            throw new BusinessException("Erreur création trace traçabilité: " + e.getMessage());
        }
    }

    /**
     * 🎯 Exécute la logique métier selon les 3 cas possibles
     */
    private VersementCollecteur executerLogiqueMtier(
            Collecteur collecteur,
            Journal journal,
            CompteServiceEntity compteService,
            CompteManquant compteManquant,
            CompteAgence compteAgence,
            Double montantCollecte,
            Double montantVerse,
            VersementCollecteurRequestDTO request) {

        String cas = determinerCasVersement(montantCollecte, montantVerse);
        log.info("🎯 Exécution logique métier - Cas détecté: {}", cas);

        // Créer l'enregistrement de versement
        VersementCollecteur versement = VersementCollecteur.builder()
                .collecteur(collecteur)
                .journal(journal)
                .dateVersement(request.getDate())
                .montantCollecte(montantCollecte)
                .montantVerse(montantVerse)
                .statut(VersementCollecteur.StatutVersement.VALIDE)
                .commentaire(request.getCommentaire())
                .creePar(securityService.getCurrentUsername())
                .build();

        versement = versementRepository.save(versement);

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
     * ✅ CAS NORMAL: montant versé = solde compte service
     */
    private void executerCasNormal(CompteServiceEntity compteService, CompteAgence compteAgence,
                                   Double montant, Journal journal) {
        log.info("✅ Exécution CAS NORMAL - Montant: {} FCFA", montant);

        // Mouvement unique: Crédit compte service, Débit compte agence
        Mouvement mouvement = creerMouvement(
                compteService, compteAgence, montant,
                "Versement normal - Clôture journal",
                "credit", "VERSEMENT_NORMAL", journal);

        // Mise à jour des soldes
        compteService.setSolde(0.0); // Remise à zéro
        compteAgenceService.debiterCompteAgence(compteAgence, montant); // Rendre plus négatif

        // Sauvegarder
        compteServiceRepository.save(compteService);
        mouvementRepository.save(mouvement);

        log.info("✅ CAS NORMAL terminé - Compte service: 0, Compte agence débité de: {}", montant);
    }

    /**
     * 📈 CAS EXCÉDENT: montant versé > solde compte service
     */
    private void executerCasExcedent(CompteServiceEntity compteService, CompteManquant compteManquant,
                                     CompteAgence compteAgence, Double montantCollecte,
                                     Double montantVerse, Journal journal) {
        Double constatExcedent = montantVerse - montantCollecte;
        log.info("📈 Exécution CAS EXCÉDENT - Montant collecté: {}, Versé: {}, Excédent: {}",
                montantCollecte, montantVerse, constatExcedent);

        // MOUVEMENT 1: Ajuster le compte service avec l'excédent
        Mouvement mouvement1 = creerMouvement(
                compteService, compteManquant, constatExcedent,
                "Excédent collecteur - Ajustement compte service",
                "debit", "EXCEDENT_AJUSTEMENT", journal);

        // MOUVEMENT 2: Versement du montant total à l'agence
        Mouvement mouvement2 = creerMouvement(
                compteService, compteAgence, montantVerse,
                "Versement avec excédent - Clôture journal",
                "credit", "VERSEMENT_EXCEDENT", journal);

        // Mise à jour des soldes
        compteService.setSolde(0.0); // Remise à zéro après ajustement et versement
        compteManquant.setSolde(compteManquant.getSolde() + constatExcedent); // Crédit excédent
        compteAgenceService.debiterCompteAgence(compteAgence, montantVerse); // Débit montant versé

        // Sauvegarder
        compteServiceRepository.save(compteService);
        compteManquantRepository.save(compteManquant);
        mouvementRepository.save(mouvement1);
        mouvementRepository.save(mouvement2);

        log.info("✅ CAS EXCÉDENT terminé - Excédent: {} transféré au compte manquant", constatExcedent);
    }

    /**
     * 📉 CAS MANQUANT: montant versé < solde compte service
     */
    private void executerCasManquant(CompteServiceEntity compteService, CompteManquant compteManquant,
                                     CompteAgence compteAgence, Double montantCollecte,
                                     Double montantVerse, Journal journal) {
        Double constatManquant = montantCollecte - montantVerse;
        log.info("📉 Exécution CAS MANQUANT - Montant collecté: {}, Versé: {}, Manquant: {}",
                montantCollecte, montantVerse, constatManquant);

        // MOUVEMENT 1: Prélever le manquant du compte manquant pour ajuster le service
        Mouvement mouvement1 = creerMouvement(
                compteManquant, compteService, constatManquant,
                "Manquant collecteur - Ajustement compte service",
                "debit", "MANQUANT_AJUSTEMENT", journal);

        // MOUVEMENT 2: Versement du montant réduit à l'agence
        Mouvement mouvement2 = creerMouvement(
                compteService, compteAgence, montantVerse,
                "Versement avec manquant - Clôture journal",
                "credit", "VERSEMENT_MANQUANT", journal);

        // Mise à jour des soldes
        compteService.setSolde(0.0); // Remise à zéro après ajustement et versement
        compteManquant.setSolde(compteManquant.getSolde() - constatManquant); // Débit manquant
        compteAgenceService.debiterCompteAgence(compteAgence, montantVerse); // Débit montant versé

        // Sauvegarder
        compteServiceRepository.save(compteService);
        compteManquantRepository.save(compteManquant);
        mouvementRepository.save(mouvement1);
        mouvementRepository.save(mouvement2);

        log.info("✅ CAS MANQUANT terminé - Manquant: {} prélevé du compte manquant", constatManquant);
    }

    /**
     * 🆕 NOTIFICATION AU COLLECTEUR
     */
    private void envoyerNotificationCollecteur(Collecteur collecteur, VersementCollecteur versement) {
        try {
            String cas = determinerCasVersement(versement.getMontantCollecte(), versement.getMontantVerse());

            switch (cas) {
                case "NORMAL":
                    notificationService.notifierCollecteurApresClotureOK(collecteur, versement);
                    break;
                case "EXCEDENT":
                    notificationService.notifierExcedent(collecteur, versement);
                    break;
                case "MANQUANT":
                    notificationService.notifierManquantImportant(collecteur, versement);
                    break;
            }

            log.info("📲 Notification envoyée au collecteur: {}", collecteur.getId());

        } catch (Exception e) {
            log.error("❌ Erreur envoi notification: {}", e.getMessage(), e);
            // Ne pas faire échouer la transaction pour une erreur de notification
        }
    }

    // =====================================
    // MÉTHODES UTILITAIRES (inchangées)
    // =====================================

    private String determinerCasVersement(Double montantCollecte, Double montantVerse) {
        if (montantVerse.equals(montantCollecte)) {
            return "NORMAL";
        } else if (montantVerse > montantCollecte) {
            return "EXCEDENT";
        } else {
            return "MANQUANT";
        }
    }

    private Mouvement creerMouvement(Compte source, Compte destination, Double montant,
                                     String libelle, String sens, String type, Journal journal) {
        return Mouvement.builder()
                .compteSource(source)
                .compteDestination(destination)
                .montant(montant)
                .libelle(libelle)
                .sens(sens)
                .typeMouvement(type)
                .dateOperation(dateTimeService.getCurrentDateTime())
                .journal(journal)
                .build();
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

    // =====================================
    // 🆕 MÉTHODES DE TRAÇABILITÉ
    // =====================================

    /**
     * 📊 Récupère l'historique des collectes d'un collecteur
     */
    @Transactional(readOnly = true)
    public List<TraceabiliteCollecteQuotidienne> getHistoriqueCollectes(Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        return traceabiliteRepository.findByCollecteurAndDateRange(collecteurId, dateDebut, dateFin);
    }

    /**
     * 📈 Statistiques de collecte pour un collecteur
     */
    @Transactional(readOnly = true)
    public Object[] getStatistiquesCollecte(Long collecteurId, int annee, int mois) {
        return traceabiliteRepository.getStatistiquesMensuelles(collecteurId, annee, mois);
    }
}
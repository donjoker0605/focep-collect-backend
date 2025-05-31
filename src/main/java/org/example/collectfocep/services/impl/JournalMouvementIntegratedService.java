package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.JournalCompletDTO;
import org.example.collectfocep.dto.MouvementJournalDTO;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JournalMouvementIntegratedService {

    private final JournalRepository journalRepository;
    private final MouvementRepository mouvementRepository;
    private final CollecteurRepository collecteurRepository;

    @Autowired
    public JournalMouvementIntegratedService(
            JournalRepository journalRepository,
            MouvementRepository mouvementRepository,
            CollecteurRepository collecteurRepository) {
        this.journalRepository = journalRepository;
        this.mouvementRepository = mouvementRepository;
        this.collecteurRepository = collecteurRepository;
    }

    /**
     * MÉTHODE PRINCIPALE : Récupère OU crée le journal du jour avec TOUS ses mouvements
     * Cette méthode résout votre problème principal
     */
    @Transactional(readOnly = true)
    public JournalCompletDTO getJournalCompletDuJour(Long collecteurId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("🔍 Récupération journal complet - Collecteur: {}, Date: {}", collecteurId, date);

        // 1. Vérifier que le collecteur existe
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        // 2. Chercher le journal existant
        Optional<Journal> journalOpt = journalRepository.findByCollecteurAndDate(collecteur, date);

        Journal journal;
        if (journalOpt.isPresent()) {
            journal = journalOpt.get();
            log.info("✅ Journal existant trouvé: ID={}", journal.getId());
        } else {
            // Si pas de journal, on le crée (en transaction séparée)
            journal = creerJournalDuJour(collecteurId, date);
            log.info("🆕 Nouveau journal créé: ID={}", journal.getId());
        }

        // 3. Récupérer TOUS les mouvements du jour pour ce collecteur
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Mouvement> mouvements = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                collecteurId, startOfDay, endOfDay);

        log.info("📊 {} mouvements trouvés pour le {}", mouvements.size(), date);

        // 4. Construire le DTO complet
        return JournalCompletDTO.builder()
                .journalId(journal.getId())
                .collecteurId(collecteurId)
                .collecteurNom(collecteur.getNom())
                .collecteurPrenom(collecteur.getPrenom())
                .date(date)
                .reference(journal.getReference())
                .statut(journal.getStatut())
                .estCloture(journal.isEstCloture())
                .dateCreation(journal.getDateDebut().atStartOfDay())
                .dateCloture(journal.getDateCloture())
                .nombreOperations(mouvements.size())
                .mouvements(mouvements.stream()
                        .map(this::convertToMouvementJournalDTO)
                        .collect(Collectors.toList()))
                .totalEpargne(calculerTotalParType(mouvements, "epargne"))
                .totalRetraits(calculerTotalParType(mouvements, "retrait"))
                .build();
    }

    /**
     * CRÉATION SÉCURISÉE DU JOURNAL
     * Transaction séparée pour éviter les conflits
     */
    @Transactional
    public Journal creerJournalDuJour(Long collecteurId, LocalDate date) {
        log.info("🔨 Création journal du jour - Collecteur: {}, Date: {}", collecteurId, date);

        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        // Double-check en transaction
        Optional<Journal> existingJournal = journalRepository.findByCollecteurAndDate(collecteur, date);
        if (existingJournal.isPresent()) {
            log.info("⚠️ Journal déjà créé par transaction concurrente: {}", existingJournal.get().getId());
            return existingJournal.get();
        }

        // Créer le nouveau journal
        String reference = genererReference(collecteur, date);

        Journal journal = Journal.builder()
                .collecteur(collecteur)
                .dateDebut(date)
                .dateFin(date)
                .statut("OUVERT")
                .estCloture(false)
                .reference(reference)
                .build();

        Journal savedJournal = journalRepository.save(journal);
        log.info("✅ Journal créé avec succès: ID={}, Référence={}", savedJournal.getId(), reference);

        return savedJournal;
    }

    /**
     * CLÔTURE DU JOURNAL
     */
    @Transactional
    public JournalCompletDTO cloturerJournalDuJour(Long collecteurId, LocalDate date) {
        log.info("🔒 Clôture journal - Collecteur: {}, Date: {}", collecteurId, date);

        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        Journal journal = journalRepository.findByCollecteurAndDate(collecteur, date)
                .orElseThrow(() -> new ResourceNotFoundException("Journal non trouvé pour cette date"));

        if (journal.isEstCloture()) {
            log.warn("⚠️ Journal déjà clôturé: {}", journal.getId());
        } else {
            journal.cloturerJournal();
            journalRepository.save(journal);
            log.info("✅ Journal clôturé: ID={}", journal.getId());
        }

        // Retourner le journal complet après clôture
        return getJournalCompletDuJour(collecteurId, date);
    }

    // MÉTHODES UTILITAIRES
    private String genererReference(Collecteur collecteur, LocalDate date) {
        return String.format("J-%s-%s-%s",
                collecteur.getAgence() != null ? collecteur.getAgence().getCodeAgence() : "DEFAULT",
                collecteur.getId(),
                date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }

    private MouvementJournalDTO convertToMouvementJournalDTO(Mouvement mouvement) {
        return MouvementJournalDTO.builder()
                .id(mouvement.getId())
                .montant(mouvement.getMontant())
                .libelle(mouvement.getLibelle())
                .sens(mouvement.getSens())
                .typeMouvement(mouvement.getTypeMouvement())
                .dateOperation(mouvement.getDateOperation())
                .compteSourceNumero(mouvement.getCompteSourceNumero())
                .compteDestinationNumero(mouvement.getCompteDestinationNumero())
                .clientNom(mouvement.getClient() != null ? mouvement.getClient().getNom() : null)
                .clientPrenom(mouvement.getClient() != null ? mouvement.getClient().getPrenom() : null)
                .build();
    }

    private Double calculerTotalParType(List<Mouvement> mouvements, String type) {
        return mouvements.stream()
                .filter(m -> type.equals(m.getSens()))
                .mapToDouble(Mouvement::getMontant)
                .sum();
    }
}
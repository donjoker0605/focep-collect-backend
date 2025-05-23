package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.dto.DashboardDTO;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.mappers.ClientMapper;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.services.interfaces.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ClientRepository clientRepository;
    private final MouvementRepository mouvementRepository;
    private final JournalRepository journalRepository;
    private final ClientMapper clientMapper;

    @Override
    public DashboardDTO buildDashboard(Collecteur collecteur) {
        log.info("Construction du dashboard pour le collecteur: {}", collecteur.getId());

        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutSemaine = aujourdhui.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 1);
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);

        // Statistiques générales
        Long totalClients = clientRepository.countByCollecteur(collecteur);

        // ✅ CORRECTION : Remplacement de ?? par Optional.ofNullable().orElse()
        // Calculs pour l'épargne
        Double totalEpargne = Optional.ofNullable(
                mouvementRepository.sumMontantByCollecteurAndType(collecteur.getId(), "EPARGNE")
        ).orElse(0.0);

        Double totalRetraits = Optional.ofNullable(
                mouvementRepository.sumMontantByCollecteurAndType(collecteur.getId(), "RETRAIT")
        ).orElse(0.0);

        Double soldeTotal = totalEpargne - totalRetraits;

        // Statistiques du jour
        Long transactionsAujourdhui = mouvementRepository.countByCollecteurAndDate(
                collecteur.getId(), aujourdhui);

        Double montantEpargneAujourdhui = Optional.ofNullable(
                mouvementRepository.sumMontantByCollecteurAndTypeAndDate(
                        collecteur.getId(), "EPARGNE", aujourdhui)
        ).orElse(0.0);

        Double montantRetraitAujourdhui = Optional.ofNullable(
                mouvementRepository.sumMontantByCollecteurAndTypeAndDate(
                        collecteur.getId(), "RETRAIT", aujourdhui)
        ).orElse(0.0);

        Long nouveauxClientsAujourdhui = clientRepository.countByCollecteurAndDateCreation(
                collecteur, aujourdhui);

        // Statistiques de la semaine
        Double montantEpargneSemaine = Optional.ofNullable(
                mouvementRepository.sumMontantByCollecteurAndTypeAndDateRange(
                        collecteur.getId(), "EPARGNE", debutSemaine, aujourdhui)
        ).orElse(0.0);

        Double montantRetraitSemaine = Optional.ofNullable(
                mouvementRepository.sumMontantByCollecteurAndTypeAndDateRange(
                        collecteur.getId(), "RETRAIT", debutSemaine, aujourdhui)
        ).orElse(0.0);

        Long transactionsSemaine = mouvementRepository.countByCollecteurAndDateRange(
                collecteur.getId(), debutSemaine, aujourdhui);

        // Statistiques du mois
        Double montantEpargneMois = Optional.ofNullable(
                mouvementRepository.sumMontantByCollecteurAndTypeAndDateRange(
                        collecteur.getId(), "EPARGNE", debutMois, aujourdhui)
        ).orElse(0.0);

        Double montantRetraitMois = Optional.ofNullable(
                mouvementRepository.sumMontantByCollecteurAndTypeAndDateRange(
                        collecteur.getId(), "RETRAIT", debutMois, aujourdhui)
        ).orElse(0.0);

        Long transactionsMois = mouvementRepository.countByCollecteurAndDateRange(
                collecteur.getId(), debutMois, aujourdhui);

        // Journal actuel
        Journal journalActuel = journalRepository.findActiveJournalByCollecteur(collecteur)
                .orElse(null);

        DashboardDTO.JournalDTO journalDTO = null;
        if (journalActuel != null) {
            journalDTO = DashboardDTO.JournalDTO.builder()
                    .id(journalActuel.getId())
                    .dateDebut(journalActuel.getDateDebut())
                    .dateFin(journalActuel.getDateFin())
                    .statut(journalActuel.getStatut())
                    .soldeInitial(0.0) // À calculer selon la logique métier
                    .soldeActuel(soldeTotal)
                    .nombreTransactions(transactionsAujourdhui)
                    .build();
        }

        // Transactions récentes
        List<Mouvement> mouvementsRecents = mouvementRepository.findRecentByCollecteur(
                collecteur.getId(), PageRequest.of(0, 10));

        List<DashboardDTO.TransactionRecenteDTO> transactionsRecentes = mouvementsRecents.stream()
                .map(mouvement -> DashboardDTO.TransactionRecenteDTO.builder()
                        .id(mouvement.getId())
                        .type(mouvement.getTypeMouvement())
                        .montant(mouvement.getMontant())
                        .date(mouvement.getDateMouvement())
                        .clientNom(mouvement.getClient() != null ? mouvement.getClient().getNom() : "N/A")
                        .clientPrenom(mouvement.getClient() != null ? mouvement.getClient().getPrenom() : "N/A")
                        .statut("VALIDE")
                        .build())
                .collect(Collectors.toList());

        // Clients actifs récents
        List<Client> clientsActifs = clientRepository.findActiveByCollecteur(
                collecteur, PageRequest.of(0, 5));

        List<ClientDTO> clientsActifsDTO = clientsActifs.stream()
                .map(clientMapper::toDTO)
                .collect(Collectors.toList());

        // Construction du dashboard final
        return DashboardDTO.builder()
                .totalClients(totalClients)
                .totalEpargne(totalEpargne)
                .totalRetraits(totalRetraits)
                .soldeTotal(soldeTotal)
                .objectifMensuel(collecteur.getMontantMaxRetrait()) // Utiliser comme objectif temporaire
                .progressionObjectif(calculerProgressionObjectif(montantEpargneMois, collecteur.getMontantMaxRetrait()))
                .transactionsAujourdhui(transactionsAujourdhui)
                .montantEpargneAujourdhui(montantEpargneAujourdhui)
                .montantRetraitAujourdhui(montantRetraitAujourdhui)
                .nouveauxClientsAujourdhui(nouveauxClientsAujourdhui)
                .montantEpargneSemaine(montantEpargneSemaine)
                .montantRetraitSemaine(montantRetraitSemaine)
                .transactionsSemaine(transactionsSemaine)
                .montantEpargneMois(montantEpargneMois)
                .montantRetraitMois(montantRetraitMois)
                .transactionsMois(transactionsMois)
                .commissionsMois(0.0) // À implémenter avec le système de commissions
                .commissionsAujourdhui(0.0) // À implémenter avec le système de commissions
                .journalActuel(journalDTO)
                .transactionsRecentes(transactionsRecentes)
                .clientsActifs(clientsActifsDTO)
                .alertes(List.of()) // À implémenter selon les besoins
                .build();
    }

    private Double calculerProgressionObjectif(Double montantMois, Double objectif) {
        if (objectif == null || objectif == 0) return 0.0;
        return (montantMois / objectif) * 100;
    }
}
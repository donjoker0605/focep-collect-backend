package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.dto.DashboardDTO;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.entities.CompteClient; // ✅ IMPORT MANQUANT AJOUTÉ
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

        // ✅ UTILISATION DES MÉTHODES EXISTANTES DANS LE REPOSITORY
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
                    .statut(journalActuel.getStatut()) // ✅ Maintenant le champ existe dans Journal
                    .soldeInitial(0.0)
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
                        .type(mouvement.getTypeMouvement() != null ? mouvement.getTypeMouvement() : determinerTypeMouvement(mouvement))
                        .montant(mouvement.getMontant())
                        .date(mouvement.getDateMouvement() != null ? mouvement.getDateMouvement() : mouvement.getDateOperation())
                        .clientNom(mouvement.getClient() != null ? mouvement.getClient().getNom() : obtenirNomClient(mouvement))
                        .clientPrenom(mouvement.getClient() != null ? mouvement.getClient().getPrenom() : obtenirPrenomClient(mouvement))
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
                .objectifMensuel(collecteur.getMontantMaxRetrait())
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
                .commissionsMois(0.0)
                .commissionsAujourdhui(0.0)
                .journalActuel(journalDTO)
                .transactionsRecentes(transactionsRecentes)
                .clientsActifs(clientsActifsDTO)
                .alertes(List.of())
                .build();
    }

    // ✅ MÉTHODES UTILITAIRES POUR COMPENSER LES CHAMPS MANQUANTS (maintenant avec import CompteClient)
    private String determinerTypeMouvement(Mouvement mouvement) {
        if (mouvement.getLibelle() != null) {
            String libelle = mouvement.getLibelle().toLowerCase();
            if (libelle.contains("épargne") || libelle.contains("depot")) {
                return "EPARGNE";
            } else if (libelle.contains("retrait")) {
                return "RETRAIT";
            }
        }
        return mouvement.getSens() != null ? mouvement.getSens().toUpperCase() : "INCONNU";
    }

    private String obtenirNomClient(Mouvement mouvement) {
        // Essayer d'obtenir le client depuis le compte destination (pour les épargnes)
        if (mouvement.getCompteDestination() != null && mouvement.getCompteDestination() instanceof CompteClient) {
            CompteClient compteClient = (CompteClient) mouvement.getCompteDestination();
            return compteClient.getClient() != null ? compteClient.getClient().getNom() : "N/A";
        }

        // Essayer depuis le compte source (pour les retraits)
        if (mouvement.getCompteSource() != null && mouvement.getCompteSource() instanceof CompteClient) {
            CompteClient compteClient = (CompteClient) mouvement.getCompteSource();
            return compteClient.getClient() != null ? compteClient.getClient().getNom() : "N/A";
        }

        return "N/A";
    }

    private String obtenirPrenomClient(Mouvement mouvement) {
        // Même logique que pour le nom
        if (mouvement.getCompteDestination() != null && mouvement.getCompteDestination() instanceof CompteClient) {
            CompteClient compteClient = (CompteClient) mouvement.getCompteDestination();
            return compteClient.getClient() != null ? compteClient.getClient().getPrenom() : "N/A";
        }

        if (mouvement.getCompteSource() != null && mouvement.getCompteSource() instanceof CompteClient) {
            CompteClient compteClient = (CompteClient) mouvement.getCompteSource();
            return compteClient.getClient() != null ? compteClient.getClient().getPrenom() : "N/A";
        }

        return "N/A";
    }

    private Double calculerProgressionObjectif(Double montantMois, Double objectif) {
        if (objectif == null || objectif == 0) return 0.0;
        return (montantMois / objectif) * 100;
    }
}
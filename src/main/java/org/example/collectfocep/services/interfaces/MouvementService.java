package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.BalanceVerificationDTO;
import org.example.collectfocep.dto.MouvementCommissionDTO;
import org.example.collectfocep.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MouvementService {

    /**
     * Enregistre une opération d'épargne
     */
    Mouvement enregistrerEpargne(Client client, double montant, Journal journal);

    /**
     * Enregistre une opération de retrait
     */
    Mouvement enregistrerRetrait(Client client, double montant, Journal journal);

    /**
     * Enregistre un versement en agence
     */
    Mouvement enregistrerVersement(Collecteur collecteur, double montant, Journal journal);

    /**
     * Effectue un mouvement entre deux comptes
     */
    Mouvement effectuerMouvement(Mouvement mouvement);

    /**
     * Clôture une journée de travail
     */
    Journal cloturerJournee(Collecteur collecteur, Journal journal);

    /**
     * Trouve les mouvements par collecteur et date
     */
    Page<Mouvement> findByCollecteurAndDate(Long collecteurId, String date, Pageable pageable);

    /**
     * Vérifie le solde d'un client pour un retrait
     */
    BalanceVerificationDTO verifyClientBalance(Long clientId, Double montant);

    /**
     * Récupère les mouvements d'un journal
     */
    List<Mouvement> findByJournalId(Long journalId);

    /**
     * Récupère les mouvements d'un journal avec comptes (JOIN FETCH)
     */
    List<Mouvement> findByJournalIdWithAccounts(Long journalId);

    /**
     * Récupère les mouvements d'un journal en utilisant des projections (optimisé)
     */
    List<MouvementCommissionDTO> findMouvementsDtoByJournalId(Long journalId);

    /**
     * Convertit une liste de mouvements en DTO
     */
    List<MouvementCommissionDTO> convertToDto(List<Mouvement> mouvements);

    /**
     * Traitement asynchrone des commissions
     */
    void traiterCommissionsAsync(Mouvement mouvement);

    /**
     * Calcule le montant de commission pour un mouvement
     */
    double calculerMontantCommission(Mouvement mouvement);
}
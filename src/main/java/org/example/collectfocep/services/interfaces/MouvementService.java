package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.BalanceVerificationDTO;
import org.example.collectfocep.entities.Mouvement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MouvementService {

    // Méthode manquante pour les transactions par collecteur et date
    Page<Mouvement> findByCollecteurAndDate(Long collecteurId, String date, Pageable pageable);

    // Méthode manquante pour vérification de solde
    BalanceVerificationDTO verifyClientBalance(Long clientId, Double montant);

    // Méthodes existantes...
}

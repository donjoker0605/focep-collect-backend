package org.example.collectfocep.Validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Compte;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.InvalidOperationException;
import org.example.collectfocep.exceptions.SoldeInsuffisantException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class MouvementValidator {

    public void validateMouvement(Mouvement mouvement) {
        validateBasicFields(mouvement);
        validateComptes(mouvement);
        validateMontant(mouvement);
        validateDate(mouvement);
        validateJournal(mouvement);
    }

    private void validateBasicFields(Mouvement mouvement) {
        if (mouvement.getLibelle() == null || mouvement.getLibelle().trim().isEmpty()) {
            throw new InvalidOperationException("Le libellé du mouvement est obligatoire");
        }

        if (mouvement.getSens() == null || mouvement.getSens().trim().isEmpty()) {
            throw new InvalidOperationException("Le sens du mouvement est obligatoire");
        }

        if (!mouvement.getSens().equalsIgnoreCase("DEBIT") &&
                !mouvement.getSens().equalsIgnoreCase("CREDIT")) {
            throw new InvalidOperationException("Le sens du mouvement doit être DEBIT ou CREDIT");
        }
    }

    private void validateComptes(Mouvement mouvement) {
        if (mouvement.getCompteSource() == null) {
            throw new InvalidOperationException("Le compte source est obligatoire");
        }

        if (mouvement.getCompteDestination() == null) {
            throw new InvalidOperationException("Le compte destination est obligatoire");
        }

        if (mouvement.getCompteSource().equals(mouvement.getCompteDestination())) {
            throw new InvalidOperationException("Les comptes source et destination ne peuvent pas être identiques");
        }
    }

    private void validateMontant(Mouvement mouvement) {
        if (mouvement.getMontant() <= 0) {
            throw new InvalidOperationException("Le montant doit être supérieur à zéro");
        }

        Compte compteSource = mouvement.getCompteSource();
        if (mouvement.getSens().equalsIgnoreCase("DEBIT") &&
                compteSource.getSolde() < mouvement.getMontant()) {
            throw new SoldeInsuffisantException(
                    String.format("Solde insuffisant sur le compte %s. Solde: %f, Montant demandé: %f",
                            compteSource.getNomCompte(),
                            compteSource.getSolde(),
                            mouvement.getMontant())
            );
        }
    }

    private void validateDate(Mouvement mouvement) {
        if (mouvement.getDateOperation() == null) {
            throw new InvalidOperationException("La date d'opération est obligatoire");
        }

        if (mouvement.getDateOperation().isAfter(LocalDateTime.now())) {
            throw new InvalidOperationException("La date d'opération ne peut pas être dans le futur");
        }
    }

    private void validateJournal(Mouvement mouvement) {
        Journal journal = mouvement.getJournal();
        if (journal != null && journal.isEstCloture()) {
            throw new InvalidOperationException("Impossible d'ajouter un mouvement à un journal clôturé");
        }
    }

    public void validateForCloture(Journal journal) {
        if (journal.isEstCloture()) {
            throw new InvalidOperationException("Le journal est déjà clôturé");
        }

        if (journal.getMouvements().isEmpty()) {
            throw new InvalidOperationException("Impossible de clôturer un journal sans mouvements");
        }
    }
}

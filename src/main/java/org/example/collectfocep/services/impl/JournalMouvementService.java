package org.example.collectfocep.services.impl;

import org.example.collectfocep.dto.MouvementJournalDTO;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.services.interfaces.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class JournalMouvementService {

    @Autowired
    private MouvementRepository mouvementRepository;

    @Autowired
    private JournalService journalService;

    public List<MouvementJournalDTO> getOperationsDuJour(Long collecteurId, String date) {
        LocalDate dateRecherche = date != null ? LocalDate.parse(date) : LocalDate.now();

        // Récupération du journal
        Journal journal = journalService.getOrCreateJournalDuJour(collecteurId, dateRecherche);

        // Approche robuste avec entités complètes
        List<Mouvement> mouvements = mouvementRepository.findMouvementsWithDetailsByJournalId(journal.getId());

        return mouvements.stream()
                .map(this::convertToJournalDTO)
                .collect(Collectors.toList());
    }

    private MouvementJournalDTO convertToJournalDTO(Mouvement mouvement) {
        return MouvementJournalDTO.builder()
                .id(mouvement.getId())
                .montant(mouvement.getMontant())
                .libelle(mouvement.getLibelle())
                .sens(mouvement.getSens())
                .dateOperation(mouvement.getDateOperation())
                .compteSourceNumero(mouvement.getCompteSource() != null ?
                        mouvement.getCompteSource().getNumeroCompte() : "")
                .compteDestinationNumero(mouvement.getCompteDestination() != null ?
                        mouvement.getCompteDestination().getNumeroCompte() : "")
                .typeMouvement(determinerTypeMouvement(mouvement))
                .clientNom(extraireNomClient(mouvement))
                .clientPrenom(extrairePrenomClient(mouvement))
                .build();
    }

    private String determinerTypeMouvement(Mouvement mouvement) {
        if (mouvement.getTypeMouvement() != null) {
            return mouvement.getTypeMouvement();
        }
        return mouvement.getSens() != null ? mouvement.getSens().toUpperCase() : "INCONNU";
    }

    private String extraireNomClient(Mouvement mouvement) {
        if (mouvement.getClient() != null) {
            return mouvement.getClient().getNom();
        }
        // Logique de fallback via les comptes
        return "N/A";
    }

    private String extrairePrenomClient(Mouvement mouvement) {
        if (mouvement.getClient() != null) {
            return mouvement.getClient().getPrenom();
        }
        return "N/A";
    }
}
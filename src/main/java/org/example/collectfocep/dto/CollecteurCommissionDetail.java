package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurCommissionDetail {

    private Long collecteurId;
    private String collecteurNom;
    private String collecteurEmail;

    // Statistiques période
    private Integer nombreClients;
    private BigDecimal montantTotalCollecte;
    private BigDecimal totalCommissions;
    private BigDecimal remunerationCollecteur;
    private BigDecimal partEMF;
    private BigDecimal totalTVA;

    // Détails par client
    private List<CommissionCalculation> detailsClients;

    // Métadonnées
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean nouveauCollecteur;
    private String typeRemuneration; // "FIXE" ou "POURCENTAGE"

    // Statut traitement
    private boolean success;
    private String errorMessage;
    private List<String> warnings;

    public static CollecteurCommissionDetail fromProcessingResult(
            CommissionProcessingResult result,
            String collecteurNom,
            String collecteurEmail,
            boolean isNouveauCollecteur) {

        return CollecteurCommissionDetail.builder()
                .collecteurId(result.getCollecteurId())
                .collecteurNom(collecteurNom)
                .collecteurEmail(collecteurEmail)
                .nombreClients(result.getNombreClients())
                .totalCommissions(result.getTotalCommissions())
                .remunerationCollecteur(result.getRemunerationCollecteur())
                .partEMF(result.getPartEMF())
                .totalTVA(result.getTotalTVA())
                .detailsClients(result.getCalculations())
                .nouveauCollecteur(isNouveauCollecteur)
                .typeRemuneration(isNouveauCollecteur ? "FIXE" : "POURCENTAGE")
                .success(result.isSuccess())
                .errorMessage(result.getErrorMessage())
                .warnings(result.getWarnings())
                .build();
    }
}
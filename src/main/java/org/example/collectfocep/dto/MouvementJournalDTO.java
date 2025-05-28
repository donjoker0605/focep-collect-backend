package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MouvementJournalDTO {
    private Long id;
    private Double montant;
    private String libelle;
    private String sens;
    private LocalDateTime dateOperation;
    private String compteSourceNumero;
    private String compteDestinationNumero;
    private String typeMouvement;
    private String clientNom;
    private String clientPrenom;

    // Constructeur pour mapping depuis projection
    public MouvementJournalDTO(MouvementProjection projection, String typeMouvement, String clientNom, String clientPrenom) {
        this.id = projection.getId();
        this.montant = projection.getMontant();
        this.libelle = projection.getLibelle();
        this.sens = projection.getSens();
        this.dateOperation = projection.getDateOperation();
        this.compteSourceNumero = projection.getCompteSourceNumero();
        this.compteDestinationNumero = projection.getCompteDestinationNumero();
        this.typeMouvement = typeMouvement;
        this.clientNom = clientNom;
        this.clientPrenom = clientPrenom;
    }
}
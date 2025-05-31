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
}
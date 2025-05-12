package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetraitRequest extends MouvementRequest {
    // Vous pouvez ajouter des champs spécifiques au retrait ici
    private Long clientId;
    private double montant;
    private Long journalId;
    private Long collecteurId;
    private Long agenceId;
}
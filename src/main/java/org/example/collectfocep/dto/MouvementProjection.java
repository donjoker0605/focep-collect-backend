package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementProjection {
    private Long id;
    private double montant;
    private String libelle;
    private String sens;
    private LocalDateTime dateOperation;
    private String compteSourceNumero;
    private String compteDestinationNumero;
}
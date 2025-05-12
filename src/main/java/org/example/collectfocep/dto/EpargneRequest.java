package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpargneRequest {
    private Long clientId;
    private double montant;
    private Long journalId;
    private Long collecteurId;
    private Long agenceId;
}
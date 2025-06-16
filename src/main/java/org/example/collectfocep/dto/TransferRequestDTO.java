package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {

    @NotNull(message = "Le collecteur source est obligatoire")
    private Long sourceCollecteurId;

    @NotNull(message = "Le collecteur destination est obligatoire")
    private Long destinationCollecteurId;

    @NotEmpty(message = "Au moins un client doit être sélectionné")
    private List<Long> clientIds;

    private String motif;
    private Boolean transfererHistorique;

    // Validation
    private String validePar;
    private String codeValidation;
}
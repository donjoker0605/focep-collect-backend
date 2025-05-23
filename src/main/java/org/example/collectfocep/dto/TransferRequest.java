package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class TransferRequest {

    @NotNull(message = "L'ID du collecteur source est requis")
    @Positive(message = "L'ID du collecteur source doit être positif")
    private Long sourceCollecteurId;

    @NotNull(message = "L'ID du collecteur de destination est requis")
    @Positive(message = "L'ID du collecteur de destination doit être positif")
    private Long targetCollecteurId;

    // Pour compatibilité avec le frontend
    private Long destinationCollecteurId;

    @NotEmpty(message = "La liste des clients à transférer ne peut pas être vide")
    private List<Long> clientIds;

    private String justification;

    // Getter pour la compatibilité
    public Long getDestinationCollecteurId() {
        return destinationCollecteurId != null ? destinationCollecteurId : targetCollecteurId;
    }
}
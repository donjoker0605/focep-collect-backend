package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class TransferRequest {
    @NotNull(message = "L'ID du collecteur source est requis")
    private Long sourceCollecteurId;

    @NotNull(message = "L'ID du collecteur destination est requis")
    private Long destinationCollecteurId;

    @NotEmpty(message = "Au moins un client doit être sélectionné")
    private List<Long> clientIds;
}

package org.example.collectfocep.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionParameterDTO {
    private Long id;

    // Scope - un seul doit être non-null
    private Long clientId;
    private Long collecteurId;
    private Long agenceId;

    @NotNull(message = "Le type de commission est requis")
    private String typeCommission; // FIXED, PERCENTAGE, TIER

    @Positive(message = "La valeur doit être positive")
    private BigDecimal valeurCommission;

    private String codeProduit;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    @Builder.Default
    private boolean actif = true;

    @Valid
    private List<CommissionTierDTO> paliers;

    // Validation personnalisée
    public boolean isValidScope() {
        int scopeCount = 0;
        if (clientId != null) scopeCount++;
        if (collecteurId != null) scopeCount++;
        if (agenceId != null) scopeCount++;
        return scopeCount == 1;
    }
    public boolean isValid() {
        return isValidScope();
    }


    /**
     * Récupère le scope défini
     */
    public String getScope() {
        if (clientId != null) return "CLIENT";
        if (collecteurId != null) return "COLLECTEUR";
        if (agenceId != null) return "AGENCE";
        return "UNDEFINED";
    }
}
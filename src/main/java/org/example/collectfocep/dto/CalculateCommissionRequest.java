package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateCommissionRequest {

    @NotNull(message = "La date de début est requise")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est requise")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;

    // Optionnel - pour filtrer par collecteur spécifique
    private Long collecteurId;

    // Optionnel - pour filtrer par client spécifique
    private Long clientId;

    // Optionnel - forcer le recalcul même si commissions existent
    @Builder.Default
    private Boolean forceRecalcul = false;

    // Optionnel - inclure les clients inactifs
    @Builder.Default
    private Boolean includeInactifs = false;

    public boolean isValid() {
        return dateDebut != null && dateFin != null && !dateDebut.isAfter(dateFin);
    }
}
package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTotals {
    private double totalCommissions;
    private double totalTVAClient;
    private double montantRemuneration;
    private double montantTVAEMF;
    private double montantEMF;
    private double montantManquant;

    public void addCommission(double commission) {
        this.totalCommissions += commission;
    }

    public void addTVAClient(double tva) {
        this.totalTVAClient += tva;
    }

    public void calculateEMF(double tauxEMF) {
        this.montantEMF = this.totalCommissions * tauxEMF;
        this.montantTVAEMF = this.montantEMF * 0.1925; // TVA 19.25%
    }

    public void calculateRemuneration(double tauxRemuneration) {
        this.montantRemuneration = this.totalCommissions * tauxRemuneration;
    }

    public void calculateManquant() {
        if (this.montantRemuneration > this.totalCommissions) {
            this.montantManquant = this.montantRemuneration - this.totalCommissions;
        } else {
            this.montantManquant = 0;
        }
    }
}
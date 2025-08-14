package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "parametre_commission")
public class ParametreCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    @Column(name = "type_operation", nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeOperation typeOperation;

    @Column(name = "pourcentage_commission", nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentageCommission;

    @Column(name = "montant_fixe")
    private BigDecimal montantFixe;

    @Column(name = "montant_minimum")
    private BigDecimal montantMinimum;

    @Column(name = "montant_maximum")
    private BigDecimal montantMaximum;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    public enum TypeOperation {
        DEPOT("Dépôt"),
        RETRAIT("Retrait"),
        TRANSFERT("Transfert"),
        PAIEMENT("Paiement"),
        CONSULTATION_SOLDE("Consultation de solde");

        private final String displayName;

        TypeOperation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public BigDecimal calculerCommission(BigDecimal montantTransaction) {
        if (montantTransaction == null || montantTransaction.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal commission = BigDecimal.ZERO;

        if (pourcentageCommission != null) {
            commission = montantTransaction.multiply(pourcentageCommission).divide(BigDecimal.valueOf(100));
        }

        if (montantFixe != null) {
            commission = commission.add(montantFixe);
        }

        if (montantMinimum != null && commission.compareTo(montantMinimum) < 0) {
            commission = montantMinimum;
        }

        if (montantMaximum != null && commission.compareTo(montantMaximum) > 0) {
            commission = montantMaximum;
        }

        return commission;
    }

    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(typeOperation.getDisplayName()).append(" - ");
        
        if (pourcentageCommission != null) {
            desc.append(pourcentageCommission).append("%");
        }
        
        if (montantFixe != null) {
            if (pourcentageCommission != null) {
                desc.append(" + ");
            }
            desc.append(montantFixe).append(" FCFA");
        }
        
        if (montantMinimum != null || montantMaximum != null) {
            desc.append(" (");
            if (montantMinimum != null) {
                desc.append("min: ").append(montantMinimum);
            }
            if (montantMaximum != null) {
                if (montantMinimum != null) {
                    desc.append(", ");
                }
                desc.append("max: ").append(montantMaximum);
            }
            desc.append(")");
        }
        
        return desc.toString();
    }

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (actif == null) {
            actif = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
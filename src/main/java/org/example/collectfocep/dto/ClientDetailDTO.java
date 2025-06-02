package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.CommissionType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientDetailDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String numeroCni;
    private String ville;
    private String quartier;
    private String telephone;
    private String photoPath;
    private String numeroCompte;
    private Boolean valide;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long collecteurId;
    private Long agenceId;

    // Informations des transactions
    private List<MouvementDTO> transactions;
    private Integer totalTransactions;
    private Double soldeTotal;

    // Statistiques rapides
    private Double totalEpargne;
    private Double totalRetraits;

    private CommissionType commissionType;
    private Double montantFixe;
    private Double pourcentage;
    private List<PalierCommissionDTO> paliersCommission;
    private String codeCommission;
    private CommissionParameterDTO commissionParameter;
}
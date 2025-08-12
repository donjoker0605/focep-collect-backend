package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.CompteClient;
import org.example.collectfocep.entities.CommissionParameter;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO enrichi pour les clients avec soldes, transactions et totaux
 * Utilisé pour l'affichage dans l'application mobile avec système de double solde
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientSummaryDTO {
    
    private Long id;
    private String nom;
    private String prenom;
    private String numeroCompte;
    private String telephone;
    private Boolean valide;
    private String quartier;
    private String ville;
    private LocalDateTime dateCreation;
    
    // 🔥 COMPTE CLIENT AVEC SOLDE
    private CompteClientDTO compteClient;
    
    // 🔥 TRANSACTIONS RÉCENTES
    private List<MouvementDTO> transactions;
    
    // 🔥 TOTAUX CALCULÉS
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeNet; // totalEpargne - totalRetraits
    
    // 🔥 PARAMÈTRES DE COMMISSION (pour calcul solde disponible)
    private CommissionParameterDTO commissionParameter;
    
    // 🔥 STATISTIQUES
    private Integer nombreTransactions;
    private LocalDateTime derniereTransaction;
    
    // Constructeur depuis Client entity
    public static ClientSummaryDTO fromClient(Client client) {
        ClientSummaryDTOBuilder builder = ClientSummaryDTO.builder()
                .id(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .numeroCompte(client.getNumeroCompte())
                .telephone(client.getTelephone())
                .valide(client.getValide())
                .quartier(client.getQuartier())
                .ville(client.getVille())
                .dateCreation(client.getDateCreation());
        
        // Convertir le compte client si présent
        if (client.getCompteClient() != null) {
            builder.compteClient(CompteClientDTO.fromCompteClient(client.getCompteClient()));
        }
        
        // 🔥 RÉCUPÉRER LES PARAMÈTRES DE COMMISSION ACTIFS
        if (client.hasCommissionParameters()) {
            // Prendre le premier paramètre actif (ou adapter selon vos règles métier)
            var activeParams = client.getActiveCommissionParameters();
            if (!activeParams.isEmpty()) {
                var param = activeParams.get(0);
                builder.commissionParameter(CommissionParameterDTO.fromCommissionParameter(param));
            }
        }
        
        return builder.build();
    }
    
    // Calcul automatique du solde net
    public void calculateSoldeNet() {
        if (totalEpargne != null && totalRetraits != null) {
            this.soldeNet = totalEpargne - totalRetraits;
        }
    }
    
    /**
     * DTO pour le compte client
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompteClientDTO {
        private Long id;
        private String numeroCompte;
        private Double solde;
        private String typeCompte;
        
        public static CompteClientDTO fromCompteClient(CompteClient compte) {
            return CompteClientDTO.builder()
                    .id(compte.getId())
                    .numeroCompte(compte.getNumeroCompte())
                    .solde(compte.getSolde())
                    .typeCompte(compte.getTypeCompte())
                    .build();
        }
    }
    
    /**
     * DTO pour les paramètres de commission
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommissionParameterDTO {
        private String typeCommission; // POURCENTAGE, FIXE, PALIER
        private Double pourcentage;
        private Double montantFixe;
        private List<PalierCommissionDTO> paliers;
        
        public static CommissionParameterDTO fromCommissionParameter(CommissionParameter param) {
            CommissionParameterDTOBuilder builder = CommissionParameterDTO.builder();
            
            if (param.getType() != null) {
                // Mapper les types selon votre enum
                switch (param.getType()) {
                    case PERCENTAGE:
                        builder.typeCommission("POURCENTAGE")
                               .pourcentage(param.getValeur() != null ? param.getValeur().doubleValue() : null);
                        break;
                    case FIXED:
                        builder.typeCommission("FIXE")
                               .montantFixe(param.getValeur() != null ? param.getValeur().doubleValue() : null);
                        break;
                    case TIER:
                        builder.typeCommission("PALIER");
                        // TODO: Mapper les paliers si vous avez une relation avec CommissionTier
                        break;
                    default:
                        builder.typeCommission(param.getType().toString());
                }
            }
            
            return builder.build();
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PalierCommissionDTO {
            private Double montantMin;
            private Double montantMax;
            private Double pourcentage;
        }
    }
}
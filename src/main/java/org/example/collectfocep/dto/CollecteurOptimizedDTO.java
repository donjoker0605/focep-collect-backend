package org.example.collectfocep.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🎯 DTO optimisé pour les collecteurs avec projection JPA
 * Contient uniquement les données essentielles pour l'interface SuperAdmin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurOptimizedDTO {
    
    // Informations de base
    private Long id;
    private String nom;
    private String prenom;
    private String adresseMail;
    private String telephone;
    private String numeroCni;
    private Boolean active;
    private Integer ancienneteEnMois;
    private Double montantMaxRetrait;
    
    // Informations agence
    @JsonProperty("agence")
    private AgenceInfo agence;
    
    // Statistiques clients
    @JsonProperty("statistiques")
    private StatistiquesInfo statistiques;
    
    // Soldes des comptes
    @JsonProperty("comptes")
    private ComptesInfo comptes;
    
    // Données financières
    @JsonProperty("performance")
    private PerformanceInfo performance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgenceInfo {
        private Long id;
        private String nom;
        private String code;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiquesInfo {
        private Long nombreClients;
        private Long nombreClientsActifs;
        private Double tauxClientsActifs; // Calculé
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComptesInfo {
        private Double soldeCompteService;
        private Double soldeCompteSalaire;
        private Double soldeCompteManquant;
        private Double soldeTotalDisponible; // Calculé
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceInfo {
        private Double totalEpargneCollectee;
        private Double totalCommissionsGagnees;
        private Double moyenneEpargneParClient; // Calculé
    }
    
    /**
     * 🏗️ Factory method pour créer le DTO depuis une projection
     */
    public static CollecteurOptimizedDTO fromProjection(CollecteurProjection projection) {
        // Calculs dérivés
        Double tauxClientsActifs = projection.getNombreClients() > 0 
            ? (projection.getNombreClientsActifs() * 100.0) / projection.getNombreClients() 
            : 0.0;
            
        Double soldeTotalDisponible = projection.getSoldeCompteService() + projection.getSoldeCompteSalaire();
            
        Double moyenneEpargneParClient = projection.getNombreClients() > 0
            ? projection.getTotalEpargneCollectee() / projection.getNombreClients()
            : 0.0;
        
        return CollecteurOptimizedDTO.builder()
            .id(projection.getId())
            .nom(projection.getNom())
            .prenom(projection.getPrenom())
            .adresseMail(projection.getAdresseMail())
            .telephone(projection.getTelephone())
            .numeroCni(projection.getNumeroCni())
            .active(projection.getActive())
            .ancienneteEnMois(projection.getAncienneteEnMois())
            .montantMaxRetrait(projection.getMontantMaxRetrait())
            
            .agence(AgenceInfo.builder()
                .id(projection.getAgenceId())
                .nom(projection.getAgenceNom())
                .code(projection.getAgenceCode())
                .build())
                
            .statistiques(StatistiquesInfo.builder()
                .nombreClients(projection.getNombreClients())
                .nombreClientsActifs(projection.getNombreClientsActifs())
                .tauxClientsActifs(tauxClientsActifs)
                .build())
                
            .comptes(ComptesInfo.builder()
                .soldeCompteService(projection.getSoldeCompteService())
                .soldeCompteSalaire(projection.getSoldeCompteSalaire())
                .soldeCompteManquant(projection.getSoldeCompteManquant())
                .soldeTotalDisponible(soldeTotalDisponible)
                .build())
                
            .performance(PerformanceInfo.builder()
                .totalEpargneCollectee(projection.getTotalEpargneCollectee())
                .totalCommissionsGagnees(projection.getTotalCommissionsGagnees())
                .moyenneEpargneParClient(moyenneEpargneParClient)
                .build())
                
            .build();
    }
}
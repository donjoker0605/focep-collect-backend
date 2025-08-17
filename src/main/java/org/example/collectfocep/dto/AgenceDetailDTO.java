package org.example.collectfocep.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🏢 DTO pour les détails complets d'une agence avec tous ses utilisateurs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgenceDetailDTO {
    
    // Informations de base de l'agence
    private Long id;
    private String codeAgence;
    private String nomAgence;
    private String adresse;
    private String telephone;
    private String responsable;
    private Boolean active;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateModification;
    
    // Statistiques générales
    private Integer nombreAdmins;
    private Integer nombreCollecteurs;
    private Integer nombreCollecteursActifs;
    private Integer nombreClients;
    private Integer nombreClientsActifs;
    
    // Listes détaillées des utilisateurs
    private List<SuperAdminDTO> admins;
    private List<CollecteurDTO> collecteurs;
    private List<ClientDTO> clients;
    
    // Paramètres de commission
    private List<ParametreCommissionDTO> parametresCommission;
    
    // Métriques calculées
    private Double tauxCollecteursActifs;
    private Double tauxClientsActifs;
    private String displayName;
    
    // ================================
    // MÉTHODES CALCULÉES
    // ================================
    
    public String getDisplayName() {
        if (codeAgence != null && !codeAgence.isEmpty()) {
            return String.format("%s (%s)", nomAgence, codeAgence);
        }
        return nomAgence;
    }
    
    public Double getTauxCollecteursActifs() {
        if (nombreCollecteurs == null || nombreCollecteurs == 0) return 0.0;
        if (nombreCollecteursActifs == null) return 0.0;
        return (nombreCollecteursActifs.doubleValue() / nombreCollecteurs.doubleValue()) * 100.0;
    }
    
    public Double getTauxClientsActifs() {
        if (nombreClients == null || nombreClients == 0) return 0.0;
        if (nombreClientsActifs == null) return 0.0;
        return (nombreClientsActifs.doubleValue() / nombreClients.doubleValue()) * 100.0;
    }
    
    public boolean hasUsers() {
        return (nombreAdmins != null && nombreAdmins > 0) ||
               (nombreCollecteurs != null && nombreCollecteurs > 0) ||
               (nombreClients != null && nombreClients > 0);
    }
    
    public String getStatusLabel() {
        return active != null && active ? "Active" : "Inactive";
    }
}
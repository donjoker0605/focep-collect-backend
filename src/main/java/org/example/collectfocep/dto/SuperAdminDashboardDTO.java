package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 📊 DTO pour le dashboard SuperAdmin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuperAdminDashboardDTO {
    
    // Statistiques des agences
    private Long totalAgences;
    private Long totalAgencesActives;
    
    // Statistiques des admins
    private Long totalAdmins;
    
    // Statistiques des collecteurs
    private Long totalCollecteurs;
    private Long totalCollecteursActifs;
    
    // Statistiques des clients
    private Long totalClients;
    private Long totalClientsActifs;
    
    // Métadonnées
    private LocalDateTime lastUpdate;
    @Builder.Default
    private String periode = "Global";
    
    // Statistiques calculées
    public Double getTauxAgencesActives() {
        if (totalAgences == null || totalAgences == 0) return 0.0;
        return (totalAgencesActives.doubleValue() / totalAgences.doubleValue()) * 100.0;
    }
    
    public Double getTauxCollecteursActifs() {
        if (totalCollecteurs == null || totalCollecteurs == 0) return 0.0;
        return (totalCollecteursActifs.doubleValue() / totalCollecteurs.doubleValue()) * 100.0;
    }
    
    public Double getTauxClientsActifs() {
        if (totalClients == null || totalClients == 0) return 0.0;
        return (totalClientsActifs.doubleValue() / totalClients.doubleValue()) * 100.0;
    }
}
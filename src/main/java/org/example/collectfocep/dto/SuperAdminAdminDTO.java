package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 👤 DTO pour les admins vus par le SuperAdmin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuperAdminAdminDTO {
    
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Long agenceId;
    private String agenceNom;
    private LocalDateTime dateCreation;
    private Boolean active;
    
    // Statistiques de l'admin (optionnel)
    private Long nombreCollecteurs;
    private Long nombreClients;
    private LocalDateTime derniereConnexion;
    
    public String getDisplayName() {
        return String.format("%s %s", prenom, nom);
    }
    
    public String getStatusText() {
        return Boolean.TRUE.equals(active) ? "Actif" : "Inactif";
    }
}
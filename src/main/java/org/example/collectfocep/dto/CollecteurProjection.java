package org.example.collectfocep.dto;

/**
 * 🎯 Projection JPA pour les données essentielles du collecteur
 * Évite le chargement de toutes les relations et améliore les performances
 */
public interface CollecteurProjection {
    Long getId();
    String getNom();
    String getPrenom();
    String getAdresseMail();
    String getTelephone();
    String getNumeroCni();
    Boolean getActive();
    Integer getAncienneteEnMois();
    Double getMontantMaxRetrait();
    
    // Agence
    Long getAgenceId();
    String getAgenceNom();
    String getAgenceCode();
    
    // Statistiques calculées
    Long getNombreClients();
    Long getNombreClientsActifs();
    
    // Soldes des comptes (calculés via sous-requêtes)
    Double getSoldeCompteService();
    Double getSoldeCompteSalaire();
    Double getSoldeCompteManquant();
    
    // Données financières
    Double getTotalEpargneCollectee();
    Double getTotalCommissionsGagnees();
}
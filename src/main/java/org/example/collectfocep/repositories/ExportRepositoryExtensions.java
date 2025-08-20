package org.example.collectfocep.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 📊 Extensions des repositories pour les exports Excel
 * Méthodes additionnelles pour calculs et statistiques
 */
public interface ExportRepositoryExtensions {

    // ================================
    // EXTENSIONS CLIENT REPOSITORY
    // ================================
    
    @Query("SELECT COALESCE(SUM(c.soldeTotal), 0.0) FROM Client c WHERE c.agence.id = :agenceId AND c.valide = true")
    Double sumSoldesByAgenceId(@Param("agenceId") Long agenceId);
    
    @Query("SELECT COALESCE(SUM(c.soldeTotal), 0.0) FROM Client c WHERE c.collecteur.id = :collecteurId AND c.valide = true")
    Double sumSoldesByCollecteurId(@Param("collecteurId") Long collecteurId);
    
    @Query("SELECT c FROM Client c ORDER BY c.dateCreation DESC LIMIT 5000")
    List<org.example.collectfocep.entities.Client> findTop5000ByOrderByDateCreationDesc();

    // ================================
    // EXTENSIONS MOUVEMENT REPOSITORY  
    // ================================
    
    @Query("SELECT COALESCE(SUM(CASE WHEN m.sens = 'EPARGNE' THEN m.montant ELSE -m.montant END), 0.0) " +
           "FROM Mouvement m WHERE m.clientId = :clientId")
    Double calculateSoldeByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.clientId = :clientId")
    Long countByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT MAX(m.dateMouvement) FROM Mouvement m WHERE m.clientId = :clientId")
    LocalDateTime findLastTransactionDateByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT m FROM Mouvement m WHERE m.dateMouvement BETWEEN :dateDebut AND :dateFin ORDER BY m.dateMouvement DESC")
    List<org.example.collectfocep.entities.Mouvement> findByDateMouvementBetweenOrderByDateMouvementDesc(
        @Param("dateDebut") LocalDateTime dateDebut, 
        @Param("dateFin") LocalDateTime dateFin
    );
    
    @Query("SELECT m FROM Mouvement m ORDER BY m.dateMouvement DESC LIMIT 1000")
    List<org.example.collectfocep.entities.Mouvement> findTop1000ByOrderByDateMouvementDesc();

    // ================================
    // EXTENSIONS COLLECTEUR REPOSITORY
    // ================================
    
    @Query("SELECT COUNT(c) FROM Collecteur c WHERE c.adminId = :adminId")
    Long countByAdminId(@Param("adminId") Long adminId);

    // ================================
    // EXTENSIONS ADMIN REPOSITORY
    // ================================
    
    @Query("SELECT a FROM Admin a WHERE a.agence.id = :agenceId")
    List<org.example.collectfocep.entities.Admin> findByAgenceId(@Param("agenceId") Long agenceId);
}
package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.CompteChargeCollecte;
import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteChargeCollecteRepository extends JpaRepository<CompteChargeCollecte, Long> {
    Optional<CompteChargeCollecte> findFirstByAgence(Agence agence);
    List<CompteChargeCollecte> findAllByAgence(Agence agence);
    boolean existsByAgence(Agence agence);
    
    // Méthodes de compatibilité pour la migration depuis CompteCharge
    // Trouve par collecteur (utilise l'agence du collecteur)
    @Query("SELECT c FROM CompteChargeCollecte c WHERE c.agence = :#{#collecteur.agence}")
    Optional<CompteChargeCollecte> findFirstByCollecteur(@Param("collecteur") Collecteur collecteur);
    
    @Query("SELECT c FROM CompteChargeCollecte c WHERE c.agence = :#{#collecteur.agence}")
    List<CompteChargeCollecte> findAllByCollecteur(@Param("collecteur") Collecteur collecteur);
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CompteChargeCollecte c WHERE c.agence = :#{#collecteur.agence}")
    boolean existsByCollecteur(@Param("collecteur") Collecteur collecteur);
}
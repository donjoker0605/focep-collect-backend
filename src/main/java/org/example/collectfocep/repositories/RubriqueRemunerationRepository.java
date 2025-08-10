package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.RubriqueRemuneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RubriqueRemunerationRepository extends JpaRepository<RubriqueRemuneration, Long> {

    /**
     * Récupère les rubriques actives pour un collecteur donné
     * ✅ CORRIGÉ: Utilisation de JPQL avec MEMBER OF pour éviter les erreurs de sérialisation
     * Note: La vérification du délai est faite en Java pour éviter les complexités SQL
     */
    @Query("SELECT DISTINCT r FROM RubriqueRemuneration r " +
           "WHERE r.active = true " +
           "AND :collecteurId MEMBER OF r.collecteurIds " +
           "AND r.dateApplication <= :currentDate " +
           "ORDER BY r.dateApplication ASC")
    List<RubriqueRemuneration> findActiveRubriquesByCollecteur(
            @Param("collecteurId") Long collecteurId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Récupère toutes les rubriques actives
     */
    @Query("SELECT r FROM RubriqueRemuneration r " +
           "WHERE r.active = true " +
           "ORDER BY r.dateApplication ASC")
    List<RubriqueRemuneration> findAllActive();

    /**
     * Récupère les rubriques qui vont expirer dans les N prochains jours
     */
    @Query(value = "SELECT * FROM rubrique_remuneration r " +
           "WHERE r.active = true " +
           "AND r.delai_jours IS NOT NULL " +
           "AND DATE_ADD(r.date_application, INTERVAL r.delai_jours DAY) BETWEEN :currentDate AND :expirationDate", nativeQuery = true)
    List<RubriqueRemuneration> findExpiringRubriques(
            @Param("currentDate") LocalDate currentDate,
            @Param("expirationDate") LocalDate expirationDate);

    /**
     * Alias pour la compatibilité avec filtrage des rubriques expirées
     * ✅ CORRIGÉ: Filtre aussi les rubriques expirées en Java
     */
    default List<RubriqueRemuneration> findActiveRubriquesByCollecteurId(Long collecteurId) {
        List<RubriqueRemuneration> allRubriques = findActiveRubriquesByCollecteur(collecteurId, LocalDate.now());
        
        // Filtrer les rubriques expirées
        return allRubriques.stream()
                .filter(RubriqueRemuneration::isCurrentlyValid)
                .toList();
    }
}
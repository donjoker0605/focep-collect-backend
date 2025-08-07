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
     */
    @Query(value = "SELECT DISTINCT r.* FROM rubrique_remuneration r " +
           "INNER JOIN rubrique_collecteurs rc ON r.id = rc.rubrique_id " +
           "WHERE r.active = true " +
           "AND rc.collecteur_id = :collecteurId " +
           "AND r.date_application <= :currentDate " +
           "AND (r.delai_jours IS NULL OR DATE_ADD(r.date_application, INTERVAL r.delai_jours DAY) >= :currentDate) " +
           "ORDER BY r.date_application ASC", nativeQuery = true)
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
     * Alias pour la compatibilité
     */
    default List<RubriqueRemuneration> findActiveRubriquesByCollecteurId(Long collecteurId) {
        return findActiveRubriquesByCollecteur(collecteurId, LocalDate.now());
    }
}
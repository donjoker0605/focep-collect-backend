package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Mouvement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MouvementRepositoryExtension {

    /**
     * Compter mouvements par collecteurs et période
     */
    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.collecteur.id IN :collecteurIds AND m.dateOperation >= :since")
    Long countByCollecteurIdsAndDateAfter(@Param("collecteurIds") List<Long> collecteurIds, @Param("since") LocalDateTime since);

    /**
     * Somme des montants par collecteur et période
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation >= :since AND m.typeMouvement = :type")
    Double sumByCollecteurAndTypeAndDateAfter(@Param("collecteurId") Long collecteurId,
                                              @Param("type") String type,
                                              @Param("since") LocalDateTime since);

    /**
     * Mouvements suspects (montants élevés)
     */
    @Query("SELECT m FROM Mouvement m WHERE m.collecteur.id = :collecteurId AND m.montant > :threshold " +
            "AND m.dateOperation >= :since ORDER BY m.dateOperation DESC")
    List<Mouvement> findSuspiciousMovements(@Param("collecteurId") Long collecteurId,
                                            @Param("threshold") Double threshold,
                                            @Param("since") LocalDateTime since);

    /**
     * Calcul solde journalier collecteur
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN m.typeMouvement = 'EPARGNE' THEN m.montant ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN m.typeMouvement = 'RETRAIT' THEN m.montant ELSE 0 END), 0) " +
            "FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "AND DATE(m.dateOperation) = CURRENT_DATE")
    Double calculateSoldeJournalierCollecteur(@Param("collecteurId") Long collecteurId);

    /**
     * Vérifier si épargne aujourd'hui
     */
    @Query("SELECT COUNT(m) > 0 FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "AND m.typeMouvement = 'EPARGNE' AND DATE(m.dateOperation) = CURRENT_DATE")
    boolean hasEpargneToday(@Param("collecteurId") Long collecteurId);
}

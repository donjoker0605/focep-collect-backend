package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.CollecteurNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 🔔 Repository pour la gestion des notifications des collecteurs
 *
 * FONCTIONNALITÉS :
 * - CRUD de base pour les notifications
 * - Requêtes optimisées avec pagination
 * - Filtrage par collecteur, statut, priorité
 * - Gestion de l'expiration automatique
 * - Statistiques et métriques
 * - Nettoyage automatique des notifications expirées
 */
@Repository
public interface CollecteurNotificationRepository extends JpaRepository<CollecteurNotification, Long> {

    // =====================================
    // REQUÊTES PAR COLLECTEUR
    // =====================================

    /**
     * 📋 Récupère toutes les notifications d'un collecteur avec pagination
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId ORDER BY n.dateCreation DESC")
    Page<CollecteurNotification> findByCollecteurId(@Param("collecteurId") Long collecteurId, Pageable pageable);

    /**
     * 📮 Notifications non lues d'un collecteur
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.lu = false ORDER BY n.priorite, n.dateCreation DESC")
    List<CollecteurNotification> findUnreadByCollecteurId(@Param("collecteurId") Long collecteurId);

    /**
     * 📮 Notifications non lues avec pagination
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.lu = false ORDER BY n.priorite, n.dateCreation DESC")
    Page<CollecteurNotification> findUnreadByCollecteurId(@Param("collecteurId") Long collecteurId, Pageable pageable);

    /**
     * 🚨 Notifications urgentes non lues d'un collecteur
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.lu = false AND n.priorite = 'URGENT' ORDER BY n.dateCreation DESC")
    List<CollecteurNotification> findUrgentUnreadByCollecteurId(@Param("collecteurId") Long collecteurId);

    /**
     * 📊 Compte des notifications non lues par collecteur
     */
    @Query("SELECT COUNT(n) FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.lu = false")
    Long countUnreadByCollecteurId(@Param("collecteurId") Long collecteurId);

    /**
     * 🚨 Compte des notifications urgentes non lues
     */
    @Query("SELECT COUNT(n) FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.lu = false AND n.priorite = 'URGENT'")
    Long countUrgentUnreadByCollecteurId(@Param("collecteurId") Long collecteurId);

    // =====================================
    // REQUÊTES PAR TYPE ET PRIORITÉ
    // =====================================

    /**
     * 🔍 Notifications par type avec pagination
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.type = :type ORDER BY n.dateCreation DESC")
    Page<CollecteurNotification> findByCollecteurIdAndType(@Param("collecteurId") Long collecteurId,
                                                           @Param("type") CollecteurNotification.NotificationType type,
                                                           Pageable pageable);

    /**
     * ⚡ Notifications par priorité
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.priorite = :priorite ORDER BY n.dateCreation DESC")
    List<CollecteurNotification> findByCollecteurIdAndPriorite(@Param("collecteurId") Long collecteurId,
                                                               @Param("priorite") CollecteurNotification.Priorite priorite);

    /**
     * 📂 Notifications par catégorie
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.categorie = :categorie ORDER BY n.dateCreation DESC")
    List<CollecteurNotification> findByCollecteurIdAndCategorie(@Param("collecteurId") Long collecteurId,
                                                                @Param("categorie") String categorie);

    // =====================================
    // REQUÊTES TEMPORELLES
    // =====================================

    /**
     * 📅 Notifications créées dans une période
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.dateCreation BETWEEN :debut AND :fin ORDER BY n.dateCreation DESC")
    List<CollecteurNotification> findByCollecteurIdAndDateCreationBetween(@Param("collecteurId") Long collecteurId,
                                                                          @Param("debut") LocalDateTime debut,
                                                                          @Param("fin") LocalDateTime fin);

    /**
     * 📅 Notifications récentes (dernières 24h)
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.dateCreation > :depuis ORDER BY n.dateCreation DESC")
    List<CollecteurNotification> findRecentByCollecteurId(@Param("collecteurId") Long collecteurId,
                                                          @Param("depuis") LocalDateTime depuis);

    /**
     * ⏰ Notifications expirées
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.dateExpiration < :maintenant")
    List<CollecteurNotification> findExpired(@Param("maintenant") LocalDateTime maintenant);

    /**
     * ⏰ Notifications expirées pour un collecteur
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId AND n.dateExpiration < :maintenant")
    List<CollecteurNotification> findExpiredByCollecteurId(@Param("collecteurId") Long collecteurId,
                                                           @Param("maintenant") LocalDateTime maintenant);

    // =====================================
    // REQUÊTES AVANCÉES AVEC FILTRES
    // =====================================

    /**
     * 🔍 Recherche avancée avec filtres multiples
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND (:lu IS NULL OR n.lu = :lu) " +
            "AND (:type IS NULL OR n.type = :type) " +
            "AND (:priorite IS NULL OR n.priorite = :priorite) " +
            "AND (:depuis IS NULL OR n.dateCreation >= :depuis) " +
            "AND (:jusqu IS NULL OR n.dateCreation <= :jusqu) " +
            "ORDER BY n.dateCreation DESC")
    Page<CollecteurNotification> findWithFilters(@Param("collecteurId") Long collecteurId,
                                                 @Param("lu") Boolean lu,
                                                 @Param("type") CollecteurNotification.NotificationType type,
                                                 @Param("priorite") CollecteurNotification.Priorite priorite,
                                                 @Param("depuis") LocalDateTime depuis,
                                                 @Param("jusqu") LocalDateTime jusqu,
                                                 Pageable pageable);

    /**
     * 📊 Recherche par contenu (titre ou message)
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND (LOWER(n.titre) LIKE LOWER(CONCAT('%', :recherche, '%')) " +
            "OR LOWER(n.message) LIKE LOWER(CONCAT('%', :recherche, '%'))) " +
            "ORDER BY n.dateCreation DESC")
    Page<CollecteurNotification> findByContenu(@Param("collecteurId") Long collecteurId,
                                               @Param("recherche") String recherche,
                                               Pageable pageable);

    // =====================================
    // REQUÊTES POUR ENTITÉS LIÉES
    // =====================================

    /**
     * 🔗 Notifications liées à une entité spécifique
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND n.entityType = :entityType AND n.entityId = :entityId " +
            "ORDER BY n.dateCreation DESC")
    List<CollecteurNotification> findByEntity(@Param("collecteurId") Long collecteurId,
                                              @Param("entityType") String entityType,
                                              @Param("entityId") Long entityId);

    /**
     * 🔗 Notifications pour un type d'entité
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND n.entityType = :entityType ORDER BY n.dateCreation DESC")
    List<CollecteurNotification> findByEntityType(@Param("collecteurId") Long collecteurId,
                                                  @Param("entityType") String entityType);

    // =====================================
    // STATISTIQUES ET MÉTRIQUES
    // =====================================

    /**
     * 📊 Statistiques par type pour un collecteur
     */
    @Query("SELECT n.type as type, COUNT(n) as total, " +
            "SUM(CASE WHEN n.lu = false THEN 1 ELSE 0 END) as nonLues " +
            "FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "GROUP BY n.type")
    List<Object[]> getStatistiquesByType(@Param("collecteurId") Long collecteurId);

    /**
     * 📊 Statistiques par priorité
     */
    @Query("SELECT n.priorite as priorite, COUNT(n) as total, " +
            "SUM(CASE WHEN n.lu = false THEN 1 ELSE 0 END) as nonLues " +
            "FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "GROUP BY n.priorite")
    List<Object[]> getStatistiquesByPriorite(@Param("collecteurId") Long collecteurId);

    /**
     * 📈 Notifications par jour sur une période
     */
    @Query("SELECT DATE(n.dateCreation) as jour, COUNT(n) as total " +
            "FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND n.dateCreation BETWEEN :debut AND :fin " +
            "GROUP BY DATE(n.dateCreation) ORDER BY jour")
    List<Object[]> getNotificationsParJour(@Param("collecteurId") Long collecteurId,
                                           @Param("debut") LocalDateTime debut,
                                           @Param("fin") LocalDateTime fin);

    /**
     * 🎯 Taux de lecture des notifications
     */
    @Query("SELECT COUNT(n) as total, SUM(CASE WHEN n.lu = true THEN 1 ELSE 0 END) as lues " +
            "FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND n.dateCreation BETWEEN :debut AND :fin")
    Object[] getTauxLecture(@Param("collecteurId") Long collecteurId,
                            @Param("debut") LocalDateTime debut,
                            @Param("fin") LocalDateTime fin);

    // =====================================
    // OPÉRATIONS DE MODIFICATION
    // =====================================

    /**
     * ✅ Marquer une notification comme lue
     */
    @Modifying
    @Query("UPDATE CollecteurNotification n SET n.lu = true, n.dateLecture = :dateLecture " +
            "WHERE n.id = :notificationId AND n.collecteurId = :collecteurId")
    int markAsRead(@Param("notificationId") Long notificationId,
                   @Param("collecteurId") Long collecteurId,
                   @Param("dateLecture") LocalDateTime dateLecture);

    /**
     * ✅ Marquer toutes les notifications comme lues pour un collecteur
     */
    @Modifying
    @Query("UPDATE CollecteurNotification n SET n.lu = true, n.dateLecture = :dateLecture " +
            "WHERE n.collecteurId = :collecteurId AND n.lu = false")
    int markAllAsRead(@Param("collecteurId") Long collecteurId,
                      @Param("dateLecture") LocalDateTime dateLecture);

    /**
     * ✅ Marquer les notifications d'un type comme lues
     */
    @Modifying
    @Query("UPDATE CollecteurNotification n SET n.lu = true, n.dateLecture = :dateLecture " +
            "WHERE n.collecteurId = :collecteurId AND n.type = :type AND n.lu = false")
    int markTypeAsRead(@Param("collecteurId") Long collecteurId,
                       @Param("type") CollecteurNotification.NotificationType type,
                       @Param("dateLecture") LocalDateTime dateLecture);

    /**
     * 📤 Marquer une notification comme envoyée
     */
    @Modifying
    @Query("UPDATE CollecteurNotification n SET n.envoye = true, n.dateEnvoi = :dateEnvoi " +
            "WHERE n.id = :notificationId")
    int markAsSent(@Param("notificationId") Long notificationId,
                   @Param("dateEnvoi") LocalDateTime dateEnvoi);

    /**
     * 🔄 Incrémenter les tentatives d'envoi
     */
    @Modifying
    @Query("UPDATE CollecteurNotification n SET n.tentativesEnvoi = n.tentativesEnvoi + 1, " +
            "n.erreurEnvoi = :erreur WHERE n.id = :notificationId")
    int incrementSendAttempts(@Param("notificationId") Long notificationId,
                              @Param("erreur") String erreur);

    // =====================================
    // NETTOYAGE ET MAINTENANCE
    // =====================================

    /**
     * 🧹 Supprimer les notifications expirées et lues
     */
    @Modifying
    @Query("DELETE FROM CollecteurNotification n WHERE n.dateExpiration < :maintenant " +
            "AND n.lu = true AND n.persistante = false")
    int deleteExpiredAndRead(@Param("maintenant") LocalDateTime maintenant);

    /**
     * 🧹 Supprimer les notifications anciennes pour un collecteur
     */
    @Modifying
    @Query("DELETE FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND n.dateCreation < :dateLimit AND n.lu = true AND n.persistante = false")
    int deleteOldNotifications(@Param("collecteurId") Long collecteurId,
                               @Param("dateLimit") LocalDateTime dateLimit);

    /**
     * 🧹 Compter les notifications à nettoyer
     */
    @Query("SELECT COUNT(n) FROM CollecteurNotification n WHERE n.dateExpiration < :maintenant " +
            "AND n.lu = true AND n.persistante = false")
    Long countNotificationsToCleanup(@Param("maintenant") LocalDateTime maintenant);

    // =====================================
    // REQUÊTES POUR ADMINISTRATION
    // =====================================

    /**
     * 👥 Notifications créées par un admin
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.adminId = :adminId " +
            "ORDER BY n.dateCreation DESC")
    List<CollecteurNotification> findByAdminId(@Param("adminId") Long adminId);

    /**
     * 📊 Statistiques globales pour un admin
     */
    @Query("SELECT COUNT(n) as total, " +
            "SUM(CASE WHEN n.lu = true THEN 1 ELSE 0 END) as lues, " +
            "SUM(CASE WHEN n.envoye = true THEN 1 ELSE 0 END) as envoyees " +
            "FROM CollecteurNotification n WHERE n.adminId = :adminId")
    Object[] getAdminStatistics(@Param("adminId") Long adminId);

    /**
     * 🔍 Recherche de notifications par collecteur et admin
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.adminId = :adminId " +
            "AND (:collecteurId IS NULL OR n.collecteurId = :collecteurId) " +
            "ORDER BY n.dateCreation DESC")
    Page<CollecteurNotification> findByAdminAndCollecteur(@Param("adminId") Long adminId,
                                                          @Param("collecteurId") Long collecteurId,
                                                          Pageable pageable);

    // =====================================
    // REQUÊTES D'EXISTENCE ET UNICITÉ
    // =====================================

    /**
     * ✓ Vérifie si une notification existe pour une entité
     */
    @Query("SELECT COUNT(n) > 0 FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND n.entityType = :entityType AND n.entityId = :entityId " +
            "AND n.type = :type AND n.lu = false")
    boolean existsUnreadForEntity(@Param("collecteurId") Long collecteurId,
                                  @Param("entityType") String entityType,
                                  @Param("entityId") Long entityId,
                                  @Param("type") CollecteurNotification.NotificationType type);

    /**
     * 🔍 Trouver la dernière notification d'un type pour une entité
     */
    @Query("SELECT n FROM CollecteurNotification n WHERE n.collecteurId = :collecteurId " +
            "AND n.entityType = :entityType AND n.entityId = :entityId " +
            "AND n.type = :type ORDER BY n.dateCreation DESC LIMIT 1")
    Optional<CollecteurNotification> findLastByEntityAndType(@Param("collecteurId") Long collecteurId,
                                                             @Param("entityType") String entityType,
                                                             @Param("entityId") Long entityId,
                                                             @Param("type") CollecteurNotification.NotificationType type);
}
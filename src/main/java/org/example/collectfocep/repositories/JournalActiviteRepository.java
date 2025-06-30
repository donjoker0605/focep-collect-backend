package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.JournalActivite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JournalActiviteRepository extends JpaRepository<JournalActivite, Long> {

    Page<JournalActivite> findByUserIdAndTimestampBetween(
            Long userId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            Pageable pageable
    );

    Page<JournalActivite> findByAgenceIdAndTimestampBetween(
            Long agenceId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            Pageable pageable
    );

    @Query("SELECT ja FROM JournalActivite ja WHERE ja.userId = :userId " +
            "AND DATE(ja.timestamp) = :date ORDER BY ja.timestamp DESC")
    List<JournalActivite> findByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(ja) FROM JournalActivite ja WHERE ja.userId = :userId " +
            "AND ja.action = :action AND DATE(ja.timestamp) = CURRENT_DATE")
    Long countTodayActionsByUser(@Param("userId") Long userId, @Param("action") String action);
}
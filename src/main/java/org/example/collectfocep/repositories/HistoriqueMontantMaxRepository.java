package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.HistoriqueMontantMax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoriqueMontantMaxRepository extends JpaRepository<HistoriqueMontantMax, Long> {
}
package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Historique des rémunérations effectuées pour éviter les doublons
 */
@Entity
@Table(name = "historique_remuneration", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_collecteur_periode",
           columnNames = {"collecteur_id", "date_debut_periode", "date_fin_periode"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueRemuneration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id", nullable = false)
    private Collecteur collecteur;
    
    @Column(name = "date_debut_periode", nullable = false)
    private LocalDate dateDebutPeriode;
    
    @Column(name = "date_fin_periode", nullable = false)
    private LocalDate dateFinPeriode;
    
    @Column(name = "montant_s_initial", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantSInitial;
    
    @Column(name = "total_rubriques_vi", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRubriquesVi;
    
    @Column(name = "montant_emf", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantEmf;
    
    @Column(name = "montant_tva", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantTva;
    
    @Column(name = "date_remuneration", nullable = false)
    private LocalDateTime dateRemuneration;
    
    @Column(name = "effectue_par", nullable = false)
    private String effectuePar;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}
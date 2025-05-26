package org.example.collectfocep.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rapport_mensuels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"collecteurs", "commissions"})
@EqualsAndHashCode(exclude = {"collecteurs", "commissions"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RapportMensuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mois", nullable = false)
    private Integer mois; // 1-12

    @Column(name = "annee", nullable = false)
    private Integer annee;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "est_cloture", nullable = false)
    @Builder.Default
    private Boolean estCloture = false;

    @Column(name = "total_epargne")
    @Builder.Default
    private Double totalEpargne = 0.0;

    @Column(name = "total_retraits")
    @Builder.Default
    private Double totalRetraits = 0.0;

    @Column(name = "nombre_transactions")
    @Builder.Default
    private Integer nombreTransactions = 0;

    @Column(name = "nombre_clients_actifs")
    @Builder.Default
    private Integer nombreClientsActifs = 0;

    @Column(name = "total_commissions")
    @Builder.Default
    private Double totalCommissions = 0.0;

    @Column(name = "total_tva")
    @Builder.Default
    private Double totalTVA = 0.0;

    @Column(name = "remuneration_collecteur")
    @Builder.Default
    private Double remunerationCollecteur = 0.0;

    @Column(name = "part_emf")
    @Builder.Default
    private Double partEMF = 0.0;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "statut", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutRapport statut = StatutRapport.EN_COURS;

    @OneToMany(mappedBy = "rapport", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"rapport", "clients", "comptes", "agence"})
    @Builder.Default
    private List<Collecteur> collecteurs = new ArrayList<>();

    @OneToMany(mappedBy = "rapportMensuel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"rapportMensuel"})
    @Builder.Default
    private List<RapportCommission> commissions = new ArrayList<>();

    @Version
    @Builder.Default
    private Long version = 0L;

    public enum StatutRapport {
        EN_COURS,
        VALIDE,
        REJETE,
        ARCHIVE
    }

    public RapportMensuel(Integer mois, Integer annee) {
        this.mois = mois;
        this.annee = annee;
        this.dateCreation = LocalDateTime.now();
        this.estCloture = false;
        this.statut = StatutRapport.EN_COURS;

        // Calculer les dates de début et fin du mois
        this.dateDebut = LocalDate.of(annee, mois, 1);
        this.dateFin = this.dateDebut.withDayOfMonth(this.dateDebut.lengthOfMonth());
    }

    public void addCollecteur(Collecteur collecteur) {
        if (collecteurs == null) {
            collecteurs = new ArrayList<>();
        }
        collecteurs.add(collecteur);
        collecteur.setRapport(this);
    }

    public void removeCollecteur(Collecteur collecteur) {
        if (collecteurs != null) {
            collecteurs.remove(collecteur);
            collecteur.setRapport(null);
        }
    }

    public void addCommission(RapportCommission commission) {
        if (commissions == null) {
            commissions = new ArrayList<>();
        }
        commissions.add(commission);
        commission.setRapportMensuel(this);
    }

    public void cloturerRapport() {
        this.estCloture = true;
        this.dateCloture = LocalDateTime.now();
        this.statut = StatutRapport.VALIDE;
    }

    public boolean isValide() {
        return StatutRapport.VALIDE.equals(this.statut);
    }

    public String getPeriodeLibelle() {
        return String.format("%02d/%d", this.mois, this.annee);
    }

    public Double getChiffreAffaires() {
        return (totalEpargne != null ? totalEpargne : 0.0) +
                (totalRetraits != null ? totalRetraits : 0.0);
    }

    @PrePersist
    @PreUpdate
    private void calculerTotaux() {
        if (commissions != null && !commissions.isEmpty()) {
            this.totalCommissions = commissions.stream()
                    .mapToDouble(c -> c.getTotalCommissions())
                    .sum();

            this.totalTVA = commissions.stream()
                    .mapToDouble(c -> c.getTotalTVA())
                    .sum();

            this.remunerationCollecteur = commissions.stream()
                    .mapToDouble(c -> c.getRemunerationCollecteur())
                    .sum();

            this.partEMF = commissions.stream()
                    .mapToDouble(c -> c.getPartEMF())
                    .sum();
        }
    }
}
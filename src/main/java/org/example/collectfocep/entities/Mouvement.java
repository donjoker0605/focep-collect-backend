package org.example.collectfocep.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// ✅ FIX : Gestion des références circulaires
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mouvement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double montant;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private String sens; // "debit" ou "credit"

    @Column(name = "date_operation", nullable = false)
    private LocalDateTime dateOperation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_source")
    // ✅ FIX : Éviter récursion avec Compte
    @JsonIgnoreProperties({"mouvements", "client", "collecteur", "agence"})
    private Compte compteSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_destination")
    // ✅ FIX : Éviter récursion avec Compte
    @JsonIgnoreProperties({"mouvements", "client", "collecteur", "agence"})
    private Compte compteDestination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    // ✅ FIX : Éviter récursion avec Journal
    @JsonIgnoreProperties({"mouvements", "collecteur"})
    private Journal journal;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfert_id")
    @JsonIgnoreProperties({"mouvements"})
    private TransfertCompte transfert;

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    // ✅ Méthodes utilitaires pour éviter lazy loading exceptions
    public String getCompteSourceNumero() {
        return compteSource != null ? compteSource.getNumeroCompte() : null;
    }

    public String getCompteDestinationNumero() {
        return compteDestination != null ? compteDestination.getNumeroCompte() : null;
    }
}
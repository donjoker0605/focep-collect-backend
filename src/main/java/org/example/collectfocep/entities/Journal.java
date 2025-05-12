package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Journal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_collecteur", nullable = false)
    private Collecteur collecteur;

    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Mouvement> mouvements = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean estCloture = false;

    private LocalDateTime dateCloture;

    @Version
    private Long version;

    public void addMouvement(Mouvement mouvement) {
        mouvements.add(mouvement);
        mouvement.setJournal(this);
    }

    public void removeMouvement(Mouvement mouvement) {
        mouvements.remove(mouvement);
        mouvement.setJournal(null);
    }
}
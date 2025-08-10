package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_collecteur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class AdminCollecteur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id", nullable = false)
    private Collecteur collecteur;

    @CreationTimestamp
    @Column(name = "date_assignation")
    private LocalDateTime dateAssignation;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    // Constructeur pour créer une relation facilement
    public AdminCollecteur(Admin admin, Collecteur collecteur) {
        this.admin = admin;
        this.collecteur = collecteur;
        this.active = true;
    }
}
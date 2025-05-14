package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "comptes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Compte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mapping explicite pour éviter tout conflit avec la stratégie de nommage
    @Column(name = "nom_compte", nullable = false)
    private String nomCompte;

    @Column(name = "numero_compte", nullable = false, unique = true)
    private String numeroCompte;

    @Column(name = "solde", nullable = false)
    private double solde;

    @Column(name = "type_compte", nullable = false)
    private String typeCompte;

    @Version
    private Long version;
}
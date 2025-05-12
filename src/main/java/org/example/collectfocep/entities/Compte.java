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

    @Column(nullable = false)
    private String nomCompte;

    @Column(nullable = false, unique = true)
    private String numeroCompte;  // Ajout du champ numeroCompte

    @Column(nullable = false)
    private double solde;

    @Column(nullable = false)
    private String typeCompte;

    @Version
    private Long version;
}
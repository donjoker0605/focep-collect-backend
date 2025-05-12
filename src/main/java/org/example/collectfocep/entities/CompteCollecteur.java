package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "compte_collecteur")
public class CompteCollecteur extends Compte {
    @ManyToOne
    @JoinColumn(name = "id_collecteur")
    private Collecteur collecteur;
}
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
@Table(name = "compte_liaison")
public class CompteLiaison extends Compte {
    @OneToOne
    @JoinColumn(name = "id_agence")
    private Agence agence;
}
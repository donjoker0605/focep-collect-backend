package org.example.collectfocep.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
public class CompteLiaison extends Compte {
    @OneToOne
    @JoinColumn(name = "id_agence")
    private Agence agence;

}

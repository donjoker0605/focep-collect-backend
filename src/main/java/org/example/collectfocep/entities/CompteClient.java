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
@Table(name = "compte_client")
public class CompteClient extends Compte {
    @OneToOne
    @JoinColumn(name = "id_client")
    private Client client;
}
package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transferts_compte_client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransfertCompteClient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transfert_id")
    private TransfertCompte transfert;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "old_solde")
    private double oldSolde;

    @Column(name = "new_solde")
    private double newSolde;

    @Column(name = "status")
    private String status;
}
package org.example.collectfocep.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@Table(name = "admin")
@SuperBuilder
public class Admin extends Utilisateur {

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;

    public Admin() {
        super();
    }
}

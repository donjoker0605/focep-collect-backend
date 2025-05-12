package org.example.collectfocep.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Agence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codeAgence;
    private String nomAgence;

    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL)
    private List<Collecteur> collecteurs;

    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL)
    private List<Client> clients;


}

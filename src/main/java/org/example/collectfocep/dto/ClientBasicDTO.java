package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientBasicDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String numeroCni;
    private String numeroCompte;
    private String telephone;
    private String ville;
    private String quartier;
    private Boolean valide;
}

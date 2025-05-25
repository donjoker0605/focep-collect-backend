package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CollecteurBasicDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String adresseMail;
    private String telephone;
    private Long agenceId;
    private String agenceNom;
}

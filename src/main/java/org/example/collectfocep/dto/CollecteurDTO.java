package org.example.collectfocep.dto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class CollecteurDTO {
    private Long id;
    private String nom;
    private String prenom;
    private Long agenceId;
    private double montantMaxRetrait;
    private String numeroCni;
    private String adresseMail;
    private String telephone;
    private String justificationModification;

    @JsonIgnore
    private String password;

}
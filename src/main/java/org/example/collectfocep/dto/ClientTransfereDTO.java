package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientTransfereDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String numeroCni;
}
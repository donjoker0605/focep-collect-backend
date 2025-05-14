package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgenceDTO {
    private Long id;

    @Size(max = 10, message = "Le code agence ne doit pas dépasser 10 caractères")
    private String codeAgence; // Optionnel, sera généré automatiquement si vide

    @NotBlank(message = "Le nom de l'agence est obligatoire")
    @Size(max = 200, message = "Le nom de l'agence ne doit pas dépasser 200 caractères")
    private String nomAgence;
}
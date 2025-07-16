package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réinitialisation de mot de passe par l'admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDTO {

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 6, max = 128, message = "Le mot de passe doit contenir entre 6 et 128 caractères")
    private String newPassword;

    private String reason; // Optionnel : raison de la réinitialisation

    /**
     * Constructeur de commodité
     */
    public PasswordResetRequestDTO(String newPassword) {
        this.newPassword = newPassword;
    }
}
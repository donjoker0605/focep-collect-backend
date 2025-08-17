package org.example.collectfocep.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🏢 DTO Agence avec validation Bean stricte pour SuperAdmin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgenceDTO {
    
    private Long id;

    @Size(max = 10, message = "Le code agence ne doit pas dépasser 10 caractères")
    @Pattern(regexp = "^[A-Z0-9]{3,10}$", message = "Le code agence doit contenir uniquement des lettres majuscules et des chiffres (3-10 caractères)")
    private String codeAgence; // Optionnel, sera généré automatiquement si vide

    @NotBlank(message = "Le nom de l'agence est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom de l'agence doit contenir entre 2 et 100 caractères")
    private String nomAgence;

    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String adresse;

    @NotBlank(message = "La ville est obligatoire")
    @Size(min = 2, max = 50, message = "La ville doit contenir entre 2 et 50 caractères")
    private String ville;

    @NotBlank(message = "Le quartier est obligatoire")
    @Size(min = 2, max = 50, message = "Le quartier doit contenir entre 2 et 50 caractères")
    private String quartier;

    @Pattern(regexp = "^(\\+237|237)?[\\s]?[679]\\d{8}$", message = "Format de téléphone invalide (exemple: +237XXXXXXXXX)")
    private String telephone;

    @Size(max = 100, message = "Le nom du responsable ne doit pas dépasser 100 caractères")
    private String responsable;

    @NotNull(message = "Le statut actif est obligatoire")
    @Builder.Default
    private Boolean active = true;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateModification;

    // ================================
    // PARAMÈTRES DE COMMISSION
    // ================================
    
    private List<ParametreCommissionDTO> parametresCommission;

    // ================================
    // STATISTIQUES (READ-ONLY)
    // ================================
    
    private Integer nombreCollecteurs;
    private Integer nombreCollecteursActifs;
    private Integer nombreClients;
    private Integer nombreClientsActifs;
    
    // ================================
    // MÉTHODES CALCULÉES
    // ================================
    
    public String getDisplayName() {
        if (codeAgence != null && !codeAgence.isEmpty()) {
            return String.format("%s (%s)", nomAgence, codeAgence);
        }
        return nomAgence;
    }
    
    public Double getTauxCollecteursActifs() {
        if (nombreCollecteurs == null || nombreCollecteurs == 0) return 0.0;
        if (nombreCollecteursActifs == null) return 0.0;
        return (nombreCollecteursActifs.doubleValue() / nombreCollecteurs.doubleValue()) * 100.0;
    }
    
    public Double getTauxClientsActifs() {
        if (nombreClients == null || nombreClients == 0) return 0.0;
        if (nombreClientsActifs == null) return 0.0;
        return (nombreClientsActifs.doubleValue() / nombreClients.doubleValue()) * 100.0;
    }
    
    public boolean hasCollecteurs() {
        return nombreCollecteurs != null && nombreCollecteurs > 0;
    }
    
    public boolean hasClients() {
        return nombreClients != null && nombreClients > 0;
    }
}
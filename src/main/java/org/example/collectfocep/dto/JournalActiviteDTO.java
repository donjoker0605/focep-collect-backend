package org.example.collectfocep.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;


import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "Représentation d'une activité dans le journal")
public class JournalActiviteDTO {

    @ApiModelProperty(value = "ID de l'activité", example = "123")
    private Long id;

    @ApiModelProperty(value = "ID de l'utilisateur", example = "1")
    private Long userId;

    @ApiModelProperty(value = "Nom d'utilisateur", example = "collecteur01")
    private String username;

    @ApiModelProperty(value = "Type d'utilisateur", example = "COLLECTEUR")
    private String userType;

    @ApiModelProperty(value = "Action effectuée", example = "CREATE_CLIENT")
    private String action;

    @ApiModelProperty(value = "Nom affiché de l'action", example = "Création client")
    private String actionDisplayName;

    @ApiModelProperty(value = "Type d'entité", example = "CLIENT")
    private String entityType;

    @ApiModelProperty(value = "ID de l'entité", example = "456")
    private Long entityId;

    @ApiModelProperty(value = "Détails de l'action en JSON", example = "{\"nom\":\"Dupont\"}")
    private String details;

    @ApiModelProperty(value = "Adresse IP", example = "192.168.1.100")
    private String ipAddress;

    @ApiModelProperty(value = "User Agent", example = "Mozilla/5.0...")
    private String userAgent;

    @ApiModelProperty(value = "Date et heure de l'action")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @ApiModelProperty(value = "ID de l'agence", example = "1")
    private Long agenceId;

    @ApiModelProperty(value = "Description lisible de l'action",
            example = "Création client effectuée sur CLIENT #456")
    private String description;

    // Champs additionnels utiles pour l'affichage
    @ApiModelProperty(value = "Indicateur de succès", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "Durée de l'action en ms", example = "150")
    private Long duration;
}
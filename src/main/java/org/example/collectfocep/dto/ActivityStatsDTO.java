package org.example.collectfocep.dto;

import lombok.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "Statistiques d'activité")
public class ActivityStatsDTO {

    @ApiModelProperty(value = "Nombre total d'activités", example = "150")
    private Long totalActivities;

    @ApiModelProperty(value = "Répartition par type d'action")
    private Map<String, Long> activitiesByType;

    @ApiModelProperty(value = "Nombre d'erreurs", example = "5")
    private Long errorCount;

    @ApiModelProperty(value = "Actions les plus fréquentes")
    private Map<String, Long> topActions;

    @ApiModelProperty(value = "Période analysée en jours", example = "7")
    private Integer periodDays;
}
package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusToggleDTO {

    @NotNull(message = "Le statut est obligatoire")
    private boolean active;
}

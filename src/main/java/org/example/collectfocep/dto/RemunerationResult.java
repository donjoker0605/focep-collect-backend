package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RemunerationResult {
    private double montantRemuneration;
    private double partFixe;
    private double partVariable;
}

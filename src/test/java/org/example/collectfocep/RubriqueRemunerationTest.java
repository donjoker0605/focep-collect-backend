package org.example.collectfocep;

import org.example.collectfocep.dto.RubriqueRemunerationDTO;
import org.example.collectfocep.entities.RubriqueRemuneration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RubriqueRemunerationTest {

    @Test
    public void testEntitySerialization() {
        // Given
        RubriqueRemuneration rubrique = RubriqueRemuneration.builder()
                .id(1L)
                .nom("Test Rubrique")
                .type(RubriqueRemuneration.TypeRubrique.CONSTANT)
                .valeur(new BigDecimal("40000.00"))
                .dateApplication(LocalDate.of(2025, 8, 7))
                .delaiJours(60)
                .collecteurIds(List.of(4L))
                .active(true)
                .build();

        // When - Test de conversion vers DTO (simulation de sérialisation)
        RubriqueRemunerationDTO dto = RubriqueRemunerationDTO.fromEntity(rubrique);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test Rubrique", dto.getNom());
        assertEquals(RubriqueRemuneration.TypeRubrique.CONSTANT, dto.getType());
        assertEquals(new BigDecimal("40000.00"), dto.getValeur());
        assertEquals(LocalDate.of(2025, 8, 7), dto.getDateApplication());
        assertEquals(60, dto.getDelaiJours());
        assertEquals(List.of(4L), dto.getCollecteurIds());
        assertTrue(dto.isActive());
        
        // Vérification des champs calculés
        assertNotNull(dto.getTypeLabel());
        assertNotNull(dto.getValeurFormatted());
        assertTrue(dto.isCurrentlyValid());
    }
    
    @Test
    public void testEntityValidation() {
        // Given
        RubriqueRemuneration rubrique = RubriqueRemuneration.builder()
                .id(1L)
                .nom("Rubrique Active")
                .type(RubriqueRemuneration.TypeRubrique.PERCENTAGE)
                .valeur(new BigDecimal("2.50"))
                .dateApplication(LocalDate.now().minusDays(1))
                .delaiJours(30)
                .collecteurIds(List.of(4L, 5L))
                .active(true)
                .build();
        
        // When & Then
        assertTrue(rubrique.isCurrentlyValid(), "La rubrique devrait être valide");
        assertTrue(rubrique.appliesToCollecteur(4L), "La rubrique devrait s'appliquer au collecteur 4");
        assertFalse(rubrique.appliesToCollecteur(999L), "La rubrique ne devrait pas s'appliquer au collecteur 999");
        
        // Test calcul Vi
        BigDecimal S = new BigDecimal("1000.00");
        BigDecimal expectedVi = new BigDecimal("25.00"); // 2.5% de 1000
        assertEquals(0, expectedVi.compareTo(rubrique.calculateVi(S)), "Le calcul Vi devrait être correct");
    }
}
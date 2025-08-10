package org.example.collectfocep;

import org.example.collectfocep.dto.RubriqueRemunerationDTO;
import org.example.collectfocep.entities.RubriqueRemuneration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test simple pour valider les corrections des rubriques (sans Spring Context)
 */
public class SimpleRubriqueTest {

    @Test
    public void testEntityCreation() {
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

        // When & Then - Test que l'entité se crée correctement
        assertNotNull(rubrique);
        assertEquals(1L, rubrique.getId());
        assertEquals("Test Rubrique", rubrique.getNom());
        assertEquals(RubriqueRemuneration.TypeRubrique.CONSTANT, rubrique.getType());
        assertEquals(new BigDecimal("40000.00"), rubrique.getValeur());
        assertEquals(LocalDate.of(2025, 8, 7), rubrique.getDateApplication());
        assertEquals(60, rubrique.getDelaiJours());
        assertEquals(List.of(4L), rubrique.getCollecteurIds());
        assertTrue(rubrique.isActive());
        
        System.out.println("✅ Entity création: OK");
    }

    @Test
    public void testDTOConversion() {
        // Given
        RubriqueRemuneration rubrique = RubriqueRemuneration.builder()
                .id(1L)
                .nom("Test DTO")
                .type(RubriqueRemuneration.TypeRubrique.PERCENTAGE)
                .valeur(new BigDecimal("2.50"))
                .dateApplication(LocalDate.now().minusDays(1))
                .delaiJours(30)
                .collecteurIds(List.of(4L, 5L))
                .active(true)
                .build();

        // When - Test de conversion vers DTO (simulation de sérialisation JSON)
        RubriqueRemunerationDTO dto = RubriqueRemunerationDTO.fromEntity(rubrique);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test DTO", dto.getNom());
        assertEquals(RubriqueRemuneration.TypeRubrique.PERCENTAGE, dto.getType());
        assertEquals(new BigDecimal("2.50"), dto.getValeur());
        assertEquals(rubrique.getDateApplication(), dto.getDateApplication());
        assertEquals(30, dto.getDelaiJours());
        assertEquals(List.of(4L, 5L), dto.getCollecteurIds());
        assertTrue(dto.isActive());
        
        // Vérification des champs calculés
        assertNotNull(dto.getTypeLabel());
        assertNotNull(dto.getValeurFormatted());
        assertTrue(dto.isCurrentlyValid());
        
        System.out.println("✅ DTO conversion: OK");
        System.out.println("🏷️  Type Label: " + dto.getTypeLabel());
        System.out.println("💰 Valeur Formatted: " + dto.getValeurFormatted());
    }
    
    @Test
    public void testBusinessLogic() {
        // Given
        RubriqueRemuneration rubriquePercentage = RubriqueRemuneration.builder()
                .id(1L)
                .nom("Commission 2.5%")
                .type(RubriqueRemuneration.TypeRubrique.PERCENTAGE)
                .valeur(new BigDecimal("2.50"))
                .dateApplication(LocalDate.now().minusDays(1))
                .collecteurIds(List.of(4L))
                .active(true)
                .build();
        
        // When & Then - Test logique métier
        assertTrue(rubriquePercentage.isCurrentlyValid(), "La rubrique devrait être valide");
        assertTrue(rubriquePercentage.appliesToCollecteur(4L), "La rubrique devrait s'appliquer au collecteur 4");
        assertFalse(rubriquePercentage.appliesToCollecteur(999L), "La rubrique ne devrait pas s'appliquer au collecteur 999");
        
        // Test calcul Vi pour pourcentage
        BigDecimal S = new BigDecimal("1000.00");
        BigDecimal calculatedVi = rubriquePercentage.calculateVi(S);
        assertEquals(0, new BigDecimal("25.00").compareTo(calculatedVi), "2.5% de 1000 devrait donner 25");
        
        System.out.println("✅ Business Logic: OK");
        System.out.println("💡 Calcul Vi: 2.5% de 1000 = " + calculatedVi);
    }
}
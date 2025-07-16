package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.util.ApiResponse;
import org.example.collectfocep.util.PasswordTestUtility;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🧪 Contrôleur pour tester et diagnostiquer les mots de passe
 *
 * ⚠️ ATTENTION: Ce contrôleur ne doit être activé qu'en mode développement
 * Configuration: app.debug.password-testing.enabled=true
 */
@RestController
@RequestMapping("/api/debug/passwords")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.debug.password-testing.enabled", havingValue = "true")
@PreAuthorize("hasRole('SUPER_ADMIN')")  // Seuls les super admins peuvent utiliser ces endpoints
public class PasswordTestController {

    private final PasswordTestUtility passwordTestUtility;

    /**
     * 🔍 Diagnostique complet de tous les mots de passe
     */
    @GetMapping("/diagnose")
    public ResponseEntity<ApiResponse<Map<String, Object>>> diagnosePasswords() {
        log.info("🔍 API: Diagnostic des mots de passe demandé");

        try {
            Map<String, Object> diagnostic = passwordTestUtility.diagnoseAllPasswords();

            return ResponseEntity.ok(ApiResponse.success(
                    diagnostic,
                    "Diagnostic des mots de passe effectué"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors du diagnostic des mots de passe", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors du diagnostic: " + e.getMessage()));
        }
    }

    /**
     * 🧪 Teste un mot de passe spécifique pour un collecteur
     */
    @PostMapping("/test/{collecteurId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testPassword(
            @PathVariable Long collecteurId,
            @RequestBody Map<String, String> request) {

        String password = request.get("password");
        log.info("🧪 API: Test de mot de passe pour collecteur {}", collecteurId);

        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le mot de passe est requis"));
        }

        try {
            boolean matches = passwordTestUtility.testPassword(collecteurId, password);

            Map<String, Object> result = Map.of(
                    "collecteurId", collecteurId,
                    "passwordMatches", matches,
                    "testedAt", System.currentTimeMillis()
            );

            return ResponseEntity.ok(ApiResponse.success(
                    result,
                    matches ? "Mot de passe correct" : "Mot de passe incorrect"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors du test de mot de passe", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors du test: " + e.getMessage()));
        }
    }

    /**
     * 🔧 Teste les mots de passe communs sur tous les collecteurs
     */
    @PostMapping("/test-common")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testCommonPasswords() {
        log.info("🔧 API: Test des mots de passe communs demandé");

        try {
            Map<String, Object> results = passwordTestUtility.testCommonPasswords();

            return ResponseEntity.ok(ApiResponse.success(
                    results,
                    "Test des mots de passe communs effectué"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors du test des mots de passe communs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors du test: " + e.getMessage()));
        }
    }

    /**
     * 🏥 Génère un rapport de santé complet
     */
    @GetMapping("/health-report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthReport() {
        log.info("🏥 API: Rapport de santé des mots de passe demandé");

        try {
            Map<String, Object> report = passwordTestUtility.generateHealthReport();

            return ResponseEntity.ok(ApiResponse.success(
                    report,
                    "Rapport de santé généré"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du rapport", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la génération: " + e.getMessage()));
        }
    }

    /**
     * 🔧 Simulation de correction automatique
     */
    @PostMapping("/auto-fix")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoFixPasswords(
            @RequestParam(defaultValue = "true") boolean dryRun) {

        log.info("🔧 API: {} des mots de passe demandé",
                dryRun ? "Simulation de correction" : "Correction automatique");

        if (!dryRun) {
            log.warn("⚠️ ATTENTION: Correction automatique des mots de passe en cours");
        }

        try {
            Map<String, Object> fixResults = passwordTestUtility.autoFixPasswords(dryRun);

            return ResponseEntity.ok(ApiResponse.success(
                    fixResults,
                    dryRun ? "Simulation de correction effectuée" : "Correction automatique effectuée"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la correction automatique", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la correction: " + e.getMessage()));
        }
    }

    /**
     * 📊 Statistiques rapides
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuickStats() {
        log.info("📊 API: Statistiques rapides des mots de passe");

        try {
            Map<String, Object> diagnostic = passwordTestUtility.diagnoseAllPasswords();
            Map<String, Object> weakTest = passwordTestUtility.testCommonPasswords();

            // Extraire les statistiques essentielles
            Map<String, Object> quickStats = Map.of(
                    "totalCollecteurs", diagnostic.get("totalCollecteurs"),
                    "healthScore", diagnostic.get("healthScore"),
                    "weakPasswords", weakTest.get("foundWeakPasswords"),
                    "overallSecurity", determineOverallSecurity(diagnostic, weakTest),
                    "lastCheck", System.currentTimeMillis()
            );

            return ResponseEntity.ok(ApiResponse.success(
                    quickStats,
                    "Statistiques rapides récupérées"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la récupération: " + e.getMessage()));
        }
    }

    /**
     * 🧪 Endpoint pour tester la création d'un collecteur avec mot de passe
     */
    @PostMapping("/test-creation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testCollecteurCreation(
            @RequestBody Map<String, String> testData) {

        log.info("🧪 API: Test de création de collecteur avec mot de passe");

        try {
            String testPassword = testData.getOrDefault("testPassword", "TestCreation123!");
            String testEmail = testData.getOrDefault("testEmail",
                    "test.creation." + System.currentTimeMillis() + "@collectfocep.com");

            // Simuler la création (sans vraiment créer)
            Map<String, Object> simulationResult = Map.of(
                    "testEmail", testEmail,
                    "testPassword", testPassword,
                    "passwordValidation", validateTestPassword(testPassword),
                    "simulation", true,
                    "recommendations", generateCreationRecommendations()
            );

            return ResponseEntity.ok(ApiResponse.success(
                    simulationResult,
                    "Simulation de création effectuée"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors du test de création", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors du test: " + e.getMessage()));
        }
    }

    // =====================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // =====================================

    private String determineOverallSecurity(Map<String, Object> diagnostic, Map<String, Object> weakTest) {
        Double healthScore = (Double) diagnostic.get("healthScore");
        Integer weakPasswordCount = (Integer) weakTest.get("foundWeakPasswords");

        if (healthScore < 70 || (weakPasswordCount != null && weakPasswordCount > 0)) {
            return "FAIBLE";
        } else if (healthScore < 90) {
            return "MOYEN";
        } else {
            return "BON";
        }
    }

    private Map<String, Object> validateTestPassword(String password) {
        return Map.of(
                "length", password.length(),
                "hasUppercase", password.matches(".*[A-Z].*"),
                "hasLowercase", password.matches(".*[a-z].*"),
                "hasDigit", password.matches(".*[0-9].*"),
                "hasSpecial", password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*"),
                "isValid", password.length() >= 6 && password.matches(".*[a-zA-Z].*")
        );
    }

    private java.util.List<String> generateCreationRecommendations() {
        return java.util.List.of(
                "✅ S'assurer que dto.getPassword() est utilisé dans CollecteurServiceImpl",
                "🔒 Valider le mot de passe avant le cryptage",
                "📝 Logger la création avec masquage du mot de passe",
                "🧪 Tester la connexion après création",
                "🔄 Mettre en place des tests automatisés"
        );
    }
}
package org.example.collectfocep.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔧 Utilitaire pour tester et diagnostiquer les problèmes de mots de passe
 *
 * UTILISATION:
 * - Injection dans un contrôleur ou service de test
 * - Appel des méthodes de diagnostic
 * - Correction des problèmes identifiés
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordTestUtility {

    private final CollecteurRepository collecteurRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 🔍 Diagnostique tous les mots de passe des collecteurs
     */
    public Map<String, Object> diagnoseAllPasswords() {
        log.info("🔍 Début du diagnostic des mots de passe des collecteurs");

        List<Collecteur> collecteurs = collecteurRepository.findAll();
        Map<String, Object> diagnostic = new HashMap<>();

        List<Map<String, Object>> collecteurAnalysis = new ArrayList<>();
        Map<String, Integer> statusCounts = new HashMap<>();
        List<String> suspiciousPasswords = new ArrayList<>();

        for (Collecteur collecteur : collecteurs) {
            Map<String, Object> analysis = analyzeCollecteurPassword(collecteur);
            collecteurAnalysis.add(analysis);

            String status = (String) analysis.get("status");
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);

            if ("SUSPICIOUS".equals(status) || "WEAK".equals(status)) {
                suspiciousPasswords.add(collecteur.getAdresseMail());
            }
        }

        diagnostic.put("timestamp", LocalDateTime.now());
        diagnostic.put("totalCollecteurs", collecteurs.size());
        diagnostic.put("statusCounts", statusCounts);
        diagnostic.put("collecteurDetails", collecteurAnalysis);
        diagnostic.put("suspiciousPasswords", suspiciousPasswords);
        diagnostic.put("healthScore", calculateHealthScore(statusCounts, collecteurs.size()));

        log.info("✅ Diagnostic terminé: {} collecteurs analysés", collecteurs.size());
        return diagnostic;
    }

    /**
     * 🔬 Analyse le mot de passe d'un collecteur spécifique
     */
    private Map<String, Object> analyzeCollecteurPassword(Collecteur collecteur) {
        Map<String, Object> analysis = new HashMap<>();

        analysis.put("id", collecteur.getId());
        analysis.put("email", collecteur.getAdresseMail());
        analysis.put("nom", collecteur.getNom());
        analysis.put("prenom", collecteur.getPrenom());
        analysis.put("active", collecteur.getActive());

        String password = collecteur.getPassword();

        if (password == null || password.trim().isEmpty()) {
            analysis.put("status", "NULL_PASSWORD");
            analysis.put("recommendation", "URGENT: Définir un mot de passe");
            return analysis;
        }

        // Analyser le format du hash
        if (!password.startsWith("$argon2id$")) {
            analysis.put("status", "WRONG_ALGORITHM");
            analysis.put("algorithm", detectAlgorithm(password));
            analysis.put("recommendation", "Mettre à jour vers Argon2id");
        } else if (password.length() < 50) {
            analysis.put("status", "TOO_SHORT");
            analysis.put("length", password.length());
            analysis.put("recommendation", "Hash trop court, régénérer");
        } else {
            analysis.put("status", "OK");
            analysis.put("algorithm", "Argon2id");
            analysis.put("length", password.length());
            analysis.put("recommendation", "Aucune action requise");
        }

        return analysis;
    }

    /**
     * 🧪 Teste si un mot de passe correspond au hash stocké
     */
    public boolean testPassword(Long collecteurId, String plainPassword) {
        log.info("🧪 Test de mot de passe pour le collecteur: {}", collecteurId);

        try {
            Optional<Collecteur> collecteurOpt = collecteurRepository.findById(collecteurId);
            if (collecteurOpt.isEmpty()) {
                log.warn("❌ Collecteur {} non trouvé", collecteurId);
                return false;
            }

            Collecteur collecteur = collecteurOpt.get();
            String hashedPassword = collecteur.getPassword();

            if (hashedPassword == null || hashedPassword.trim().isEmpty()) {
                log.warn("❌ Aucun mot de passe défini pour le collecteur {}", collecteurId);
                return false;
            }

            boolean matches = passwordEncoder.matches(plainPassword, hashedPassword);

            log.info("🔑 Test mot de passe collecteur {}: {}",
                    collecteurId, matches ? "✅ CORRESPOND" : "❌ NE CORRESPOND PAS");

            return matches;

        } catch (Exception e) {
            log.error("❌ Erreur lors du test de mot de passe pour le collecteur {}: {}",
                    collecteurId, e.getMessage());
            return false;
        }
    }

    /**
     * 🔧 Teste les mots de passe probables pour les collecteurs problématiques
     */
    public Map<String, Object> testCommonPasswords() {
        log.info("🔧 Test des mots de passe communs sur les collecteurs");

        List<String> commonPasswords = Arrays.asList(
                "ChangeMe123!",  // Le mot de passe forcé par le bug
                "password",
                "123456",
                "collecteur",
                "admin",
                "test123",
                "password123"
        );

        List<Collecteur> collecteurs = collecteurRepository.findAll();
        Map<String, Object> results = new HashMap<>();
        Map<String, String> foundPasswords = new HashMap<>();
        List<String> problematicCollecteurs = new ArrayList<>();

        for (Collecteur collecteur : collecteurs) {
            for (String testPassword : commonPasswords) {
                if (testPassword(collecteur.getId(), testPassword)) {
                    String key = collecteur.getAdresseMail();
                    foundPasswords.put(key, testPassword);
                    problematicCollecteurs.add(key);
                    log.warn("⚠️ Collecteur {} utilise le mot de passe faible: {}",
                            key, testPassword);
                    break; // Arrêter dès qu'on trouve un match
                }
            }
        }

        results.put("timestamp", LocalDateTime.now());
        results.put("testedPasswords", commonPasswords);
        results.put("totalCollecteurs", collecteurs.size());
        results.put("foundWeakPasswords", foundPasswords.size());
        results.put("weakPasswords", foundPasswords);
        results.put("problematicCollecteurs", problematicCollecteurs);
        results.put("securityRisk", foundPasswords.size() > 0 ? "HIGH" : "LOW");

        log.info("📊 Test terminé: {}/{} collecteurs avec mots de passe faibles",
                foundPasswords.size(), collecteurs.size());

        return results;
    }

    /**
     * 🏥 Génère un rapport de santé complet
     */
    public Map<String, Object> generateHealthReport() {
        log.info("🏥 Génération du rapport de santé des mots de passe");

        Map<String, Object> report = new HashMap<>();

        // Diagnostic général
        Map<String, Object> generalDiagnostic = diagnoseAllPasswords();

        // Test des mots de passe faibles
        Map<String, Object> weakPasswordTest = testCommonPasswords();

        // Statistiques avancées
        List<Collecteur> collecteurs = collecteurRepository.findAll();
        long activeCollecteurs = collecteurs.stream()
                .filter(Collecteur::getActive)
                .count();

        long inactiveCollecteurs = collecteurs.size() - activeCollecteurs;

        // Recommandations
        List<String> recommendations = generateRecommendations(generalDiagnostic, weakPasswordTest);

        report.put("timestamp", LocalDateTime.now());
        report.put("generalDiagnostic", generalDiagnostic);
        report.put("weakPasswordTest", weakPasswordTest);
        report.put("statistics", Map.of(
                "totalCollecteurs", collecteurs.size(),
                "activeCollecteurs", activeCollecteurs,
                "inactiveCollecteurs", inactiveCollecteurs
        ));
        report.put("recommendations", recommendations);
        report.put("overallSecurity", determineOverallSecurity(generalDiagnostic, weakPasswordTest));

        log.info("✅ Rapport de santé généré avec succès");
        return report;
    }

    /**
     * 🔧 Corrige automatiquement les mots de passe problématiques
     */
    public Map<String, Object> autoFixPasswords(boolean dryRun) {
        log.info("🔧 {} des mots de passe problématiques",
                dryRun ? "Simulation de correction" : "Correction");

        Map<String, Object> fixResults = new HashMap<>();
        List<String> fixedCollecteurs = new ArrayList<>();
        List<String> skippedCollecteurs = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        Map<String, Object> weakPasswordTest = testCommonPasswords();
        @SuppressWarnings("unchecked")
        Map<String, String> weakPasswords = (Map<String, String>) weakPasswordTest.get("weakPasswords");

        for (Map.Entry<String, String> entry : weakPasswords.entrySet()) {
            String email = entry.getKey();
            String weakPassword = entry.getValue();

            try {
                Optional<Collecteur> collecteurOpt = collecteurRepository.findByAdresseMail(email);
                if (collecteurOpt.isEmpty()) {
                    skippedCollecteurs.add(email + " (non trouvé)");
                    continue;
                }

                Collecteur collecteur = collecteurOpt.get();

                if (!dryRun) {
                    // Générer un nouveau mot de passe sécurisé
                    String newPassword = generateSecurePassword();
                    String encodedPassword = passwordEncoder.encode(newPassword);

                    collecteur.setPassword(encodedPassword);
                    collecteurRepository.save(collecteur);

                    fixedCollecteurs.add(email + " (nouveau mot de passe: " + newPassword + ")");
                    log.info("✅ Mot de passe corrigé pour {}: {}", email, newPassword);
                } else {
                    fixedCollecteurs.add(email + " (serait corrigé)");
                    log.info("🔍 [DRY RUN] Collecteur {} serait corrigé", email);
                }

            } catch (Exception e) {
                String errorMsg = email + ": " + e.getMessage();
                errors.add(errorMsg);
                log.error("❌ Erreur lors de la correction pour {}: {}", email, e.getMessage());
            }
        }

        fixResults.put("timestamp", LocalDateTime.now());
        fixResults.put("dryRun", dryRun);
        fixResults.put("totalProcessed", weakPasswords.size());
        fixResults.put("fixed", fixedCollecteurs.size());
        fixResults.put("skipped", skippedCollecteurs.size());
        fixResults.put("errors", errors.size());
        fixResults.put("fixedCollecteurs", fixedCollecteurs);
        fixResults.put("skippedCollecteurs", skippedCollecteurs);
        fixResults.put("errorDetails", errors);

        log.info("📊 Correction terminée: {}/{} collecteurs traités",
                fixedCollecteurs.size(), weakPasswords.size());

        return fixResults;
    }

    // =====================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // =====================================

    private String detectAlgorithm(String password) {
        if (password.startsWith("$2a$") || password.startsWith("$2b$")) {
            return "BCrypt";
        } else if (password.startsWith("$argon2id$")) {
            return "Argon2id";
        } else if (password.startsWith("{bcrypt}")) {
            return "Spring BCrypt";
        } else if (password.length() == 32 && password.matches("[a-f0-9]+")) {
            return "MD5 (DANGEREUX)";
        } else if (password.length() == 40 && password.matches("[a-f0-9]+")) {
            return "SHA1 (DANGEREUX)";
        }
        return "Inconnu";
    }

    private double calculateHealthScore(Map<String, Integer> statusCounts, int totalCollecteurs) {
        int okCount = statusCounts.getOrDefault("OK", 0);
        return totalCollecteurs > 0 ? (double) okCount / totalCollecteurs * 100 : 0;
    }

    private String generateSecurePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        StringBuilder password = new StringBuilder();

        // Assurer la diversité
        password.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt((int) (Math.random() * 26))); // Majuscule
        password.append("abcdefghijklmnopqrstuvwxyz".charAt((int) (Math.random() * 26))); // Minuscule
        password.append("0123456789".charAt((int) (Math.random() * 10))); // Chiffre
        password.append("!@#$%&*".charAt((int) (Math.random() * 7))); // Spécial

        // Compléter
        for (int i = 4; i < 10; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }

        return password.toString();
    }

    private List<String> generateRecommendations(Map<String, Object> generalDiagnostic,
                                                 Map<String, Object> weakPasswordTest) {
        List<String> recommendations = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Map<String, Integer> statusCounts = (Map<String, Integer>) generalDiagnostic.get("statusCounts");

        if (statusCounts.getOrDefault("NULL_PASSWORD", 0) > 0) {
            recommendations.add("🚨 URGENT: Certains collecteurs n'ont pas de mot de passe défini");
        }

        if (statusCounts.getOrDefault("WRONG_ALGORITHM", 0) > 0) {
            recommendations.add("⚠️ Mettre à jour les mots de passe vers l'algorithme Argon2id");
        }

        Integer weakPasswordCount = (Integer) weakPasswordTest.get("foundWeakPasswords");
        if (weakPasswordCount != null && weakPasswordCount > 0) {
            recommendations.add("🔒 Forcer le changement des mots de passe faibles identifiés");
        }

        recommendations.add("📋 Implémenter une politique de mots de passe forts");
        recommendations.add("🔄 Planifier une rotation périodique des mots de passe");
        recommendations.add("📊 Surveiller régulièrement la santé des mots de passe");

        return recommendations;
    }

    private String determineOverallSecurity(Map<String, Object> generalDiagnostic,
                                            Map<String, Object> weakPasswordTest) {
        Double healthScore = (Double) generalDiagnostic.get("healthScore");
        Integer weakPasswordCount = (Integer) weakPasswordTest.get("foundWeakPasswords");

        if (healthScore < 70 || (weakPasswordCount != null && weakPasswordCount > 0)) {
            return "FAIBLE";
        } else if (healthScore < 90) {
            return "MOYEN";
        } else {
            return "BON";
        }
    }
}
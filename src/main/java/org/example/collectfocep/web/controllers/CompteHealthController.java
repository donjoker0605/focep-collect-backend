package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteCollecteur;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.CompteCollecteurRepository;
import org.example.collectfocep.repositories.CompteServiceRepository;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/compte-health")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Slf4j
@RequiredArgsConstructor
public class CompteHealthController {

    private final CollecteurRepository collecteurRepository;
    private final CompteCollecteurRepository compteCollecteurRepository;
    private final CompteServiceRepository compteServiceRepository;
    private final CompteService compteService;

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<CompteHealthReport>> checkCompteHealth() {
        log.info("Vérification de la santé des comptes collecteurs");

        CompteHealthReport report = new CompteHealthReport();
        List<Collecteur> allCollecteurs = collecteurRepository.findAll();

        for (Collecteur collecteur : allCollecteurs) {
            CollecteurCompteStatus status = checkCollecteurComptes(collecteur);
            report.addStatus(status);
        }

        return ResponseEntity.ok(ApiResponse.success(report, "Rapport de santé des comptes"));
    }

    @PostMapping("/fix/{collecteurId}")
    public ResponseEntity<ApiResponse<String>> fixCollecteurComptes(@PathVariable Long collecteurId) {
        log.info("Correction des comptes pour collecteur ID={}", collecteurId);

        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        try {
            compteService.createCollecteurAccounts(collecteur);
            return ResponseEntity.ok(ApiResponse.success("OK",
                    "Comptes créés/corrigés pour collecteur " + collecteur.getNom()));
        } catch (Exception e) {
            log.error("Erreur lors de la correction des comptes: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("FIX_ERROR", e.getMessage()));
        }
    }

    private CollecteurCompteStatus checkCollecteurComptes(Collecteur collecteur) {
        CollecteurCompteStatus status = new CollecteurCompteStatus();
        status.setCollecteurId(collecteur.getId());
        status.setCollecteurNom(collecteur.getNom() + " " + collecteur.getPrenom());

        // Vérifier CompteCollecteur de type SERVICE
        Optional<CompteCollecteur> compteCollecteur = compteCollecteurRepository
                .findByCollecteurAndTypeCompte(collecteur, "SERVICE");
        status.setHasCompteCollecteur(compteCollecteur.isPresent());

        // Vérifier CompteServiceEntity
        boolean hasCompteService = compteServiceRepository.existsByCollecteur(collecteur);
        status.setHasCompteServiceEntity(hasCompteService);

        // Vérifier si findServiceAccount fonctionne
        try {
            compteService.findServiceAccount(collecteur);
            status.setCanFindServiceAccount(true);
        } catch (Exception e) {
            status.setCanFindServiceAccount(false);
            status.setError(e.getMessage());
        }

        return status;
    }

    // Classes internes pour le rapport
    static class CompteHealthReport {
        private List<CollecteurCompteStatus> collecteurStatuses = new ArrayList<>();
        private int totalCollecteurs;
        private int collecteursWithIssues;

        public void addStatus(CollecteurCompteStatus status) {
            collecteurStatuses.add(status);
            totalCollecteurs++;
            if (!status.isHealthy()) {
                collecteursWithIssues++;
            }
        }

        // Getters/Setters
        public List<CollecteurCompteStatus> getCollecteurStatuses() { return collecteurStatuses; }
        public int getTotalCollecteurs() { return totalCollecteurs; }
        public int getCollecteursWithIssues() { return collecteursWithIssues; }
    }

    static class CollecteurCompteStatus {
        private Long collecteurId;
        private String collecteurNom;
        private boolean hasCompteCollecteur;
        private boolean hasCompteServiceEntity;
        private boolean canFindServiceAccount;
        private String error;

        public boolean isHealthy() {
            return canFindServiceAccount;
        }

        // Getters/Setters
        public Long getCollecteurId() { return collecteurId; }
        public void setCollecteurId(Long collecteurId) { this.collecteurId = collecteurId; }
        public String getCollecteurNom() { return collecteurNom; }
        public void setCollecteurNom(String collecteurNom) { this.collecteurNom = collecteurNom; }
        public boolean isHasCompteCollecteur() { return hasCompteCollecteur; }
        public void setHasCompteCollecteur(boolean hasCompteCollecteur) { this.hasCompteCollecteur = hasCompteCollecteur; }
        public boolean isHasCompteServiceEntity() { return hasCompteServiceEntity; }
        public void setHasCompteServiceEntity(boolean hasCompteServiceEntity) { this.hasCompteServiceEntity = hasCompteServiceEntity; }
        public boolean isCanFindServiceAccount() { return canFindServiceAccount; }
        public void setCanFindServiceAccount(boolean canFindServiceAccount) { this.canFindServiceAccount = canFindServiceAccount; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
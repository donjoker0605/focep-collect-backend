package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Commission;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionType;
import org.example.collectfocep.exceptions.InvalidOperationException;
import org.example.collectfocep.security.annotations.AgenceAccess;
import org.example.collectfocep.services.impl.CommissionService;
import org.example.collectfocep.security.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commissions")
@Slf4j
public class CommissionController {
    private final CommissionService commissionService;
    private final SecurityService securityService;

    @Autowired
    public CommissionController(CommissionService commissionService, SecurityService securityService) {
        this.commissionService = commissionService;
        this.securityService = securityService;
    }

    @GetMapping("/agence/{agenceId}")
    @AgenceAccess
    public ResponseEntity<List<Commission>> getCommissionsByAgence(@PathVariable Long agenceId) {
        log.info("Récupération des commissions pour l'agence: {}", agenceId);
        return ResponseEntity.ok(commissionService.findByAgenceId(agenceId));
    }

    @GetMapping("/collecteur/{collecteurId}")
    @PreAuthorize("@securityService.canAccessCollecteur(authentication, #collecteurId)")
    public ResponseEntity<List<Commission>> getCommissionsByCollecteur(@PathVariable Long collecteurId) {
        log.info("Récupération des commissions pour le collecteur: {}", collecteurId);
        return ResponseEntity.ok(commissionService.findByCollecteurId(collecteurId));
    }

    @PostMapping("/parameters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<CommissionParameter> createCommissionParameter(
            @Valid @RequestBody CommissionParameter parameter) {
        log.info("Création d'un nouveau paramètre de commission");
        validateCommissionParameter(parameter);
        return ResponseEntity.ok(commissionService.saveCommissionParameter(parameter));
    }

    @PutMapping("/parameters/{id}")
    @PreAuthorize("@securityService.canManageCommissionParameter(authentication, #id)")
    public ResponseEntity<CommissionParameter> updateCommissionParameter(
            @PathVariable Long id,
            @Valid @RequestBody CommissionParameter parameter) {
        log.info("Mise à jour du paramètre de commission: {}", id);
        validateCommissionParameter(parameter);
        parameter.setId(id);
        return ResponseEntity.ok(commissionService.saveCommissionParameter(parameter));
    }

    private void validateCommissionParameter(CommissionParameter parameter) {
        if (parameter.getType() == CommissionType.TIER &&
                (parameter.getTiers() == null || parameter.getTiers().isEmpty())) {
            throw new InvalidOperationException("Les paliers sont requis pour le type TIER");
        }
    }
}
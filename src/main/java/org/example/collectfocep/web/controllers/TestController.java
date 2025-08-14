package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.util.ApiResponse;
import org.example.collectfocep.entities.ParametreCommission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getStatus() {
        log.info("🧪 Test endpoint appelé");
        return ResponseEntity.ok(ApiResponse.success("Service SuperAdmin opérationnel", "Tests endpoints OK"));
    }

    @GetMapping("/types-operation")
    public ResponseEntity<ApiResponse<List<ParametreCommission.TypeOperation>>> getTypesOperation() {
        log.info("🧪 Test types d'opération");
        List<ParametreCommission.TypeOperation> types = Arrays.asList(ParametreCommission.TypeOperation.values());
        return ResponseEntity.ok(ApiResponse.success(types, "Types d'opération récupérés avec succès"));
    }

    @PostMapping("/calculer-commission")
    public ResponseEntity<ApiResponse<BigDecimal>> testCalculCommission(
            @RequestParam BigDecimal montantTransaction,
            @RequestParam(defaultValue = "2.5") BigDecimal pourcentage,
            @RequestParam(defaultValue = "100") BigDecimal montantFixe) {
        
        log.info("🧪 Test calcul commission - montant: {}, pourcentage: {}, fixe: {}", 
                montantTransaction, pourcentage, montantFixe);
        
        // Calcul simple pour test
        BigDecimal commission = montantTransaction.multiply(pourcentage).divide(BigDecimal.valueOf(100));
        commission = commission.add(montantFixe);
        
        return ResponseEntity.ok(ApiResponse.success(commission, "Commission calculée (test)"));
    }

    @GetMapping("/structure-reponse")
    public ResponseEntity<ApiResponse<Object>> testStructureReponse() {
        log.info("🧪 Test structure de réponse API");
        
        Object mockData = new Object() {
            public final String message = "Test de la structure de réponse";
            public final int code = 200;
            public final boolean success = true;
        };
        
        return ResponseEntity.ok(ApiResponse.success(mockData, "Structure de réponse testée"));
    }
}
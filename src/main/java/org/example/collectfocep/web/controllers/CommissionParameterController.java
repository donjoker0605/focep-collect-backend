package org.example.collectfocep.web.controllers;

import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.example.collectfocep.exceptions.ResourceNotFoundException; // Import correct
import org.example.collectfocep.repositories.CommissionParameterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commission-parameters")
public class CommissionParameterController {

    @Autowired
    private CommissionParameterRepository commissionParameterRepository;

    @PostMapping("/{id}/tiers")
    public ResponseEntity<CommissionParameter> addTier(@PathVariable Long id, @RequestBody CommissionTier tier) {
        CommissionParameter param = commissionParameterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter not found")); // Utilisation de ResourceNotFoundException
        param.getTiers().add(tier);
        commissionParameterRepository.save(param);
        return ResponseEntity.ok(param);
    }
}
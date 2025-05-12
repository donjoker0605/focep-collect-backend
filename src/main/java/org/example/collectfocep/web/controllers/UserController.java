package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.security.config.RoleConfig;
import org.example.collectfocep.entities.Admin;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.AdminRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.dto.AdminDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final AdminRepository adminRepository;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(AdminRepository adminRepository,
                          SecurityService securityService,
                          PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.securityService = securityService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Admin> createAdmin(@Valid @RequestBody AdminDTO adminDto) {
        log.info("Création d'un nouvel administrateur");
        Admin admin = new Admin();
        admin.setNom(adminDto.getNom());
        admin.setPrenom(adminDto.getPrenom());
        admin.setAdresseMail(adminDto.getEmail());
        admin.setPassword(passwordEncoder.encode(adminDto.getPassword()));
        admin.setRole(RoleConfig.ADMIN);

        Admin savedAdmin = adminRepository.save(admin);
        return ResponseEntity.ok(savedAdmin);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Admin> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody org.example.collectfocep.dto.AdminDTO adminDto) {
        log.info("Mise à jour de l'administrateur: {}", id);
        return adminRepository.findById(id)
                .map(admin -> {
                    admin.setNom(adminDto.getNom());
                    admin.setPrenom(adminDto.getPrenom());
                    Admin updatedAdmin = adminRepository.save(admin);
                    return ResponseEntity.ok(updatedAdmin);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé"));
    }
}
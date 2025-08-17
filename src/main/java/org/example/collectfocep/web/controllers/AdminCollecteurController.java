package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.dto.JournalDTO;
import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.CollecteurMapper;
import org.example.collectfocep.mappers.ClientMapper;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.CollecteurService;
import org.example.collectfocep.services.interfaces.JournalService;
import org.example.collectfocep.services.interfaces.ClientService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/collecteurs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminCollecteurController {

    private final CollecteurService collecteurService;
    private final SecurityService securityService;
    private final CollecteurMapper collecteurMapper; // ✅ AJOUT
    private final JournalService journalService; // ✅ AJOUT
    private final ClientService clientService; // ✅ AJOUT
    private final ClientMapper clientMapper; // ✅ AJOUT

    /**
     * 🔥 ENDPOINT 1: Récupérer un collecteur par ID (pour admin)
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    public ResponseEntity<ApiResponse<CollecteurDTO>> getCollecteurById(@PathVariable Long id) {
        log.info("🔍 Admin récupération collecteur: {}", id);

        Collecteur collecteur = collecteurService.getCollecteurById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + id));

        CollecteurDTO dto = collecteurMapper.toDTO(collecteur);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * 🔥 ENDPOINT 2: Statistiques d'un collecteur
     */
    @GetMapping("/{id}/statistics")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCollecteurStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("📊 Stats collecteur {}: {} à {}", id, dateDebut, dateFin);

        // Dates par défaut si non spécifiées
        LocalDate debut = dateDebut != null ? dateDebut : LocalDate.now().minusMonths(1);
        LocalDate fin = dateFin != null ? dateFin : LocalDate.now();

        // ✅ CORRIGER: Utiliser la méthode avec bons paramètres
        Map<String, Object> stats = collecteurService.getCollecteurStatisticsWithDateRange(id, debut, fin);

        ApiResponse<Map<String, Object>> response = ApiResponse.success(stats);
        response.addMeta("collecteurId", id);
        response.addMeta("periode", debut + " au " + fin);

        return ResponseEntity.ok(response);
    }

    /**
     * 🔥 ENDPOINT 3: Journaux d'un collecteur
     */
    @GetMapping("/{id}/journaux")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    public ResponseEntity<ApiResponse<Page<JournalDTO>>> getCollecteurJournaux(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("📋 Journaux collecteur {}: page {}", id, page);

        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "dateCreation"));

        // ✅ UTILISER LA MÉTHODE CORRECTE
        Page<JournalDTO> journaux = journalService.getJournauxByCollecteurPaginated(id, pageRequest);

        return ResponseEntity.ok(ApiResponse.success(journaux));
    }

    /**
     * 🔥 ENDPOINT 4: Changer le statut d'un collecteur
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    public ResponseEntity<ApiResponse<String>> toggleCollecteurStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> statusData) {

        Boolean active = statusData.get("active");
        log.info("⚡ Changement statut collecteur {}: {}", id, active);

        // UTILISER LA MÉTHODE CORRECTE
        collecteurService.updateCollecteurStatus(id, active);

        String message = active ? "Collecteur activé" : "Collecteur suspendu";
        return ResponseEntity.ok(ApiResponse.success("OK", message));
    }

    /**
     * 🔥 ENDPOINT 5: Performance d'un collecteur
     */
    @GetMapping("/{id}/performance")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCollecteurPerformance(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {

        log.info("📈 Performance collecteur {} sur {} jours", id, days);

        LocalDate dateDebut = LocalDate.now().minusDays(days);
        LocalDate dateFin = LocalDate.now();

        // UTILISER LA MÉTHODE CORRECTE
        Map<String, Object> performance = collecteurService.getCollecteurPerformanceMetrics(id, dateDebut, dateFin);

        return ResponseEntity.ok(ApiResponse.success(performance));
    }

    /**
     * 🔥 ENDPOINT MANQUANT 1: Récupérer tous les collecteurs assignés à un admin
     */
    @GetMapping("/mes-collecteurs")
    public ResponseEntity<ApiResponse<Page<CollecteurDTO>>> getMesCollecteurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("👥 Admin demande ses collecteurs assignés - page={}, size={}", page, size);
        
        try {
            PageRequest pageRequest = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.ASC, "nom"));
            
            // Récupérer les collecteurs via le service existant
            Page<Collecteur> collecteursPage = collecteurService.getCollecteursByAgence(
                securityService.getCurrentUserAgenceId(), pageRequest
            );
            
            // Mapper vers DTOs
            Page<CollecteurDTO> dtoPage = collecteursPage.map(collecteurMapper::toDTO);
            
            return ResponseEntity.ok(ApiResponse.success(dtoPage, 
                String.format("Récupéré %d collecteurs", dtoPage.getNumberOfElements())));
                
        } catch (Exception e) {
            log.error("❌ Erreur récupération collecteurs assignés: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("COLLECTEURS_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * 🔥 ENDPOINT MANQUANT 2: Récupérer les clients d'un collecteur spécifique (pour admin)
     * RÉCUPÈRE DIRECTEMENT LES DONNÉES SANS REDIRECTION
     */
    @GetMapping("/{collecteurId}/clients")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<Page<ClientDTO>>> getCollecteurClients(
            @PathVariable Long collecteurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("📋 Admin demande clients du collecteur {} - page={}, size={}", collecteurId, page, size);
        
        try {
            // Récupérer directement les clients du collecteur via le service
            PageRequest pageRequest = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "dateCreation"));
            
            Page<Client> clientsPage = clientService.findByCollecteurId(collecteurId, pageRequest);
            
            // Convertir en DTOs
            Page<ClientDTO> clientsDTO = clientsPage.map(clientMapper::toDTO);
            
            log.info("✅ {} clients récupérés pour le collecteur {}", clientsDTO.getTotalElements(), collecteurId);
            
            return ResponseEntity.ok(ApiResponse.success(clientsDTO,
                String.format("Récupéré %d clients pour le collecteur %d", clientsDTO.getNumberOfElements(), collecteurId)));
                
        } catch (Exception e) {
            log.error("❌ Erreur récupération clients collecteur {}: {}", collecteurId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("CLIENTS_ERROR", "Erreur: " + e.getMessage()));
        }
    }
}
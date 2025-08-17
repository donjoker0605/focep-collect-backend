package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.ValidationException;
import org.example.collectfocep.services.SuperAdminValidationService;
import org.example.collectfocep.services.SuperAdminAgenceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation SuperAdmin pour gestion complète des agences
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SuperAdminAgenceServiceImpl implements SuperAdminAgenceService {

    private final AgenceRepository agenceRepository;
    private final ParametreCommissionRepository parametreCommissionRepository;
    private final AdminRepository adminRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminValidationService superAdminValidationService;
    private final CompteAgenceService compteAgenceService;

    // ================================
    // CRUD AGENCES
    // ================================

    @Override
    @Transactional(readOnly = true)
    public Page<AgenceDTO> getAllAgences(Pageable pageable) {
        log.info("📊 SuperAdmin - Récupération agences paginées");
        
        Page<Agence> agencesPage = agenceRepository.findAll(pageable);
        return agencesPage.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgenceDTO> getAllAgences() {
        log.info("📊 SuperAdmin - Récupération toutes agences");
        
        List<Agence> agences = agenceRepository.findAllOrderByNomAgence();
        return agences.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AgenceDTO getAgenceById(Long agenceId) {
        log.info("🔍 SuperAdmin - Récupération agence: {}", agenceId);
        
        Agence agence = agenceRepository.findByIdWithDetails(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        return mapToDTOWithDetails(agence);
    }

    @Override
    public AgenceDTO createAgence(AgenceDTO agenceDTO) {
        log.info("✨ SuperAdmin - Création agence: {}", agenceDTO.getNomAgence());
        
        // Validation business
        validateAgenceCreation(agenceDTO);
        
        Agence agence = mapToEntity(agenceDTO);
        
        // Générer code agence si nécessaire
        if (agence.getCodeAgence() == null || agence.getCodeAgence().isEmpty()) {
            agence.setCodeAgence(generateUniqueAgenceCode(agence.getNomAgence()));
        }
        
        agence.setActive(true);
        agence.setDateCreation(LocalDateTime.now());
        
        Agence savedAgence = agenceRepository.save(agence);
        
        // 🏦 CRÉER AUTOMATIQUEMENT TOUS LES COMPTES DE L'AGENCE
        try {
            log.info("🏗️ Création des comptes automatiques pour l'agence: {}", savedAgence.getNomAgence());
            compteAgenceService.createAllAgencyAccounts(savedAgence);
            log.info("✅ Comptes créés automatiquement pour l'agence: {}", savedAgence.getNomAgence());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création des comptes automatiques: {}", e.getMessage(), e);
            // Ne pas faire échouer la création d'agence pour les comptes
            // mais logger l'erreur pour investigation
        }
        
        // Créer paramètres de commission par défaut si fournis
        if (agenceDTO.getParametresCommission() != null && !agenceDTO.getParametresCommission().isEmpty()) {
            setAgenceCommissionParams(savedAgence.getId(), agenceDTO.getParametresCommission());
        } else {
            // Créer paramètres par défaut
            createDefaultCommissionParams(savedAgence.getId());
        }
        
        log.info("✅ Agence créée: {} - {}", savedAgence.getCodeAgence(), savedAgence.getNomAgence());
        return mapToDTO(savedAgence);
    }

    @Override
    public AgenceDTO updateAgence(Long agenceId, AgenceDTO agenceDTO) {
        log.info("🔄 SuperAdmin - Mise à jour agence: {}", agenceId);
        
        // Validation business
        validateAgenceUpdate(agenceId, agenceDTO);
        
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        // Mise à jour des champs
        agence.setNomAgence(agenceDTO.getNomAgence());
        agence.setAdresse(agenceDTO.getAdresse());
        agence.setVille(agenceDTO.getVille());
        agence.setQuartier(agenceDTO.getQuartier());
        agence.setTelephone(agenceDTO.getTelephone());
        agence.setResponsable(agenceDTO.getResponsable());
        agence.setDateModification(LocalDateTime.now());
        
        // Mise à jour du code agence si fourni et différent
        if (agenceDTO.getCodeAgence() != null && 
            !agenceDTO.getCodeAgence().equals(agence.getCodeAgence())) {
            
            if (agenceRepository.existsByCodeAgence(agenceDTO.getCodeAgence())) {
                throw new DuplicateResourceException("Code agence déjà utilisé: " + agenceDTO.getCodeAgence());
            }
            agence.setCodeAgence(agenceDTO.getCodeAgence());
        }
        
        Agence updatedAgence = agenceRepository.save(agence);
        
        log.info("✅ Agence mise à jour: {}", updatedAgence.getId());
        return mapToDTO(updatedAgence);
    }

    @Override
    public AgenceDTO toggleAgenceStatus(Long agenceId) {
        log.info("🔄 SuperAdmin - Toggle status agence: {}", agenceId);
        
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        boolean newStatus = !agence.isActive();
        agence.setActive(newStatus);
        agence.setDateModification(LocalDateTime.now());
        
        Agence updatedAgence = agenceRepository.save(agence);
        
        log.info("✅ Status agence modifié: {} -> {}", agenceId, newStatus ? "ACTIVE" : "INACTIVE");
        return mapToDTO(updatedAgence);
    }

    @Override
    public void deleteAgence(Long agenceId) {
        log.info("🗑️ SuperAdmin - Suppression agence: {}", agenceId);
        
        // Validation business
        validateAgenceDeletion(agenceId);
        
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        agenceRepository.delete(agence);
        
        log.info("✅ Agence supprimée: {}", agenceId);
    }

    // ================================
    // GESTION PARAMÈTRES COMMISSION
    // ================================

    @Override
    @Transactional(readOnly = true)
    public List<ParametreCommissionDTO> getAgenceCommissionParams(Long agenceId) {
        log.info("💰 SuperAdmin - Paramètres commission agence: {}", agenceId);
        
        // Vérifier que l'agence existe
        if (!agenceRepository.existsById(agenceId)) {
            throw new ResourceNotFoundException("Agence non trouvée: " + agenceId);
        }
        
        List<ParametreCommission> parametres = parametreCommissionRepository.findByAgenceId(agenceId);
        return parametres.stream()
                .map(this::mapParametreToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParametreCommissionDTO> setAgenceCommissionParams(Long agenceId, List<ParametreCommissionDTO> parametres) {
        log.info("💰 SuperAdmin - Définition paramètres commission agence: {}", agenceId);
        
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        // Supprimer les anciens paramètres
        parametreCommissionRepository.deleteByAgenceId(agenceId);
        
        // Créer les nouveaux paramètres
        List<ParametreCommission> nouveauxParametres = parametres.stream()
                .map(dto -> {
                    ParametreCommission param = mapParametreToEntity(dto);
                    param.setAgence(agence);
                    param.setDateCreation(LocalDateTime.now());
                    return param;
                })
                .collect(Collectors.toList());
        
        List<ParametreCommission> savedParametres = parametreCommissionRepository.saveAll(nouveauxParametres);
        
        log.info("✅ {} paramètres commission définis pour agence: {}", savedParametres.size(), agenceId);
        
        return savedParametres.stream()
                .map(this::mapParametreToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ParametreCommissionDTO updateCommissionParam(Long agenceId, Long parametreId, ParametreCommissionDTO parametre) {
        log.info("💰 SuperAdmin - Mise à jour paramètre commission: {} agence: {}", parametreId, agenceId);
        
        ParametreCommission existingParam = parametreCommissionRepository.findById(parametreId)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètre commission non trouvé: " + parametreId));
        
        if (!existingParam.getAgence().getId().equals(agenceId)) {
            throw new ValidationException("Le paramètre ne correspond pas à l'agence spécifiée");
        }
        
        // Mise à jour des champs
        existingParam.setTypeOperation(parametre.getTypeOperation());
        existingParam.setPourcentageCommission(parametre.getPourcentageCommission());
        existingParam.setMontantMinimum(parametre.getMontantMinimum());
        existingParam.setMontantMaximum(parametre.getMontantMaximum());
        existingParam.setActif(parametre.getActif());
        existingParam.setDateModification(LocalDateTime.now());
        
        ParametreCommission updatedParam = parametreCommissionRepository.save(existingParam);
        
        log.info("✅ Paramètre commission mis à jour: {}", parametreId);
        return mapParametreToDTO(updatedParam);
    }

    @Override
    public void deleteCommissionParam(Long agenceId, Long parametreId) {
        log.info("🗑️ SuperAdmin - Suppression paramètre commission: {} agence: {}", parametreId, agenceId);
        
        ParametreCommission param = parametreCommissionRepository.findById(parametreId)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètre commission non trouvé: " + parametreId));
        
        if (!param.getAgence().getId().equals(agenceId)) {
            throw new ValidationException("Le paramètre ne correspond pas à l'agence spécifiée");
        }
        
        parametreCommissionRepository.delete(param);
        
        log.info("✅ Paramètre commission supprimé: {}", parametreId);
    }

    // ================================
    // VALIDATION & BUSINESS RULES
    // ================================

    @Override
    public void validateAgenceCreation(AgenceDTO agenceDTO) {
        log.debug("🔍 Validation création agence: {}", agenceDTO.getNomAgence());
        
        // Validation nom agence
        if (agenceDTO.getNomAgence() == null || agenceDTO.getNomAgence().trim().isEmpty()) {
            throw new ValidationException("Le nom de l'agence est obligatoire");
        }
        
        if (agenceRepository.existsByNomAgence(agenceDTO.getNomAgence().trim())) {
            throw new DuplicateResourceException("Une agence avec ce nom existe déjà: " + agenceDTO.getNomAgence());
        }
        
        // Validation code agence si fourni
        if (agenceDTO.getCodeAgence() != null && !agenceDTO.getCodeAgence().trim().isEmpty()) {
            if (agenceRepository.existsByCodeAgence(agenceDTO.getCodeAgence().trim())) {
                throw new DuplicateResourceException("Une agence avec ce code existe déjà: " + agenceDTO.getCodeAgence());
            }
        }
        
        log.debug("✅ Validation création agence réussie");
    }

    @Override
    public void validateAgenceUpdate(Long agenceId, AgenceDTO agenceDTO) {
        log.debug("🔍 Validation mise à jour agence: {}", agenceId);
        
        // Validation nom agence
        if (agenceDTO.getNomAgence() == null || agenceDTO.getNomAgence().trim().isEmpty()) {
            throw new ValidationException("Le nom de l'agence est obligatoire");
        }
        
        // Vérifier unicité nom (exclure l'agence courante)
        agenceRepository.findByNomAgence(agenceDTO.getNomAgence().trim())
                .ifPresent(existingAgence -> {
                    if (!existingAgence.getId().equals(agenceId)) {
                        throw new DuplicateResourceException("Une autre agence avec ce nom existe déjà: " + agenceDTO.getNomAgence());
                    }
                });
        
        // Validation code agence si fourni
        if (agenceDTO.getCodeAgence() != null && !agenceDTO.getCodeAgence().trim().isEmpty()) {
            agenceRepository.findByCodeAgence(agenceDTO.getCodeAgence().trim())
                    .ifPresent(existingAgence -> {
                        if (!existingAgence.getId().equals(agenceId)) {
                            throw new DuplicateResourceException("Une autre agence avec ce code existe déjà: " + agenceDTO.getCodeAgence());
                        }
                    });
        }
        
        log.debug("✅ Validation mise à jour agence réussie");
    }

    @Override
    public void validateAgenceDeletion(Long agenceId) {
        log.debug("🔍 Validation suppression agence: {}", agenceId);
        
        Agence agence = agenceRepository.findByIdWithDetails(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        // Vérifier qu'il n'y a pas de collecteurs
        if (agence.getCollecteurs() != null && !agence.getCollecteurs().isEmpty()) {
            throw new ValidationException("Impossible de supprimer une agence ayant des collecteurs. " +
                    "Nombre de collecteurs: " + agence.getCollecteurs().size());
        }
        
        // Vérifier qu'il n'y a pas de clients
        if (agence.getClients() != null && !agence.getClients().isEmpty()) {
            throw new ValidationException("Impossible de supprimer une agence ayant des clients. " +
                    "Nombre de clients: " + agence.getClients().size());
        }
        
        log.debug("✅ Validation suppression agence réussie");
    }

    @Override
    public String generateUniqueAgenceCode(String nomAgence) {
        log.debug("🔧 Génération code agence pour: {}", nomAgence);
        
        // Nettoyer le nom et extraire le préfixe
        String cleanedName = nomAgence.replaceAll("[^A-Za-z]", "");
        String prefix = cleanedName.substring(0, Math.min(3, cleanedName.length())).toUpperCase();
        
        if (prefix.isEmpty()) {
            prefix = "AGE";
        }
        
        // Trouver un suffixe unique
        String code;
        int counter = 1;
        
        do {
            code = prefix + String.format("%03d", counter);
            counter++;
        } while (agenceRepository.existsByCodeAgence(code) && counter <= 999);
        
        if (counter > 999) {
            throw new ValidationException("Impossible de générer un code agence unique pour: " + nomAgence);
        }
        
        log.debug("✅ Code agence généré: {}", code);
        return code;
    }

    // ================================
    // STATISTIQUES & MONITORING
    // ================================

    @Override
    @Transactional(readOnly = true)
    public AgenceDTO getAgenceWithStats(Long agenceId) {
        log.info("📊 SuperAdmin - Statistiques agence: {}", agenceId);
        
        Agence agence = agenceRepository.findByIdWithDetails(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        return mapToDTOWithDetails(agence);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgenceDTO> getAgencesWithPerformance() {
        log.info("📊 SuperAdmin - Agences avec performance");
        
        List<Agence> agences = agenceRepository.findByPerformanceWithActiveCollecteurs();
        return agences.stream()
                .map(this::mapToDTOWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgenceDTO> getProblematicAgences() {
        log.info("⚠️ SuperAdmin - Agences problématiques");
        
        List<Agence> agences = agenceRepository.findAgencesWithoutCollecteurs();
        return agences.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long countAgencesByStatus(boolean active) {
        return agenceRepository.countByActive(active);
    }

    // ================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ================================
    
    private void createDefaultCommissionParams(Long agenceId) {
        log.info("💰 Création paramètres commission par défaut pour agence: {}", agenceId);
        
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        // Paramètres par défaut pour DEPOT
        ParametreCommission depotParam = ParametreCommission.builder()
                .agence(agence)
                .typeOperation(ParametreCommission.TypeOperation.DEPOT)
                .pourcentageCommission(new BigDecimal("2.0"))
                .montantFixe(new BigDecimal("0"))
                .montantMinimum(new BigDecimal("1000"))
                .montantMaximum(new BigDecimal("1000000"))
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .createdBy("SYSTEM")
                .build();
        
        // Paramètres par défaut pour RETRAIT
        ParametreCommission retraitParam = ParametreCommission.builder()
                .agence(agence)
                .typeOperation(ParametreCommission.TypeOperation.RETRAIT)
                .pourcentageCommission(new BigDecimal("1.5"))
                .montantFixe(new BigDecimal("0"))
                .montantMinimum(new BigDecimal("1000"))
                .montantMaximum(new BigDecimal("500000"))
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .createdBy("SYSTEM")
                .build();
        
        parametreCommissionRepository.save(depotParam);
        parametreCommissionRepository.save(retraitParam);
        
        log.info("✅ Paramètres commission par défaut créés pour agence: {}", agenceId);
    }

    // ================================
    // MÉTHODES DE MAPPING
    // ================================

    private AgenceDTO mapToDTO(Agence agence) {
        return AgenceDTO.builder()
                .id(agence.getId())
                .codeAgence(agence.getCodeAgence())
                .nomAgence(agence.getNomAgence())
                .adresse(agence.getAdresse())
                .ville(agence.getVille())
                .quartier(agence.getQuartier())
                .telephone(agence.getTelephone())
                .responsable(agence.getResponsable())
                .active(agence.getActive())
                .dateCreation(agence.getDateCreation())
                .dateModification(agence.getDateModification())
                .build();
    }

    private AgenceDTO mapToDTOWithDetails(Agence agence) {
        AgenceDTO dto = mapToDTO(agence);
        
        // Ajouter statistiques
        dto.setNombreCollecteurs(agence.getNombreCollecteurs());
        dto.setNombreCollecteursActifs(agence.getNombreCollecteursActifs());
        dto.setNombreClients(agence.getNombreClients());
        dto.setNombreClientsActifs(agence.getNombreClientsActifs());
        
        return dto;
    }

    private Agence mapToEntity(AgenceDTO dto) {
        return Agence.builder()
                .codeAgence(dto.getCodeAgence())
                .nomAgence(dto.getNomAgence())
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .quartier(dto.getQuartier())
                .telephone(dto.getTelephone())
                .responsable(dto.getResponsable())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

    private ParametreCommissionDTO mapParametreToDTO(ParametreCommission param) {
        return ParametreCommissionDTO.builder()
                .id(param.getId())
                .typeOperation(param.getTypeOperation())
                .typeOperationDisplay(param.getTypeOperation() != null ? param.getTypeOperation().getDisplayName() : null)
                .pourcentageCommission(param.getPourcentageCommission())
                .montantFixe(param.getMontantFixe())
                .montantMinimum(param.getMontantMinimum())
                .montantMaximum(param.getMontantMaximum())
                .actif(param.getActif())
                .agenceId(param.getAgence().getId())
                .agenceNom(param.getAgence().getNomAgence())
                .dateCreation(param.getDateCreation())
                .dateModification(param.getDateModification())
                .createdBy(param.getCreatedBy())
                .updatedBy(param.getUpdatedBy())
                .description(param.getDescription())
                .build();
    }

    private ParametreCommission mapParametreToEntity(ParametreCommissionDTO dto) {
        return ParametreCommission.builder()
                .typeOperation(dto.getTypeOperation())
                .pourcentageCommission(dto.getPourcentageCommission())
                .montantFixe(dto.getMontantFixe())
                .montantMinimum(dto.getMontantMinimum())
                .montantMaximum(dto.getMontantMaximum())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .createdBy(dto.getCreatedBy())
                .updatedBy(dto.getUpdatedBy())
                .build();
    }
    
    // ================================
    // NOUVELLES MÉTHODES POUR GESTION UTILISATEURS PAR AGENCE
    // ================================
    
    @Override
    @Transactional(readOnly = true)
    public List<SuperAdminDTO> getAdminsByAgence(Long agenceId) {
        log.info("👥 SuperAdmin - Récupération admins agence: {}", agenceId);
        
        List<Admin> admins = adminRepository.findByAgenceId(agenceId);
        return admins.stream()
                .map(this::mapAdminToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CollecteurDTO> getCollecteursByAgence(Long agenceId) {
        log.info("👥 SuperAdmin - Récupération collecteurs agence: {}", agenceId);
        
        List<Collecteur> collecteurs = collecteurRepository.findByAgenceId(agenceId);
        return collecteurs.stream()
                .map(this::mapCollecteurToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ClientDTO> getClientsByAgence(Long agenceId) {
        log.info("👥 SuperAdmin - Récupération clients agence: {}", agenceId);
        
        List<Client> clients = clientRepository.findByAgenceId(agenceId);
        return clients.stream()
                .map(this::mapClientToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ClientDTO> getClientsByCollecteur(Long collecteurId) {
        log.info("👥 SuperAdmin - Récupération clients collecteur: {}", collecteurId);
        
        List<Client> clients = clientRepository.findByCollecteurId(collecteurId);
        return clients.stream()
                .map(this::mapClientToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public AgenceDetailDTO getAgenceDetailsComplete(Long agenceId) {
        log.info("🏢 SuperAdmin - Récupération détails complets agence: {}", agenceId);
        
        Agence agence = agenceRepository.findByIdBasic(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + agenceId));
        
        // Récupération des utilisateurs
        List<SuperAdminDTO> admins = getAdminsByAgence(agenceId);
        List<CollecteurDTO> collecteurs = getCollecteursByAgence(agenceId);
        List<ClientDTO> clients = getClientsByAgence(agenceId);
        
        // Récupération des paramètres de commission
        List<ParametreCommissionDTO> parametresCommission = getAgenceCommissionParams(agenceId);
        
        // Calcul des statistiques
        long collecteursActifs = collecteurs.stream()
                .filter(c -> c.getActive() != null && c.getActive())
                .count();
        
        long clientsActifs = clients.stream()
                .filter(c -> c.getValide() != null && c.getValide())
                .count();
        
        return AgenceDetailDTO.builder()
                .id(agence.getId())
                .codeAgence(agence.getCodeAgence())
                .nomAgence(agence.getNomAgence())
                .adresse(agence.getAdresse())
                .telephone(agence.getTelephone())
                .responsable(agence.getResponsable())
                .active(agence.getActive())
                .dateCreation(agence.getDateCreation())
                .dateModification(agence.getDateModification())
                .nombreAdmins(admins.size())
                .nombreCollecteurs(collecteurs.size())
                .nombreCollecteursActifs((int) collecteursActifs)
                .nombreClients(clients.size())
                .nombreClientsActifs((int) clientsActifs)
                .admins(admins)
                .collecteurs(collecteurs)
                .clients(clients)
                .parametresCommission(parametresCommission)
                .build();
    }
    
    // ================================
    // MÉTHODES DE MAPPING POUR UTILISATEURS
    // ================================
    
    private SuperAdminDTO mapAdminToDTO(Admin admin) {
        SuperAdminDTO dto = new SuperAdminDTO();
        dto.setNom(admin.getNom());
        dto.setPrenom(admin.getPrenom());
        dto.setEmail(admin.getAdresseMail());
        dto.setTelephone(admin.getTelephone());
        dto.setNumeroCni(admin.getNumeroCni());
        return dto;
    }
    
    private CollecteurDTO mapCollecteurToDTO(Collecteur collecteur) {
        return CollecteurDTO.builder()
                .id(collecteur.getId())
                .nom(collecteur.getNom())
                .prenom(collecteur.getPrenom())
                .adresseMail(collecteur.getAdresseMail())
                .telephone(collecteur.getTelephone())
                .numeroCni(collecteur.getNumeroCni())
                .active(collecteur.getActive())
                .montantMaxRetrait(collecteur.getMontantMaxRetrait())
                .agenceId(collecteur.getAgence() != null ? collecteur.getAgence().getId() : null)
                .build();
    }
    
    private ClientDTO mapClientToDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setNom(client.getNom());
        dto.setPrenom(client.getPrenom());
        dto.setTelephone(client.getTelephone());
        dto.setNumeroCni(client.getNumeroCni());
        dto.setValide(client.getValide());
        dto.setCollecteurId(client.getCollecteur() != null ? client.getCollecteur().getId() : null);
        dto.setAgenceId(client.getAgence() != null ? client.getAgence().getId() : null);
        dto.setDateCreation(client.getDateCreation());
        return dto;
    }
    
    // ================================
    // IMPLÉMENTATION GESTION COMPLÈTE DES ADMINS
    // ================================
    
    @Override
    public SuperAdminAdminDTO createAdmin(CreateAdminDTO createAdminDTO) {
        log.info("✨ SuperAdmin - Création admin: {}", createAdminDTO.getAdresseMail());
        
        // Validation business
        validateAdminCreation(createAdminDTO);
        
        // Vérifier que l'agence existe
        Agence agence = agenceRepository.findById(createAdminDTO.getAgenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + createAdminDTO.getAgenceId()));
        
        // Créer l'admin
        Admin admin = Admin.builder()
                .nom(createAdminDTO.getNom())
                .prenom(createAdminDTO.getPrenom())
                .adresseMail(createAdminDTO.getAdresseMail())
                .password(passwordEncoder.encode(createAdminDTO.getPassword()))
                .numeroCni(createAdminDTO.getNumeroCni())
                .telephone(createAdminDTO.getTelephone())
                .role("ADMIN")
                .agence(agence)
                .dateCreation(LocalDateTime.now())
                .build();
        
        Admin savedAdmin = adminRepository.save(admin);
        
        log.info("✅ Admin créé: {} - {}", savedAdmin.getId(), savedAdmin.getAdresseMail());
        return mapAdminToSuperAdminDTO(savedAdmin);
    }
    
    @Override
    public SuperAdminAdminDTO updateAdmin(Long adminId, CreateAdminDTO updateAdminDTO) {
        log.info("🔄 SuperAdmin - Modification admin: {}", adminId);
        
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé: " + adminId));
        
        // Validation business
        validateAdminUpdate(adminId, updateAdminDTO);
        
        // Mise à jour des champs
        admin.setNom(updateAdminDTO.getNom());
        admin.setPrenom(updateAdminDTO.getPrenom());
        admin.setTelephone(updateAdminDTO.getTelephone());
        admin.setDateModification(LocalDateTime.now());
        
        // Mise à jour agence si différente
        if (!admin.getAgence().getId().equals(updateAdminDTO.getAgenceId())) {
            Agence newAgence = agenceRepository.findById(updateAdminDTO.getAgenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + updateAdminDTO.getAgenceId()));
            admin.setAgence(newAgence);
        }
        
        Admin updatedAdmin = adminRepository.save(admin);
        
        log.info("✅ Admin modifié: {}", updatedAdmin.getId());
        return mapAdminToSuperAdminDTO(updatedAdmin);
    }
    
    // ================================
    // IMPLÉMENTATION GESTION COMPLÈTE DES COLLECTEURS
    // ================================
    
    @Override
    @Transactional(readOnly = true)
    public List<CollecteurDTO> getAllCollecteurs() {
        log.info("📋 SuperAdmin - Récupération de tous les collecteurs");
        
        List<Collecteur> collecteurs = collecteurRepository.findAll();
        return collecteurs.stream()
                .map(this::mapCollecteurToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public CollecteurDTO getCollecteurDetails(Long collecteurId) {
        log.info("🔍 SuperAdmin - Détails collecteur: {}", collecteurId);
        
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));
        
        return mapCollecteurToDTO(collecteur);
    }
    
    @Override
    public CollecteurDTO createCollecteur(CreateCollecteurDTO createCollecteurDTO) {
        log.info("✨ SuperAdmin - Création collecteur: {}", createCollecteurDTO.getAdresseMail());
        
        // Validation business
        validateCollecteurCreation(createCollecteurDTO);
        
        // Vérifier que l'agence et l'admin existent
        if (!agenceRepository.existsById(createCollecteurDTO.getAgenceId())) {
            throw new ResourceNotFoundException("Agence non trouvée: " + createCollecteurDTO.getAgenceId());
        }
        
        if (!adminRepository.existsById(createCollecteurDTO.getAdminId())) {
            throw new ResourceNotFoundException("Admin non trouvé: " + createCollecteurDTO.getAdminId());
        }
        
        // Créer le collecteur
        Collecteur collecteur = Collecteur.builder()
                .nom(createCollecteurDTO.getNom())
                .prenom(createCollecteurDTO.getPrenom())
                .adresseMail(createCollecteurDTO.getAdresseMail())
                .password(passwordEncoder.encode(createCollecteurDTO.getPassword()))
                .numeroCni(createCollecteurDTO.getNumeroCni())
                .telephone(createCollecteurDTO.getTelephone())
                .role("COLLECTEUR")
                .montantMaxRetrait(createCollecteurDTO.getMontantMaxRetrait())
                .active(createCollecteurDTO.getActive())
                .agenceId(createCollecteurDTO.getAgenceId())
                .dateCreation(LocalDateTime.now())
                .build();
        
        Collecteur savedCollecteur = collecteurRepository.save(collecteur);
        
        // Créer les paramètres de commission si fournis
        if (createCollecteurDTO.hasCommissionParams()) {
            createCollecteurCommissionParams(savedCollecteur.getId(), createCollecteurDTO.getParametresCommission());
        }
        
        log.info("✅ Collecteur créé: {} - {}", savedCollecteur.getId(), savedCollecteur.getAdresseMail());
        return mapCollecteurToDTO(savedCollecteur);
    }
    
    @Override
    public CollecteurDTO updateCollecteur(Long collecteurId, CreateCollecteurDTO updateCollecteurDTO) {
        log.info("🔄 SuperAdmin - Modification collecteur: {}", collecteurId);
        
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));
        
        // Validation business
        validateCollecteurUpdate(collecteurId, updateCollecteurDTO);
        
        // Mise à jour des champs
        collecteur.setNom(updateCollecteurDTO.getNom());
        collecteur.setPrenom(updateCollecteurDTO.getPrenom());
        collecteur.setTelephone(updateCollecteurDTO.getTelephone());
        collecteur.setMontantMaxRetrait(updateCollecteurDTO.getMontantMaxRetrait());
        collecteur.setDateModification(LocalDateTime.now());
        
        // Mise à jour agence si différente
        if (!collecteur.getAgenceId().equals(updateCollecteurDTO.getAgenceId())) {
            if (!agenceRepository.existsById(updateCollecteurDTO.getAgenceId())) {
                throw new ResourceNotFoundException("Agence non trouvée: " + updateCollecteurDTO.getAgenceId());
            }
            collecteur.setAgenceId(updateCollecteurDTO.getAgenceId());
        }
        
        Collecteur updatedCollecteur = collecteurRepository.save(collecteur);
        
        log.info("✅ Collecteur modifié: {}", updatedCollecteur.getId());
        return mapCollecteurToDTO(updatedCollecteur);
    }
    
    @Override
    public CollecteurDTO toggleCollecteurStatus(Long collecteurId) {
        log.info("🔄 SuperAdmin - Toggle status collecteur: {}", collecteurId);
        
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));
        
        boolean newStatus = !collecteur.getActive();
        collecteur.setActive(newStatus);
        collecteur.setDateModification(LocalDateTime.now());
        
        Collecteur updatedCollecteur = collecteurRepository.save(collecteur);
        
        log.info("✅ Status collecteur modifié: {} -> {}", collecteurId, newStatus ? "ACTIF" : "INACTIF");
        return mapCollecteurToDTO(updatedCollecteur);
    }
    
    // ================================
    // IMPLÉMENTATION JOURNAUX D'ACTIVITÉS
    // ================================
    
    @Override
    @Transactional(readOnly = true)
    public List<JournalDTO> getAllJournaux(int page, int size, Long agenceId, Long collecteurId) {
        log.info("📋 SuperAdmin - Récupération journaux avec filtres: agence={}, collecteur={}", agenceId, collecteurId);
        
        // Pour l'instant, retournons une liste vide - l'implémentation complète nécessite le repository Journal
        return new ArrayList<>();
    }
    
    // ================================
    // IMPLÉMENTATION GESTION CLIENTS
    // ================================
    
    @Override
    @Transactional(readOnly = true)
    public List<ClientDTO> getAllClients(int page, int size, Long agenceId, Long collecteurId) {
        log.info("📋 SuperAdmin - Récupération clients avec filtres: agence={}, collecteur={}", agenceId, collecteurId);
        
        List<Client> clients;
        
        if (collecteurId != null) {
            clients = clientRepository.findByCollecteurId(collecteurId);
        } else if (agenceId != null) {
            clients = clientRepository.findByAgenceId(agenceId);
        } else {
            clients = clientRepository.findAll();
        }
        
        return clients.stream()
                .map(this::mapClientToDTO)
                .collect(Collectors.toList());
    }
    
    // ================================
    // MÉTHODES DE VALIDATION
    // ================================
    
    private void validateAdminCreation(CreateAdminDTO createAdminDTO) {
        // Vérifier unicité email
        if (utilisateurRepository.existsByAdresseMail(createAdminDTO.getAdresseMail())) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe déjà: " + createAdminDTO.getAdresseMail());
        }
        
        // Vérifier unicité CNI
        if (utilisateurRepository.existsByNumeroCni(createAdminDTO.getNumeroCni())) {
            throw new DuplicateResourceException("Un utilisateur avec ce CNI existe déjà: " + createAdminDTO.getNumeroCni());
        }
    }
    
    private void validateAdminUpdate(Long adminId, CreateAdminDTO updateAdminDTO) {
        // Vérifier unicité email (exclure l'admin courant)
        utilisateurRepository.findByAdresseMail(updateAdminDTO.getAdresseMail())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(adminId)) {
                        throw new DuplicateResourceException("Un autre utilisateur avec cet email existe déjà: " + updateAdminDTO.getAdresseMail());
                    }
                });
        
        // Vérifier unicité CNI (exclure l'admin courant)
        utilisateurRepository.findByNumeroCni(updateAdminDTO.getNumeroCni())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(adminId)) {
                        throw new DuplicateResourceException("Un autre utilisateur avec ce CNI existe déjà: " + updateAdminDTO.getNumeroCni());
                    }
                });
    }
    
    private void validateCollecteurCreation(CreateCollecteurDTO createCollecteurDTO) {
        // Vérifier unicité email
        if (utilisateurRepository.existsByAdresseMail(createCollecteurDTO.getAdresseMail())) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe déjà: " + createCollecteurDTO.getAdresseMail());
        }
        
        // Vérifier unicité CNI
        if (utilisateurRepository.existsByNumeroCni(createCollecteurDTO.getNumeroCni())) {
            throw new DuplicateResourceException("Un utilisateur avec ce CNI existe déjà: " + createCollecteurDTO.getNumeroCni());
        }
    }
    
    private void validateCollecteurUpdate(Long collecteurId, CreateCollecteurDTO updateCollecteurDTO) {
        // Vérifier unicité email (exclure le collecteur courant)
        utilisateurRepository.findByAdresseMail(updateCollecteurDTO.getAdresseMail())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(collecteurId)) {
                        throw new DuplicateResourceException("Un autre utilisateur avec cet email existe déjà: " + updateCollecteurDTO.getAdresseMail());
                    }
                });
    }
    
    private void createCollecteurCommissionParams(Long collecteurId, List<ParametreCommissionDTO> parametres) {
        log.info("💰 Création paramètres commission pour collecteur: {}", collecteurId);
        
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));
        
        Agence agence = agenceRepository.findById(collecteur.getAgenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée: " + collecteur.getAgenceId()));
        
        List<ParametreCommission> nouveauxParametres = parametres.stream()
                .map(dto -> {
                    ParametreCommission param = mapParametreToEntity(dto);
                    param.setAgence(agence);
                    param.setDateCreation(LocalDateTime.now());
                    return param;
                })
                .collect(Collectors.toList());
        
        parametreCommissionRepository.saveAll(nouveauxParametres);
        
        log.info("✅ {} paramètres commission créés pour collecteur: {}", nouveauxParametres.size(), collecteurId);
    }
    
    private SuperAdminAdminDTO mapAdminToSuperAdminDTO(Admin admin) {
        return SuperAdminAdminDTO.builder()
                .id(admin.getId())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .email(admin.getAdresseMail())
                .agenceId(admin.getAgence() != null ? admin.getAgence().getId() : null)
                .agenceNom(admin.getAgence() != null ? admin.getAgence().getNomAgence() : null)
                .dateCreation(admin.getDateCreation())
                .active(true) // À adapter selon votre logique
                .build();
    }
}
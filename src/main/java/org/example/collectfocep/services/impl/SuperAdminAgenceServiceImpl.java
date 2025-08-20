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
import java.util.function.Function;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
    private final MouvementRepository mouvementRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminValidationService superAdminValidationService;
    private final CompteAgenceService compteAgenceService;
    
    // Repositories pour les comptes d'agence
    private final CompteAgenceRepository compteAgenceRepository;
    // TODO: Ajouter ces repositories quand nécessaire
    // private final CompteProduitCollecteRepository compteProduitCollecteRepository;
    // private final CompteChargeCollecteRepository compteChargeCollecteRepository;
    // private final ComptePassageCommissionCollecteRepository comptePassageCommissionCollecteRepository;
    // private final ComptePassageTaxeRepository comptePassageTaxeRepository;
    // private final CompteTaxeRepository compteTaxeRepository;

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
        
        // Récupération des collecteurs avec comptes (première requête)
        List<Collecteur> collecteurs = collecteurRepository.findByAgenceIdWithFullData(agenceId);
        
        // Récupération des collecteurs avec clients (deuxième requête) 
        List<Collecteur> collecteursWithClients = collecteurRepository.findByAgenceIdWithClients(agenceId);
        
        // Fusion des données clients dans les collecteurs principaux
        Map<Long, List<Client>> clientsMap = collecteursWithClients.stream()
                .collect(Collectors.toMap(
                    Collecteur::getId,
                    c -> c.getClients() != null ? c.getClients() : new ArrayList<>()
                ));
        
        // Assignation des clients aux collecteurs
        collecteurs.forEach(c -> {
            if (clientsMap.containsKey(c.getId())) {
                c.setClients(clientsMap.get(c.getId()));
            }
        });
        
        return collecteurs.stream()
                .map(this::mapCollecteurToEnrichedDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ClientDTO> getClientsByAgence(Long agenceId) {
        log.info("💰 SuperAdmin - Récupération clients agence avec données enrichies: {}", agenceId);
        
        List<Client> clients = clientRepository.findByAgenceIdWithFullData(agenceId);
        return clients.stream()
                .map(this::mapClientToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ClientDTO> getClientsByCollecteur(Long collecteurId) {
        log.info("💰 SuperAdmin - Récupération clients collecteur avec données enrichies: {}", collecteurId);
        
        List<Client> clients = clientRepository.findByCollecteurIdWithFullData(collecteurId);
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
        
        // Récupération des soldes des comptes d'agence
        Map<String, BigDecimal> soldesComptes = getSoldesComptesAgence(agenceId);
        
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
                .soldesComptes(soldesComptes)
                .build();
    }
    
    /**
     * Récupère les soldes de tous les comptes d'agence
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getSoldesComptesAgence(Long agenceId) {
        log.info("💰 SuperAdmin - Récupération soldes comptes agence: {}", agenceId);
        
        Map<String, BigDecimal> soldes = new HashMap<>();
        
        try {
            // Compte Agence (C.A)
            Optional<CompteAgence> compteAgenceOpt = compteAgenceRepository.findByAgenceId(agenceId);
            if (compteAgenceOpt.isPresent()) {
                CompteAgence compteAgence = compteAgenceOpt.get();
                soldes.put("compte_agence", BigDecimal.valueOf(compteAgence.getSolde()));
            } else {
                soldes.put("compte_agence", BigDecimal.ZERO);
            }
            
            // TODO: Réactiver quand les repositories seront disponibles
            /*
            // Compte Produit Collecte (C.P.C)
            CompteProduitCollecte compteProduitCollecte = compteProduitCollecteRepository.findByAgenceId(agenceId);
            if (compteProduitCollecte != null) {
                soldes.put("compte_produit_collecte", BigDecimal.valueOf(compteProduitCollecte.getSolde()));
            } else {
                soldes.put("compte_produit_collecte", BigDecimal.ZERO);
            }
            
            // Compte Charge Collecte (C.C.C)
            CompteChargeCollecte compteChargeCollecte = compteChargeCollecteRepository.findByAgenceId(agenceId);
            if (compteChargeCollecte != null) {
                soldes.put("compte_charge_collecte", BigDecimal.valueOf(compteChargeCollecte.getSolde()));
            } else {
                soldes.put("compte_charge_collecte", BigDecimal.ZERO);
            }
            
            // Compte Passage Commission Collecte (C.P.C.C)
            ComptePassageCommissionCollecte comptePassageCommissionCollecte = 
                comptePassageCommissionCollecteRepository.findByAgenceId(agenceId);
            if (comptePassageCommissionCollecte != null) {
                soldes.put("compte_passage_commission_collecte", 
                    BigDecimal.valueOf(comptePassageCommissionCollecte.getSolde()));
            } else {
                soldes.put("compte_passage_commission_collecte", BigDecimal.ZERO);
            }
            
            // Compte Passage Taxe (C.P.T)
            ComptePassageTaxe comptePassageTaxe = comptePassageTaxeRepository.findByAgenceId(agenceId);
            if (comptePassageTaxe != null) {
                soldes.put("compte_passage_taxe", BigDecimal.valueOf(comptePassageTaxe.getSolde()));
            } else {
                soldes.put("compte_passage_taxe", BigDecimal.ZERO);
            }
            */
            
            /*
            // Compte Taxe (C.T)
            CompteTaxe compteTaxe = compteTaxeRepository.findByAgenceId(agenceId);
            if (compteTaxe != null) {
                soldes.put("compte_taxe", BigDecimal.valueOf(compteTaxe.getSolde()));
            } else {
                soldes.put("compte_taxe", BigDecimal.ZERO);
            }
            */
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des soldes des comptes d'agence {}: {}", 
                agenceId, e.getMessage());
            // Retour des soldes à zéro en cas d'erreur
            soldes.put("compte_agence", BigDecimal.ZERO);
            soldes.put("compte_produit_collecte", BigDecimal.ZERO);
            soldes.put("compte_charge_collecte", BigDecimal.ZERO);
            soldes.put("compte_passage_commission_collecte", BigDecimal.ZERO);
            soldes.put("compte_passage_taxe", BigDecimal.ZERO);
            soldes.put("compte_taxe", BigDecimal.ZERO);
        }
        
        return soldes;
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

    /**
     * 💰 Map Collecteur vers CollecteurDTO avec données enrichies (comptes, clients, ancienneté)
     */
    private CollecteurDTO mapCollecteurToEnrichedDTO(Collecteur collecteur) {
        CollecteurDTO dto = new CollecteurDTO();
        
        // Données de base
        dto.setId(collecteur.getId());
        dto.setNom(collecteur.getNom());
        dto.setPrenom(collecteur.getPrenom());
        dto.setAdresseMail(collecteur.getAdresseMail());
        dto.setTelephone(collecteur.getTelephone());
        dto.setNumeroCni(collecteur.getNumeroCni());
        dto.setActive(collecteur.getActive());
        dto.setDateCreation(collecteur.getDateCreation());
        dto.setDateModification(collecteur.getDateModification());
        
        // Nom complet
        dto.setNomComplet(String.format("%s %s", collecteur.getPrenom(), collecteur.getNom()));
        
        // Montant max retrait
        if (collecteur.getMontantMaxRetrait() != null) {
            dto.setMontantMaxRetrait(collecteur.getMontantMaxRetrait());
        }
        
        // Informations agence
        if (collecteur.getAgence() != null) {
            dto.setAgenceId(collecteur.getAgence().getId());
            dto.setAgenceNom(collecteur.getAgence().getNomAgence());
        }
        
        // Ancienneté et niveau
        dto.setAncienneteEnMois(collecteur.getAncienneteEnMois());
        dto.setNiveauAnciennete(collecteur.getNiveauAnciennete());
        dto.setAncienneteSummary(collecteur.getAncienneteSummary());
        dto.setCoefficientAnciennete(collecteur.getCoefficientAnciennete());
        
        // Comptes financiers
        if (collecteur.getComptes() != null && !collecteur.getComptes().isEmpty()) {
            // Calculer soldes par type de compte
            double soldeSalaire = collecteur.getComptes().stream()
                    .filter(compte -> "REMUNERATION".equals(compte.getTypeCompte()))
                    .mapToDouble(compte -> compte.getSolde())
                    .sum();
            
            double soldeManquant = collecteur.getComptes().stream()
                    .filter(compte -> "MANQUANT".equals(compte.getTypeCompte()))
                    .mapToDouble(compte -> compte.getSolde())
                    .sum();
            
            double soldeService = collecteur.getComptes().stream()
                    .filter(compte -> "SERVICE".equals(compte.getTypeCompte()))
                    .mapToDouble(compte -> compte.getSolde())
                    .sum();
            
            dto.setSoldeSalaire(soldeSalaire);
            dto.setSoldeManquant(soldeManquant);
            dto.setSoldeService(soldeService);
            dto.setSoldeTotal(soldeSalaire + soldeManquant + soldeService);
        }
        
        // Statistiques clients
        if (collecteur.getClients() != null) {
            dto.setNombreClients(collecteur.getClients().size());
            dto.setNombreClientsActifs((int) collecteur.getClients().stream()
                    .filter(client -> Boolean.TRUE.equals(client.getValide()))
                    .count());
            
            // TODO: Calculer le solde total des clients via une requête séparée si nécessaire
            dto.setSoldeTotalClients(0.0);
        } else {
            dto.setNombreClients(0);
            dto.setNombreClientsActifs(0);
            dto.setSoldeTotalClients(0.0);
        }
        
        return dto;
    }
    
    /**
     * 🎯 Map CollecteurOptimizedDTO vers CollecteurDTO (pour compatibilité avec l'API existante)
     */
    private CollecteurDTO mapOptimizedToCollecteurDTO(CollecteurOptimizedDTO optimized) {
        CollecteurDTO dto = new CollecteurDTO();
        
        // Données de base
        dto.setId(optimized.getId());
        dto.setNom(optimized.getNom());
        dto.setPrenom(optimized.getPrenom());
        dto.setAdresseMail(optimized.getAdresseMail());
        dto.setTelephone(optimized.getTelephone());
        dto.setNumeroCni(optimized.getNumeroCni());
        dto.setActive(optimized.getActive());
        dto.setMontantMaxRetrait(optimized.getMontantMaxRetrait() != null 
            ? BigDecimal.valueOf(optimized.getMontantMaxRetrait()) 
            : null);
        dto.setNomComplet(String.format("%s %s", optimized.getPrenom(), optimized.getNom()));
        
        // Ancienneté
        dto.setAncienneteEnMois(optimized.getAncienneteEnMois());
        
        // Agence
        if (optimized.getAgence() != null) {
            dto.setAgenceId(optimized.getAgence().getId());
            dto.setAgenceNom(optimized.getAgence().getNom());
        }
        
        // Statistiques clients  
        if (optimized.getStatistiques() != null) {
            dto.setNombreClients(optimized.getStatistiques().getNombreClients().intValue());
            dto.setNombreClientsActifs(optimized.getStatistiques().getNombreClientsActifs().intValue());
        }
        
        // Soldes des comptes
        if (optimized.getComptes() != null) {
            dto.setSoldeService(optimized.getComptes().getSoldeCompteService());
            dto.setSoldeSalaire(optimized.getComptes().getSoldeCompteSalaire());
            dto.setSoldeManquant(optimized.getComptes().getSoldeCompteManquant());
            dto.setSoldeTotal(optimized.getComptes().getSoldeTotalDisponible());
        }
        
        // Performance (nouvelles données)
        if (optimized.getPerformance() != null) {
            dto.setSoldeTotalClients(optimized.getPerformance().getTotalEpargneCollectee());
        }
        
        return dto;
    }
    
    /**
     * 💰 Map Client vers ClientDTO avec données enrichies (finances, localisation, commission)
     */
    private ClientDTO mapClientToDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        
        // Données de base
        dto.setId(client.getId());
        dto.setNom(client.getNom());
        dto.setPrenom(client.getPrenom());
        dto.setTelephone(client.getTelephone());
        dto.setNumeroCni(client.getNumeroCni());
        dto.setVille(client.getVille());
        dto.setQuartier(client.getQuartier());
        dto.setPhotoPath(client.getPhotoPath());
        dto.setValide(client.getValide());
        dto.setNumeroCompte(client.getNumeroCompte());
        dto.setDateCreation(client.getDateCreation());
        dto.setDateModification(client.getDateModification());
        
        // Relations principales
        dto.setCollecteurId(client.getCollecteur() != null ? client.getCollecteur().getId() : null);
        dto.setAgenceId(client.getAgence() != null ? client.getAgence().getId() : null);
        
        // 🌍 GÉOLOCALISATION
        if (client.getLatitude() != null) {
            dto.setLatitude(client.getLatitude().doubleValue());
        }
        if (client.getLongitude() != null) {
            dto.setLongitude(client.getLongitude().doubleValue());
        }
        dto.setCoordonneesSaisieManuelle(client.getCoordonneesSaisieManuelle());
        dto.setAdresseComplete(client.getAdresseComplete());
        dto.setDateMajCoordonnees(client.getDateMajCoordonnees());
        
        // 💰 PARAMÈTRES DE COMMISSION
        // Récupérer le premier paramètre de commission actif pour ce client
        if (client.getCommissionParameters() != null && !client.getCommissionParameters().isEmpty()) {
            client.getActiveCommissionParameters().stream()
                    .findFirst()
                    .ifPresent(param -> {
                        CommissionParameterDTO commissionDTO = new CommissionParameterDTO();
                        commissionDTO.setId(param.getId());
                        commissionDTO.setType(param.getType());
                        commissionDTO.setValeur(param.getValeur().doubleValue()); // Conversion BigDecimal vers Double
                        commissionDTO.setCodeProduit(param.getCodeProduit()); // Utiliser le champ existant
                        commissionDTO.setActive(param.getActive());
                        commissionDTO.setValidFrom(param.getValidFrom()); // Utiliser les champs de date existants
                        commissionDTO.setValidTo(param.getValidTo());
                        dto.setCommissionParameter(commissionDTO);
                    });
        }
        
        // 💳 SOLDE DU COMPTE CLIENT - FIX POUR LES BALANCES À ZÉRO
        if (client.getCompteClient() != null) {
            dto.setSolde(client.getCompteClient().getSolde());
        } else {
            dto.setSolde(0.0); // Valeur par défaut si pas de compte
        }
        
        return dto;
    }

    /**
     * 💳 Map Mouvement vers MouvementDTO avec données enrichies
     */
    private MouvementDTO mapMouvementToDTO(Mouvement mouvement) {
        return MouvementDTO.builder()
                .id(mouvement.getId())
                .montant(mouvement.getMontant())
                .libelle(mouvement.getLibelle())
                .sens(mouvement.getSens())
                .dateOperation(mouvement.getDateOperation())
                .typeMouvement(mouvement.getTypeMouvement())
                // Utiliser seulement les champs existants dans l'entité Mouvement
                .journalId(mouvement.getJournal() != null ? mouvement.getJournal().getId() : null)
                .compteSourceId(mouvement.getCompteSource() != null ? mouvement.getCompteSource().getId() : null)
                .compteDestinationId(mouvement.getCompteDestination() != null ? mouvement.getCompteDestination().getId() : null)
                // Mapper les relations client et collecteur (si disponibles)
                .client(mouvement.getClient() != null ? ClientBasicDTO.builder()
                        .id(mouvement.getClient().getId())
                        .nom(mouvement.getClient().getNom())
                        .prenom(mouvement.getClient().getPrenom())
                        .numeroCompte(mouvement.getClient().getNumeroCompte())
                        .build() : null)
                .collecteur(mouvement.getCollecteur() != null ? CollecteurBasicDTO.builder()
                        .id(mouvement.getCollecteur().getId())
                        .nom(mouvement.getCollecteur().getNom())
                        .prenom(mouvement.getCollecteur().getPrenom())
                        .build() : null)
                .build();
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
        log.info("📋 SuperAdmin - Récupération de tous les collecteurs avec données enrichies (PROJECTION JPA)");
        
        try {
            // 🎯 Utilisation de la projection JPA optimisée
            List<CollecteurProjection> projections = collecteurRepository.findAllCollecteursWithData();
            
            return projections.stream()
                    .map(projection -> {
                        // Convertir la projection en DTO optimisé puis en CollecteurDTO
                        CollecteurOptimizedDTO optimized = CollecteurOptimizedDTO.fromProjection(projection);
                        return mapOptimizedToCollecteurDTO(optimized);
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.warn("⚠️ Échec projection JPA, fallback vers méthode classique: {}", e.getMessage());
            
            // FALLBACK: Ancienne méthode si la projection échoue
            List<Collecteur> collecteursWithComptes = collecteurRepository.findAllWithComptes();
            Map<Long, Collecteur> collecteurMap = collecteursWithComptes.stream()
                    .collect(Collectors.toMap(Collecteur::getId, Function.identity()));
            
            List<Collecteur> collecteursWithClients = collecteurRepository.findAllWithClients();
            
            for (Collecteur collecteurWithClients : collecteursWithClients) {
                Collecteur collecteurWithComptes = collecteurMap.get(collecteurWithClients.getId());
                if (collecteurWithComptes != null) {
                    collecteurWithComptes.setClients(collecteurWithClients.getClients());
                }
            }
            
            return collecteursWithComptes.stream()
                    .map(this::mapCollecteurToEnrichedDTO)
                    .collect(Collectors.toList());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public CollecteurDTO getCollecteurDetails(Long collecteurId) {
        log.info("🔍 SuperAdmin - Détails collecteur avec données enrichies (PROJECTION): {}", collecteurId);
        
        try {
            // 🎯 Utiliser la projection JPA pour les détails 
            CollecteurProjection projection = collecteurRepository.findCollecteurWithDataById(collecteurId);
            if (projection != null) {
                CollecteurOptimizedDTO optimized = CollecteurOptimizedDTO.fromProjection(projection);
                return mapOptimizedToCollecteurDTO(optimized);
            }
        } catch (Exception e) {
            log.warn("⚠️ Échec projection JPA pour collecteur {}, fallback: {}", collecteurId, e.getMessage());
        }
        
        // FALLBACK: Ancienne méthode avec correction des soldes
        Collecteur collecteurWithComptes = collecteurRepository.findByIdWithComptes(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));
        
        Optional<Collecteur> collecteurWithClients = collecteurRepository.findByIdWithClients(collecteurId);
        if (collecteurWithClients.isPresent()) {
            collecteurWithComptes.setClients(collecteurWithClients.get().getClients());
        }
        
        return mapCollecteurToEnrichedDTO(collecteurWithComptes);
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
        log.info("💰 SuperAdmin - Récupération clients avec données enrichies: agence={}, collecteur={}", agenceId, collecteurId);
        
        List<Client> clients;
        
        // Utiliser les nouvelles méthodes avec EntityGraph pour récupérer toutes les données
        if (collecteurId != null) {
            log.debug("🔍 Récupération clients collecteur {} avec données complètes", collecteurId);
            clients = clientRepository.findByCollecteurIdWithFullData(collecteurId);
        } else if (agenceId != null) {
            log.debug("🏢 Récupération clients agence {} avec données complètes", agenceId);
            clients = clientRepository.findByAgenceIdWithFullData(agenceId);
        } else {
            log.debug("📋 Récupération tous clients avec données complètes");
            clients = clientRepository.findAllWithFullData();
        }
        
        log.info("✅ {} clients récupérés avec données financières complètes", clients.size());
        
        return clients.stream()
                .map(this::mapClientToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDTO getClientDetailsComplete(Long clientId) {
        log.info("💎 SuperAdmin - Récupération détails complets client: {}", clientId);
        
        Client client = clientRepository.findByIdWithCompleteData(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + clientId));
        
        ClientDTO clientDTO = mapClientToDTO(client);
        
        log.info("✅ Détails complets récupérés pour client: {} - {} {}", 
                clientId, client.getPrenom(), client.getNom());
        
        return clientDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementDTO> getClientTransactions(Long clientId, int page, int size) {
        log.info("💳 SuperAdmin - Récupération transactions client: {} (page={}, size={})", clientId, page, size);
        
        // Vérifier que le client existe
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client non trouvé: " + clientId);
        }
        
        // Récupérer les mouvements avec toutes les relations
        List<Mouvement> mouvements = mouvementRepository.findByClientIdWithAllRelations(clientId);
        
        // Limiter les résultats selon la pagination demandée
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, mouvements.size());
        
        List<Mouvement> pagedMouvements = fromIndex < mouvements.size() ? 
                mouvements.subList(fromIndex, toIndex) : List.of();
        
        List<MouvementDTO> transactionDTOs = pagedMouvements.stream()
                .map(this::mapMouvementToDTO)
                .collect(Collectors.toList());
        
        log.info("✅ {} transactions récupérées pour client {}", transactionDTOs.size(), clientId);
        
        return transactionDTOs;
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
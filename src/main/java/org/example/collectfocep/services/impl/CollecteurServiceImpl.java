package org.example.collectfocep.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.Validation.CollecteurValidator;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.*;
import org.example.collectfocep.mappers.CollecteurMapper;
import org.example.collectfocep.mappers.JournalMapper;
import org.example.collectfocep.mappers.MouvementMapperV2;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.CollecteurService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.services.interfaces.JournalService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CollecteurServiceImpl implements CollecteurService {

    @PersistenceContext
    private EntityManager entityManager;

    private final CollecteurRepository collecteurRepository;
    private final AgenceRepository agenceRepository;
    private final SecurityService securityService;
    private final CompteService compteService;
    private final HistoriqueMontantMaxRepository historiqueRepository;
    private final CollecteurValidator collecteurValidator;
    private final PasswordEncoder passwordEncoder;
    private final CollecteurMapper collecteurMapper;
    private final ClientRepository clientRepository;
    private final MouvementRepository mouvementRepository;
    private final JournalMapper journalMapper;
    private final JournalService journalService;
    private final MouvementMapperV2 mouvementMapper;
    private final CompteCollecteurRepository compteCollecteurRepository;

    // ================================
    // MÉTHODES PRINCIPALES - NOUVELLES ET SÉCURISÉES
    // ================================

    /**
     * MÉTHODE PRINCIPALE DE CRÉATION
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collecteur saveCollecteur(CollecteurCreateDTO dto) {
        try {
            log.info("Début de création du collecteur pour l'agence: {}", dto.getAgenceId());

            // SÉCURITÉ CRITIQUE: FORCER L'AGENCE DE L'ADMIN CONNECTÉ
            Long agenceIdFromAuth = securityService.getCurrentUserAgenceId();

            if (agenceIdFromAuth == null) {
                log.error("❌ Impossible de déterminer l'agence de l'utilisateur connecté");
                throw new UnauthorizedException("Accès non autorisé - agence non déterminée");
            }

            // FORCER L'AGENCE DE L'ADMIN - IGNORER CELLE ENVOYÉE PAR LE CLIENT
            dto.setAgenceId(agenceIdFromAuth);

            log.info("✅ Création d'un collecteur pour l'agence auto-assignée: {} par l'admin: {}",
                    agenceIdFromAuth, securityService.getCurrentUsername());

            // Vérifier que l'agence existe
            Agence agence = agenceRepository.findById(dto.getAgenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée avec l'ID: " + dto.getAgenceId()));

            // VÉRIFICATION UNICITÉ EMAIL
            if (collecteurRepository.existsByAdresseMail(dto.getAdresseMail())) {
                throw new BusinessException("Un collecteur avec cet email existe déjà: " + dto.getAdresseMail());
            }

            // Créer l'entité Collecteur via le mapper
            Collecteur collecteur = collecteurMapper.toEntity(dto);

            // Définir l'agence managée
            collecteur.setAgence(agence);

            // UTILISER LE MOT DE PASSE DU DTO
            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                log.info("✅ Utilisation du mot de passe fourni pour le collecteur");
                collecteur.setPassword(passwordEncoder.encode(dto.getPassword()));
            } else {
                log.warn("⚠️ Aucun mot de passe fourni, utilisation d'un mot de passe temporaire");
                String tempPassword = generateTemporaryPassword();
                collecteur.setPassword(passwordEncoder.encode(tempPassword));
                log.info("🔑 Mot de passe temporaire généré: {}", tempPassword);
            }

            // 🔥 MODIFICATION: Collecteurs créés inactifs par défaut selon requirements
            collecteur.setActive(dto.getActive() != null ? dto.getActive() : false);
            collecteur.setRole("COLLECTEUR");
            collecteur.setAncienneteEnMois(0);

            if (collecteur.getMontantMaxRetrait() == null) {
                collecteur.setMontantMaxRetrait(BigDecimal.valueOf(100000.0));
            }

            // Validation
            collecteurValidator.validateCollecteur(collecteur);

            // Sauvegarde avec flush pour récupérer l'ID
            Collecteur savedCollecteur = collecteurRepository.saveAndFlush(collecteur);
            entityManager.refresh(savedCollecteur);

            log.info("Création des comptes pour le nouveau collecteur: {}", savedCollecteur.getId());
            compteService.createCollecteurAccounts(savedCollecteur);

            log.info("✅ Collecteur et comptes créés avec succès: {} pour l'agence: {}",
                    savedCollecteur.getId(), agenceIdFromAuth);
            return savedCollecteur;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du collecteur", e);
            throw new CollecteurServiceException("Erreur lors de la création du collecteur: " + e.getMessage(), e);
        }
    }

    /**
     * Réinitialiser le mot de passe par l'admin
     */
    @Override
    @Transactional
    public void resetCollecteurPassword(Long collecteurId, String newPassword) {
        log.info("🔑 Réinitialisation du mot de passe pour le collecteur: {}", collecteurId);

        try {
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // VÉRIFICATION DE SÉCURITÉ
            if (!securityService.hasPermissionForCollecteur(collecteur.getId())) {
                throw new UnauthorizedException("Accès non autorisé à ce collecteur");
            }

            // Valider le nouveau mot de passe
            validatePassword(newPassword);

            // Encoder et sauvegarder
            collecteur.setPassword(passwordEncoder.encode(newPassword));
            collecteur.setDateModificationMontantMax(LocalDateTime.now()); // Pour tracer la modification
            collecteur.setModifiePar(securityService.getCurrentUsername());

            collecteurRepository.saveAndFlush(collecteur);

            log.info("✅ Mot de passe réinitialisé avec succès pour le collecteur: {}", collecteurId);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la réinitialisation du mot de passe", e);
            throw new CollecteurServiceException("Erreur lors de la réinitialisation: " + e.getMessage(), e);
        }
    }

    /**
     * RÉCUPÉRER LES COLLECTEURS FILTRÉS PAR AGENCE DE L'ADMIN CONNECTÉ
     */
    public Page<Collecteur> getCollecteursByAgence(Long agenceId, Pageable pageable) {
        log.info("👥 Récupération des collecteurs pour l'agence: {}", agenceId);

        // VÉRIFICATION DE SÉCURITÉ
        if (!securityService.isUserFromAgence(agenceId)) {
            throw new UnauthorizedException("Accès non autorisé à cette agence");
        }

        return collecteurRepository.findByAgenceId(agenceId, pageable);
    }

    /**
     * RECHERCHER LES COLLECTEURS PAR AGENCE AVEC TERME DE RECHERCHE
     */
    public Page<Collecteur> searchCollecteursByAgence(Long agenceId, String search, Pageable pageable) {
        log.info("🔍 Recherche de collecteurs dans l'agence {}: '{}'", agenceId, search);

        // VÉRIFICATION DE SÉCURITÉ
        if (!securityService.isUserFromAgence(agenceId)) {
            throw new UnauthorizedException("Accès non autorisé à cette agence");
        }

        return collecteurRepository.findByAgenceIdAndSearchTerm(agenceId, search, pageable);
    }

    /**
     * BASCULER LE STATUT D'UN COLLECTEUR AVEC SÉCURITÉ
     */
    @Transactional
    public Collecteur toggleCollecteurStatus(Long collecteurId) {
        log.info("🔄 Basculement du statut du collecteur: {}", collecteurId);

        try {
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // ✅ VÉRIFICATION DE SÉCURITÉ
            if (!securityService.hasPermissionForCollecteur(collecteur.getId())) {
                throw new UnauthorizedException("Accès non autorisé à ce collecteur");
            }

            // BASCULER LE STATUT
            boolean newStatus = !collecteur.getActive();
            collecteur.setActive(newStatus);

            Collecteur updatedCollecteur = collecteurRepository.saveAndFlush(collecteur);

            String action = newStatus ? "activé" : "désactivé";
            log.info("✅ Collecteur {} {} avec succès", collecteurId, action);

            return updatedCollecteur;

        } catch (Exception e) {
            log.error("❌ Erreur lors du basculement de statut du collecteur {}", collecteurId, e);
            throw new CollecteurServiceException("Erreur lors du changement de statut: " + e.getMessage(), e);
        }
    }

    /**
     * RÉCUPÉRER LES STATISTIQUES D'UN COLLECTEUR
     */
    public CollecteurStatisticsDTO getCollecteurStatistics(Long collecteurId) {
        log.info("📈 Récupération des statistiques pour le collecteur: {}", collecteurId);

        try {
            Collecteur collecteur = getCollecteurById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // VÉRIFICATION DE SÉCURITÉ
            if (!securityService.hasPermissionForCollecteur(collecteur.getId())) {
                throw new UnauthorizedException("Accès non autorisé à ce collecteur");
            }

            // Calculer les statistiques
            Long totalClients = clientRepository.countByCollecteurId(collecteurId);

            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime now = LocalDateTime.now();

            Double volumeEpargne = mouvementRepository.sumEpargneByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfMonth, now);
            Double volumeRetraits = mouvementRepository.sumRetraitByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfMonth, now);

            return CollecteurStatisticsDTO.builder()
                    .collecteurId(collecteurId)
                    .collecteurNom(collecteur.getNom())
                    .collecteurPrenom(collecteur.getPrenom())
                    .totalClients(totalClients != null ? totalClients : 0L)
                    .totalEpargne(volumeEpargne != null ? volumeEpargne : 0.0)
                    .totalRetraits(volumeRetraits != null ? volumeRetraits : 0.0)
                    .soldeNet((volumeEpargne != null ? volumeEpargne : 0.0) -
                            (volumeRetraits != null ? volumeRetraits : 0.0))
                    .dateCalcul(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques", e);
            throw new CollecteurServiceException("Erreur lors du calcul des statistiques: " + e.getMessage(), e);
        }
    }

    /**
     * MISE À JOUR SÉCURISÉE D'UN COLLECTEUR
     */
    @Override
    @Transactional
    public Collecteur updateCollecteur(Long id, CollecteurUpdateDTO dto) {
        try {
            Collecteur collecteur = collecteurRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé avec l'ID: " + id));

            // VÉRIFICATION DES DROITS D'ACCÈS
            if (!securityService.hasPermissionForCollecteur(collecteur.getId())) {
                throw new UnauthorizedException("Non autorisé à modifier ce collecteur");
            }

            // EMPÊCHER LE CHANGEMENT D'AGENCE (SÉCURITÉ)
            Long currentAgenceId = collecteur.getAgence().getId();

            // VÉRIFIER L'UNICITÉ DE L'EMAIL (SAUF POUR LE COLLECTEUR ACTUEL)
            if (dto.getAdresseMail() != null &&
                    !collecteur.getAdresseMail().equals(dto.getAdresseMail()) &&
                    collecteurRepository.existsByAdresseMail(dto.getAdresseMail())) {
                throw new BusinessException("Un collecteur avec cet email existe déjà");
            }

            // GESTION DU CHANGEMENT DE MOT DE PASSE
            if (dto.getNewPassword() != null && !dto.getNewPassword().trim().isEmpty()) {
                log.info("🔑 Changement de mot de passe demandé pour le collecteur: {}", id);
                validatePassword(dto.getNewPassword());
                collecteur.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                collecteur.setDateModificationMontantMax(LocalDateTime.now());
                collecteur.setModifiePar(securityService.getCurrentUsername());
            }

            // Mise à jour via mapper
            collecteurMapper.updateEntityFromDTO(dto, collecteur);

            // RESTAURER L'AGENCE ORIGINALE (SÉCURITÉ)
            collecteur.setAgence(agenceRepository.findById(currentAgenceId).orElse(collecteur.getAgence()));

            // Validation
            collecteurValidator.validateCollecteur(collecteur);

            Collecteur updatedCollecteur = collecteurRepository.saveAndFlush(collecteur);

            log.info("✅ Collecteur {} mis à jour avec succès", id);
            return updatedCollecteur;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour du collecteur {}", id, e);
            throw new CollecteurServiceException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    // ================================
    // MÉTHODES EXISTANTES CONSERVÉES
    // ================================

    @Override
    @Transactional
    public Collecteur updateMontantMaxRetrait(Long collecteurId, BigDecimal nouveauMontant, String justification) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        if (!securityService.hasPermissionForCollecteur(collecteur.getId())) {
            throw new UnauthorizedAccessException("Non autorisé à modifier le montant maximal");
        }

        // Sauvegarde de l'historique
        sauvegarderHistoriqueMontantMax(collecteur, justification);

        collecteur.setMontantMaxRetrait(nouveauMontant);
        collecteur.setDateModificationMontantMax(LocalDateTime.now());
        collecteur.setModifiePar(securityService.getCurrentUsername());

        return collecteurRepository.saveAndFlush(collecteur);
    }

    @Override
    @Cacheable(value = "collecteurs", key = "#id")
    public Optional<Collecteur> getCollecteurById(Long id) {
        return collecteurRepository.findByIdWithAgence(id);
    }

    @Override
    public CollecteurDTO convertToDTO(Collecteur collecteur) {
        if (collecteur == null) return null;

        // Mapping de base sans les collections lazy
        CollecteurDTO dto = collecteurMapper.toDTO(collecteur);

        // CALCUL OPTIMISÉ : Nombre de clients avec requête COUNT
        if (dto != null && collecteur.getId() != null) {
            try {
                // Requête COUNT optimisée pour les clients
                Long nombreClients = clientRepository.countByCollecteurId(collecteur.getId());
                dto.setNombreClients(nombreClients != null ? nombreClients.intValue() : 0);

                // Requête COUNT optimisée pour les comptes collecteur
                Long nombreComptes = compteCollecteurRepository.countByCollecteurId(collecteur.getId());
                dto.setNombreComptes(nombreComptes != null ? nombreComptes.intValue() : 0);

                log.debug("Collecteur {} - {} clients, {} comptes",
                        collecteur.getId(), dto.getNombreClients(), dto.getNombreComptes());

            } catch (Exception e) {
                log.warn("Erreur calcul nombres pour collecteur {}: {}",
                        collecteur.getId(), e.getMessage());
                // Valeurs par défaut en cas d'erreur
                dto.setNombreClients(0);
                dto.setNombreComptes(0);
            }
        }

        return dto;
    }

    /**
     *  Conversion avec calculs pour listes
     * Optimisée pour traiter plusieurs collecteurs en une fois
     */
    public List<CollecteurDTO> convertToDTOList(List<Collecteur> collecteurs) {
        if (collecteurs == null || collecteurs.isEmpty()) {
            return new ArrayList<>();
        }

        // Extraire les IDs pour requêtes groupées
        List<Long> collecteurIds = collecteurs.stream()
                .map(Collecteur::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Requêtes groupées pour tous les collecteurs
        Map<Long, Long> nombreClientsMap = new HashMap<>();
        Map<Long, Long> nombreComptesMap = new HashMap<>();

        try {
            // OPTIMISATION : Une seule requête pour tous les nombres de clients
            List<Object[]> clientCounts = clientRepository.countByCollecteurIds(collecteurIds);
            for (Object[] row : clientCounts) {
                Long collecteurId = (Long) row[0];
                Long count = (Long) row[1];
                nombreClientsMap.put(collecteurId, count);
            }

            // OPTIMISATION : Une seule requête pour tous les nombres de comptes
            List<Object[]> compteCounts = compteCollecteurRepository.countByCollecteurIds(collecteurIds);
            for (Object[] row : compteCounts) {
                Long collecteurId = (Long) row[0];
                Long count = (Long) row[1];
                nombreComptesMap.put(collecteurId, count);
            }

        } catch (Exception e) {
            log.warn("Erreur lors du calcul groupé des nombres: {}", e.getMessage());
        }

        // Conversion avec injection des nombres calculés
        return collecteurs.stream()
                .map(collecteur -> {
                    CollecteurDTO dto = collecteurMapper.toDTO(collecteur);
                    if (dto != null && collecteur.getId() != null) {
                        Long collecteurId = collecteur.getId();
                        dto.setNombreClients(nombreClientsMap.getOrDefault(collecteurId, 0L).intValue());
                        dto.setNombreComptes(nombreComptesMap.getOrDefault(collecteurId, 0L).intValue());
                    }
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Collecteur> findByAgenceId(Long agenceId) {
        return collecteurRepository.findByAgenceId(agenceId);
    }

    @Override
    public Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable) {
        return collecteurRepository.findByAgenceId(agenceId, pageable);
    }

    @Override
    @CacheEvict(value = "collecteurs", key = "#id")
    @Transactional
    public void deactivateCollecteur(Long id) {
        Collecteur collecteur = collecteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));
        collecteur.setActive(false);
        collecteurRepository.saveAndFlush(collecteur);
    }

    @Override
    public List<Collecteur> getAllCollecteurs() {
        return collecteurRepository.findAll();
    }

    @Override
    public Page<Collecteur> getAllCollecteurs(Pageable pageable) {
        return collecteurRepository.findAll(pageable);
    }

    @Override
    public boolean hasActiveOperations(Collecteur collecteur) {
        return collecteur.getClients() != null && !collecteur.getClients().isEmpty();
    }

    @Override
    public CollecteurDashboardDTO getDashboardStats(Long collecteurId) {
        log.info("Calcul des statistiques dashboard pour collecteur: {}", collecteurId);

        try {
            Collecteur collecteur = getCollecteurById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // Compter les clients
            Long totalClientsCount = clientRepository.countByCollecteurId(collecteurId);
            Integer totalClients = totalClientsCount != null ? totalClientsCount.intValue() : 0;

            // 🔥 NOUVEAU: Calculer depuis la dernière clôture au lieu du mois
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = getLastClosureDate(collecteurId);
            
            log.info("📅 Calcul depuis la dernière clôture: {} pour collecteur: {}", startDate, collecteurId);

            Double totalEpargne = mouvementRepository.sumEpargneByCollecteurIdAndDateOperationBetween(
                    collecteurId, startDate, now);

            Double totalRetraits = mouvementRepository.sumRetraitByCollecteurIdAndDateOperationBetween(
                    collecteurId, startDate, now);

            // Solde quotidien = épargne collectée - retraits distribués depuis dernière clôture
            Double soldeQuotidien = (totalEpargne != null ? totalEpargne : 0.0) -
                    (totalRetraits != null ? totalRetraits : 0.0);

            // Conserver soldeTotal pour compatibilité (même valeur)
            Double soldeTotal = soldeQuotidien;

            // 🆕 NOUVEAU: Calculs mensuels (mois calendaire)
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            
            Double epargneMensuelle = mouvementRepository.sumEpargneByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfMonth, now);
            
            Double retraitsMensuels = mouvementRepository.sumRetraitByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfMonth, now);
            
            Double soldeMensuel = (epargneMensuelle != null ? epargneMensuelle : 0.0) -
                    (retraitsMensuels != null ? retraitsMensuels : 0.0);
            
            log.info("📊 Calculs mensuels pour collecteur {}: épargne={}, retraits={}, solde={}", 
                    collecteurId, epargneMensuelle, retraitsMensuels, soldeMensuel);

            // Utiliser le bon type DTO
            List<CollecteurDashboardDTO.MouvementDTO> transactionsRecentes = List.of();

            try {
                // Récupérer les 5 dernières transactions
                PageRequest lastTransactions = PageRequest.of(0, 5, Sort.by("dateOperation").descending());
                Page<Mouvement> recentMovements = mouvementRepository
                        .findByCollecteurId(collecteurId, lastTransactions);

                // MAPPER CORRECTEMENT VERS LE BON TYPE
                transactionsRecentes = recentMovements.getContent()
                        .stream()
                        .map(this::mapToCollecteurDashboardMouvementDTO)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Erreur lors de la récupération des transactions récentes: {}", e.getMessage());
                transactionsRecentes = List.of();
            }

            // Récupérer le journal actuel (si existe)
            CollecteurDashboardDTO.JournalDTO journalActuel = null;
            try {
                Journal journal = journalService.getJournalActif(collecteurId);
                if (journal != null) {
                    journalActuel = mapToCollecteurDashboardJournalDTO(journal, soldeTotal);
                }
            } catch (Exception e) {
                log.warn("Aucun journal actif trouvé pour le collecteur: {}", collecteurId);
            }

            return CollecteurDashboardDTO.builder()
                    .collecteurId(collecteurId)
                    .collecteurNom(collecteur.getNom())
                    .collecteurPrenom(collecteur.getPrenom())
                    .totalClients(totalClients)
                    .totalEpargne(totalEpargne != null ? totalEpargne : 0.0)
                    .totalRetraits(totalRetraits != null ? totalRetraits : 0.0)
                    .soldeTotal(soldeTotal)
                    .soldeQuotidien(soldeQuotidien) // Depuis dernière clôture
                    .epargneMensuelle(epargneMensuelle != null ? epargneMensuelle : 0.0) // 🆕 NOUVEAU
                    .retraitsMensuels(retraitsMensuels != null ? retraitsMensuels : 0.0) // 🆕 NOUVEAU
                    .soldeMensuel(soldeMensuel) // 🆕 NOUVEAU
                    .transactionsRecentes(transactionsRecentes)
                    .journalActuel(journalActuel)
                    .lastUpdate(LocalDateTime.now())
                    .transactionsAujourdhui(0L)
                    .montantEpargneAujourdhui(0.0)
                    .montantRetraitAujourdhui(0.0)
                    .nouveauxClientsAujourdhui(0L)
                    .montantEpargneSemaine(0.0)
                    .montantRetraitSemaine(0.0)
                    .transactionsSemaine(0L)
                    .montantEpargneMois(epargneMensuelle != null ? epargneMensuelle : 0.0) // Cohérent
                    .montantRetraitMois(retraitsMensuels != null ? retraitsMensuels : 0.0) // Cohérent
                    .transactionsMois(0L)
                    .objectifMensuel(collecteur.getMontantMaxRetrait() != null ?
                            collecteur.getMontantMaxRetrait().doubleValue() : 100000.0)
                    .progressionObjectif(calculerProgressionObjectif(epargneMensuelle, collecteur.getMontantMaxRetrait()))
                    .commissionsMois(0.0)
                    .commissionsAujourdhui(0.0)
                    .clientsActifs(List.of())
                    .alertes(List.of())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du calcul des statistiques dashboard", e);
            throw new BusinessException("Erreur lors du calcul des statistiques: " + e.getMessage());
        }
    }

    /**
     * Statistiques avec plage de dates
     */
    @Override
    public Map<String, Object> getCollecteurStatisticsWithDateRange(Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        log.info("📊 Calcul statistiques collecteur {} du {} au {}", collecteurId, dateDebut, dateFin);

        // 1. Récupérer le collecteur
        Collecteur collecteur = getCollecteurById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        // 2. Compter les clients
        Long totalClients = clientRepository.countByCollecteurId(collecteurId);
        Long clientsActifs = clientRepository.countActiveByCollecteurId(collecteurId, dateDebut, dateFin);

        // 3. Calculer les montants
        Double totalEpargne = mouvementRepository.sumEpargneByCollecteur(collecteurId, dateDebut, dateFin);
        Double totalRetraits = mouvementRepository.sumRetraitsByCollecteur(collecteurId, dateDebut, dateFin);
        Double soldeNet = (totalEpargne != null ? totalEpargne : 0.0) - (totalRetraits != null ? totalRetraits : 0.0);

        // 4. Compter les transactions
        Long nombreTransactions = mouvementRepository.countByCollecteurAndPeriod(collecteurId, dateDebut, dateFin);

        // 5. Calculer les moyennes
        Double moyenneParClient = totalClients > 0 ? soldeNet / totalClients : 0.0;
        Double moyenneParTransaction = nombreTransactions > 0 ? totalEpargne / nombreTransactions : 0.0;

        // 6. Performance vs objectifs
        Double objectifMensuel = collecteur.getMontantMaxRetrait() != null ?
                collecteur.getMontantMaxRetrait().doubleValue() : 100000.0;
        Double tauxRealisation = objectifMensuel > 0 ? (totalEpargne != null ? totalEpargne : 0.0) / objectifMensuel * 100 : 0.0;

        // Utiliser HashMap au lieu de Map.of()
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("collecteurId", collecteurId);
        statistics.put("collecteurNom", collecteur.getNom());
        statistics.put("collecteurPrenom", collecteur.getPrenom());
        statistics.put("dateCalcul", LocalDateTime.now());
        statistics.put("totalClients", totalClients != null ? totalClients : 0L);
        statistics.put("clientsActifs", clientsActifs != null ? clientsActifs : 0L);
        statistics.put("totalEpargne", totalEpargne != null ? totalEpargne : 0.0);
        statistics.put("totalRetraits", totalRetraits != null ? totalRetraits : 0.0);
        statistics.put("soldeNet", soldeNet);
        statistics.put("nombreTransactions", nombreTransactions != null ? nombreTransactions : 0L);
        statistics.put("moyenneParClient", moyenneParClient);
        statistics.put("moyenneParTransaction", moyenneParTransaction);
        statistics.put("objectifMensuel", objectifMensuel);
        statistics.put("tauxRealisation", tauxRealisation);

        // Déterminer la performance
        String performance;
        if (tauxRealisation >= 100) {
            performance = "EXCELLENT";
        } else if (tauxRealisation >= 80) {
            performance = "BON";
        } else if (tauxRealisation >= 60) {
            performance = "MOYEN";
        } else {
            performance = "FAIBLE";
        }
        statistics.put("performance", performance);

        return statistics;
    }

    /**
     * Métriques de performance
     */
    @Override
    public Map<String, Object> getCollecteurPerformanceMetrics(Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        log.info("📈 Calcul performance collecteur {} du {} au {}", collecteurId, dateDebut, dateFin);

        // Évolution jour par jour
        List<Map<String, Object>> evolutionJournaliere = new ArrayList<>();
        LocalDate currentDate = dateDebut;

        while (!currentDate.isAfter(dateFin)) {
            Double epargneJour = mouvementRepository.sumEpargneByCollecteurAndDate(collecteurId, currentDate);
            Double retraitsJour = mouvementRepository.sumRetraitsByCollecteurAndDate(collecteurId, currentDate);

            // HashMap pour éviter les limites de Map.of()
            Map<String, Object> journee = new HashMap<>();
            journee.put("date", currentDate);
            journee.put("epargne", epargneJour != null ? epargneJour : 0.0);
            journee.put("retraits", retraitsJour != null ? retraitsJour : 0.0);
            journee.put("net", (epargneJour != null ? epargneJour : 0.0) - (retraitsJour != null ? retraitsJour : 0.0));

            evolutionJournaliere.add(journee);
            currentDate = currentDate.plusDays(1);
        }

        // Tendance (croissante, stable, décroissante)
        String tendance = calculerTendance(evolutionJournaliere);

        // Ranking par rapport aux autres collecteurs de l'agence
        Integer ranking = calculateCollecteurRanking(collecteurId, dateDebut, dateFin);

        //  HashMap pour le résultat final
        Map<String, Object> performance = new HashMap<>();
        performance.put("evolutionJournaliere", evolutionJournaliere);
        performance.put("tendance", tendance);
        performance.put("ranking", ranking);
        performance.put("periode", dateDebut + " au " + dateFin);

        return performance;
    }

    /**
     *  Mise à jour du statut
     */
    @Override
    public void updateCollecteurStatus(Long collecteurId, Boolean active) {
        log.info("⚡ Changement statut collecteur {}: {}", collecteurId, active);

        Collecteur collecteur = getCollecteurById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        collecteur.setActive(active);
        collecteurRepository.save(collecteur);

        log.info("✅ Statut collecteur {} mis à jour: {}", collecteurId, active);
    }

    // ================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ================================

    /**
     * Génère un mot de passe temporaire sécurisé
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();

        // Assurer au moins une majuscule, une minuscule et un chiffre
        password.append("C"); // Majuscule
        password.append("p"); // Minuscule
        password.append("1"); // Chiffre

        // Ajouter 5 caractères aléatoires
        for (int i = 0; i < 5; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }

        return password.toString();
    }

    /**
     * Valide un mot de passe selon les règles métier
     */
    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }

        if (password.length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit avoir au moins 6 caractères");
        }

        if (password.length() > 128) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas dépasser 128 caractères");
        }

        // Vérifier qu'il contient au moins une lettre
        if (!password.matches(".*[a-zA-Z].*")) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins une lettre");
        }
    }

    /**
     * Sauvegarde l'historique des modifications de montant maximum
     */
    private void sauvegarderHistoriqueMontantMax(Collecteur collecteur, String justification) {
        HistoriqueMontantMax historique = HistoriqueMontantMax.builder()
                .collecteur(collecteur)
                .ancienMontant(collecteur.getMontantMaxRetrait())
                .dateModification(LocalDateTime.now())
                .modifiePar(securityService.getCurrentUsername())
                .justification(justification)
                .build();
        historiqueRepository.save(historique);
    }

    /**
     * Mappe un mouvement vers le DTO dashboard
     */
    private CollecteurDashboardDTO.MouvementDTO mapToCollecteurDashboardMouvementDTO(Mouvement mouvement) {
        return CollecteurDashboardDTO.MouvementDTO.builder()
                .id(mouvement.getId())
                .type(determinerTypeMouvement(mouvement))
                .montant(mouvement.getMontant())
                .date(mouvement.getDateOperation() != null ? mouvement.getDateOperation() : LocalDateTime.now())
                .clientNom(obtenirNomClient(mouvement))
                .clientPrenom(obtenirPrenomClient(mouvement))
                .statut("VALIDE")
                .build();
    }

    /**
     * Mappe un journal vers le DTO dashboard
     */
    private CollecteurDashboardDTO.JournalDTO mapToCollecteurDashboardJournalDTO(Journal journal, Double soldeActuel) {
        return CollecteurDashboardDTO.JournalDTO.builder()
                .id(journal.getId())
                .statut(journal.getStatut())
                .dateDebut(journal.getDateDebut().atStartOfDay())
                .dateFin(journal.getDateFin().atStartOfDay())
                .soldeInitial(0.0)
                .soldeActuel(soldeActuel)
                .nombreTransactions(0L)
                .build();
    }

    /**
     * Détermine le type d'un mouvement
     */
    private String determinerTypeMouvement(Mouvement mouvement) {
        if (mouvement.getLibelle() != null) {
            String libelle = mouvement.getLibelle().toLowerCase();
            if (libelle.contains("épargne") || libelle.contains("depot")) {
                return "EPARGNE";
            } else if (libelle.contains("retrait")) {
                return "RETRAIT";
            }
        }
        return mouvement.getSens() != null ? mouvement.getSens().toUpperCase() : "INCONNU";
    }

    /**
     * Obtient le nom du client d'un mouvement
     */
    private String obtenirNomClient(Mouvement mouvement) {
        if (mouvement.getClient() != null) {
            return mouvement.getClient().getNom();
        }
        return "N/A";
    }

    /**
     * Obtient le prénom du client d'un mouvement
     */
    private String obtenirPrenomClient(Mouvement mouvement) {
        if (mouvement.getClient() != null) {
            return mouvement.getClient().getPrenom();
        }
        return "N/A";
    }

    /**
     * Calcule la progression vers l'objectif
     */
    private Double calculerProgressionObjectif(Double montantMois, BigDecimal objectifBD) {
        if (objectifBD == null || objectifBD.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        if (montantMois == null) return 0.0;

        Double objectif = objectifBD.doubleValue();
        return (montantMois / objectif) * 100;
    }

    /**
     * Calcule la tendance d'évolution
     */
    private String calculerTendance(List<Map<String, Object>> evolution) {
        if (evolution.size() < 3) return "STABLE";

        // Comparer les 3 derniers jours avec les 3 premiers
        double debut = evolution.subList(0, 3).stream()
                .mapToDouble(e -> (Double) e.get("net")).average().orElse(0.0);
        double fin = evolution.subList(evolution.size() - 3, evolution.size()).stream()
                .mapToDouble(e -> (Double) e.get("net")).average().orElse(0.0);

        double variation = Math.abs(debut) > 0 ? ((fin - debut) / Math.abs(debut)) * 100 : 0;

        if (variation > 10) return "CROISSANTE";
        if (variation < -10) return "DECROISSANTE";
        return "STABLE";
    }

    /**
     * Récupère la date de la dernière clôture pour un collecteur
     */
    private LocalDateTime getLastClosureDate(Long collecteurId) {
        try {
            // Utiliser journalService pour récupérer la dernière date de clôture
            Optional<LocalDate> lastClosureDateOpt = journalService.getLastClosureDateByCollecteur(collecteurId);
            if (lastClosureDateOpt.isPresent()) {
                return lastClosureDateOpt.get().atStartOfDay();
            }
        } catch (Exception e) {
            log.warn("Impossible de récupérer la dernière date de clôture pour collecteur {}: {}", 
                    collecteurId, e.getMessage());
        }
        
        // Fallback: début du jour actuel si pas de clôture
        return LocalDate.now().atStartOfDay();
    }

    /**
     * Calcule le ranking d'un collecteur dans son agence
     */
    private Integer calculateCollecteurRanking(Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        // Récupérer l'agence du collecteur
        Collecteur collecteur = getCollecteurById(collecteurId).orElse(null);
        if (collecteur == null) return null;

        // Calculer le ranking dans l'agence
        List<Object[]> rankings = mouvementRepository.getCollecteurRankingInAgence(
                collecteur.getAgence().getId(), dateDebut, dateFin);

        for (int i = 0; i < rankings.size(); i++) {
            Long id = (Long) rankings.get(i)[0];
            if (id.equals(collecteurId)) {
                return i + 1;
            }
        }

        return rankings.size();
    }

    // ================================
    // MÉTHODES DEPRECATED POUR COMPATIBILITÉ
    // ================================

    @Override
    @Deprecated
    public Collecteur saveCollecteur(CollecteurDTO dto, Long agenceId) {
        CollecteurCreateDTO createDTO = CollecteurCreateDTO.builder()
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .numeroCni(dto.getNumeroCni())
                .adresseMail(dto.getAdresseMail())
                .telephone(dto.getTelephone())
                .agenceId(agenceId)
                .montantMaxRetrait(dto.getMontantMaxRetrait())
                .password("TempPass123") // Mot de passe temporaire
                .build();

        return saveCollecteur(createDTO);
    }

    @Override
    @Deprecated
    public Collecteur saveCollecteur(Collecteur collecteur) {
        return collecteurRepository.saveAndFlush(collecteur);
    }

    @Override
    @Deprecated
    public Collecteur convertToEntity(CollecteurDTO dto) {
        // UTILISONS directement le mapper pour CollecteurDTO
        return collecteurMapper.toEntity(dto);
    }

    @Override
    @Deprecated
    public void updateCollecteurFromDTO(Collecteur collecteur, CollecteurDTO dto) {
        // UTILISONS directement le mapper pour CollecteurDTO
        collecteurMapper.updateEntityFromDTO(dto, collecteur);
    }

    @Override
    @Deprecated
    public Collecteur updateCollecteur(Collecteur collecteur) {
        return collecteurRepository.saveAndFlush(collecteur);
    }

    @Override
    @Deprecated
    public Collecteur updateCollecteur(Long id, CollecteurDTO dto) {
        CollecteurUpdateDTO updateDTO = CollecteurUpdateDTO.builder()
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .telephone(dto.getTelephone())
                .montantMaxRetrait(dto.getMontantMaxRetrait())
                .active(dto.getActive())
                .build();

        return updateCollecteur(id, updateDTO);
    }
}
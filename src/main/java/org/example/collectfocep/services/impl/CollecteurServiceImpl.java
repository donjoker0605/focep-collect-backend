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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final MouvementRepository  mouvementRepository;
    private final JournalMapper journalMapper;
    private final JournalService journalService;
    private final MouvementMapperV2 mouvementMapper;








    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collecteur saveCollecteur(CollecteurCreateDTO dto) {
        try {
            log.info("Début de création du collecteur pour l'agence: {}", dto.getAgenceId());

            // Vérifier que l'agence existe
            Agence agence = agenceRepository.findById(dto.getAgenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée avec l'ID: " + dto.getAgenceId()));

            // Créer l'entité Collecteur via le mapper
            Collecteur collecteur = collecteurMapper.toEntity(dto);

            // Définir l'agence managée
            collecteur.setAgence(agence);

            // Définir le mot de passe
            collecteur.setPassword(passwordEncoder.encode("ChangeMe123!"));

            // Validation
            collecteurValidator.validateCollecteur(collecteur);

            // Sauvegarde avec flush pour récupérer l'ID
            Collecteur savedCollecteur = collecteurRepository.saveAndFlush(collecteur);
            entityManager.refresh(savedCollecteur);

            // Créer les comptes
            log.info("Création des comptes pour le nouveau collecteur: {}", savedCollecteur.getId());
            compteService.createCollecteurAccounts(savedCollecteur);

            log.info("Collecteur et comptes créés avec succès: {}", savedCollecteur.getId());
            return savedCollecteur;

        } catch (Exception e) {
            log.error("Erreur lors de la création du collecteur", e);
            throw new CollecteurServiceException("Erreur lors de la création du collecteur: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Collecteur updateCollecteur(Long id, CollecteurUpdateDTO dto) {
        Collecteur collecteur = collecteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé avec l'ID: " + id));

        // Vérification des droits d'accès
        if (!securityService.hasPermissionForCollecteur(collecteur)) {
            throw new UnauthorizedException("Non autorisé à modifier ce collecteur");
        }

        // Mise à jour via mapper
        collecteurMapper.updateEntityFromDTO(dto, collecteur);

        // Validation
        collecteurValidator.validateCollecteur(collecteur);

        return collecteurRepository.saveAndFlush(collecteur);
    }

    @Override
    @Transactional
    public Collecteur updateMontantMaxRetrait(Long collecteurId, Double nouveauMontant, String justification) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        if (!securityService.hasPermissionForCollecteur(collecteur)) {
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
        return collecteurRepository.findById(id);
    }

    @Override
    public CollecteurDTO convertToDTO(Collecteur collecteur) {
        return collecteurMapper.toDTO(collecteur);
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

    // Méthodes deprecated pour compatibilité - À SUPPRIMER PROGRESSIVEMENT
    @Override
    @Deprecated
    public Collecteur saveCollecteur(CollecteurDTO dto, Long agenceId) {
        CollecteurCreateDTO createDTO = new CollecteurCreateDTO();
        createDTO.setNom(dto.getNom());
        createDTO.setPrenom(dto.getPrenom());
        createDTO.setNumeroCni(dto.getNumeroCni());
        createDTO.setAdresseMail(dto.getAdresseMail());
        createDTO.setTelephone(dto.getTelephone());
        createDTO.setAgenceId(agenceId);
        createDTO.setMontantMaxRetrait(dto.getMontantMaxRetrait());

        return saveCollecteur(createDTO);
    }

    @Override
    @Deprecated
    public Collecteur saveCollecteur(Collecteur collecteur) {
        // Implémentation de compatibilité
        return collecteurRepository.saveAndFlush(collecteur);
    }

    @Override
    @Deprecated
    public Collecteur convertToEntity(CollecteurDTO dto) {
        // Implémentation de compatibilité - utiliser le mapper à la place
        return collecteurMapper.toEntity(new CollecteurCreateDTO(
                dto.getNom(),
                dto.getPrenom(),
                dto.getNumeroCni(),
                dto.getAdresseMail(),
                dto.getTelephone(),
                dto.getAgenceId(),
                dto.getMontantMaxRetrait()
        ));
    }

    @Override
    @Deprecated
    public void updateCollecteurFromDTO(Collecteur collecteur, CollecteurDTO dto) {
        // Implémentation de compatibilité - utiliser updateCollecteur(Long, CollecteurUpdateDTO) à la place
        CollecteurUpdateDTO updateDTO = new CollecteurUpdateDTO();
        updateDTO.setNom(dto.getNom());
        updateDTO.setPrenom(dto.getPrenom());
        updateDTO.setTelephone(dto.getTelephone());
        updateDTO.setMontantMaxRetrait(dto.getMontantMaxRetrait());
        updateDTO.setActive(dto.isActive());

        collecteurMapper.updateEntityFromDTO(updateDTO, collecteur);
    }

    @Override
    @Deprecated
    public Collecteur updateCollecteur(Collecteur collecteur) {
        return collecteurRepository.saveAndFlush(collecteur);
    }

    @Override
    @Deprecated
    public Collecteur updateCollecteur(Long id, CollecteurDTO dto) {
        CollecteurUpdateDTO updateDTO = new CollecteurUpdateDTO();
        updateDTO.setNom(dto.getNom());
        updateDTO.setPrenom(dto.getPrenom());
        updateDTO.setTelephone(dto.getTelephone());
        updateDTO.setMontantMaxRetrait(dto.getMontantMaxRetrait());
        updateDTO.setActive(dto.isActive());

        return updateCollecteur(id, updateDTO);
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

            // Calculer les totaux des mouvements pour le mois courant
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime now = LocalDateTime.now();

            Double totalEpargne = mouvementRepository.sumEpargneByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfMonth, now);

            Double totalRetraits = mouvementRepository.sumRetraitByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfMonth, now);

            Double soldeTotal = (totalEpargne != null ? totalEpargne : 0.0) -
                    (totalRetraits != null ? totalRetraits : 0.0);

            // ✅ CORRECTION: Utiliser le bon type DTO
            List<CollecteurDashboardDTO.MouvementDTO> transactionsRecentes = List.of();

            try {
                // Récupérer les 5 dernières transactions
                PageRequest lastTransactions = PageRequest.of(0, 5, Sort.by("dateOperation").descending());
                Page<Mouvement> recentMovements = mouvementRepository
                        .findByCollecteurId(collecteurId, lastTransactions);

                // ✅ MAPPER CORRECTEMENT VERS LE BON TYPE
                transactionsRecentes = recentMovements.getContent()
                        .stream()
                        .map(this::mapToCollecteurDashboardMouvementDTO) // ✅ Nouvelle méthode de mapping
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Erreur lors de la récupération des transactions récentes: {}", e.getMessage());
                transactionsRecentes = List.of(); // Fallback à une liste vide
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
                    .transactionsRecentes(transactionsRecentes) // ✅ Type correct maintenant
                    .journalActuel(journalActuel)
                    .lastUpdate(LocalDateTime.now())

                    // ✅ Valeurs par défaut pour éviter les erreurs
                    .transactionsAujourdhui(0L)
                    .montantEpargneAujourdhui(0.0)
                    .montantRetraitAujourdhui(0.0)
                    .nouveauxClientsAujourdhui(0L)
                    .montantEpargneSemaine(0.0)
                    .montantRetraitSemaine(0.0)
                    .transactionsSemaine(0L)
                    .montantEpargneMois(totalEpargne != null ? totalEpargne : 0.0)
                    .montantRetraitMois(totalRetraits != null ? totalRetraits : 0.0)
                    .transactionsMois(0L)
                    .objectifMensuel(collecteur.getMontantMaxRetrait())
                    .progressionObjectif(calculerProgressionObjectif(totalEpargne, collecteur.getMontantMaxRetrait()))
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

    private String obtenirNomClient(Mouvement mouvement) {
        if (mouvement.getClient() != null) {
            return mouvement.getClient().getNom();
        }
        // Logique de fallback si nécessaire
        return "N/A";
    }

    private String obtenirPrenomClient(Mouvement mouvement) {
        if (mouvement.getClient() != null) {
            return mouvement.getClient().getPrenom();
        }
        // Logique de fallback si nécessaire
        return "N/A";
    }

    private Double calculerProgressionObjectif(Double montantMois, Double objectif) {
        if (objectif == null || objectif == 0) return 0.0;
        if (montantMois == null) return 0.0;
        return (montantMois / objectif) * 100;
    }
}

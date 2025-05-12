package org.example.collectfocep.services.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.example.collectfocep.Validation.CollecteurValidator;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.entities.HistoriqueMontantMax;
import org.example.collectfocep.exceptions.CollecteurServiceException;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.UnauthorizedAccessException;
import org.springframework.cache.annotation.Cacheable;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.HistoriqueMontantMaxRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.services.MetricsService;
import org.example.collectfocep.services.interfaces.CollecteurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.exceptions.UnauthorizedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class CollecteurServiceImpl implements CollecteurService {

    @Autowired
    private MeterRegistry registry;
    private Timer timer;

    private final CollecteurRepository collecteurRepository;
    private final SecurityService securityService;
    private final org.example.collectfocep.services.interfaces.CompteService compteService;
    private final MetricsService metricsService;
    private final HistoriqueMontantMaxRepository historiqueRepository;
    private final CollecteurValidator collecteurValidator;
    private final PasswordEncoder passwordEncoder;

    // Constants
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String METRICS_TAG = "collecteur_service";

    @PostConstruct
    public void init() {
        timer = registry.timer("collecteur.save.time");
    }

    @Autowired
    public CollecteurServiceImpl(
            CollecteurRepository collecteurRepository,
            SecurityService securityService,
            org.example.collectfocep.services.interfaces.CompteService compteService,
            MetricsService metricsService,
            HistoriqueMontantMaxRepository historiqueRepository,
            CollecteurValidator collecteurValidator,
            PasswordEncoder passwordEncoder) {
        this.collecteurRepository = collecteurRepository;
        this.securityService = securityService;
        this.compteService = compteService;
        this.metricsService = metricsService;
        this.historiqueRepository = historiqueRepository;
        this.collecteurValidator = collecteurValidator;
        this.passwordEncoder = passwordEncoder;
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
    @Cacheable(value = "collecteurs", key = "#id")
    public Optional<Collecteur> getCollecteurById(Long id) {
        return collecteurRepository.findById(id)
                .map(collecteur -> {
                    metricsService.incrementCounter("collecteur.access", METRICS_TAG);
                    return collecteur;
                });
    }

    @Override
    @Retryable(maxAttempts = MAX_RETRY_ATTEMPTS)
    @CircuitBreaker(name = "collecteurService")
    @CacheEvict(value = "collecteurs", key = "#collecteur.id")
    public Collecteur saveCollecteur(Collecteur collecteur) {
        Timer.Sample sample = null;
        try {
            if (metricsService != null) {
                sample = metricsService.startTimer();
            }

            // Validation des données du collecteur via le validator
            collecteurValidator.validateCollecteur(collecteur);

            // Sauvegarde du collecteur
            Collecteur savedCollecteur = collecteurRepository.save(collecteur);

            // Création des comptes associés si nécessaire
            compteService.createCollecteurAccounts(savedCollecteur);

            return savedCollecteur;
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du collecteur", e);
            throw new CollecteurServiceException("Erreur lors de la sauvegarde du collecteur", e);
        } finally {
            // Sécuriser l'appel aux métriques pour éviter les NPE
            if (sample != null && metricsService != null) {
                Timer timer = metricsService.getTimer("collecteur.save");
                if (timer != null) {
                    sample.stop(timer);
                }
            }
        }
    }

    @Override
    @CacheEvict(value = "collecteurs", key = "#id")
    @Transactional
    public void deactivateCollecteur(Long id) {
        Collecteur collecteur = collecteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));
        collecteur.setActive(false);
        collecteurRepository.save(collecteur);
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
    @Transactional
    public Collecteur updateCollecteur(Collecteur collecteur) {
        // Vérifier si le collecteur existe
        if (collecteur.getId() == null) {
            throw new ValidationException("L'ID du collecteur ne peut pas être null pour une mise à jour");
        }

        collecteurRepository.findById(collecteur.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        return collecteurRepository.save(collecteur);
    }

    @Override
    public CollecteurDTO convertToDTO(Collecteur collecteur) {
        if (collecteur == null) return null;

        CollecteurDTO dto = new CollecteurDTO();
        dto.setId(collecteur.getId());
        dto.setNom(collecteur.getNom());
        dto.setPrenom(collecteur.getPrenom());
        dto.setAgenceId(collecteur.getAgence() != null ? collecteur.getAgence().getId() : null);
        dto.setMontantMaxRetrait(collecteur.getMontantMaxRetrait());
        dto.setNumeroCni(collecteur.getNumeroCni());
        dto.setAdresseMail(collecteur.getAdresseMail());
        dto.setTelephone(collecteur.getTelephone());
        // Ne pas exposer le mot de passe
        return dto;
    }

    @Override
    public Collecteur convertToEntity(CollecteurDTO dto) {
        if (dto == null) return null;

        Collecteur collecteur = new Collecteur();
        collecteur.setId(dto.getId());
        collecteur.setNom(dto.getNom());
        collecteur.setPrenom(dto.getPrenom());
        collecteur.setMontantMaxRetrait(dto.getMontantMaxRetrait());
        collecteur.setNumeroCni(dto.getNumeroCni());
        collecteur.setAdresseMail(dto.getAdresseMail());
        collecteur.setTelephone(dto.getTelephone());

        return collecteur;
    }
    @Override
    public void updateCollecteurFromDTO(Collecteur collecteur, CollecteurDTO dto) {
        if (collecteur == null || dto == null) return;

        collecteur.setNom(dto.getNom());
        collecteur.setPrenom(dto.getPrenom());
        collecteur.setMontantMaxRetrait(dto.getMontantMaxRetrait());
        // L'agence n'est pas mise à jour ici
    }

    @Override
    public boolean hasActiveOperations(Collecteur collecteur) {
        // Vérifier si le collecteur a des clients actifs
        return !collecteur.getClients().isEmpty();
    }

    private void validateCollecteur(Collecteur collecteur) {
        if (collecteur.getMontantMaxRetrait() <= 0) {
            throw new ValidationException("Le montant maximum de retrait doit être positif");
        }

        if (collecteur.getAncienneteEnMois() < 0) {
            throw new ValidationException("L'ancienneté ne peut pas être négative");
        }

        // Vérification des doublons
        if (collecteur.getId() == null && collecteurRepository.existsByNumeroCni(collecteur.getNumeroCni())) {
            throw new DuplicateResourceException("Un collecteur existe déjà avec ce numéro d'identification");
        }

        if (collecteur.getId() == null && collecteurRepository.existsByAdresseMail(collecteur.getAdresseMail())) {
            throw new DuplicateResourceException("Un collecteur existe déjà avec cette adresse email");
        }
    }

    @Retry(name = "collecteurRetry")
    @Transactional
    public Collecteur updateMontantMaxRetrait(Long collecteurId, Double nouveauMontant, String justification) {
        log.debug("Tentative de mise à jour du montant max de retrait pour le collecteur: {}", collecteurId);

        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        if (!securityService.hasPermissionForCollecteur(collecteur)) {
            throw new UnauthorizedAccessException("Non autorisé à modifier le montant maximal");
        }

        log.info("Modification du montant max de retrait pour le collecteur {} : {} -> {}",
                collecteur.getId(), collecteur.getMontantMaxRetrait(), nouveauMontant);

        // Sauvegarde de l'historique
        sauvegarderHistoriqueMontantMax(collecteur, justification);

        collecteur.setMontantMaxRetrait(nouveauMontant);
        collecteur.setDateModificationMontantMax(LocalDateTime.now());
        collecteur.setModifiePar(securityService.getCurrentUsername());

        // Retourner le collecteur sauvegardé au lieu d'appeler récursivement la même méthode
        return collecteurRepository.save(collecteur);
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

    @Override
    @Transactional
    public Collecteur saveCollecteur(CollecteurDTO dto, Long agenceId) {
        // Créer l'entité Collecteur à partir du DTO
        Collecteur collecteur = convertToEntity(dto);

        // Définir les propriétés non mappées automatiquement
        if (collecteur.getId() == null) {
            // C'est un nouveau collecteur
            collecteur.setRole("COLLECTEUR");
            collecteur.setPassword(passwordEncoder.encode("ChangeMe123!"));
            collecteur.setActive(true);
            collecteur.setAncienneteEnMois(0);  // Nouveau collecteur
        }

        // Définir l'agence
        if (agenceId != null) {
            Agence agence = new Agence();
            agence.setId(agenceId);
            collecteur.setAgence(agence);
        }

        // Validation du collecteur
        collecteurValidator.validateCollecteur(collecteur);

        // Vérification des droits d'accès
        if (collecteur.getId() != null && !securityService.hasPermissionForCollecteur(collecteur)) {
            throw new UnauthorizedException("Non autorisé à gérer ce collecteur");
        }

        // Sauvegarde du collecteur
        boolean isNew = collecteur.getId() == null;
        Collecteur savedCollecteur = collecteurRepository.save(collecteur);

        // Si c'est un nouveau collecteur, créer tous les comptes nécessaires
        if (isNew) {
            log.info("Création des comptes pour le nouveau collecteur: {}", savedCollecteur.getId());

            // Créer les comptes standard (service, rémunération, charge)
            compteService.createCollecteurAccounts(savedCollecteur);

            log.info("Comptes créés avec succès pour le collecteur: {}", savedCollecteur.getId());
        }

        return savedCollecteur;
    }

    @Override
    @Transactional
    public Collecteur updateCollecteur(Long id, CollecteurDTO dto) {
        Collecteur collecteur = collecteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        // Mise à jour de l'entité avec les données du DTO
        updateCollecteurFromDTO(collecteur, dto);

        // Validation et sauvegarde
        collecteurValidator.validateCollecteur(collecteur);
        return collecteurRepository.save(collecteur);
    }
}
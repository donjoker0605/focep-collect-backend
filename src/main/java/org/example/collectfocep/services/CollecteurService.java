package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.Validation.CollecteurValidator;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.HistoriqueMontantMax;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.mappers.CollecteurMapper;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.CompteCollecteurRepository;
import org.example.collectfocep.repositories.HistoriqueMontantMaxRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.util.CompteUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollecteurService {
    private final CollecteurRepository collecteurRepository;
    private final CollecteurValidator collecteurValidator;
    private final SecurityService securityService;
    private final HistoriqueMontantMaxRepository historiqueRepository;
    private final CompteService compteService;
    private final CompteUtility compteUtility;
    private final PasswordEncoder passwordEncoder;
    private final CompteCollecteurRepository compteCollecteurRepository;
    private final CollecteurMapper collecteurMapper; // Injectez votre mapper ici

    /**
     * Mettre à jour le montant maximal de retrait d'un collecteur
     */
    @Transactional
    public Collecteur updateMontantMaxRetrait(Long collecteurId, double nouveauMontant, String justification) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        // Vérification des droits d'accès
        if (!securityService.hasPermissionForCollecteur(collecteur)) {
            throw new UnauthorizedException("Non autorisé à modifier le montant maximal");
        }

        log.info("Modification du montant max de retrait pour le collecteur {} : {} -> {}",
                collecteur.getId(), collecteur.getMontantMaxRetrait(), nouveauMontant);

        // Sauvegarde de l'ancien montant dans l'historique
        sauvegarderHistoriqueMontantMax(collecteur, justification);

        // Mise à jour du montant
        collecteur.setMontantMaxRetrait(nouveauMontant);
        collecteur.setDateModificationMontantMax(LocalDateTime.now());
        collecteur.setModifiePar(securityService.getCurrentUsername());

        return collecteurRepository.save(collecteur);
    }

    /**
     * Enregistrer l'historique des modifications du montant max
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
     * Recherche les collecteurs par agence
     */
    public List<Collecteur> findByAgenceId(Long agenceId) {
        return collecteurRepository.findByAgenceId(agenceId);
    }

    /**
     * Recherche paginée des collecteurs par agence
     */
    public Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable) {
        return collecteurRepository.findByAgenceId(agenceId, pageable);
    }

    /**
     * Enregistrement complet d'un collecteur avec création des comptes associés
     */
    @Transactional
    public Collecteur saveCollecteur(CollecteurDTO dto, Long agenceId) {
        // Créer l'entité Collecteur à partir du DTO
        Collecteur collecteur = collecteurMapper.toEntity(dto);

        // Définir les propriétés non mappées automatiquement
        if (collecteur.getId() == null) {
            // C'est un nouveau collecteur
            collecteur.setRole("COLLECTEUR");
            collecteur.setPassword(passwordEncoder.encode("ChangeMe123!"));  // Mot de passe par défaut
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

            // S'assurer que le compte d'attente existe aussi
            compteUtility.ensureCompteAttenteExists(savedCollecteur);

            log.info("Comptes créés avec succès pour le collecteur: {}", savedCollecteur.getId());
        }

        return savedCollecteur;
    }

    /**
     * Met à jour un collecteur existant
     */
    @Transactional
    public Collecteur updateCollecteur(Long id, CollecteurDTO dto) {
        Collecteur collecteur = collecteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        // Mise à jour de l'entité avec les données du DTO
        collecteurMapper.updateEntityFromDTO(dto, collecteur);

        // Validation et sauvegarde
        collecteurValidator.validateCollecteur(collecteur);
        return collecteurRepository.save(collecteur);
    }

    /**
     * Récupération d'un collecteur par son ID
     */
    public Optional<Collecteur> getCollecteurById(Long id) {
        return collecteurRepository.findById(id);
    }

    /**
     * Désactivation d'un collecteur
     */
    @Transactional
    public void deactivateCollecteur(Long id) {
        Collecteur collecteur = collecteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        collecteur.setActive(false);
        collecteurRepository.save(collecteur);

        log.info("Collecteur désactivé: {}", id);
    }

    /**
     * Vérifier si un collecteur a des opérations actives
     */
    public boolean hasActiveOperations(Collecteur collecteur) {
        // Vérification des opérations actives du collecteur
        return compteCollecteurRepository.findByCollecteurAndSoldeGreaterThan(collecteur, 0.0).isPresent();
    }
}
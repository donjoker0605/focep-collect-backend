package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CreateParametreCommissionRequest;
import org.example.collectfocep.dto.ParametreCommissionDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.ParametreCommission;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.repositories.ParametreCommissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParametreCommissionService {

    private final ParametreCommissionRepository parametreCommissionRepository;
    private final AgenceRepository agenceRepository;

    public List<ParametreCommissionDTO> getParametresByAgence(Long agenceId) {
        log.info("Récupération des paramètres de commission pour l'agence {}", agenceId);
        
        List<ParametreCommission> parametres = parametreCommissionRepository.findByAgenceIdAndActifTrue(agenceId);
        
        return parametres.stream()
                .map(ParametreCommissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ParametreCommissionDTO> getAllParametres() {
        log.info("Récupération de tous les paramètres de commission");
        
        List<ParametreCommission> parametres = parametreCommissionRepository.findAll();
        
        return parametres.stream()
                .map(ParametreCommissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<ParametreCommissionDTO> getParametreById(Long id) {
        log.info("Récupération du paramètre de commission avec l'ID {}", id);
        
        return parametreCommissionRepository.findById(id)
                .map(ParametreCommissionDTO::fromEntity);
    }

    public ParametreCommissionDTO createParametre(CreateParametreCommissionRequest request, String currentUser) {
        log.info("Création d'un nouveau paramètre de commission pour l'agence {}", request.getAgenceId());

        // Vérifier que l'agence existe
        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée avec l'ID: " + request.getAgenceId()));

        // Vérifier qu'il n'existe pas déjà un paramètre actif pour ce type d'opération
        boolean exists = parametreCommissionRepository.existsByAgenceIdAndTypeOperationAndActifTrue(
                request.getAgenceId(), request.getTypeOperation());
        
        if (exists) {
            throw new RuntimeException("Un paramètre de commission actif existe déjà pour ce type d'opération");
        }

        // Validation des montants
        validateMontants(request);

        ParametreCommission parametre = ParametreCommission.builder()
                .agence(agence)
                .typeOperation(request.getTypeOperation())
                .pourcentageCommission(request.getPourcentageCommission())
                .montantFixe(request.getMontantFixe())
                .montantMinimum(request.getMontantMinimum())
                .montantMaximum(request.getMontantMaximum())
                .actif(request.getActif())
                .createdBy(currentUser)
                .build();

        parametre = parametreCommissionRepository.save(parametre);
        
        log.info("Paramètre de commission créé avec succès avec l'ID {}", parametre.getId());
        
        return ParametreCommissionDTO.fromEntity(parametre);
    }

    public ParametreCommissionDTO updateParametre(Long id, CreateParametreCommissionRequest request, String currentUser) {
        log.info("Mise à jour du paramètre de commission avec l'ID {}", id);

        ParametreCommission parametre = parametreCommissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paramètre de commission non trouvé avec l'ID: " + id));

        // Validation des montants
        validateMontants(request);

        // Vérifier qu'il n'existe pas déjà un autre paramètre actif pour ce type d'opération
        Optional<ParametreCommission> existingParametre = parametreCommissionRepository
                .findByAgenceIdAndTypeOperationAndActifTrue(request.getAgenceId(), request.getTypeOperation());
        
        if (existingParametre.isPresent() && !existingParametre.get().getId().equals(id)) {
            throw new RuntimeException("Un autre paramètre de commission actif existe déjà pour ce type d'opération");
        }

        parametre.setPourcentageCommission(request.getPourcentageCommission());
        parametre.setMontantFixe(request.getMontantFixe());
        parametre.setMontantMinimum(request.getMontantMinimum());
        parametre.setMontantMaximum(request.getMontantMaximum());
        parametre.setActif(request.getActif());
        parametre.setUpdatedBy(currentUser);

        parametre = parametreCommissionRepository.save(parametre);
        
        log.info("Paramètre de commission mis à jour avec succès");
        
        return ParametreCommissionDTO.fromEntity(parametre);
    }

    public void deleteParametre(Long id) {
        log.info("Suppression du paramètre de commission avec l'ID {}", id);

        ParametreCommission parametre = parametreCommissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paramètre de commission non trouvé avec l'ID: " + id));

        // Soft delete - désactiver au lieu de supprimer
        parametre.setActif(false);
        parametreCommissionRepository.save(parametre);
        
        log.info("Paramètre de commission désactivé avec succès");
    }

    public void activerParametre(Long id) {
        log.info("Activation du paramètre de commission avec l'ID {}", id);

        ParametreCommission parametre = parametreCommissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paramètre de commission non trouvé avec l'ID: " + id));

        // Vérifier qu'il n'existe pas déjà un paramètre actif pour ce type d'opération
        boolean exists = parametreCommissionRepository.existsByAgenceIdAndTypeOperationAndActifTrue(
                parametre.getAgence().getId(), parametre.getTypeOperation());
        
        if (exists) {
            throw new RuntimeException("Un paramètre de commission actif existe déjà pour ce type d'opération");
        }

        parametre.setActif(true);
        parametreCommissionRepository.save(parametre);
        
        log.info("Paramètre de commission activé avec succès");
    }

    public BigDecimal calculerCommission(Long agenceId, ParametreCommission.TypeOperation typeOperation, BigDecimal montantTransaction) {
        log.debug("Calcul de commission pour agence {}, operation {}, montant {}", agenceId, typeOperation, montantTransaction);

        Optional<ParametreCommission> parametreOpt = parametreCommissionRepository
                .findByAgenceIdAndTypeOperationAndActifTrue(agenceId, typeOperation);

        if (parametreOpt.isEmpty()) {
            log.warn("Aucun paramètre de commission trouvé pour l'agence {} et l'opération {}", agenceId, typeOperation);
            return BigDecimal.ZERO;
        }

        ParametreCommission parametre = parametreOpt.get();
        BigDecimal commission = parametre.calculerCommission(montantTransaction);
        
        log.debug("Commission calculée: {}", commission);
        
        return commission;
    }

    private void validateMontants(CreateParametreCommissionRequest request) {
        // Au moins un des montants doit être défini
        if (request.getPourcentageCommission() == null && request.getMontantFixe() == null) {
            throw new RuntimeException("Au moins un pourcentage de commission ou un montant fixe doit être défini");
        }

        // Validation montant minimum vs maximum
        if (request.getMontantMinimum() != null && request.getMontantMaximum() != null) {
            if (request.getMontantMinimum().compareTo(request.getMontantMaximum()) > 0) {
                throw new RuntimeException("Le montant minimum ne peut pas être supérieur au montant maximum");
            }
        }
    }
}
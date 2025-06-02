package org.example.collectfocep.mappers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.dto.PalierCommissionDTO;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommissionParameterMapper {

    private final ClientRepository clientRepository;
    private final CollecteurRepository collecteurRepository;
    private final AgenceRepository agenceRepository;

    /**
     * Conversion entité -> DTO (lecture)
     */
    public CommissionParameterDTO toDTO(CommissionParameter entity) {
        if (entity == null) {
            return null;
        }

        return CommissionParameterDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .valeur(entity.getValeur())
                .codeProduit(entity.getCodeProduit())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .active(entity.isActive())
                // Relations - extraire les IDs
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .collecteurId(entity.getCollecteur() != null ? entity.getCollecteur().getId() : null)
                .agenceId(entity.getAgence() != null ? entity.getAgence().getId() : null)
                // Noms pour affichage
                .clientNom(entity.getClient() != null ?
                        entity.getClient().getPrenom() + " " + entity.getClient().getNom() : null)
                .collecteurNom(entity.getCollecteur() != null ?
                        entity.getCollecteur().getPrenom() + " " + entity.getCollecteur().getNom() : null)
                .agenceNom(entity.getAgence() != null ? entity.getAgence().getNom() : null)
                // Paliers
                .paliersCommission(tiersToDTO(entity.getTiers()))
                .build();
    }

    /**
     * Conversion DTO -> entité (création/modification)
     */
    public CommissionParameter toEntity(CommissionParameterDTO dto) {
        if (dto == null) {
            return null;
        }

        CommissionParameter entity = CommissionParameter.builder()
                .type(dto.getType())
                .valeur(dto.getValeur())
                .codeProduit(dto.getCodeProduit())
                .validFrom(dto.getValidFrom())
                .validTo(dto.getValidTo())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        // ✅ GESTION INTELLIGENTE DES RELATIONS
        resolveRelations(entity, dto);

        // ✅ GESTION DES PALIERS AVEC RÉFÉRENCE PARENT
        if (dto.getPaliersCommission() != null && !dto.getPaliersCommission().isEmpty()) {
            List<CommissionTier> tiers = dto.getPaliersCommission().stream()
                    .map(this::dtoToTier)
                    .peek(tier -> tier.setCommissionParameter(entity)) // ✅ Référence parent
                    .collect(Collectors.toList());
            entity.setTiers(tiers);
        }

        return entity;
    }

    /**
     * Mise à jour d'une entité existante avec les données du DTO
     */
    public void updateEntityFromDTO(CommissionParameterDTO dto, CommissionParameter entity) {
        if (dto == null || entity == null) {
            return;
        }

        // Mettre à jour les champs simples
        entity.setType(dto.getType());
        entity.setValeur(dto.getValeur());
        entity.setCodeProduit(dto.getCodeProduit());
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidTo(dto.getValidTo());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Relations
        resolveRelations(entity, dto);

        // ✅ MISE À JOUR INTELLIGENTE DES PALIERS
        updateTiers(entity, dto.getPaliersCommission());
    }

    /**
     * Résolution des relations avec gestion d'erreurs
     */
    private void resolveRelations(CommissionParameter entity, CommissionParameterDTO dto) {
        try {
            if (dto.getClientId() != null) {
                Client client = clientRepository.findById(dto.getClientId())
                        .orElse(null);
                if (client != null) {
                    entity.setClient(client);
                    log.debug("Relation client définie: {}", client.getId());
                } else {
                    log.warn("Client non trouvé: {}", dto.getClientId());
                }
            }

            if (dto.getCollecteurId() != null) {
                Collecteur collecteur = collecteurRepository.findById(dto.getCollecteurId())
                        .orElse(null);
                if (collecteur != null) {
                    entity.setCollecteur(collecteur);
                    log.debug("Relation collecteur définie: {}", collecteur.getId());
                } else {
                    log.warn("Collecteur non trouvé: {}", dto.getCollecteurId());
                }
            }

            if (dto.getAgenceId() != null) {
                Agence agence = agenceRepository.findById(dto.getAgenceId())
                        .orElse(null);
                if (agence != null) {
                    entity.setAgence(agence);
                    log.debug("Relation agence définie: {}", agence.getId());
                } else {
                    log.warn("Agence non trouvée: {}", dto.getAgenceId());
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la résolution des relations: {}", e.getMessage(), e);
        }
    }

    /**
     * Mise à jour intelligente des paliers
     */
    private void updateTiers(CommissionParameter entity, List<PalierCommissionDTO> newTiers) {
        if (entity.getTiers() == null) {
            entity.setTiers(Collections.emptyList());
        }

        // Vider la liste existante
        entity.getTiers().clear();

        // Ajouter les nouveaux paliers
        if (newTiers != null && !newTiers.isEmpty()) {
            List<CommissionTier> tiers = newTiers.stream()
                    .map(this::dtoToTier)
                    .peek(tier -> tier.setCommissionParameter(entity))
                    .collect(Collectors.toList());
            entity.getTiers().addAll(tiers);
        }
    }

    /**
     * Conversion CommissionTier -> PalierCommissionDTO
     */
    public PalierCommissionDTO tierToDTO(CommissionTier tier) {
        if (tier == null) {
            return null;
        }

        return PalierCommissionDTO.builder()
                .id(tier.getId())
                .montantMin(tier.getMontantMin())
                .montantMax(tier.getMontantMax())
                .taux(tier.getTaux())
                .build();
    }

    /**
     * Conversion PalierCommissionDTO -> CommissionTier
     */
    public CommissionTier dtoToTier(PalierCommissionDTO dto) {
        if (dto == null) {
            return null;
        }

        return CommissionTier.builder()
                .montantMin(dto.getMontantMin())
                .montantMax(dto.getMontantMax())
                .taux(dto.getTaux())
                .build();
    }

    /**
     * Conversion listes
     */
    public List<PalierCommissionDTO> tiersToDTO(List<CommissionTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return Collections.emptyList();
        }
        return tiers.stream()
                .map(this::tierToDTO)
                .collect(Collectors.toList());
    }

    public List<CommissionTier> dtosToTiers(List<PalierCommissionDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        return dtos.stream()
                .map(this::dtoToTier)
                .collect(Collectors.toList());
    }

    /**
     * ✅ VALIDATION DES PALIERS
     */
    public boolean validateTiers(List<PalierCommissionDTO> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return true; // Pas de paliers = valide
        }

        // Trier par montant minimum
        List<PalierCommissionDTO> sortedTiers = tiers.stream()
                .sorted((a, b) -> Double.compare(a.getMontantMin(), b.getMontantMin()))
                .collect(Collectors.toList());

        // Vérifier la continuité et l'absence de chevauchements
        for (int i = 0; i < sortedTiers.size(); i++) {
            PalierCommissionDTO current = sortedTiers.get(i);

            // Vérifier que min < max
            if (current.getMontantMin() >= current.getMontantMax()) {
                log.warn("Palier invalide: montantMin ({}) >= montantMax ({})",
                        current.getMontantMin(), current.getMontantMax());
                return false;
            }

            // Vérifier l'absence de chevauchement avec le palier suivant
            if (i < sortedTiers.size() - 1) {
                PalierCommissionDTO next = sortedTiers.get(i + 1);
                if (current.getMontantMax() >= next.getMontantMin()) {
                    log.warn("Chevauchement entre paliers: {} et {}", current, next);
                    return false;
                }
            }
        }

        log.debug("Validation des paliers réussie: {} paliers", tiers.size());
        return true;
    }
}
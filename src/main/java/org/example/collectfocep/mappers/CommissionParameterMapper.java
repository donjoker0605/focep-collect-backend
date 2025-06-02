package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.dto.PalierCommissionDTO;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CommissionParameter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommissionParameterMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "collecteurId", source = "collecteur.id")
    @Mapping(target = "agenceId", source = "agence.id")
    @Mapping(target = "clientNom", expression = "java(getClientDisplayName(entity.getClient()))")
    @Mapping(target = "collecteurNom", expression = "java(getCollecteurDisplayName(entity.getCollecteur()))")
    @Mapping(target = "agenceNom", source = "agence.nom")
    CommissionParameterDTO toDTO(CommissionParameter entity);

    @Mapping(target = "client", ignore = true)  // Sera géré par le service
    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "tiers", ignore = true)
    CommissionParameter toEntity(CommissionParameterDTO dto);

    void updateEntityFromDTO(CommissionParameterDTO dto, @MappingTarget CommissionParameter entity);

    default String getClientDisplayName(Client client) {
        return client != null ? client.getPrenom() + " " + client.getNom() : null;
    }

    default String getCollecteurDisplayName(Collecteur collecteur) {
        return collecteur != null ? collecteur.getPrenom() + " " + collecteur.getNom() : null;
    }

    // ✅ AJOUT DE LA MÉTHODE MANQUANTE
    default boolean validateTiers(List<PalierCommissionDTO> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return true; // Pas de paliers = valide
        }

        // Trier par montant minimum pour vérifier la continuité
        List<PalierCommissionDTO> sortedTiers = tiers.stream()
                .sorted((a, b) -> Double.compare(a.getMontantMin(), b.getMontantMax()))
                .toList();

        // Vérifier la cohérence de chaque palier
        for (int i = 0; i < sortedTiers.size(); i++) {
            PalierCommissionDTO current = sortedTiers.get(i);

            // Vérifier que min < max
            if (current.getMontantMin() >= current.getMontantMax()) {
                return false;
            }

            // Vérifier l'absence de chevauchement avec le palier suivant
            if (i < sortedTiers.size() - 1) {
                PalierCommissionDTO next = sortedTiers.get(i + 1);
                if (current.getMontantMax() > next.getMontantMin()) {
                    return false;
                }
            }
        }

        return true;
    }
}
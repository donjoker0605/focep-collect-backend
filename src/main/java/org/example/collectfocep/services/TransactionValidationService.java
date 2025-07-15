package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ValidationResult;
import org.example.collectfocep.dto.TransactionPreValidationDTO;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service de validation des transactions avant exécution
 * Intègre toutes les validations métier nécessaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionValidationService {

    private final ClientRepository clientRepository;
    private final SoldeCollecteurValidationService soldeCollecteurValidationService;

    /**
     * 📋 Validation complète avant épargne
     */
    public TransactionPreValidationDTO validateEpargne(Long clientId, Long collecteurId, Double montant) {
        log.info("📋 Validation épargne: client={}, collecteur={}, montant={}",
                clientId, collecteurId, montant);

        try {
            // 1. Récupérer le client
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

            // 2. Vérifier le téléphone
            Boolean hasPhone = hasValidPhone(client);

            return TransactionPreValidationDTO.builder()
                    .canProceed(true)
                    .clientId(clientId)
                    .clientName(String.format("%s %s", client.getPrenom(), client.getNom()))
                    .numeroCompte(client.getNumeroCompte())
                    .hasValidPhone(hasPhone)
                    .phoneWarningMessage(hasPhone ? null :
                            "Ce client n'a pas de numéro de téléphone renseigné. Voulez-vous continuer ?")
                    .soldeCollecteurSuffisant(true) // Pas de vérification pour épargne
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur validation épargne: {}", e.getMessage(), e);
            return TransactionPreValidationDTO.builder()
                    .canProceed(false)
                    .errorMessage("Erreur lors de la validation: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 📋 Validation complète avant retrait
     */
    public TransactionPreValidationDTO validateRetrait(Long clientId, Long collecteurId, Double montant) {
        log.info("📋 Validation retrait: client={}, collecteur={}, montant={}",
                clientId, collecteurId, montant);

        try {
            // 1. Récupérer le client
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

            // 2. Vérifier le téléphone
            Boolean hasPhone = hasValidPhone(client);

            // 3. ✅ VALIDATION SOLDE COLLECTEUR (service existant) - CORRIGÉ
            ValidationResult soldeValidation = soldeCollecteurValidationService
                    .validateRetraitPossible(collecteurId, montant);

            return TransactionPreValidationDTO.builder()
                    .canProceed(soldeValidation.isSuccess()) // ✅ CORRIGÉ: isSuccess() au lieu de isValid()
                    .clientId(clientId)
                    .clientName(String.format("%s %s", client.getPrenom(), client.getNom()))
                    .numeroCompte(client.getNumeroCompte())
                    .hasValidPhone(hasPhone)
                    .phoneWarningMessage(hasPhone ? null :
                            "Ce client n'a pas de numéro de téléphone renseigné. Voulez-vous continuer ?")
                    .soldeCollecteurSuffisant(soldeValidation.isSuccess()) // ✅ CORRIGÉ
                    .soldeCollecteurMessage(soldeValidation.isSuccess() ? null : soldeValidation.getMessage()) // ✅ CORRIGÉ
                    .errorMessage(soldeValidation.isSuccess() ? null : soldeValidation.getMessage()) // ✅ CORRIGÉ
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur validation retrait: {}", e.getMessage(), e);
            return TransactionPreValidationDTO.builder()
                    .canProceed(false)
                    .errorMessage("Erreur lors de la validation: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 📞 Vérifier uniquement le téléphone d'un client
     */
    public Boolean clientHasValidPhone(Long clientId) {
        try {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));
            return hasValidPhone(client);
        } catch (Exception e) {
            log.error("❌ Erreur vérification téléphone client {}: {}", clientId, e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NOUVEAU : Méthode utilitaire privée pour vérifier si un client a un téléphone valide
     * Évite d'utiliser clientRepository.hasValidPhone() qui pourrait ne pas exister
     */
    private Boolean hasValidPhone(Client client) {
        return client.getTelephone() != null && !client.getTelephone().trim().isEmpty();
    }
}
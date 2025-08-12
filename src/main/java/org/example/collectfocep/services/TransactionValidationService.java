package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ValidationResult;
import org.example.collectfocep.dto.TransactionPreValidationDTO;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import java.util.List;
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
    private final MouvementRepository mouvementRepository;

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

            // 4. 🔥 NOUVEAU: VALIDATION SOLDE DISPONIBLE CLIENT
            Double soldeDisponible = calculateClientAvailableBalance(client);
            boolean soldeClientSuffisant = soldeDisponible >= montant;
            String soldeClientMessage = null;
            
            if (!soldeClientSuffisant) {
                soldeClientMessage = String.format("Solde disponible insuffisant. Disponible: %.2f FCFA, Demandé: %.2f FCFA", 
                    soldeDisponible, montant);
                log.warn("⚠️ Solde client insuffisant pour retrait: client={}, disponible={}, demandé={}", 
                    clientId, soldeDisponible, montant);
            }

            // 5. Validation globale : les deux conditions doivent être remplies
            boolean canProceed = soldeValidation.isSuccess() && soldeClientSuffisant;
            String errorMessage = null;
            
            if (!canProceed) {
                if (!soldeValidation.isSuccess() && !soldeClientSuffisant) {
                    errorMessage = "Solde collecteur et client insuffisants";
                } else if (!soldeValidation.isSuccess()) {
                    errorMessage = soldeValidation.getMessage();
                } else {
                    errorMessage = soldeClientMessage;
                }
            }

            return TransactionPreValidationDTO.builder()
                    .canProceed(canProceed)
                    .clientId(clientId)
                    .clientName(String.format("%s %s", client.getPrenom(), client.getNom()))
                    .numeroCompte(client.getNumeroCompte())
                    .hasValidPhone(hasPhone)
                    .phoneWarningMessage(hasPhone ? null :
                            "Ce client n'a pas de numéro de téléphone renseigné. Voulez-vous continuer ?")
                    .soldeCollecteurSuffisant(soldeValidation.isSuccess())
                    .soldeCollecteurMessage(soldeValidation.isSuccess() ? null : soldeValidation.getMessage())
                    .errorMessage(errorMessage)
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

    /**
     * 🔥 NOUVEAU: Calcule le solde disponible d'un client (solde total - commission simulée)
     * @param client Le client
     * @return Le solde disponible (après déduction de la commission simulée)
     */
    private Double calculateClientAvailableBalance(Client client) {
        try {
            // 1. Calculer le solde total depuis les transactions
            List<Mouvement> transactions = mouvementRepository.findByClientId(client.getId());
            Double soldeTotal = transactions.stream()
                    .mapToDouble(t -> {
                        if ("epargne".equalsIgnoreCase(t.getSens()) || "EPARGNE".equalsIgnoreCase(t.getTypeMouvement())) {
                            return t.getMontant();
                        } else if ("retrait".equalsIgnoreCase(t.getSens()) || "RETRAIT".equalsIgnoreCase(t.getTypeMouvement())) {
                            return -t.getMontant();
                        }
                        return 0.0;
                    })
                    .sum();

            // 2. TODO: Calculer la commission simulée basée sur les paramètres de commission
            // Pour l'instant, retourner le solde total (pas de déduction de commission)
            // Cette partie sera implémentée quand nous aurons le service de commission côté backend
            Double commissionSimulee = 0.0; // TODO: Implémenter calcul commission
            
            Double soldeDisponible = Math.max(0.0, soldeTotal - commissionSimulee);
            
            log.debug("💰 Solde calculé pour client {}: total={}, commission={}, disponible={}", 
                client.getId(), soldeTotal, commissionSimulee, soldeDisponible);
            
            return soldeDisponible;
            
        } catch (Exception e) {
            log.error("❌ Erreur calcul solde disponible client {}: {}", client.getId(), e.getMessage());
            // En cas d'erreur, retourner 0 pour sécuriser (interdire le retrait)
            return 0.0;
        }
    }
}
package org.example.collectfocep.Validation;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.ClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClientValidator implements Validator {

    private final ClientRepository clientRepository;

    // Constantes de validation
    private static final int LONGUEUR_MIN_NOM = 2;
    private static final int LONGUEUR_MAX_NOM = 50;
    private static final int LONGUEUR_CNI = 13;
    private static final String REGEX_TELEPHONE = "^[0-9]{9}$";

    @Override
    public boolean supports(Class<?> clazz) {
        return Client.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Client client = (Client) target;

        // Validation du nom
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            errors.rejectValue("nom",
                    "nom.required",
                    "Le nom est obligatoire");
        } else if (client.getNom().length() < LONGUEUR_MIN_NOM) {
            errors.rejectValue("nom",
                    "nom.trop.court",
                    String.format("Le nom doit contenir au moins %d caractères", LONGUEUR_MIN_NOM));
        } else if (client.getNom().length() > LONGUEUR_MAX_NOM) {
            errors.rejectValue("nom",
                    "nom.trop.long",
                    String.format("Le nom ne peut pas dépasser %d caractères", LONGUEUR_MAX_NOM));
        }

        // Validation du prénom
        if (client.getPrenom() == null || client.getPrenom().trim().isEmpty()) {
            errors.rejectValue("prenom",
                    "prenom.required",
                    "Le prénom est obligatoire");
        } else if (client.getPrenom().length() < LONGUEUR_MIN_NOM) {
            errors.rejectValue("prenom",
                    "prenom.trop.court",
                    String.format("Le prénom doit contenir au moins %d caractères", LONGUEUR_MIN_NOM));
        } else if (client.getPrenom().length() > LONGUEUR_MAX_NOM) {
            errors.rejectValue("prenom",
                    "prenom.trop.long",
                    String.format("Le prénom ne peut pas dépasser %d caractères", LONGUEUR_MAX_NOM));
        }

        // Validation du numéro CNI
        if (client.getNumeroCni() == null || client.getNumeroCni().trim().isEmpty()) {
            errors.rejectValue("numeroCni",
                    "numeroCni.required",
                    "Le numéro de CNI est obligatoire");
        } else if (client.getId() == null &&
                clientRepository.existsByNumeroCni(client.getNumeroCni())) {
            errors.rejectValue("numeroCni",
                    "numeroCni.duplique",
                    "Ce numéro de CNI est déjà utilisé");
        }

        // Validation du téléphone (si fourni)
        if (client.getTelephone() != null && !client.getTelephone().trim().isEmpty() &&
                !client.getTelephone().matches(REGEX_TELEPHONE)) {
            errors.rejectValue("telephone",
                    "telephone.invalid",
                    "Le numéro de téléphone doit contenir 9 chiffres");
        }

        // Validation des relations
        if (client.getCollecteur() == null) {
            errors.rejectValue("collecteur",
                    "collecteur.required",
                    "Le collecteur est obligatoire");
        }

        if (client.getAgence() == null) {
            errors.rejectValue("agence",
                    "agence.required",
                    "L'agence est obligatoire");
        }
    }

    /**
     * Valide un client et lance des exceptions en cas d'erreurs
     * Cette méthode est utilisée par le service pour valider un client
     * avant sauvegarde
     *
     * @param client Le client à valider
     * @throws ValidationException Si le client n'est pas valide
     * @throws DuplicateResourceException Si des ressources sont dupliquées
     */
    public void validateClient(Client client) {
        if (client == null) {
            throw new ValidationException("Le client ne peut pas être null");
        }

        // Utiliser la méthode validate existante et convertir les erreurs en exceptions
        Errors errors = new BeanPropertyBindingResult(client, "client");
        validate(client, errors);

        if (errors.hasErrors()) {
            FieldError firstError = errors.getFieldErrors().get(0);
            String message = firstError.getDefaultMessage();

            // Gérer les erreurs de duplication spécifiquement
            if (firstError.getCode() != null && firstError.getCode().contains("duplique")) {
                throw new DuplicateResourceException(message);
            }

            throw new ValidationException(message);
        }
    }

    /**
     * Vérifie si le numéro CNI est unique pour un nouveau client
     * @param numeroCni Le numéro CNI à vérifier
     * @param clientId L'ID du client (null pour un nouveau client)
     * @throws DuplicateResourceException Si le numéro CNI est déjà utilisé
     */
    public void validateNumeroCniUnique(String numeroCni, Long clientId) {
        if (numeroCni == null || numeroCni.trim().isEmpty()) {
            throw new ValidationException("Le numéro de CNI est obligatoire");
        }

        clientRepository.findByNumeroCni(numeroCni)
                .ifPresent(existingClient -> {
                    // S'il s'agit d'une mise à jour (clientId non null), vérifier que ce n'est pas le même client
                    if (clientId == null || !existingClient.getId().equals(clientId)) {
                        throw new DuplicateResourceException(
                                "Un client avec le numéro CNI " + numeroCni + " existe déjà");
                    }
                });
    }

    /**
     * Valide les données du client avant création ou mise à jour
     * @param client Le client à valider
     * @throws BusinessException Si les données sont invalides
     */
    public void validateClientData(Client client) {
        try {
            // Vérifier les champs obligatoires
            if (client.getNom() == null || client.getNom().trim().isEmpty()) {
                throw new BusinessException("Le nom du client est obligatoire", "MISSING_FIELD", "nom");
            }

            if (client.getPrenom() == null || client.getPrenom().trim().isEmpty()) {
                throw new BusinessException("Le prénom du client est obligatoire", "MISSING_FIELD", "prenom");
            }

            if (client.getNumeroCni() == null || client.getNumeroCni().trim().isEmpty()) {
                throw new BusinessException("Le numéro CNI du client est obligatoire", "MISSING_FIELD", "numeroCni");
            }

            // Vérifier les données relationnelles
            if (client.getAgenceId() == null) {
                throw new BusinessException("L'ID de l'agence est obligatoire", "MISSING_FIELD", "agenceId");
            }

            if (client.getCollecteurId() == null) {
                throw new BusinessException("L'ID du collecteur est obligatoire", "MISSING_FIELD", "collecteurId");
            }

            // Valider le numéro CNI unique
            validateNumeroCniUnique(client.getNumeroCni(), client.getId());

        } catch (DuplicateResourceException e) {
            // Remontez directement les exceptions de duplication
            throw e;
        } catch (BusinessException e) {
            // Remontez directement les exceptions métier
            throw e;
        } catch (Exception e) {
            // Encapsulez les autres exceptions
            log.error("Erreur lors de la validation du client", e);
            throw new BusinessException("Validation du client échouée", "VALIDATION_ERROR", e.getMessage());
        }
    }
}
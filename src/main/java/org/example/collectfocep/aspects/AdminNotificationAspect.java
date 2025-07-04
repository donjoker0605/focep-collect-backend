package org.example.collectfocep.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.services.impl.AdminNotificationService;
import org.example.collectfocep.security.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Aspect
@Component
@Slf4j
public class AdminNotificationAspect {

    @Autowired
    private AdminNotificationService adminNotificationService;

    @Autowired
    private SecurityService securityService;

    /**
     * 💰 INTERCEPTER TES TRANSACTIONS (RETRAIT/ÉPARGNE)
     */
    @AfterReturning(pointcut = "execution(* org.example.collectfocep.web.controllers.MouvementController.effectuerRetrait(..))",
            returning = "result")
    public void onRetraitEffectue(JoinPoint joinPoint, Object result) {
        try {
            log.debug("🔍 Aspect: Retrait effectué");

            // Extraire les données de la requête
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof RetraitRequest) {
                RetraitRequest request = (RetraitRequest) args[0];

                // Extraire le mouvement du résultat
                MouvementDTO mouvement = extractMouvementFromResponse(result);
                if (mouvement != null) {

                    // Créer événement de notification asynchrone
                    CompletableFuture.runAsync(() -> {
                        ActivityEvent event = ActivityEvent.builder()
                                .type("TRANSACTION_RETRAIT")
                                .collecteurId(request.getCollecteurId())
                                .entityId(mouvement.getId())
                                .montant(request.getMontant())
                                .agenceId(getCurrentUserAgenceId())
                                .timestamp(LocalDateTime.now())
                                .build();

                        adminNotificationService.evaluateAndNotify(event);
                    });
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur aspect retrait: {}", e.getMessage(), e);
        }
    }

    @AfterReturning(pointcut = "execution(* org.example.collectfocep.web.controllers.MouvementController.enregistrerEpargne(..))",
            returning = "result")
    public void onEpargneEnregistree(JoinPoint joinPoint, Object result) {
        try {
            log.debug("🔍 Aspect: Épargne enregistrée");

            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof EpargneRequest) {
                EpargneRequest request = (EpargneRequest) args[0];

                MouvementDTO mouvement = extractMouvementFromResponse(result);
                if (mouvement != null) {

                    CompletableFuture.runAsync(() -> {
                        ActivityEvent event = ActivityEvent.builder()
                                .type("TRANSACTION_EPARGNE")
                                .collecteurId(request.getCollecteurId())
                                .entityId(mouvement.getId())
                                .montant(request.getMontant())
                                .agenceId(getCurrentUserAgenceId())
                                .timestamp(LocalDateTime.now())
                                .build();

                        adminNotificationService.evaluateAndNotify(event);
                    });
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur aspect épargne: {}", e.getMessage(), e);
        }
    }

    /**
     * 👤 INTERCEPTER CRÉATION/MODIFICATION CLIENT
     */
    @AfterReturning(pointcut = "execution(* org.example.collectfocep.web.controllers.ClientController.createClient(..))",
            returning = "result")
    public void onClientCree(JoinPoint joinPoint, Object result) {
        try {
            log.debug("🔍 Aspect: Client créé");

            ClientDTO client = extractClientFromResponse(result);
            if (client != null) {

                CompletableFuture.runAsync(() -> {
                    ActivityEvent event = ActivityEvent.builder()
                            .type("CREATE_CLIENT")
                            .collecteurId(client.getCollecteurId())
                            .entityId(client.getId())
                            .agenceId(getCurrentUserAgenceId())
                            .timestamp(LocalDateTime.now())
                            .build();

                    adminNotificationService.evaluateAndNotify(event);
                });
            }
        } catch (Exception e) {
            log.error("❌ Erreur aspect création client: {}", e.getMessage(), e);
        }
    }

    /**
     * 👥 INTERCEPTER CRÉATION/MODIFICATION COLLECTEUR
     */
    @AfterReturning(pointcut = "execution(* org.example.collectfocep.web.controllers.CollecteurController.createCollecteur(..))",
            returning = "result")
    public void onCollecteurCree(JoinPoint joinPoint, Object result) {
        try {
            log.debug("🔍 Aspect: Collecteur créé");

            CollecteurDTO collecteur = extractCollecteurFromResponse(result);
            if (collecteur != null) {

                CompletableFuture.runAsync(() -> {
                    ActivityEvent event = ActivityEvent.builder()
                            .type("CREATE_COLLECTEUR")
                            .collecteurId(collecteur.getId())
                            .entityId(collecteur.getId())
                            .agenceId(collecteur.getAgenceId())
                            .timestamp(LocalDateTime.now())
                            .build();

                    adminNotificationService.evaluateAndNotify(event);
                });
            }
        } catch (Exception e) {
            log.error("❌ Erreur aspect création collecteur: {}", e.getMessage(), e);
        }
    }

    @AfterReturning(pointcut = "execution(* org.example.collectfocep.web.controllers.CollecteurController.updateCollecteur(..))",
            returning = "result")
    public void onCollecteurModifie(JoinPoint joinPoint, Object result) {
        try {
            log.debug("🔍 Aspect: Collecteur modifié");

            CollecteurDTO collecteur = extractCollecteurFromResponse(result);
            if (collecteur != null) {

                CompletableFuture.runAsync(() -> {
                    ActivityEvent event = ActivityEvent.builder()
                            .type("MODIFY_COLLECTEUR")
                            .collecteurId(collecteur.getId())
                            .entityId(collecteur.getId())
                            .agenceId(collecteur.getAgenceId())
                            .timestamp(LocalDateTime.now())
                            .build();

                    adminNotificationService.evaluateAndNotify(event);
                });
            }
        } catch (Exception e) {
            log.error("❌ Erreur aspect modification collecteur: {}", e.getMessage(), e);
        }
    }

    // ===== MÉTHODES UTILITAIRES D'EXTRACTION =====

    /**
     * Extraire MouvementDTO du ResponseEntity<ApiResponse<MouvementDTO>>
     */
    private MouvementDTO extractMouvementFromResponse(Object result) {
        try {
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                Object body = responseEntity.getBody();

                if (body != null) {
                    // Si c'est ApiResponse<MouvementDTO>
                    Field dataField = body.getClass().getDeclaredField("data");
                    dataField.setAccessible(true);
                    Object data = dataField.get(body);

                    if (data instanceof MouvementDTO) {
                        return (MouvementDTO) data;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Extraction mouvement impossible: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extraire ClientDTO du ResponseEntity<ApiResponse<ClientDTO>>
     */
    private ClientDTO extractClientFromResponse(Object result) {
        try {
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                Object body = responseEntity.getBody();

                if (body != null) {
                    Field dataField = body.getClass().getDeclaredField("data");
                    dataField.setAccessible(true);
                    Object data = dataField.get(body);

                    if (data instanceof ClientDTO) {
                        return (ClientDTO) data;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Extraction client impossible: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extraire CollecteurDTO du ResponseEntity<ApiResponse<CollecteurDTO>>
     */
    private CollecteurDTO extractCollecteurFromResponse(Object result) {
        try {
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                Object body = responseEntity.getBody();

                if (body != null) {
                    Field dataField = body.getClass().getDeclaredField("data");
                    dataField.setAccessible(true);
                    Object data = dataField.get(body);

                    if (data instanceof CollecteurDTO) {
                        return (CollecteurDTO) data;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Extraction collecteur impossible: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Récupérer l'agence de l'utilisateur connecté
     */
    private Long getCurrentUserAgenceId() {
        try {
            return securityService.getCurrentUserAgenceId();
        } catch (Exception e) {
            log.warn("Impossible de récupérer agenceId: {}", e.getMessage());
            return null;
        }
    }
}
package org.example.collectfocep.aspects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {

    /**
     * Action à enregistrer (ex: "CREATE_CLIENT", "MODIFY_CLIENT")
     */
    String action();

    /**
     * Type d'entité concernée (ex: "CLIENT", "MOUVEMENT")
     */
    String entityType() default "";

    /**
     * Description de l'action pour le log
     */
    String description() default "";

    /**
     * Indique si on doit enregistrer les détails de la requête
     */
    boolean includeRequestDetails() default true;

    /**
     * Indique si on doit enregistrer les détails de la réponse
     */
    boolean includeResponseDetails() default false;
}
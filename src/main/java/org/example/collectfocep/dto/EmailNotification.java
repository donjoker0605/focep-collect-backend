package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotification {
    private String destinataire;
    private String sujet;
    private String contenu;
    private String contenuHtml;
    private List<String> copiesCachees;
    private String expediteur;
    private Priority priority;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Builder.Default
    private int tentativesEnvoi = 0;

    @Builder.Default
    private int maxTentatives = 3;

    public enum Priority {
        HAUTE, NORMALE, FAIBLE
    }

    // Factory methods pour notifications admin
    public static EmailNotification urgent(String destinataire, String sujet, String contenu) {
        return EmailNotification.builder()
                .destinataire(destinataire)
                .sujet("[URGENT] " + sujet)
                .contenu(contenu)
                .priority(Priority.HAUTE)
                .build();
    }

    public static EmailNotification normale(String destinataire, String sujet, String contenu) {
        return EmailNotification.builder()
                .destinataire(destinataire)
                .sujet(sujet)
                .contenu(contenu)
                .priority(Priority.NORMALE)
                .build();
    }

    public boolean peutReessayer() {
        return tentativesEnvoi < maxTentatives;
    }

    public void incrementerTentatives() {
        this.tentativesEnvoi++;
    }
}

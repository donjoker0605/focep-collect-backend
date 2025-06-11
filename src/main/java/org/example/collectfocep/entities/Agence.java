package org.example.collectfocep.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "agence")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Agence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_agence", unique = true, nullable = false)
    private String codeAgence;

    @Column(name = "nom_agence", nullable = false)
    private String nomAgence;

    // OPTION 1 : Ajouter un champ active si besoin
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    // OPTION 2 : Ajouter des champs d'audit
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // Informations complémentaires utiles
    @Column(name = "adresse")
    private String adresse;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "responsable")
    private String responsable;

    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"agence", "clients"})
    @Builder.Default
    private List<Collecteur> collecteurs = new ArrayList<>();

    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"agence", "collecteur"})
    @Builder.Default
    private List<Client> clients = new ArrayList<>();

    public String getNom() {
        return this.nomAgence;
    }

    public boolean isActive() {
        return active != null && active;
    }

    public int getNombreCollecteurs() {
        return collecteurs != null ? collecteurs.size() : 0;
    }

    public int getNombreCollecteursActifs() {
        if (collecteurs == null) return 0;
        return (int) collecteurs.stream()
                .filter(c -> c.isActive())
                .count();
    }

    public int getNombreClients() {
        return clients != null ? clients.size() : 0;
    }

    public int getNombreClientsActifs() {
        if (clients == null) return 0;
        return (int) clients.stream()
                .filter(c -> c.getValide() != null && c.getValide())
                .count();
    }

    public String getDisplayName() {
        return String.format("%s (%s)", nomAgence, codeAgence);
    }

    public void addCollecteur(Collecteur collecteur) {
        if (collecteurs == null) {
            collecteurs = new ArrayList<>();
        }
        collecteurs.add(collecteur);
        collecteur.setAgence(this);
        collecteur.setAgenceId(this.id);
    }

    public void removeCollecteur(Collecteur collecteur) {
        if (collecteurs != null) {
            collecteurs.remove(collecteur);
            collecteur.setAgence(null);
            collecteur.setAgenceId(null);
        }
    }

    public void addClient(Client client) {
        if (clients == null) {
            clients = new ArrayList<>();
        }
        clients.add(client);
        client.setAgence(this);
    }

    public void removeClient(Client client) {
        if (clients != null) {
            clients.remove(client);
            client.setAgence(null);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Agence{id=%d, codeAgence='%s', nomAgence='%s', active=%s}",
                id, codeAgence, nomAgence, active);
    }
}
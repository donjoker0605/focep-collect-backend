-- Création des tables pour les tests
-- La séquence de création respecte les dépendances entre tables

-- Table utilisateurs (classe de base pour Admin et Collecteur)
CREATE TABLE IF NOT EXISTS utilisateurs  (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              nom VARCHAR(255) NOT NULL,
                              prenom VARCHAR(255) NOT NULL,
                              password VARCHAR(255) NOT NULL,
                              numero_cni VARCHAR(255) NOT NULL UNIQUE,
                              adresse_mail VARCHAR(255) UNIQUE,
                              telephone VARCHAR(255) NOT NULL,
                              role VARCHAR(50) NOT NULL,
                              version BIGINT
);

-- Table agences
CREATE TABLE IF NOT EXISTS agences (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         code_agence VARCHAR(50),
                         nom_agence VARCHAR(255)
);

-- Table admin (hérite de utilisateurs)
CREATE TABLE IF NOT EXISTS admin (
                       id BIGINT PRIMARY KEY,
                       agence_id BIGINT,
                       FOREIGN KEY (id) REFERENCES utilisateurs(id),
                       FOREIGN KEY (agence_id) REFERENCES agences(id)
);

-- Table rapport_commission
CREATE TABLE IF NOT EXISTS rapport_commission (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    collecteur_id BIGINT,
                                    date_debut DATE,
                                    date_fin DATE,
                                    total_commissions DOUBLE,
                                    total_tva DOUBLE,
                                    remuneration_collecteur DOUBLE,
                                    part_emf DOUBLE,
                                    tva_sur_part_emf DOUBLE,
                                    est_valide BOOLEAN
);

-- Table collecteurs (hérite de utilisateurs)
CREATE TABLE IF NOT EXISTS collecteurs (
                             id BIGINT PRIMARY KEY,
                             id_agence BIGINT,
                             anciennete_en_mois INT NOT NULL,
                             montant_max_retrait DOUBLE NOT NULL,
                             date_modification_montant TIMESTAMP,
                             modifie_par VARCHAR(255),
                             rapport_id BIGINT,
                             active BOOLEAN DEFAULT TRUE,
                             FOREIGN KEY (id) REFERENCES utilisateurs(id),
                             FOREIGN KEY (id_agence) REFERENCES agences(id),
                             FOREIGN KEY (rapport_id) REFERENCES rapport_commission(id)
);

-- Table clients
CREATE TABLE IF NOT EXISTS clients (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         nom VARCHAR(255) NOT NULL,
                         prenom VARCHAR(255) NOT NULL,
                         numero_cni VARCHAR(255) NOT NULL UNIQUE,
                         ville VARCHAR(255),
                         quartier VARCHAR(255),
                         telephone VARCHAR(255),
                         photo_path VARCHAR(255),
                         valide BOOLEAN NOT NULL,
                         id_collecteur BIGINT,
                         id_agence BIGINT,
                         FOREIGN KEY (id_collecteur) REFERENCES collecteurs(id),
                         FOREIGN KEY (id_agence) REFERENCES agences(id)
);

-- Table comptes (classe de base pour tous les types de comptes)
CREATE TABLE IF NOT EXISTS comptes (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         nom_compte VARCHAR(255) NOT NULL,
                         numero_compte VARCHAR(255) NOT NULL UNIQUE,
                         solde DOUBLE NOT NULL,
                         type_compte VARCHAR(50) NOT NULL,
                         version BIGINT
);

-- Tables pour les types spécifiques de comptes
CREATE TABLE IF NOT EXISTS compte_client (
                               id BIGINT PRIMARY KEY,
                               id_client BIGINT,
                               FOREIGN KEY (id) REFERENCES comptes(id),
                               FOREIGN KEY (id_client) REFERENCES clients(id)
);

CREATE TABLE IF NOT EXISTS compte_collecteur (
                                   id BIGINT PRIMARY KEY,
                                   id_collecteur BIGINT,
                                   FOREIGN KEY (id) REFERENCES comptes(id),
                                   FOREIGN KEY (id_collecteur) REFERENCES collecteurs(id)
);

CREATE TABLE IF NOT EXISTS compte_liaison (
                                id BIGINT PRIMARY KEY,
                                id_agence BIGINT,
                                FOREIGN KEY (id) REFERENCES comptes(id),
                                FOREIGN KEY (id_agence) REFERENCES agences(id)
);

-- Table commission_parameter
CREATE TABLE IF NOT EXISTS commission_parameter (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      type VARCHAR(20) NOT NULL,
                                      valeur DOUBLE,
                                      code_produit VARCHAR(50),
                                      valid_from DATE,
                                      valid_to DATE,
                                      is_active BOOLEAN,
                                      client_id BIGINT,
                                      collecteur_id BIGINT,
                                      agence_id BIGINT,
                                      FOREIGN KEY (client_id) REFERENCES clients(id),
                                      FOREIGN KEY (collecteur_id) REFERENCES collecteurs(id),
                                      FOREIGN KEY (agence_id) REFERENCES agences(id)
);

-- Table commission_tiers
CREATE TABLE IF NOT EXISTS commission_tiers (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  montant_min DOUBLE NOT NULL,
                                  montant_max DOUBLE NOT NULL,
                                  taux DOUBLE NOT NULL,
                                  commission_parameter_id BIGINT,
                                  FOREIGN KEY (commission_parameter_id) REFERENCES commission_parameter(id)
);

-- Table commission
CREATE TABLE IF NOT EXISTS commission (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            client_id BIGINT,
                            collecteur_id BIGINT,
                            montant DOUBLE,
                            tva DOUBLE,
                            type VARCHAR(50),
                            valeur DOUBLE,
                            date_calcul TIMESTAMP,
                            date_fin_validite TIMESTAMP,
                            compte_id BIGINT,
                            commission_parameter_id BIGINT,
                            rapport_id BIGINT,
                            FOREIGN KEY (client_id) REFERENCES clients(id),
                            FOREIGN KEY (collecteur_id) REFERENCES collecteurs(id),
                            FOREIGN KEY (compte_id) REFERENCES comptes(id),
                            FOREIGN KEY (commission_parameter_id) REFERENCES commission_parameter(id),
                            FOREIGN KEY (rapport_id) REFERENCES rapport_commission(id)
);

-- Table journaux
CREATE TABLE IF NOT EXISTS journaux (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          date_debut DATE NOT NULL,
                          date_fin DATE NOT NULL,
                          id_collecteur BIGINT NOT NULL,
                          est_cloture BOOLEAN NOT NULL DEFAULT FALSE,
                          date_cloture TIMESTAMP,
                          version BIGINT,
                          FOREIGN KEY (id_collecteur) REFERENCES collecteurs(id)
);

-- Table mouvements
CREATE TABLE IF NOT EXISTS mouvements (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            montant DOUBLE NOT NULL,
                            libelle VARCHAR(255) NOT NULL,
                            sens VARCHAR(50) NOT NULL,
                            date_operation TIMESTAMP NOT NULL,
                            compte_source BIGINT,
                            compte_destination BIGINT,
                            journal_id BIGINT,
                            version BIGINT,
                            transfert_id BIGINT,
                            FOREIGN KEY (compte_source) REFERENCES comptes(id),
                            FOREIGN KEY (compte_destination) REFERENCES comptes(id),
                            FOREIGN KEY (journal_id) REFERENCES journaux(id)
);

-- Table commission_repartition
CREATE TABLE IF NOT EXISTS commission_repartition (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        collecteur_id BIGINT,
                                        montant_total_commission DOUBLE,
                                        montant_tva_client DOUBLE,
                                        part_collecteur DOUBLE,
                                        part_emf DOUBLE,
                                        tva_sur_part_emf DOUBLE,
                                        date_repartition TIMESTAMP,
                                        FOREIGN KEY (collecteur_id) REFERENCES collecteurs(id)
);

-- Table historique_montant_max
CREATE TABLE IF NOT EXISTS historique_montant_max (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        collecteur_id BIGINT,
                                        ancien_montant DOUBLE,
                                        date_modification TIMESTAMP,
                                        modifie_par VARCHAR(255),
                                        justification VARCHAR(255),
                                        FOREIGN KEY (collecteur_id) REFERENCES collecteurs(id)
);

-- Table transferts_compte
CREATE TABLE IF NOT EXISTS transferts_compte (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   source_collecteur_id BIGINT,
                                   target_collecteur_id BIGINT,
                                   date_transfert TIMESTAMP,
                                   nombre_comptes INT,
                                   montant_total DOUBLE,
                                   montant_commissions DOUBLE,
                                   created_by VARCHAR(255),
                                   is_inter_agence BOOLEAN
);

-- Table transferts_compte_client
CREATE TABLE IF NOT EXISTS transferts_compte_client (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          transfert_id BIGINT,
                                          client_id BIGINT,
                                          old_solde DOUBLE,
                                          new_solde DOUBLE,
                                          status VARCHAR(50),
                                          FOREIGN KEY (transfert_id) REFERENCES transferts_compte(id)
);

-- Table audit_logs
CREATE TABLE IF NOT EXISTS audit_logs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(255) NOT NULL,
                            action VARCHAR(255) NOT NULL,
                            entity_type VARCHAR(255),
                            entity_id BIGINT,
                            details TEXT,
                            timestamp TIMESTAMP NOT NULL,
                            ip_address VARCHAR(255),
                            user_agent VARCHAR(255),
                            version BIGINT
);

-- Ajouter la relation de clé étrangère après la création des tables
ALTER TABLE mouvements
    ADD CONSTRAINT FK_mouvements_transfert
        FOREIGN KEY (transfert_id) REFERENCES transferts_compte(id);

ALTER TABLE rapport_commission
    ADD CONSTRAINT FK_rapport_collecteur
        FOREIGN KEY (collecteur_id) REFERENCES collecteurs(id);
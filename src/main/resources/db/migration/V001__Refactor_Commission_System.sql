-- Migration script pour le refactoring du système de commission FOCEP
-- Version : 001
-- Description : Création des nouveaux comptes spécialisés et tables de rubriques

-- ===== CRÉATION DES NOUVEAUX COMPTES SPÉCIALISÉS =====

-- Table pour C.P.C.C (Compte Passage Commission Collecte)
CREATE TABLE compte_passage_commission_collecte (
    id BIGINT NOT NULL,
    agence_id BIGINT NOT NULL,
    periode_courante VARCHAR(7), -- Format YYYY-MM
    CONSTRAINT pk_compte_pccc PRIMARY KEY (id),
    CONSTRAINT fk_pccc_agence FOREIGN KEY (agence_id) REFERENCES agences(id),
    CONSTRAINT fk_pccc_compte FOREIGN KEY (id) REFERENCES comptes(id)
);

-- Table pour C.P.T (Compte Passage Taxe)  
CREATE TABLE compte_passage_taxe (
    id BIGINT NOT NULL,
    agence_id BIGINT NOT NULL,
    taux_tva DECIMAL(5,4) DEFAULT 0.1925, -- 19,25%
    CONSTRAINT pk_compte_cpt PRIMARY KEY (id),
    CONSTRAINT fk_cpt_agence FOREIGN KEY (agence_id) REFERENCES agences(id),
    CONSTRAINT fk_cpt_compte FOREIGN KEY (id) REFERENCES comptes(id)
);

-- Table pour C.P.C (Compte Produit Collecte)
CREATE TABLE compte_produit_collecte (
    id BIGINT NOT NULL,
    agence_id BIGINT NOT NULL,
    CONSTRAINT pk_compte_cpc PRIMARY KEY (id),
    CONSTRAINT fk_cpc_agence FOREIGN KEY (agence_id) REFERENCES agences(id),
    CONSTRAINT fk_cpc_compte FOREIGN KEY (id) REFERENCES comptes(id)
);

-- Table pour C.T (Compte Taxe)
CREATE TABLE compte_taxe (
    id BIGINT NOT NULL,
    agence_id BIGINT NOT NULL,
    CONSTRAINT pk_compte_ct PRIMARY KEY (id),
    CONSTRAINT fk_ct_agence FOREIGN KEY (agence_id) REFERENCES agences(id),
    CONSTRAINT fk_ct_compte FOREIGN KEY (id) REFERENCES comptes(id)
);

-- ===== RENOMMAGE DES TABLES EXISTANTES =====

-- Renommer compte_charge en compte_charge_collecte (C.C.C)
ALTER TABLE compte_charge RENAME TO compte_charge_collecte;

-- Ajouter colonne agence_id à C.C.C
ALTER TABLE compte_charge_collecte ADD COLUMN agence_id BIGINT;
-- TODO: Remplir agence_id basé sur le collecteur

-- Renommer compte_remuneration en compte_salaire_collecteur (C.S.C)
ALTER TABLE compte_remuneration RENAME TO compte_salaire_collecteur;

-- ===== CRÉATION DU SYSTÈME DE RUBRIQUES =====

-- Table principale des rubriques de rémunération
CREATE TABLE rubrique_remuneration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('CONSTANT', 'PERCENTAGE')),
    valeur DECIMAL(12,2) NOT NULL,
    date_application DATE NOT NULL,
    delai_jours INTEGER NULL, -- NULL = indéfini
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Table de liaison rubriques-collecteurs
CREATE TABLE rubrique_collecteurs (
    rubrique_id BIGINT NOT NULL,
    collecteur_id BIGINT NOT NULL,
    CONSTRAINT pk_rubrique_collecteurs PRIMARY KEY (rubrique_id, collecteur_id),
    CONSTRAINT fk_rubrique_collecteurs_rubrique FOREIGN KEY (rubrique_id) REFERENCES rubrique_remuneration(id) ON DELETE CASCADE,
    CONSTRAINT fk_rubrique_collecteurs_collecteur FOREIGN KEY (collecteur_id) REFERENCES collecteurs(id)
);

-- ===== MISE À JOUR DES TYPES DE COMPTES =====

-- Mise à jour des types de comptes pour correspondre à la nomenclature FOCEP
UPDATE comptes SET type_compte = 'CHARGE_COLLECTE' WHERE type_compte = 'CHARGE';
UPDATE comptes SET type_compte = 'SALAIRE_COLLECTEUR' WHERE type_compte = 'REMUNERATION';

-- ===== INDEX POUR OPTIMISATION =====

-- Index sur les rubriques actives par collecteur
CREATE INDEX idx_rubrique_active_collecteur ON rubrique_collecteurs(collecteur_id);
CREATE INDEX idx_rubrique_active_date ON rubrique_remuneration(active, date_application);

-- Index sur les comptes par agence et type
CREATE INDEX idx_compte_agence_type ON comptes(type_compte);

-- ===== DONNÉES INITIALES =====

-- Création des rubriques par défaut pour tous les collecteurs existants
INSERT INTO rubrique_remuneration (nom, type, valeur, date_application, active)
VALUES 
    ('Salaire Base Collecteur', 'CONSTANT', 50000.00, CURRENT_DATE, TRUE),
    ('Prime Performance', 'PERCENTAGE', 15.00, CURRENT_DATE, TRUE);

-- Association des rubriques à tous les collecteurs existants
INSERT INTO rubrique_collecteurs (rubrique_id, collecteur_id)
SELECT r.id, c.id
FROM rubrique_remuneration r
CROSS JOIN collecteurs c
WHERE r.nom IN ('Salaire Base Collecteur', 'Prime Performance');

-- ===== CRÉATION DES COMPTES SPÉCIALISÉS POUR LES AGENCES EXISTANTES =====

-- Fonction pour créer les comptes spécialisés
DELIMITER $$

CREATE PROCEDURE CreateSpecializedAccounts()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE agence_id BIGINT;
    DECLARE agence_nom VARCHAR(255);
    DECLARE compte_id BIGINT;
    
    DECLARE agence_cursor CURSOR FOR 
        SELECT id, nom FROM agences WHERE active = TRUE;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN agence_cursor;
    
    read_loop: LOOP
        FETCH agence_cursor INTO agence_id, agence_nom;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Création C.P.C.C
        INSERT INTO comptes (nom_compte, numero_compte, solde, type_compte)
        VALUES (
            CONCAT('Compte Passage Commission Collecte - ', agence_nom),
            CONCAT('CPCC-', LPAD(agence_id, 6, '0')),
            0.0,
            'PASSAGE_COMMISSION_COLLECTE'
        );
        
        SET compte_id = LAST_INSERT_ID();
        INSERT INTO compte_passage_commission_collecte (id, agence_id) VALUES (compte_id, agence_id);
        
        -- Création C.P.T
        INSERT INTO comptes (nom_compte, numero_compte, solde, type_compte)
        VALUES (
            CONCAT('Compte Passage Taxe - ', agence_nom),
            CONCAT('CPT-', LPAD(agence_id, 6, '0')),
            0.0,
            'PASSAGE_TAXE'
        );
        
        SET compte_id = LAST_INSERT_ID();
        INSERT INTO compte_passage_taxe (id, agence_id) VALUES (compte_id, agence_id);
        
        -- Création C.P.C
        INSERT INTO comptes (nom_compte, numero_compte, solde, type_compte)
        VALUES (
            CONCAT('Compte Produit Collecte - ', agence_nom),
            CONCAT('CPC-', LPAD(agence_id, 6, '0')),
            0.0,
            'PRODUIT_COLLECTE'
        );
        
        SET compte_id = LAST_INSERT_ID();
        INSERT INTO compte_produit_collecte (id, agence_id) VALUES (compte_id, agence_id);
        
        -- Création C.T
        INSERT INTO comptes (nom_compte, numero_compte, solde, type_compte)
        VALUES (
            CONCAT('Compte Taxe - ', agence_nom),
            CONCAT('CT-', LPAD(agence_id, 6, '0')),
            0.0,
            'TAXE'
        );
        
        SET compte_id = LAST_INSERT_ID();
        INSERT INTO compte_taxe (id, agence_id) VALUES (compte_id, agence_id);
        
        -- Création C.C.C
        INSERT INTO comptes (nom_compte, numero_compte, solde, type_compte)
        VALUES (
            CONCAT('Compte Charge Collecte - ', agence_nom),
            CONCAT('CCC-', LPAD(agence_id, 6, '0')),
            0.0,
            'CHARGE_COLLECTE'
        );
        
        SET compte_id = LAST_INSERT_ID();
        INSERT INTO compte_charge_collecte (id, agence_id) VALUES (compte_id, agence_id);
        
    END LOOP;
    
    CLOSE agence_cursor;
END$$

DELIMITER ;

-- Exécution de la procédure
CALL CreateSpecializedAccounts();

-- Nettoyage
DROP PROCEDURE CreateSpecializedAccounts;

-- ===== MIGRATION DES PARAMÈTRES DE COMMISSION EXISTANTS =====

-- Ajout colonne pour configuration des paliers en JSON (si pas déjà présente)
ALTER TABLE commission_parameter ADD COLUMN IF NOT EXISTS tier_config JSON;

-- Migration des paramètres de paliers existants vers le nouveau format
UPDATE commission_parameter 
SET tier_config = JSON_OBJECT(
    'tiers', JSON_ARRAY(
        JSON_OBJECT('montantMin', 0, 'montantMax', 100000, 'taux', 5.0),
        JSON_OBJECT('montantMin', 100001, 'montantMax', 500000, 'taux', 4.0),
        JSON_OBJECT('montantMin', 500001, 'montantMax', 99999999, 'taux', 3.0)
    )
)
WHERE type = 'TIER' AND tier_config IS NULL;

-- ===== VÉRIFICATIONS FINALES =====

-- Vérification que tous les comptes spécialisés ont été créés
SELECT 
    a.nom as agence,
    COUNT(CASE WHEN c.type_compte = 'PASSAGE_COMMISSION_COLLECTE' THEN 1 END) as cpcc_count,
    COUNT(CASE WHEN c.type_compte = 'PASSAGE_TAXE' THEN 1 END) as cpt_count,
    COUNT(CASE WHEN c.type_compte = 'PRODUIT_COLLECTE' THEN 1 END) as cpc_count,
    COUNT(CASE WHEN c.type_compte = 'CHARGE_COLLECTE' THEN 1 END) as ccc_count,
    COUNT(CASE WHEN c.type_compte = 'TAXE' THEN 1 END) as ct_count
FROM agences a
LEFT JOIN comptes c ON c.id IN (
    SELECT pccc.id FROM compte_passage_commission_collecte pccc WHERE pccc.agence_id = a.id
    UNION SELECT cpt.id FROM compte_passage_taxe cpt WHERE cpt.agence_id = a.id  
    UNION SELECT cpc.id FROM compte_produit_collecte cpc WHERE cpc.agence_id = a.id
    UNION SELECT ccc.id FROM compte_charge_collecte ccc WHERE ccc.agence_id = a.id
    UNION SELECT ct.id FROM compte_taxe ct WHERE ct.agence_id = a.id
)
WHERE a.active = TRUE
GROUP BY a.id, a.nom
ORDER BY a.nom;

-- Vérification des rubriques
SELECT 
    COUNT(*) as total_rubriques,
    COUNT(CASE WHEN active = TRUE THEN 1 END) as rubriques_actives,
    (SELECT COUNT(*) FROM rubrique_collecteurs) as liaisons_collecteurs
FROM rubrique_remuneration;

COMMIT;
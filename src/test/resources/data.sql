-- Insertion de données de test pour l'environnement de test

-- Agences
INSERT INTO agences (id, code_agence, nom_agence) VALUES
                                                      (1, 'A01', 'Agence Principale'),
                                                      (2, 'A02', 'Agence Secondaire');

-- Utilisateurs - Admin
INSERT INTO utilisateurs (id, nom, prenom, password, numero_cni, adresse_mail, telephone, role, version) VALUES
                                                                                                             (1, 'Admin', 'System', '$2a$10$j8S5d7Sr4PRCPtWGdX5/6.U1.5D7Kw1kbxRxNL72CWU1LKHs8TPMy', '1234567890123', 'admin@collectfocep.com', '123456789', 'SUPER_ADMIN', 0),
                                                                                                             (2, 'Admin', 'Agence', '$2a$10$j8S5d7Sr4PRCPtWGdX5/6.U1.5D7Kw1kbxRxNL72CWU1LKHs8TPMy', '9876543210987', 'admin.agence@collectfocep.com', '987654321', 'ADMIN', 0);

-- Admin
INSERT INTO admin (id, agence_id) VALUES
                                      (1, 1),
                                      (2, 1);

-- Utilisateurs - Collecteurs
INSERT INTO utilisateurs (id, nom, prenom, password, numero_cni, adresse_mail, telephone, role, version) VALUES
                                                                                                             (3, 'Collecteur', 'Nouveau', '$2a$10$j8S5d7Sr4PRCPtWGdX5/6.U1.5D7Kw1kbxRxNL72CWU1LKHs8TPMy', '1122334455667', 'collecteur.nouveau@collectfocep.com', '112233445', 'COLLECTEUR', 0),
                                                                                                             (4, 'Collecteur', 'Experimente', '$2a$10$j8S5d7Sr4PRCPtWGdX5/6.U1.5D7Kw1kbxRxNL72CWU1LKHs8TPMy', '7766554433221', 'collecteur.experimente@collectfocep.com', '776655443', 'COLLECTEUR', 0);

-- Collecteurs (sans rapport_id pour le moment)
INSERT INTO collecteurs (id, id_agence, anciennete_en_mois, montant_max_retrait, date_modification_montant, modifie_par, active) VALUES
                                                                                                                                     (3, 1, 2, 150000.0, CURRENT_TIMESTAMP(), 'admin@collectfocep.com', true),
                                                                                                                                     (4, 1, 6, 200000.0, CURRENT_TIMESTAMP(), 'admin@collectfocep.com', true);

-- Clients
INSERT INTO clients (id, nom, prenom, numero_cni, ville, quartier, telephone, valide, id_collecteur, id_agence) VALUES
                                                                                                                    (1, 'Client', 'Un', '1111111111111', 'Ville1', 'Quartier1', '111111111', true, 3, 1),
                                                                                                                    (2, 'Client', 'Deux', '2222222222222', 'Ville1', 'Quartier2', '222222222', true, 3, 1),
                                                                                                                    (3, 'Client', 'Trois', '3333333333333', 'Ville2', 'Quartier1', '333333333', true, 4, 1),
                                                                                                                    (4, 'Client', 'Quatre', '4444444444444', 'Ville2', 'Quartier2', '444444444', true, 4, 1);

-- Comptes principaux
INSERT INTO comptes (id, nom_compte, numero_compte, solde, type_compte, version) VALUES
-- Comptes des collecteurs
(1, 'Compte Service Collecteur 1', 'SRV001', 0.0, 'SERVICE', 0),
(2, 'Compte Attente Collecteur 1', 'ATT001', 0.0, 'ATTENTE', 0),
(3, 'Compte Rémunération Collecteur 1', 'REM001', 0.0, 'REMUNERATION', 0),
(4, 'Compte Charge Collecteur 1', 'CHG001', 0.0, 'CHARGE', 0),
(5, 'Compte Service Collecteur 2', 'SRV002', 0.0, 'SERVICE', 0),
(6, 'Compte Attente Collecteur 2', 'ATT002', 0.0, 'ATTENTE', 0),
(7, 'Compte Rémunération Collecteur 2', 'REM002', 0.0, 'REMUNERATION', 0),
(8, 'Compte Charge Collecteur 2', 'CHG002', 0.0, 'CHARGE', 0),
-- Comptes clients
(9, 'Compte Client 1', 'CL001', 0.0, 'EPARGNE_JOURNALIERE', 0),
(10, 'Compte Client 2', 'CL002', 0.0, 'EPARGNE_JOURNALIERE', 0),
(11, 'Compte Client 3', 'CL003', 0.0, 'EPARGNE_JOURNALIERE', 0),
(12, 'Compte Client 4', 'CL004', 0.0, 'EPARGNE_JOURNALIERE', 0),
-- Comptes système
(13, 'Compte Liaison Agence 1', 'LIA001', 0.0, 'LIAISON', 0),
(14, 'Compte Taxe', 'TAXE001', 0.0, 'TAXE', 0),
(15, 'Compte Produit', 'PROD001', 0.0, 'PRODUIT', 0);

-- Comptes collecteurs
INSERT INTO compte_collecteur (id, id_collecteur) VALUES
                                                      (1, 3), (2, 3), (3, 3), (4, 3),
                                                      (5, 4), (6, 4), (7, 4), (8, 4);

-- Comptes clients
INSERT INTO compte_client (id, id_client) VALUES
                                              (9, 1), (10, 2), (11, 3), (12, 4);

-- Compte liaison
INSERT INTO compte_liaison (id, id_agence) VALUES
    (13, 1);

-- Journaux (pour les tests)
INSERT INTO journaux (id, date_debut, date_fin, id_collecteur, est_cloture, version) VALUES
                                                                                         (1, CURRENT_DATE(), DATEADD('DAY', 1, CURRENT_DATE()), 3, false, 0),
                                                                                         (2, CURRENT_DATE(), DATEADD('DAY', 1, CURRENT_DATE()), 4, false, 0);

-- Paramètres de commission par défaut
INSERT INTO commission_parameter (id, type, valeur, code_produit, valid_from, valid_to, is_active, agence_id) VALUES
                                                                                                                  (1, 'FIXED', 50.0, 'FIXE', CURRENT_DATE(), DATEADD('YEAR', 1, CURRENT_DATE()), true, 1),
                                                                                                                  (2, 'PERCENTAGE', 2.0, 'POURCENTAGE', CURRENT_DATE(), DATEADD('YEAR', 1, CURRENT_DATE()), true, 1);

-- Paliers de commission
INSERT INTO commission_tiers (id, montant_min, montant_max, taux, commission_parameter_id) VALUES
                                                                                               (1, 0.0, 1000.0, 3.0, null),
                                                                                               (2, 1001.0, 5000.0, 2.0, null),
                                                                                               (3, 5001.0, 9999999.99, 1.0, null);

-- Création d'un paramètre de commission par palier
INSERT INTO commission_parameter (id, type, valeur, code_produit, valid_from, valid_to, is_active, agence_id) VALUES
    (3, 'TIER', 0.0, 'PALIER', CURRENT_DATE(), DATEADD('YEAR', 1, CURRENT_DATE()), true, 1);

-- Association des paliers au paramètre
UPDATE commission_tiers SET commission_parameter_id = 3 WHERE id IN (1, 2, 3);
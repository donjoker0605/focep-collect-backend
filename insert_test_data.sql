-- 🔥 DONNÉES DE TEST RÉALISTES POUR FOCEP COLLECTE
-- Ce script crée des données permettant de tester les processus de commission et rémunération

-- =====================================
-- 1. MOUVEMENTS D'ÉPARGNE RÉALISTES
-- =====================================

-- Client 1: Épargnes moyennes
INSERT INTO mouvements (client_id, montant, sens, date_operation, libelle, created_at) VALUES
(1, 25000, 'epargne', '2025-08-01 10:00:00', 'Dépôt quotidien client 1', NOW()),
(1, 15000, 'epargne', '2025-08-02 11:30:00', 'Dépôt quotidien client 1', NOW()),
(1, 30000, 'epargne', '2025-08-03 09:15:00', 'Dépôt quotidien client 1', NOW()),
(1, 20000, 'epargne', '2025-08-05 14:20:00', 'Dépôt quotidien client 1', NOW()),
(1, 35000, 'epargne', '2025-08-08 08:45:00', 'Dépôt quotidien client 1', NOW());

-- Client 2: Gros épargnant
INSERT INTO mouvements (client_id, montant, sens, date_operation, libelle, created_at) VALUES
(2, 50000, 'epargne', '2025-08-01 09:00:00', 'Dépôt quotidien client 2', NOW()),
(2, 75000, 'epargne', '2025-08-02 10:00:00', 'Dépôt quotidien client 2', NOW()),
(2, 60000, 'epargne', '2025-08-04 12:00:00', 'Dépôt quotidien client 2', NOW()),
(2, 80000, 'epargne', '2025-08-06 15:30:00', 'Dépôt quotidien client 2', NOW()),
(2, 45000, 'epargne', '2025-08-09 11:45:00', 'Dépôt quotidien client 2', NOW());

-- Client 3: Petit épargnant
INSERT INTO mouvements (client_id, montant, sens, date_operation, libelle, created_at) VALUES
(3, 5000, 'epargne', '2025-08-01 16:00:00', 'Dépôt quotidien client 3', NOW()),
(3, 8000, 'epargne', '2025-08-03 17:30:00', 'Dépôt quotidien client 3', NOW()),
(3, 3000, 'epargne', '2025-08-07 13:15:00', 'Dépôt quotidien client 3', NOW()),
(3, 10000, 'epargne', '2025-08-10 10:20:00', 'Dépôt quotidien client 3', NOW());

-- Client 6: Épargnant régulier 
INSERT INTO mouvements (client_id, montant, sens, date_operation, libelle, created_at) VALUES
(6, 40000, 'epargne', '2025-08-02 08:30:00', 'Dépôt quotidien client 6', NOW()),
(6, 55000, 'epargne', '2025-08-05 13:45:00', 'Dépôt quotidien client 6', NOW()),
(6, 25000, 'epargne', '2025-08-08 16:00:00', 'Dépôt quotidien client 6', NOW());

-- Client 7: Gros volumes
INSERT INTO mouvements (client_id, montant, sens, date_operation, libelle, created_at) VALUES
(7, 100000, 'epargne', '2025-08-01 07:30:00', 'Dépôt quotidien client 7', NOW()),
(7, 120000, 'epargne', '2025-08-04 09:15:00', 'Dépôt quotidien client 7', NOW()),
(7, 90000, 'epargne', '2025-08-07 11:00:00', 'Dépôt quotidien client 7', NOW()),
(7, 110000, 'epargne', '2025-08-10 14:30:00', 'Dépôt quotidien client 7', NOW());

-- Client 8: Épargnes irrégulières
INSERT INTO mouvements (client_id, montant, sens, date_operation, libelle, created_at) VALUES
(8, 18000, 'epargne', '2025-08-03 12:00:00', 'Dépôt quotidien client 8', NOW()),
(8, 22000, 'epargne', '2025-08-09 15:45:00', 'Dépôt quotidien client 8', NOW());

-- =====================================
-- 2. PARAMÈTRES DE COMMISSION RÉALISTES
-- =====================================

-- Paramètre par défaut pour l'agence (taux de base)
INSERT INTO commission_parameters (agence_id, type, value, active, valid_from, created_at) VALUES
(1, 'PERCENTAGE', 2.5, true, '2025-01-01', NOW());

-- Paramètre spécialisé pour le collecteur 4 (taux privilégié)
INSERT INTO commission_parameters (collecteur_id, type, value, active, valid_from, created_at) VALUES
(4, 'PERCENTAGE', 3.0, true, '2025-01-01', NOW());

-- Paramètre VIP pour client 2 (gros épargnant - taux réduit)
INSERT INTO commission_parameters (client_id, type, value, active, valid_from, created_at) VALUES
(2, 'PERCENTAGE', 1.5, true, '2025-01-01', NOW());

-- Paramètre spécial pour client 7 (très gros volumes - commission fixe)
INSERT INTO commission_parameters (client_id, type, value, active, valid_from, created_at) VALUES
(7, 'FIXED', 5000, true, '2025-01-01', NOW());

-- =====================================
-- 3. RUBRIQUES DE RÉMUNÉRATION RÉALISTES
-- =====================================

-- Rubrique salaire de base pour tous les collecteurs
INSERT INTO rubriques_remuneration (nom, type, valeur, date_application, collecteur_ids, active, created_at) VALUES
('Salaire de Base', 'CONSTANT', 150000, '2025-08-01', '[4]', true, NOW());

-- Prime de performance basée sur commissions
INSERT INTO rubriques_remuneration (nom, type, valeur, date_application, collecteur_ids, active, created_at) VALUES
('Prime Performance', 'PERCENTAGE', 15, '2025-08-01', '[4]', true, NOW());

-- Indemnité transport
INSERT INTO rubriques_remuneration (nom, type, valeur, date_application, delai_jours, collecteur_ids, active, created_at) VALUES
('Indemnité Transport', 'CONSTANT', 25000, '2025-08-01', 30, '[4]', true, NOW());

-- =====================================
-- 4. MISE À JOUR DES SOLDES CLIENTS
-- =====================================

-- Mise à jour des soldes pour refléter les épargnes
UPDATE clients SET solde = 125000 WHERE id = 1; -- Client 1: 125k épargné
UPDATE clients SET solde = 310000 WHERE id = 2; -- Client 2: 310k épargné (gros client)
UPDATE clients SET solde = 26000 WHERE id = 3;  -- Client 3: 26k épargné
UPDATE clients SET solde = 120000 WHERE id = 6; -- Client 6: 120k épargné
UPDATE clients SET solde = 420000 WHERE id = 7; -- Client 7: 420k épargné (très gros)
UPDATE clients SET solde = 40000 WHERE id = 8;  -- Client 8: 40k épargné

-- =====================================
-- 5. COMPTES SPÉCIALISÉS INITIALISÉS
-- =====================================

-- Vérification des comptes de passage (devraient déjà exister)
-- Si pas, les créer avec des soldes initiaux

-- CPCC (Compte Passage Commission Collecte)
UPDATE comptes SET solde = 0 WHERE type_compte = 'COMPTE_PASSAGE_COMMISSION_COLLECTE';

-- CPT (Compte Passage Taxe)  
UPDATE comptes SET solde = 0 WHERE type_compte = 'COMPTE_PASSAGE_TAXE';

-- =====================================
-- RÉSULTAT ATTENDU APRÈS CES DONNÉES:
-- =====================================

-- Client 1: 125k × 3% = 3,750 FCFA commission
-- Client 2: 310k × 1.5% = 4,650 FCFA commission (taux VIP)
-- Client 3: 26k × 2.5% = 650 FCFA commission 
-- Client 6: 120k × 3% = 3,600 FCFA commission
-- Client 7: 420k × Commission fixe = 5,000 FCFA commission
-- Client 8: 40k × 3% = 1,200 FCFA commission

-- TOTAL COMMISSIONS: ~18,850 FCFA
-- TVA (19.25%): ~3,628 FCFA
-- TOTAL AVEC TVA: ~22,478 FCFA

COMMIT;
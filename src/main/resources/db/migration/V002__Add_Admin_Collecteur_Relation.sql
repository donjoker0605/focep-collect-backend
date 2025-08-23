-- V002__Add_Admin_Collecteur_Relation.sql
-- Migration pour ajouter la relation Admin-Collecteur

-- Ajouter la colonne admin_id à la table collecteurs
ALTER TABLE collecteurs ADD COLUMN admin_id BIGINT;

-- Créer l'index pour la performance
CREATE INDEX idx_collecteurs_admin_id ON collecteurs(admin_id);

-- Ajouter la contrainte de clé étrangère
ALTER TABLE collecteurs 
ADD CONSTRAINT fk_collecteurs_admin 
FOREIGN KEY (admin_id) REFERENCES admin(id) 
ON DELETE SET NULL 
ON UPDATE CASCADE;

-- Associer les collecteurs existants au premier admin de leur agence
-- Cette requête associe chaque collecteur au premier admin trouvé dans son agence
UPDATE collecteurs c
SET admin_id = (
    SELECT a.id 
    FROM admin a 
    WHERE a.agence_id = c.id_agence 
    ORDER BY a.id ASC 
    LIMIT 1
)
WHERE c.admin_id IS NULL;

-- Commentaire pour expliquer la logique
-- Les collecteurs sont maintenant liés à un admin spécifique
-- Cela permet à chaque admin de gérer uniquement ses collecteurs
-- Le SuperAdmin peut assigner les collecteurs à différents admins
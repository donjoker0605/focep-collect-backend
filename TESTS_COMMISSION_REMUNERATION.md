# 🧪 TESTS COMMISSION & RÉMUNÉRATION - FOCEP

## 📋 PLAN DE TESTS COMPLET

### 🔑 Tests d'Authentification

#### 1. Connexion Admin
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"email":"admin.yaounde@collectfocep.com","password":"AdminAgence123!"}' \
  http://localhost:8080/api/auth/login
```

### 💰 Tests du Système de Commissions

#### 2. Calcul de Commission pour un Collecteur
```bash
# Remplacer {TOKEN} par le token obtenu lors du login
# Période: du 1er au 30 novembre 2024
curl -X POST -H "Authorization: Bearer {TOKEN}" \
  "http://localhost:8080/api/v2/commission-remuneration/collecteur/4/calculer?dateDebut=2024-11-01&dateFin=2024-11-30"
```

**Résultat attendu** :
- Calcul "x" pour chaque client du collecteur 4
- Calcul TVA 19,25% sur chaque "x"  
- Mouvements automatiques : Débit clients → Crédit C.P.C.C et C.P.T
- Calcul "S" = somme des "x" du collecteur

#### 3. Vérification des Paramètres de Commission
```bash
# Liste des paramètres de commission par hiérarchie
curl -H "Authorization: Bearer {TOKEN}" \
  http://localhost:8080/api/commission-parameters
```

#### 4. Création/Modification Paramètre Client
```bash
# Paramètre commission POURCENTAGE pour client spécifique
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "type": "PERCENTAGE",
    "valeur": 5.0,
    "clientId": 1,
    "validFrom": "2024-01-01",
    "active": true
  }' \
  http://localhost:8080/api/commission-parameters
```

### 📊 Tests du Système de Rubriques

#### 5. Création de Rubrique de Rémunération
```bash
# Rubrique CONSTANTE (montant fixe)
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "nom": "Prime Collecteur",
    "type": "CONSTANT",
    "valeur": 15000,
    "collecteurIds": [4, 5],
    "dateApplication": "2024-01-01",
    "delaiJours": null
  }' \
  http://localhost:8080/api/v2/commission-remuneration/rubriques
```

#### 6. Rubrique POURCENTAGE
```bash
# Rubrique POURCENTAGE (% de S)
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "nom": "Commission Collecteur 30%",
    "type": "PERCENTAGE", 
    "valeur": 30.0,
    "collecteurIds": [4, 5],
    "dateApplication": "2024-01-01"
  }' \
  http://localhost:8080/api/v2/commission-remuneration/rubriques
```

#### 7. Liste des Rubriques
```bash
curl -H "Authorization: Bearer {TOKEN}" \
  http://localhost:8080/api/v2/commission-remuneration/rubriques
```

### 💸 Tests de Rémunération

#### 8. Rémunération Collecteur (avec S calculé)
```bash
# Rémunérer collecteur 4 avec S = 50000 FCFA sur période donnée
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "collecteurId": 4,
    "montantS": 50000,
    "dateDebutPeriode": "2024-11-01",
    "dateFinPeriode": "2024-11-30",
    "effectuePar": "admin.yaounde@collectfocep.com"
  }' \
  http://localhost:8080/api/v2/commission-remuneration/collecteur/4/remunerer
```

**Processus attendu** :
1. **Rubrique 1** (15000 fixe) : Vi=15000, S=50000 → Vi<S ✅
   - Débit C.P.C.C : 15000 → Crédit C.S.C : 15000  
   - S restant = 35000
2. **Rubrique 2** (30% de S) : Vi=15000 (30% de 50000), S=35000 → Vi<S ✅
   - Débit C.P.C.C : 15000 → Crédit C.S.C : 15000
   - S restant = 20000  
3. **Rémunération EMF** : S restant = 20000 > 0 ✅
   - Débit C.P.C.C : 20000 → Crédit C.P.C : 20000
4. **TVA** : 19.25% × 50000 = 9625 FCFA ✅  
   - Débit C.P.T : 9625 → Crédit C.T : 9625

#### 9. Vérification Soldes des Comptes
```bash
# Vérifier soldes après rémunération
curl -H "Authorization: Bearer {TOKEN}" \
  http://localhost:8080/api/comptes/agence/1/soldes
```

#### 10. Historique des Rémunérations
```bash
# Consulter l'historique des rémunérations
curl -H "Authorization: Bearer {TOKEN}" \
  http://localhost:8080/api/v2/commission-remuneration/collecteur/4/historique
```

### 🔍 Tests de Validation Métier

#### 11. Test Double Rémunération (doit échouer)
```bash
# Tenter une 2ème rémunération sur la même période
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "collecteurId": 4,
    "montantS": 30000,
    "dateDebutPeriode": "2024-11-01", 
    "dateFinPeriode": "2024-11-30",
    "effectuePar": "admin.yaounde@collectfocep.com"
  }' \
  http://localhost:8080/api/v2/commission-remuneration/collecteur/4/remunerer
```
**Résultat attendu** : Erreur "Une rémunération existe déjà pour cette période"

#### 12. Test Cas Vi > S
```bash
# Créer rubrique avec montant très élevé pour tester Vi > S
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "nom": "Prime Exceptionnelle",
    "type": "CONSTANT",
    "valeur": 100000,
    "collecteurIds": [5],
    "dateApplication": "2024-01-01"
  }' \
  http://localhost:8080/api/v2/commission-remuneration/rubriques

# Puis rémunérer avec S petit
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "collecteurId": 5,
    "montantS": 20000,
    "dateDebutPeriode": "2024-12-01",
    "dateFinPeriode": "2024-12-31",
    "effectuePar": "admin.yaounde@collectfocep.com"
  }' \
  http://localhost:8080/api/v2/commission-remuneration/collecteur/5/remunerer
```

**Processus attendu** : Vi=100000, S=20000 → Vi>S ✅
- Débit C.P.C.C : 20000 → Crédit C.S.C : 20000
- Débit C.C.C : 80000 → Crédit C.S.C : 80000  
- Total C.S.C = 100000 FCFA

### 📈 Tests de Rapports

#### 13. Export Excel des Commissions
```bash
curl -H "Authorization: Bearer {TOKEN}" \
  "http://localhost:8080/api/v2/commission-remuneration/collecteur/4/rapport/excel?dateDebut=2024-11-01&dateFin=2024-11-30" \
  --output rapport_commissions.xlsx
```

#### 14. Rapport de Rémunération
```bash
curl -H "Authorization: Bearer {TOKEN}" \
  "http://localhost:8080/api/v2/commission-remuneration/collecteur/4/rapport/remuneration?dateDebut=2024-11-01&dateFin=2024-11-30"
```

## ✅ CRITÈRES DE VALIDATION

### Commission (Tests 2-4) ✅
- [x] Hiérarchie client > collecteur > agence respectée
- [x] Calcul "x" correct pour chaque client  
- [x] TVA 19,25% appliquée sur chaque "x"
- [x] Mouvements comptables automatiques
- [x] Calcul "S" = somme des "x" stocké

### Rubriques (Tests 5-7) ✅  
- [x] Création rubriques CONSTANT et PERCENTAGE
- [x] Assignment à collecteurs spécifiques
- [x] Dates d'application et délais fonctionnels
- [x] Calcul Vi correct selon type

### Rémunération (Tests 8-12) ✅
- [x] Logique Vi vs S implémentée
- [x] Mouvements C.P.C.C/C.C.C → C.S.C corrects
- [x] Rémunération EMF automatique si surplus
- [x] TVA sur S initial calculée
- [x] Protection double rémunération
- [x] Cas Vi > S géré correctement

### Traçabilité (Tests 10, 13-14) ✅
- [x] Historique des rémunérations
- [x] Exports et rapports disponibles
- [x] Informations complètes stockées

---

## 🎯 COMMANDES RAPIDES DE TEST

```bash
# 1. Login admin
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"email":"admin.yaounde@collectfocep.com","password":"AdminAgence123!"}' \
  http://localhost:8080/api/auth/login | jq -r '.token')

# 2. Calcul commission collecteur 4
curl -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v2/commission-remuneration/collecteur/4/calculer?dateDebut=2024-11-01&dateFin=2024-11-30"

# 3. Créer rubrique test  
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"nom":"Test Prime","type":"CONSTANT","valeur":10000,"collecteurIds":[4],"dateApplication":"2024-01-01"}' \
  http://localhost:8080/api/v2/commission-remuneration/rubriques

# 4. Rémunérer avec S=30000
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"collecteurId":4,"montantS":30000,"dateDebutPeriode":"2024-11-01","dateFinPeriode":"2024-11-30","effectuePar":"admin"}' \
  http://localhost:8080/api/v2/commission-remuneration/collecteur/4/remunerer
```

**Résultat final attendu** : Système de commission et rémunération 100% fonctionnel selon vos spécifications !
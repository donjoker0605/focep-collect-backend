# 🧪 Guide de Test - Système Double Solde

## 📋 Tests à effectuer après installation

### **1. Test de compilation**
```bash
cd C:\Users\don Joker\IdeaProjects\collectFocep
mvn clean compile
```

### **2. Test de démarrage de l'application**
```bash
mvn spring-boot:run
```

### **3. Test de l'endpoint enrichi**

#### **Endpoint à tester :**
```
GET http://localhost:8080/api/clients/collecteur/4
Authorization: Bearer [ton_token_jwt]
```

#### **Structure de réponse attendue :**
```json
{
  "success": true,
  "message": "Récupéré 3 clients avec statistiques complètes",
  "data": [
    {
      "id": 1,
      "nom": "MARTIN",
      "prenom": "Jean",
      "numeroCompte": "COMPTE-001",
      "telephone": "679123456",
      "valide": true,
      "compteClient": {
        "id": 1,
        "numeroCompte": "COMPTE-001",
        "solde": 125000.0,
        "typeCompte": "EPARGNE"
      },
      "transactions": [
        {
          "id": 45,
          "montant": 25000.0,
          "sens": "EPARGNE",
          "libelle": "Épargne mensuelle",
          "dateOperation": "2025-08-10T14:30:00"
        }
      ],
      "totalEpargne": 150000.0,
      "totalRetraits": 25000.0,
      "soldeNet": 125000.0,
      "nombreTransactions": 12,
      "derniereTransaction": "2025-08-10T14:30:00",
      "commissionParameter": {
        "typeCommission": "POURCENTAGE",
        "pourcentage": 2.0
      }
    }
  ]
}
```

### **4. Test côté Frontend (app React Native)**

#### **A. Vérifier les logs de l'app mobile**
Dans la console de Metro Bundler, chercher :
```
✅ Soldes calculés: { soldeTotal: 125000, commissionSimulee: 2500, soldeDisponible: 122500 }
```

#### **B. Vérifier l'affichage dans ClientListScreen**
- Solde Total doit afficher la vraie valeur (ex: 125,000 FCFA)
- Solde Disponible doit afficher la valeur après commission (ex: 122,500 FCFA)

#### **C. Vérifier l'affichage dans ClientDetailScreen**
- Section "Soldes" avec 2 montants distincts
- Éventuellement une ligne "Commission simulée" si > 0

### **5. Tests de régression**

#### **A. Test de connexion mobile**
```bash
# Dans l'app mobile, se connecter avec :
Email: test@collectfocep.com
Mot de passe: ChangeMe123!
```

#### **B. Test de navigation**
- Dashboard → Liste des clients
- Clic sur un client → Détails du client
- Vérifier que les soldes s'affichent correctement

### **6. Tests de performance**

#### **A. Test avec plusieurs clients**
```sql
-- Vérifier le nombre de clients dans la DB
SELECT COUNT(*) FROM clients WHERE id_collecteur = 4;
```

#### **B. Mesurer le temps de réponse**
```bash
# Utiliser curl pour mesurer
curl -w "@curl-format.txt" -o /dev/null -s "http://localhost:8080/api/clients/collecteur/4" \
  -H "Authorization: Bearer [TOKEN]"
```

### **7. Tests d'erreur**

#### **A. Test sans authentification**
```bash
curl http://localhost:8080/api/clients/collecteur/4
# Doit retourner 401 Unauthorized
```

#### **B. Test collecteur inexistant**
```bash
curl -H "Authorization: Bearer [TOKEN]" \
  http://localhost:8080/api/clients/collecteur/999
# Doit retourner 404 Not Found
```

## 🔧 **Dépannage**

### **Erreur de compilation :**
```bash
# Si erreur sur @Service
mvn dependency:tree | grep spring-context

# Si erreur sur Lombok
mvn dependency:tree | grep lombok
```

### **Erreur au runtime :**
```bash
# Vérifier les logs Spring Boot
tail -f logs/collectfocep.log

# Ou dans la console directement
mvn spring-boot:run
```

### **App mobile ne voit pas les nouveaux soldes :**
```bash
# Redémarrer Metro avec cache nettoyé
npx expo start -c
```

## ✅ **Critères de réussite**

✅ **Compilation** : Aucune erreur Maven  
✅ **Démarrage** : Application démarre sans erreur  
✅ **API** : Endpoint retourne la structure enrichie  
✅ **Frontend** : Soldes s'affichent correctement  
✅ **Performance** : Réponse < 2 secondes avec 10+ clients  

## 🚀 **Prochaines étapes si tout fonctionne**

1. **Optimiser** : Ajouter du cache si nécessaire
2. **Enrichir** : Ajouter plus de statistiques client
3. **Sécuriser** : Valider les paramètres de commission
4. **Monitorer** : Ajouter des métriques de performance

**Installation terminée ! 🎉**
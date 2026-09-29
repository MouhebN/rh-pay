# Démonstration live — Assistant RH Paie

Durée cible : **8 à 12 minutes**.  
Comptes démo (profil `demo`) : employé **Ali Ben Salah** (`ali`), administrateur **`admin`**.  
Ne pas inventer d’utilisateur « Ahmed ». Identifiants : voir le README du projet.

Avant de commencer : frontend, API démo, PostgreSQL, service d’intentions `:8010`, Ollama `llama3.2:3b`.

---

## A. Lecture employé (≈ 2 min)

1. Connexion `ali` → ouvrir le widget (💬).
2. **Attendu :** accueil avec jours restants et nombre de demandes en attente. Pas d’Ollama. Pas d’écriture.
3. « **Combien de jours de congé me reste-t-il ?** »
   - Pastille **Données**, solde issu de la base.
4. « **Montre mes demandes** » puis éventuellement « **seulement celles en attente** »
   - Liste personnelle, pas la file entreprise.

**À dire :** les chiffres viennent de Spring / PostgreSQL, pas du modèle de langue.

---

## B. Sécurité (≈ 1 min)

5. « **Combien de congés en attente ?** »
   - **Attendu :** pastille **Accès refusé**, intention file admin, **aucune** liste d’entreprise.

**À dire :** même si l’intention est « file d’attente », le garde de rôle Spring décide.

---

## C. Création sûre (≈ 2 min)

6. « **Je veux un congé du 2 mars 2027 au 3 mars 2027** »
   - Carte de proposition, **Confirmer** visible.
   - **Les deux dates** doivent apparaître (2 et 3 mars).
   - Vérifier (ou affirmer) : **pas encore de ligne PENDING** pour ces dates.
7. Cliquer **Confirmer** (le bouton actif, pas une ancienne carte grisée).
   - **Attendu :** demande créée en **PENDING**.

**À dire :** le serveur a exécuté le brouillon stocké, pas un JSON libre du navigateur.

Option courte : « **Je veux un congé cette semaine** » → l’assistant **demande des dates**, n’invente pas lundi–vendredi.

---

## D. Contexte de lecture (≈ 1 min)

Si un brouillon est ouvert, bannière « Brouillon en cours ».

8. « **et mes demandes ?** » (après un tour congé / demandes)
   - Lecture personnelle, **pas d’écriture**.

Ou, côté admin plus tard : file puis « **Seulement ceux de Ali** » → filtre, pas une revue.

---

## E. Administrateur (≈ 3 min)

9. Déconnexion → connexion `admin` → widget.
10. Accueil : file + contrats à échéance.
11. « **Quelles sont les demandes de congé en attente ?** »  
    Liste **numérotée**.
12. « **Quels contrats expirent bientôt ?** »  
    Lecture admin.
13. « **Accepte 1** » (ou « Accepte la première »)
    - Récapitulatif, **Confirmer**.
    - Les statuts en base **n’ont pas encore changé**.
14. Cliquer **Confirmer**.
    - Revue appliquée (ou refus métier Spring affiché clairement).

Hors sujet pendant une revue ouverte (« Qui a gagné la CAN ? ») : l’assistant **reste sur la revue** — limiter à une phrase, c’est le comportement actuel, pas un défaut à « corriger en live ».

---

## F. FAQ / RAG (≈ 2 min)

Après la revue (brouillon fermé) :

15. « **Comment demander un congé ?** »  
    Pastille **Guide**, procédure employé — **pas** la liste d’attente.
16. « **Comment se déroule la paie ?** » ou « **Dans quel ordre le RH prépare le mois de paie ?** »  
    Étapes du cycle (variables → calcul → bulletins / exports).
17. « **Combien de jours de RTT le guide prévoit-il ?** »  
    Abstention ou hors sujet — **pas** de quota inventé.

---

## G. Conclusion à l’oral (30–45 s)

- CamemBERT + régression logistique : **quelle intention**.
- Java : **quelles dates**.
- Spring / PostgreSQL : **quels faits et quels droits**.
- RAG : **quel guide**.
- Ollama : **quelle formulation**.
- Confirmer : **quelle écriture**.

Phrase de clôture possible :

> « Nous n’avons pas entraîné Llama. Nous avons conçu un assistant qui parle français sans devenir la source de vérité RH. »

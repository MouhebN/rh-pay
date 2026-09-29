# Assistant RH Paie — explication simple pour le client

Ce document explique, en langage clair, comment fonctionne l’assistant conversationnel intégré dans RH Paie.  
Il résume ce qui existe réellement dans le projet.

---

## 1. Qu’est-ce que c’est ?

RH Paie est une application de gestion RH et paie.

L’assistant est un **chatbot français** intégré dans l’application (petite bulle de discussion).  
Il n’est **pas** un ChatGPT libre branché sur une fausse base.

Il utilise :
- les **vraies données** de l’application (PostgreSQL),
- les **mêmes droits** que les écrans (employé ou admin RH),
- un **guide RH** pour les procédures,
- une confirmation avant toute écriture (créer / annuler / valider un congé).

**Idée centrale :**  
le modèle de langage ne décide pas la vérité métier.  
**Spring + PostgreSQL** restent la source de vérité.

---

## 2. Les deux rôles

| Rôle | Qui | Ce qu’il peut faire avec l’assistant |
|---|---|---|
| **EMPLOYE** | ex. Ali | Voir son solde, ses demandes, ses contrats, ses bulletins. Créer ou annuler **sa** demande de congé. |
| **ADMIN_RH** | ex. admin | Voir la file d’attente entreprise, les contrats à échéance, le statut de paie. Approuver / refuser des congés. |

Le rôle vient du **login** (JWT), pas de la phrase tapée.

Exemple :  
Si un employé demande « Combien de congés en attente ? », cela désigne la file **entreprise**.  
L’assistant refuse l’accès. Il ne montre pas les demandes des autres.

---

## 3. Comment une question est traitée (pipeline simple)

Imaginez une **porte** :

1. L’utilisateur tape une phrase dans le widget Angular.  
2. Spring reçoit le message avec le JWT.  
3. Le système choisit **un seul type** de question (une intention).  
4. Spring vérifie le **rôle**.  
5. Ensuite, **une seule** sortie possible :

| Sortie | Signification | Pastille affichée |
|---|---|---|
| **TOOL** | Lire des données réelles dans la base | Données |
| **FAQ** | Chercher dans le guide RH | Guide |
| **ACTION** | Préparer une écriture (brouillon) | Action |
| **REFUS / HORS SUJET** | Pas autorisé ou hors RH | Accès refusé / Hors sujet |

Une phrase = **une** sortie.  
Pas de « deux demandes en même temps » dans une seule phrase.

---

## 4. CamemBERT : à quoi ça sert ?

CamemBERT est un **encodeur de phrases en français**, déjà préentraîné.  
**Nous ne l’avons pas entraîné from scratch.**

Il transforme une phrase en une liste de nombres (un **vecteur**).  
Des phrases proches en sens ont des vecteurs proches.

CamemBERT ne :
- ne calcule pas un solde,
- ne parle pas à PostgreSQL,
- n’approuve pas un congé,
- n’écrit pas la réponse finale.

Il sert seulement à représenter le sens de la phrase en nombres.

---

## 5. Régression logistique : comment on choisit le type de message ?

Après CamemBERT, une **régression logistique** choisit le **type** (l’intention).

Ce n’est pas une « intelligence qui comprend comme un humain ».  
C’est un **classifieur** entraîné sur notre corpus RH.

Dans le projet :
- environ **1 280** phrases françaises étiquetées,
- **19** classes (solde, mes demandes, FAQ congés, créer un congé, hors sujet, etc.),
- évaluation holdout : environ **76,6 %** d’accuracy, F1 macro environ **0,72**.

En live :
1. CamemBERT → vecteur  
2. Régression logistique → probabilités pour chaque classe  
3. On prend la classe la plus probable  

Règle de confiance (**0,55**) :
- modèle assez confiant → on garde son label,
- sinon mots-clés assez confiants → repli mots-clés,
- sinon → comportement sûr (pas d’outil métier dangereux).

**Phrase pour le client :**  
CamemBERT donne la forme du sens.  
La régression logistique choisit la **porte** (le type de question).

---

## 6. Spring : que se passe-t-il derrière la porte ?

Le classifieur choisit la porte.  
**Spring décide ce qu’on fait dans la pièce.**

Exemples :

| Type détecté | Ce que Spring fait |
|---|---|
| Solde de congé | Appelle le service solde → lit PostgreSQL → répond avec le vrai nombre |
| Mes demandes | Liste les demandes de **cet** employé |
| File d’attente (employé) | **Refuse** (réservé admin) |
| File d’attente (admin) | Affiche la liste numérotée |
| FAQ congés / paie | Lance la recherche dans le guide (RAG) |
| Créer un congé | Parse les dates → crée un **brouillon** → bouton Confirmer |
| Hors sujet | Répond que ce n’est pas du RH |

Donc :
- **ML** = quelle porte  
- **Spring** = droits + appels aux vrais services + sécurité

Même si le classifieur se trompe un peu, un employé ne voit pas les données admin : le **garde de rôle Spring** reste indépendant.

---

## 7. Qu’est-ce qu’un « tool » ?

Un **tool** n’est pas un robot magique.  
C’est une **fonction Spring déjà existante** qui lit (ou écrit via confirmation) la vraie base.

Exemples côté employé :
- solde de congé,
- mes demandes,
- mes contrats,
- mes bulletins.

Exemples côté admin :
- demandes en attente,
- contrats qui expirent,
- statut de paie (consultation, pas lancer le calcul).

**Principe :**  
Spring / PostgreSQL fournissent les faits.  
Ollama (Llama) peut seulement reformuler la phrase.

Si Llama invente « 12 jours » alors que la base dit « 17 », Spring garde **17**.

---

## 8. Parseur de dates Java

Pour créer un congé, les dates sont lues par des **règles Java**, pas par Llama.

Exemples supportés :
- `demain`
- `du 2 mars 2027 au 3 mars 2027`
- `10-05-2027` → 10 mai 2027
- `vendredi de cette semaine`

Cas ambigus :
- `cette semaine`
- `la semaine prochaine`  
→ l’assistant **demande de préciser**, il n’invente pas lundi–vendredi.

Corrections limitées supportées :
- `pas lundi mais mardi`
- `demain, non plutôt vendredi`

Pourquoi Java ?  
Parce qu’une date RH doit être exacte. Un LLM peut inventer.

---

## 9. FAQ / RAG : comment on répond depuis un document ?

### Comment le système sait que c’est une FAQ ?

D’abord le classifieur donne un label FAQ, par exemple :
- `FAQ_LEAVE`
- `FAQ_PAYROLL`

Tant que ce n’est pas FAQ, **aucun document n’est cherché**.

### Ensuite seulement : recherche dans les chunks

1. Les guides (`conges.md`, `paie.md`, …) et documents uploadés sont découpés en **morceaux (chunks)**.  
2. CamemBERT transforme la question et les chunks en vecteurs.  
3. On mesure la similarité (**cosine**).  
4. On garde les meilleurs (top 3).  
5. Si trop faible → **abstention** (« pas trouvé dans le guide »), sans inventer.  
6. Si assez proche → on utilise ce texte ; Ollama peut reformuler.

### Upload admin

Formats : **md, txt, pdf**.  
Catégories utiles pour tester : **Congés / Paie / Pointage**.  
« Général » est indexé mais moins bien relié aux FAQ.

Important :
- uploader un CV **ne** transforme **pas** le bot en « pose n’importe quelle question sur ce fichier » ;
- le document est cherché **seulement** si la question est classée FAQ ;
- même alors, le chunk le plus proche gagne (parfois le guide congés plutôt que votre PDF).

---

## 10. Ollama (Llama 3.2)

Modèle local : **`llama3.2:3b`**.

Il sert à **mieux formuler** une réponse TOOL ou FAQ déjà ancrée.

Il ne sert **pas** à :
- autoriser un rôle,
- calculer un solde,
- accéder à la base,
- parser une date,
- confirmer une écriture,
- lancer la paie.

Si Ollama est down ou invente un chiffre : Spring garde le gabarit / le guide.

---

## 11. Actions et confirmation (très important)

Écritures supportées dans le chat :
- employé : créer une demande de congé,
- employé : annuler une demande en attente,
- admin : revue (accepter / refuser).

Déroulement :

1. Phrase naturelle  
2. Brouillon **côté serveur**  
3. Carte **Confirmer**  
4. Clic Confirmer  
5. Spring vérifie le brouillon (utilisateur, type, délai 30 min, `proposalId`)  
6. Appel du service métier  
7. Écriture en base  

Tant qu’on n’a pas cliqué Confirmer : **pas d’écriture**.

Si un brouillon de création est ouvert et qu’on pose une autre question, l’assistant peut continuer à demander des dates.  
Il faut cliquer **Annuler** sur la bannière « Brouillon en cours », puis poser la nouvelle question.

Pour annuler une demande : il faut le **numéro**  
(`Annuler la demande 9`), pas « la dernière » / « last one ».

---

## 12. Contexte de lecture

L’assistant peut se souvenir brièvement du thème de lecture (30 minutes, en mémoire serveur), par exemple :

- `Mes demandes` puis `seulement celles en attente`
- `Demandes en attente` puis `seulement ceux de Ali`

Règle :
une **nouvelle question complète** (FAQ, autre outil, hors sujet…) **l’emporte** sur l’ancien contexte.  
Le contexte sert surtout aux petites suites incomplètes.

---

## 13. Accueil (greeting)

À l’ouverture du widget :
- employé : solde + nombre de demandes en attente,
- admin : file d’attente + contrats qui expirent.

Sans Ollama, sans écriture, sans ouvrir un brouillon.

---

## 14. Ce qui a été validé

Sur le widget réel (frontend + API + PostgreSQL + CamemBERT + Ollama), la dernière campagne manuelle a donné :

**22 scénarios / 22 OK**

Cela couvre notamment :
- lectures employé,
- refus de la file admin,
- création + confirmation,
- date ambiguë,
- annulation,
- revue admin,
- FAQ congés / paie,
- question hors guide.

---

## 15. Limites (honnêtes)

- Une phrase → une intention (pas multi-demandes).  
- Pas de Derja / arabe fiable.  
- Pas de badgeage ni lancement de paie dans le chat.  
- Le contexte lecture est local au serveur (pas Redis multi-instance).  
- Le classifieur n’est pas parfait (~76 % holdout) : d’où garde de rôle + confirmation.  
- Un document uploadé n’est pas un chatbot général sur n’importe quel fichier.

---

## 16. Phrase de synthèse (à retenir)

> CamemBERT transforme la phrase en vecteur.  
> La régression logistique choisit le type de question (la porte).  
> Spring décide ce qu’on fait derrière la porte : lire la base, chercher le guide, préparer une action, ou refuser.  
> Les dates sont lues en Java.  
> Ollama reformule seulement.  
> Toute écriture attend le bouton Confirmer.

---

## 17. Comptes démo

- Employé : `ali` / `employee123`  
- Admin : `admin` / `admin123`

Exemples de questions qui marchent bien :
- `Combien de jours de congé me reste-t-il ?`
- `Montre mes demandes`
- `Combien de congés en attente ?` (refus employé)
- `Je veux un congé du 2 mars 2027 au 3 mars 2027` puis Confirmer
- `Comment demander un congé ?`
- `Comment se déroule la paie ?`
- Admin : `Quelles sont les demandes de congé en attente ?` puis `Accepte 1` puis Confirmer

---

*Document de vulgarisation pour présentation client / jury.  
Pour le détail technique et les métriques, voir aussi `docs/RH_PAIE_ASSISTANT_RAPPORT.md`.*

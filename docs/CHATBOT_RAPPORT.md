# Assistant RH Paie — notes pour le rapport

Document de référence pour le chapitre IA / chatbot du PFE.  
Architecture réelle du dépôt (septembre 2026). **Ne pas écrire** que le LLM a été entraîné from scratch.

## 1. Objectif

Ajouter un assistant conversationnel **rôle-aware** (ADMIN_RH / EMPLOYE) qui :

1. **Comprend** la question (classifieur d’intention local)  
2. **Récupère la vérité** via des outils Spring sur PostgreSQL, ou via RAG FAQ  
3. **Propose une action** (v1 : demande de congé) avec **confirmation explicite** dans le widget  
4. **Formule** la réponse en français avec Ollama (ou un template si le LLM invente / est down)

Ollama **ne lit jamais** la base. Les chiffres viennent des services métier existants.

## 2. Pipeline

```text
Widget Angular (JWT)
    → POST /api/chat/messages
    → Intent API Python : POST /predict  (CamemBERT + Logistic Regression)
    → si confiance faible : KeywordIntentFallback
    → garde de rôle (employé ≠ admin)
    → TOOL  → ChatToolService → services RH existants
       ou FAQ → RagService (embeddings CamemBERT, cosine top-3)
       ou ACTION → CREATE/CANCEL leave (employé) ou REVIEW_LEAVE_BATCH (admin)
    → Ollama (llama3.2:3b) pour reformuler, avec historique court
    → fallback template si réponse non ancrée
    → persist chat_messages + chip source (Données / Guide / Action / Accès refusé / Hors sujet)

Confirm action (employé seulement) :
    Widget Confirmer → POST /api/chat/actions/leave-request
    → LeaveRequestService.create (PENDING)
```

### Mémoire conversationnelle

- Derniers **3** tours (`chat_messages`)  
- Réutilisation d’une intention outil/FAQ si follow-up court (« et 2025 ? », « année dernière »)  
- Parsing d’année explicite / année dernière  
- Historique passé à Ollama **pour la langue seulement**, pas pour inventer des chiffres  

### Garde-fous anti-hallucination

- Si outil vide (`Aucun…`, `salaires=2026:0`, etc.) → **pas d’Ollama**, template seul  
- Si Ollama nie les données alors que le template contient des chiffres → template  
- Si Ollama renvoie un texte sans chiffre alors que le template en a → template  

## 3. Apport ML (ce que le jury doit retenir)

| Composant | Technique | Notre contribution |
|-----------|-----------|--------------------|
| Intent | CamemBERT sentence embeddings (`Lajavaness/sentence-camembert-base`) + Logistic Regression | Dataset RH annoté, entraînement, API `/predict` |
| RAG | Même encodeur → vecteurs 768-d → similarité cosinus | Chunking FAQ + index mémoire + `/embed` |
| Génération | Ollama `llama3.2:3b` (pré-entraîné) | Prompt + grounding + fallback |
| Baseline antérieure | TF-IDF + LR | Remplacée ; même contrat HTTP |

**Formulation honnête pour le rapport :**

> Le modèle de langage (Ollama) est pré-entraîné. Notre apport ML est le **classifieur d’intentions** (CamemBERT + régression logistique sur un corpus de questions RH) et le **retrieval vectoriel** des procédures FAQ. La vérité métier est garantie par des **outils** branchés sur les services Spring / PostgreSQL.

### Jeu de données intent

- Fichier : `ml/intent/dataset.csv`  
- Construction :
  1. Corpus RH Paie maison (`build_dataset.py`, français)  
  2. Fusion Avature (`intent-classification-main`, paper *Intent Classification Methods for Human Resources Chatbots*) via `merge_avature.py`  
- Mapping Avature → RH Paie :
  - `faq_pto_policy`, `faq_maternity_paternity_policy`, `faq_remote_work_policy` → `FAQ_LEAVE`  
  - `faq_company_benefits`, `faq_salary_conversation` → `FAQ_PAYROLL`  
  - recrutement / small-talk / company FAQ → `OUT_OF_SCOPE` (échantillonné, cap 180)  
- FAQ Avature traduites EN→FR (MyMemory, cache `avature_fr_cache.json`) + version anglaise conservée  
- Taille après fusion / déduplication / cap OOS : **~1200+** lignes (outils enrichis à ~40 exemples chacun + Avature FAQ), **19** classes  
- Held-out CamemBERT (80/20 stratifié) : voir `ml/intent/metrics.txt` (accuracy + matrice de confusion)  
- Modèle production réentraîné sur **toutes** les lignes  

Citation Avature (si utilisé dans le rapport) : Lacasa et al., *Intent Classification Methods for Human Resources Chatbots*, Procesamiento del Lenguaje Natural, 2024.  
Repo local : `intent-classification-main/`.  

### API ML locale (`ml/intent/`, port **8010**)

| Endpoint | Rôle |
|----------|------|
| `GET /health` | `backend=camembert` |
| `POST /predict` | `{ "text": "…" }` → `{ intent, confidence }` |
| `POST /embed` | `{ "texts": ["…"] }` → vecteurs normalisés |

Démarrage : `ml/intent/run.sh`  
Entraînement : `python build_dataset.py && python train.py`  
Encodeur local (hors git, ~423 Mo) : `ml/intent/models/sentence-camembert-base/`

## 4. Intentions et outils

### Outils employé

| Intent | Donnée |
|--------|--------|
| `LEAVE_BALANCE` | Solde de congés |
| `MY_LEAVE_REQUESTS` | Demandes de congé |
| `MY_PAYSLIPS` | Bulletins |
| `MY_CONTRACTS` | Contrats |
| `MY_ATTENDANCE` / `FAQ_ATTENDANCE` | **Hors assistant** pour l’instant → renvoi vers le menu Pointage (pas d’outil chat). |
| `LEAVE_CALENDAR` | Calendrier d’absences |
| `MY_SALARIES` | Calculs de salaire finalisés |
| `MY_EXIT_DOCUMENTS` | Documents de sortie visibles |

### Action employé (écriture avec confirmation)

| Intent | Comportement |
|--------|----------------|
| `CREATE_LEAVE_REQUEST` | Dialogue multi-tour (jours / dates) → propose → **Confirmer** → `LeaveRequestService.create` (PENDING). Les créneaux (dates, « weekend », mois FR) sont parsés en **Java** (`LeaveDraftSlotFiller` + tests à date fixe) ; packs de synonymes FR/EN + `TextNormalize` (fautes) ; Ollama est **ignoré** sur les actions pour garder des dates exactes. Échec Confirmer : `Création refusée : {raison Spring}`. |
| `CANCEL_LEAVE_REQUEST` | Cible une demande PENDING → propose → **Confirmer** → `LeaveRequestService.cancel`. Échec : `Annulation n°{id} refusée : {raison}`. |
| `PENDING_LEAVES` + `REVIEW_LEAVE_BATCH` | Admin : liste numérotée des PENDING → « Accepte 1 2, refuseuse 3 » / « accepter tous » / « valider tous » / « ok pour tous » (synonymes + tolérance fautes via `TextNormalize`) → **Confirmer** → batch `approve` / `reject` avec résumé partiel (succès + échecs métier). Les décisions d’écriture restent des **règles Java** ; Ollama n’est pas utilisé pour les slots ni les accept/refuse. |

Admin sur cet intent → accès refusé. Pas d’approbation / paie via le chat.

### Outils admin RH

| Intent | Donnée |
|--------|--------|
| `PENDING_LEAVES` | Congés en attente |
| `EXPIRING_CONTRACTS` | Contrats à échéance |
| `PAYROLL_STATUS` | Avancement paie du mois |
| `EXIT_SUMMARY` | Départs / offboarding |
| `LEAVE_CALENDAR` | Calendrier équipe |

### FAQ (RAG)

| Intent | Guide |
|--------|--------|
| `FAQ_LEAVE` | Congés |
| `FAQ_PAYROLL` | Paie |
| `FAQ_ATTENDANCE` | **Désactivé** dans le chat (renvoi menu Pointage) |

+ `OUT_OF_SCOPE` hors RH Paie.

## 5. RAG FAQ

**Sources :**

1. Guides intégrés (non supprimables) : `backend/src/main/resources/chat/faq/{conges,paie,pointage}.md`  
2. Uploads admin : table `chat_faq_documents` (Flyway V20), fichiers sous `uploads/chat-faq`, texte extrait (md / txt / pdf via PDFBox)

**Chunking :** titres `## ` sinon blocs ~800 caractères.  
**Retrieval :** embeddings CamemBERT + cosine, top 3 ; fallback overlap de mots-clés si `/embed` down.  
**UI admin :** `/admin/chat-faq` — « Guide assistant RH ».

## 6. Frontend

- Widget flottant admin + employé  
- Chip `sourceKind` / `sourceLabel` (Données, Guide, Accès refusé, Hors sujet) + intent  
- Suggestions selon le rôle  
- Page upload FAQ admin  

## 7. Persistance

- `V19__create_chat_messages.sql` — historique chat  
- `V20__create_chat_faq_documents.sql` — procédures uploadées  

## 8. Démo jury (scénarios)

| Acteur | Question | Attendu |
|--------|----------|---------|
| Ali (employé) | Combien de jours de congé me reste-t-il ? | Chip Données + solde réel |
| Ali | Quel est mon salaire ? | Net / ou message « aucun calcul finalisé » (template) |
| Ali | Mon calendrier des absences | Absences période |
| Ali | Comment demander un congé ? | Chip Guide + procédure |
| Admin | Combien de congés en attente ? | Synthèse pending |
| Admin | Quels départs sont prévus ? | EXIT_SUMMARY |
| Admin | Upload `.md` avec `## Télétravail` puis question employé | Guide depuis upload |

Comptes démo typiques : `admin` / `admin123`, `ali` / `employee123`.

## 9. Stack runtime

| Service | Port | Commande |
|---------|------|----------|
| PostgreSQL | 5434 | container démo |
| Intent / embed | 8010 | `ml/intent/run.sh` |
| Ollama | 11434 | modèle `llama3.2:3b` |
| Spring Boot | 8080 | `SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run` |
| Angular | 4200 | `npm start` |

Config : `chat.intent-url`, `chat.min-confidence` (0.55), `chat.ollama.*`, `file.chat-faq-upload-dir`.

## 10. Limites assumées (rapport + viva)

- Dataset encore limité pour certaines classes ; keyword fallback utile  
- RAG **sans** base vectorielle persistante (index en mémoire au démarrage / reload)  
- Write-action v2 = **congés multi-tour + annulation PENDING** ; v3 admin = **liste numérotée + accepte/refuse par index** (pas de pointage, pas de paie)  
- Pas encore de streaming UI ni jeu d’évaluation figé (améliorations 8–9)  
- CamemBERT = encodeur **pré-entraîné** + tête LR entraînée chez nous ; pas un LLM from scratch  

## 11. Améliorations réalisées vs restantes

| # | Sujet | Statut |
|---|--------|--------|
| 1 | Chip source (Données / Guide / …) | Fait |
| 2 | Mémoire conversationnelle | Fait |
| 3 | Upload FAQ admin → RAG dynamique | Fait |
| 4 | Gros dataset + retrain + confusion matrix | Fait |
| 5 | CamemBERT à la place du TF-IDF | Fait |
| 6 | Embeddings RAG (cosine) | Fait |
| 7 | Plus d’outils (calendrier, salaires, sorties) | Fait |
| 8 | Streaming + état Ollama down dans l’UI | À planifier |
| 9 | Jeu d’évaluation fixe (~30 questions) | À planifier |
| 10 | Write-actions | Fait (v3 : employee leave + admin batch review) |

## 12. Chemins code utiles

- Backend chat : `backend/src/main/java/com/rhpaie/chat/`  
- FAQ upload : `…/chat/faq/`  
- ML : `ml/intent/`  
- Widget : `frontend/src/app/shared/components/chat-widget/`  
- Page guide : `frontend/src/app/features/chat-faq/`  

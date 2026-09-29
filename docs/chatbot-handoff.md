# Chatbot feature handoff

**Status:** DONE — feature frozen, re-verified 2026-09-25.  
**Decision:** do not add chatbot capabilities unless there is real-user evidence, a safety/auth issue, or a new approved product scope.

Companion docs:

- Architecture / PFE notes: [`CHATBOT_RAPPORT.md`](CHATBOT_RAPPORT.md)
- Acceptance matrix (Step 9): [`chatbot-acceptance.md`](chatbot-acceptance.md)
- Product bullets: [`../FEATURES.md`](../FEATURES.md) (section *AI Chatbot / Assistant RH*)

---

## 1. What shipped

A role-aware French HR assistant (ADMIN_RH + EMPLOYE) in the floating Angular widget.

| Layer | Responsibility |
|-------|----------------|
| Intent | Local CamemBERT + logistic regression (`ml/intent`, port 8010); keyword fallback if down / low confidence |
| Truth | Spring tools on existing RH services + FAQ RAG — **not** the LLM |
| Writes | Propose → **Confirmer** → `LeaveRequestService` (create / cancel / admin batch review) |
| Phrasing | Ollama `llama3.2:3b` only; template if unanchored or down |
| Greeting | `GET /api/chat/greeting` — READ facts, no context mutation, no Ollama |
| READ context | In-memory, keyed by `userId`, TTL 30 min — never used for writes |

Ollama **never** opens PostgreSQL. Dates and approve/refuse are Java rules.

---

## 2. How to run (demo)

| Service | Port | Command |
|---------|------|---------|
| PostgreSQL | 5434 | `docker start rh-paie-postgres` (or the `docker run` in `backend/README.md`) |
| Intent | 8010 | `ml/intent/run.sh` |
| Ollama (optional) | 11434 | model `llama3.2:3b` — template fallback if absent |
| Backend | 8080 | `SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run` in `backend/` |
| Frontend | 4200 | `npm start` in `frontend/` |

Demo accounts: **`ali` / `employee123`**, **`admin` / `admin123`**.  
Filter name in chat: **Ali** (seeded `Ali Ben Salah` — there is no Ahmed).

---

## 3. Full test results (2026-09-25)

### Automated

| Check | Result |
|-------|--------|
| `cd backend && mvn test` | **123/123 PASS**, BUILD SUCCESS |
| Chat suites | Greeting 9, ReadFollowUp 7, Context store 3, Date extractor 29, Draft filler 24, Date baseline 38, Robustness eval 4, TextNormalize/admin review 9 |
| `cd frontend && npm run build` | **PASS** (existing scss budget warnings only) |

Frozen date metrics (reconfirmed, not retuned):

```text
Regression 44/46 = 95.7%
DEV 97/100 = 97.0%
HOLDOUT 48/50 = 96.0%
False-confident 1/150 = 0.7%
```

### Live API (demo stack, this run)

| ID | Scenario | Result |
|----|----------|--------|
| A1 | Ali greeting | PASS — 17 days, pending count |
| A2 | Leave balance | PASS — DATA |
| A3 | `et mes demandes ?` | PASS — `MY_LEAVE_REQUESTS` |
| A4 | `seulement celles en attente` | PASS — filtered list |
| A5 | FAQ leave | PASS — Guide |
| A6–A7 | Contracts + nearest expiry | PASS — CDI, no end date (correct for Ali) |
| A8 | Ali asks admin pending | PASS — REFUSED |
| A9–A10 | Create 5–6 Nov propose + Confirmer | PASS — created **n°48 PENDING** |
| A11–A12 | Cancel n°48 propose + Confirmer | PASS — cancelled |
| C1–C2 | `cette semaine` / `semaine prochaine` | PASS — asks dates, no invented week |
| B1–B3 | Admin greeting, pending, `seulement ceux de Ali` | PASS — READ filter, not REVIEW |
| B4 | `Accepte 1` propose | PASS — `proposedAction`, no write yet |
| B5 | Confirmer approve overlapping / insufficient balance | PASS — Spring refused, **no silent write** |
| B5b | Confirmer **reject n°22** | PASS — `Revue appliquée` |
| D1 | OOS while REVIEW draft open | Known: stays in REVIEW clarify (no write) |

Intent health: `{"status":"ok","backend":"camembert"}`.

---

## 4. Code map

| Area | Path |
|------|------|
| HTTP | `backend/.../chat/controller/ChatController.java` |
| Orchestration | `.../chat/service/ChatService.java` |
| Tools / RAG / Ollama | `ChatToolService`, `RagService`, `OllamaClient` |
| Dates / drafts | `JavaLeaveDateExtractor`, `LeaveDraftSlotFiller` |
| Admin review | `AdminLeaveReviewService` |
| Greeting | `ChatGreetingService` |
| READ context | `chat/context/*` |
| FAQ admin API | `chat/faq/` |
| Intent ML | `ml/intent/` (`run.sh`, `train.py`, `dataset.csv`) |
| Widget | `frontend/src/app/shared/components/chat-widget/` |
| FAQ page | `frontend/src/app/features/chat-faq/` |
| Chat models/API | `frontend/src/app/features/chat/` |

Endpoints: `GET /api/chat/greeting`, `POST /api/chat/messages`,  
`POST /api/chat/actions/leave-request`, `/cancel`, `/leave-review`, `/discard`.

---

## 5. Invariants — do not break

1. No DB access from Ollama. Facts come from tools/FAQ only.
2. No leave mutation before Confirmer.
3. Greeting must not write `ConversationContext`.
4. READ context is per `userId`, 30 min, JVM-local — not for ACTION.
5. Bare `cette semaine` / `la semaine prochaine` stay **ambiguous**.
6. Do not retune the date parser against HOLDOUT failures.
7. Employee cannot execute admin tools (`PENDING_LEAVES`, batch review).
8. Do not add multi-intent, Redis context, streaming UI, or extra write-actions without a new scope.

---

## 6. Known limitations (accepted)

- No mid-utterance date correction / negation (`pas lundi, mardi`).
- No “annule le deuxième”, no FAQ+ACTION in one turn, no multi-intent.
- Context is process-local (lost on restart; not shared across instances).
- Employee self-read (`mes demandes`, `celles en attente`, solde, contrats, bulletins) now beats an open create/cancel draft. A banner shows `Brouillon en cours — Annuler`. Admin `deuxième`…`dixième` map to list indexes. Greeting still does not write conversation context. `Combien de congés en attente ?` from an employee stays refused.
- Open ACTION draft can still swallow a weak / out-of-scope follow-up (for example CAN while a review draft is open) until discard.
- Corrections, multi-intent, and Redis are still out.
- RAG index is in-memory (reload on boot / FAQ change).
- No dedicated `ChatService` confirm integration test class (covered live + unit parsers).
- Attendance intents redirect to the menu (no chat punch tool).
- Writes beyond leave create / cancel / admin review are out of scope.

---

## 7. Jury / demo script (5 min)

1. Login **ali** → open widget → greeting.  
2. “Combien de jours de congé me reste-t-il ?” → “et mes demandes ?” → “seulement celles en attente”.  
3. “Je veux un congé cette semaine” → must ask for dates.  
4. Explicit dates → **Confirmer** → PENDING appears.  
5. Login **admin** → greeting → pending list → “seulement ceux de Ali” (READ).  
6. “Accepte N” → recap → **Confirmer** (or show Spring refusal if overlap/balance).  
7. Mention chips: Données / Guide / Action / Accès refusé.

---

## 8. If something breaks

| Symptom | First check |
|---------|-------------|
| All intents OUT_OF_SCOPE / weak | `curl :8010/health` — restart `ml/intent/run.sh` |
| Greeting 500 / old week invent | Stale Spring JVM — restart demo profile after `mvn compile` |
| Numbers invented | Ollama down or unanchored → should use template; check `OllamaClient` + `preferTemplateOnly` |
| Write without Confirmer | Bug — stop, inspect `ChatService.reply` vs confirm methods |
| Admin filter becomes REVIEW | Expect READ if context is PENDING; discard open draft first |

---

## 9. Next owner checklist

- [ ] Read this file + acceptance matrix + rapport §2–4  
- [ ] `mvn test` in `backend/` and `npm run build` in `frontend/`  
- [ ] Demo script once on `ali` / `admin`  
- [ ] Treat further NLP/UX as **new work**, not unfinished chatbot scope  

**Chatbot feature development is handed off as complete and frozen.**

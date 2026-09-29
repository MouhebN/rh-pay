# RH Paie

Academic HR and payroll web application (PFE).

French UI, role-based access (`ADMIN_RH` / `EMPLOYE`), and an optional in-app HR assistant.

## Structure

| Folder | Role |
|--------|------|
| `backend/` | Spring Boot 3 API (Java 21), JWT, PostgreSQL, Flyway |
| `frontend/` | Angular app (admin + employee portals, chat widget) |
| `ml/intent/` | Local CamemBERT intent / embedding service (port 8010) |
| `docs/` | Architecture notes, demo script, run guide |

## Features (overview)

- Authentication (JWT)
- Users, departments, job positions, employees
- Contracts and contract monitoring
- Leave requests, balances, calendar, documents
- Attendance (web / manual)
- Payroll variables, calculation, payslips, exports
- Employee exits / offboarding
- Dashboards (admin + employee)
- Notifications (WebSocket)
- HR / payroll settings
- **Chat assistant** (role-aware French helper): tools on real data, FAQ RAG, confirmed leave actions

Not in scope: facial-recognition attendance, multi-intent chat, Derja, Redis chat context.

## Stack

- Frontend: Angular
- Backend: Spring Boot 3, Java 21, Spring Security, PostgreSQL, Flyway
- Intent NLP: pretrained CamemBERT embeddings + logistic regression (`ml/intent`)
- Phrasing (optional): Ollama `llama3.2:3b` local

## Quick start

Full first-time commands: **[docs/RUN_FIRST_TIME.md](docs/RUN_FIRST_TIME.md)**

Summary (5 terminals, in order):

1. **PostgreSQL** on host port `5434` (Docker container `rh-paie-postgres`)
2. **Intent service**: `cd ml/intent && ./run.sh` → `:8010`
3. **Ollama** with `llama3.2:3b` → `:11434` (optional; templates work if down)
4. **API** (demo seed):

```bash
cd backend
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run
```

5. **Frontend**:

```bash
cd frontend
npm install   # first time only
npm start
```

Open: http://localhost:4200

## Demo accounts

| User | Password | Role |
|------|----------|------|
| `admin` | `admin123` | ADMIN_RH |
| `ali` | `employee123` | EMPLOYE |
| `nour` | `employee123` | EMPLOYE |
| `sami` | `employee123` | EMPLOYE |

Demo profile seeds sample employees, leaves, contracts, payroll, etc. Safe to rerun (upsert, not full wipe).

## Chat assistant (short)

- Classifies French intents (CamemBERT + logistic regression)
- Reads business data via existing Spring services
- FAQ via in-memory RAG on internal guides (+ admin uploads)
- Leave create / cancel / admin review only after **Confirmer** (server draft)
- Ollama rephrases only; it does not own numbers, rights, or dates

More detail:

- [docs/RH_PAIE_ASSISTANT_CLIENT_SIMPLE.md](docs/RH_PAIE_ASSISTANT_CLIENT_SIMPLE.md) — simple client explanation
- [docs/RH_PAIE_ASSISTANT_DEMO.md](docs/RH_PAIE_ASSISTANT_DEMO.md) — live demo script
- [docs/RH_PAIE_ASSISTANT_RAPPORT.md](docs/RH_PAIE_ASSISTANT_RAPPORT.md) — technical report

## Main routes

**Admin:** `/admin/dashboard`, users, employees, contracts, leave-requests, attendance, payroll, settings, chat FAQ uploads  

**Employee:** `/employee/dashboard`, my leave, attendance, salaries, payslips, contracts, exit documents  

## License / academic use

Educational project — not a production banking or payroll product.

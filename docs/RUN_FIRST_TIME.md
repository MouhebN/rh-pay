# Démarrer RH Paie — première fois

Ouvrir **5 terminaux**. Respecter l’ordre : base → intent → Ollama → API → frontend.

---

## 1. PostgreSQL (port 5434)

Première fois :

```bash
docker run -d \
  --name rh-paie-postgres \
  -e POSTGRES_DB=rh_paie_db \
  -e POSTGRES_USER=rhpaie_user \
  -e POSTGRES_PASSWORD=rhpaie_password \
  -p 5434:5432 \
  -v rh_paie_postgres_data:/var/lib/postgresql/data \
  postgres:15
```

Si le container existe déjà :

```bash
docker start rh-paie-postgres
```

---

## 2. Intent / CamemBERT (port 8010)

```bash
cd /home/mnaddari/Desktop/rh/ml/intent
./run.sh
```

Attendre Uvicorn sur `127.0.0.1:8010`.

Vérifier :

```bash
curl -s http://127.0.0.1:8010/health
```

**Ne pas** lancer `build_dataset.py` seul (écrase le corpus 1280 lignes).

Le premier lancement peut télécharger CamemBERT (plus long).

---

## 3. Ollama `llama3.2:3b` (port 11434)

Si le container lab existe déjà :

```bash
docker start pfe-ollama-1
```

Sinon, Ollama installé en local :

```bash
ollama serve
```

Si le modèle manque (autre terminal) :

```bash
ollama pull llama3.2:3b
```

Vérifier :

```bash
curl -s http://127.0.0.1:11434/api/tags
```

---

## 4. API Spring Boot (port 8080) — profil demo

```bash
cd /home/mnaddari/Desktop/rh/backend
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run
```

Attendre : `Started RhPaieApplication`.

---

## 5. Angular (port 4200)

Première fois :

```bash
cd /home/mnaddari/Desktop/rh/frontend
npm install
npm start
```

Fois suivantes :

```bash
cd /home/mnaddari/Desktop/rh/frontend
npm start
```

Ouvrir : [http://localhost:4200](http://localhost:4200)  
(parfois `http://[::1]:4200`)

---

## Comptes démo

| Utilisateur | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin123` | ADMIN_RH |
| `ali` | `employee123` | EMPLOYE |
| `nour` | `employee123` | EMPLOYE |
| `sami` | `employee123` | EMPLOYE |

---

## Si un service est down

| Service | Effet |
|---|---|
| Postgres down | L’API ne démarre pas correctement |
| Intent `:8010` down | Repli mots-clés (moins précis) |
| Ollama down | Gabarit TOOL / texte guide FAQ (pas de reformulation) |

---

## Checklist rapide

- [ ] `docker ps` → `rh-paie-postgres` up, port 5434  
- [ ] `curl :8010/health` → `"backend":"camembert"`  
- [ ] `curl :11434/api/tags` → contient `llama3.2:3b`  
- [ ] API `:8080` démarrée avec profil `demo`  
- [ ] Frontend `:4200`  
- [ ] Login `ali` / `admin` OK  

---

*Voir aussi : `docs/RH_PAIE_ASSISTANT_CLIENT_SIMPLE.md` (explication chatbot), `docs/RH_PAIE_ASSISTANT_DEMO.md` (script démo).*

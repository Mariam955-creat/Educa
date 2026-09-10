# 07 — Déploiement (Docker)

Le projet se déploie via **trois conteneurs** orchestrés par `docker-compose.yml` (racine du dépôt) :

| Service | Image | Rôle |
|---|---|---|
| `db` | `postgres:18-alpine` | base de données, volume persistant `db-data` |
| `backend` | build de `backend/Dockerfile` (multi-étapes Maven → JRE 25) | API Spring Boot, profil `prod`, volume `storage-data` pour les fichiers de cours |
| `frontend` | build de `frontend/Dockerfile` (Angular → `nginx:alpine`) | sert le SPA **et** fait le reverse-proxy `/api/` → `backend:8081` |

Seul le `frontend` publie un port (`${WEB_PORT}`, défaut **8080**). `db` et `backend` ne sont
accessibles que sur le réseau interne Compose.

> ⚠️ Les fichiers Docker ont été rédigés d'après la configuration du projet mais **n'ont pas encore
> été exécutés** (poste de dev sans Docker). Vérifier `docker compose build` sur la cible avant la
> soutenance / le rendu.

---

## 1. Déploiement local (ou sur un hôte Docker unique)

### Prérequis
- Docker Engine + plugin Compose v2 (`docker compose version`).

### Étapes

```bash
# 1. configuration
cp .env.docker.example .env
#    éditer .env : POSTGRES_PASSWORD, JWT_SECRET (chaîne aléatoire >= 64 caractères)
#    générer un secret :  openssl rand -base64 48

# 2. build + démarrage
docker compose up -d --build

# 3. suivi
docker compose logs -f backend      # attendre "Started BackendApplication"
docker compose ps
```

Application : <http://localhost:8080>
API (via le proxy nginx) : <http://localhost:8080/api/v1/courses>

### Cycle de vie

```bash
docker compose down           # arrêt (les volumes db-data / storage-data persistent)
docker compose down -v        # arrêt + suppression des données
docker compose up -d --build  # redéploiement après un changement de code
```

### Migrations

Flyway s'exécute automatiquement au démarrage du `backend` (`V1`, `V2`, …). En profil `prod`,
`flyway.clean` est **désactivé**. Le schéma est créé dans le volume `db-data` vide au premier `up`.

### Données de démonstration

Le profil `prod` ne seede **rien**. Deux options :

- **Démo rapide** : mettre `BACKEND_PROFILE=dev` dans `.env` puis `docker compose up -d --build backend`
  → comptes `admin@educa.dev` / `formateur@educa.dev` / `apprenant@educa.dev` / `diplome@educa.dev`
  (`password123`) + 2 cours publiés + 1 certificat (voir `DevDataInitializer`).
- **Production réelle** : rester en `prod`, créer le premier compte via `POST /api/v1/auth/register`,
  puis le promouvoir `ADMIN` en base (`INSERT INTO user_roles …`), le reste se gère dans l'application.

---

## 2. Déploiement cloud

Le `docker-compose.yml` est **portable** : il tourne tel quel sur toute machine dotée de Docker
(VM type *e2-small* / *t3.small*, 2 vCPU / 2–4 Go, ~10 Go disque suffisent pour une démo).

### 2.a — Hôte Docker managé / VM

1. Provisionner la VM, installer Docker + Compose.
2. `git clone` le dépôt, créer `.env` (voir §1), **`CORS_ALLOWED_ORIGINS=https://<domaine>`**, `WEB_PORT=80`.
3. `docker compose up -d --build`.
4. Mettre un reverse-proxy TLS **devant** le conteneur `frontend` (Caddy, Traefik ou nginx hôte +
   Let's Encrypt) — terminaison HTTPS, redirection 80→443.

### 2.b — Base de données managée (recommandé en production)

Retirer le service `db` du compose (ou ne pas le démarrer) et pointer le `backend` sur l'instance
managée (RDS, Cloud SQL, Neon, Supabase…) :

```bash
# dans .env
POSTGRES_HOST=<hôte-managé>
POSTGRES_PORT=5432
POSTGRES_DB=educa
POSTGRES_USER=<user>
POSTGRES_PASSWORD=<mot-de-passe>
```

> Le profil `prod` lit `POSTGRES_HOST/PORT/DB/USER/PASSWORD` (voir `application-prod.yml`) ;
> aucune autre modification n'est nécessaire.

### 2.c — Plateformes conteneur (Render, Railway, Fly.io, Scaleway, Clever Cloud…)

Deux services séparés :
- **backend** : image construite depuis `backend/Dockerfile`, port `8081`, variables d'env du §1 +
  `SPRING_PROFILES_ACTIVE=prod`, base managée fournie par la plateforme.
- **frontend** : image depuis `frontend/Dockerfile`. Adapter `frontend/nginx.conf` : remplacer
  `proxy_pass http://backend:8081;` par l'URL interne du service backend de la plateforme.

### 2.d — Stockage des fichiers

En conteneur, les fichiers de cours vont dans le volume `storage-data`. Pour un déploiement
multi-instances ou sans volume persistant fiable, brancher l'implémentation S3 du module `storage`
(prévue par l'interface `StorageService`, non incluse dans le MVP).

---

## 3. Rappels de configuration

| Variable | Rôle | Défaut |
|---|---|---|
| `WEB_PORT` | port public du frontend | `8080` |
| `BACKEND_PROFILE` | `prod` (rien de seedé) ou `dev` (démo) | `prod` |
| `POSTGRES_PASSWORD` | **requis** | — |
| `JWT_SECRET` | **requis**, ≥ 64 caractères aléatoires | — |
| `CORS_ALLOWED_ORIGINS` | origine(s) publiques autorisées | `http://localhost:8080` |
| `STORAGE_MAX_FILE_SIZE_MB` | plafond upload (aligné avec `client_max_body_size` nginx) | `200` |
| `AI_ENABLED` / `ANTHROPIC_API_KEY` | chatbot live ; sans clé → repli `degraded` | `false` / vide |

---

## 4. Checklist avant mise en ligne

- [ ] `.env` rempli, **jamais** commité (déjà couvert par `.gitignore`).
- [ ] `JWT_SECRET` et `POSTGRES_PASSWORD` propres à l'environnement (pas les valeurs d'exemple).
- [ ] `CORS_ALLOWED_ORIGINS` = domaine public réel.
- [ ] HTTPS terminé par un reverse-proxy devant le `frontend`.
- [ ] Sauvegardes de la base (volume `db-data` ou base managée).
- [ ] `docker compose logs backend` : Flyway OK, `Started BackendApplication`, pas d'erreur.
- [ ] Parcours de fumée : `node scripts/e2e-mvp.mjs` adapté à l'URL publique, ou la checklist de `docs/05-demo-soutenance.md`.

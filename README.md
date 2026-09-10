# educa

Plateforme e-learning — Projet de Fin d'Études.

Trois rôles (**apprenant**, **formateur**, **administrateur**). Parcours cœur :
un formateur publie des formations (cours → chapitres → contenus), un **contrôle** par chapitre et un **examen final** par cours ;
un apprenant s'inscrit, apprend à son rythme, atteint 100 % de progression, débloque et passe l'examen final.
**Note finale pondérée = 40 % moyenne des contrôles + 60 % examen final** ; au-dessus du seuil du cours (défaut 70 %), un **certificat PDF vérifiable** est délivré.
Volets différenciants : **interface multilingue FR / EN / AR** (avec RTL) et **chatbot pédagogique** (API Claude / Anthropic).

Dépôt : https://github.com/Mariam955-creat/Educa

---

## Stack

| Couche | Techno |
|---|---|
| Frontend | Angular 19.2 (standalone) + TypeScript, i18n `@ngx-translate` (FR/EN/AR + RTL) |
| Backend | Java 25 + Spring Boot 4.1.1 (Maven), API REST `/api/v1` sur le port **8081** |
| Persistance | PostgreSQL + Spring Data JPA, migrations Flyway |
| BDD (dev) | PostgreSQL installé en local + pgAdmin (pas de Docker pour l'instant) |
| Auth | Spring Security + JWT (access + refresh haché, rotation), BCrypt, RBAC `@PreAuthorize` |
| Fichiers | Module `storage` — système de fichiers local en dev, S3-compatible en cible |
| PDF | openhtmltopdf (gabarit HTML → certificat A4 paysage) |
| IA | SDK officiel `com.anthropic:anthropic-java`, isolé dans `com.educa.backend.ai`, repli `degraded` si clé absente |
| Déploiement | `docker-compose.yml` (backend + frontend/nginx + PostgreSQL) — voir [`docs/07-deploiement.md`](docs/07-deploiement.md). En dev : PostgreSQL local, pas de Docker |

> Le détail et la justification des choix : [`docs/02-conception.md`](docs/02-conception.md) §8.

---

## Structure du dépôt

```
backend/            API Spring Boot (Maven) — package-by-feature : user, course, enrollment, quiz, certificate, storage, ai (+ common, security)
frontend/           Application Angular — core/ (transverses) + feature/ (un dossier par domaine)
docs/               Analyse, conception, plan d'implémentation, journal, démo, vérification, déploiement
scripts/            e2e-mvp.mjs — vérification bout-en-bout du parcours MVP contre l'API
docker-compose.yml  Stack de déploiement (backend + frontend/nginx + PostgreSQL)
CLAUDE.md           Contexte condensé du projet (à lire en premier)
```

---

## Documentation

| Fichier | Contenu |
|---|---|
| [`docs/01-analyse.md`](docs/01-analyse.md) | Contexte, personas, user stories, exigences, risques |
| [`docs/02-conception.md`](docs/02-conception.md) | Architecture, MCD, schéma BDD, API, sécurité, i18n, IA, wireframes |
| [`docs/03-plan-implementation.md`](docs/03-plan-implementation.md) | Tableau de bord : checklist des phases 0 → 6 |
| [`docs/04-journal-avancement.md`](docs/04-journal-avancement.md) | Journal daté par session de travail |
| [`docs/05-demo-soutenance.md`](docs/05-demo-soutenance.md) | Scénario de démonstration pas à pas pour la soutenance |
| [`docs/06-verification-mvp.md`](docs/06-verification-mvp.md) | Vérification bout-en-bout du périmètre MVP (résultats + couverture) |
| [`docs/07-deploiement.md`](docs/07-deploiement.md) | Déploiement Docker (local + cloud) |
| [`docs/assets/`](docs/assets/) | Diagrammes exportés (architecture, MCD) — sources Mermaid + SVG/PNG |

---

## Démarrage (développement)

### Prérequis
- JDK 25
- Node.js (poste en 24.12 → Angular CLI 19.2 ; ne pas passer à Angular 20+ tant que Node < 24.15)
- PostgreSQL installé localement (port 5432) + pgAdmin
- Créer, via pgAdmin, les bases `educa` (dev) et `educa_test` (tests)

### Configuration
```bash
cp .env.example .env      # renseigner au minimum POSTGRES_PASSWORD (mot de passe local actuel : change-me)
                          # ANTHROPIC_API_KEY reste un placeholder → le chatbot répond en mode "degraded"
```

### Backend
```bash
cd backend
./mvnw spring-boot:run    # http://localhost:8081/api/v1  — profil dev, données de démo seedées
./mvnw test               # profil test → base educa_test (30 tests)
```

### Frontend
```bash
cd frontend
npm install
npm start                 # http://localhost:4200
npm run build             # build de production
npm run test:ci           # Karma headless (13 tests)
```

### Vérification bout-en-bout

Backend lancé (profil `dev`), puis :

```bash
node scripts/e2e-mvp.mjs   # rejoue tout le parcours MVP contre l'API — 47 contrôles
```

### Comptes de démonstration (profil `dev`)

| Rôle | Email | Mot de passe |
|---|---|---|
| Administrateur | `admin@educa.dev` | `password123` |
| Formateur | `formateur@educa.dev` | `password123` |
| Apprenant | `apprenant@educa.dev` | `password123` |
| Apprenant (déjà certifié) | `diplome@educa.dev` | `password123` |

Deux cours publiés du formateur de démo sont seedés — « Introduction à Python » et « Les bases de Git » (chacun 2 chapitres, 4 contenus, 1 contrôle, 1 examen final). Le compte `diplome@educa.dev` a déjà suivi « Les bases de Git » de bout en bout : son certificat est disponible dans « Mes certificats » et vérifiable sur `/verify/<code>`.

---

## Déploiement (Docker)

```bash
cp .env.docker.example .env        # renseigner POSTGRES_PASSWORD + JWT_SECRET
docker compose up -d --build       # backend + frontend/nginx + PostgreSQL
# → http://localhost:8080
```

Détail (cloud, base managée, TLS, données de démo) : [`docs/07-deploiement.md`](docs/07-deploiement.md).

---

## État d'avancement

**Phases 0 → 5 (MVP) terminées.** Phase 6 (rédaction finale & soutenance) quasi terminée — reste la relecture visuelle au navigateur (voir [`docs/06-verification-mvp.md`](docs/06-verification-mvp.md) §4).
Suivi détaillé : [`docs/03-plan-implementation.md`](docs/03-plan-implementation.md) et [`docs/04-journal-avancement.md`](docs/04-journal-avancement.md).

Résiduel connu (hors périmètre MVP) : Swagger UI (springdoc pas encore compatible Spring Boot 4), clé Anthropic réelle pour le chatbot live, `pg_trgm` pour la recherche catalogue à l'échelle, sniffing de contenu des uploads (Apache Tika). Les fichiers Docker sont rédigés mais pas encore exécutés (poste de dev sans Docker).

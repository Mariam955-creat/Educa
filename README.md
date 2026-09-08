# educa

Plateforme e-learning — Projet de Fin d'Études.

Trois rôles (**apprenant**, **formateur**, **administrateur**). Parcours cœur :
un formateur publie des formations (cours → chapitres → contenus) et des quiz ;
un apprenant s'inscrit, apprend à son rythme, passe les quiz et obtient un **certificat PDF vérifiable** en cas de réussite.
Volets différenciants : **interface multilingue FR / EN / AR** (avec RTL) et **chatbot pédagogique** (API IA externe).

Dépôt : https://github.com/Mariam955-creat/Educa

---

## Stack

| Couche | Techno |
|---|---|
| Frontend | Angular + TypeScript, i18n `@ngx-translate` |
| Backend | Java 25 + Spring Boot 4.1.1 (Maven), API REST `/api/v1` |
| Persistance | PostgreSQL + Spring Data JPA, migrations Flyway |
| BDD (dev) | PostgreSQL installé en local + pgAdmin (pas de Docker pour l'instant) |
| Auth | Spring Security + JWT (access + refresh), BCrypt |
| Fichiers | Module `storage` — système de fichiers local en dev, S3-compatible en cible |
| IA | API Claude (Anthropic), isolée dans `com.educa.backend.ai` |
| Infra | Docker + docker-compose : reporté |

> Le détail et la justification des choix : [`docs/02-conception.md`](docs/02-conception.md) §8.

---

## Structure du dépôt

```
backend/    API Spring Boot (Maven)
frontend/   Application Angular
docs/       Analyse, conception, plan d'implémentation, journal d'avancement
CLAUDE.md   Contexte condensé du projet (à lire en premier)
```

---

## Documentation

| Fichier | Contenu |
|---|---|
| [`docs/01-analyse.md`](docs/01-analyse.md) | Contexte, personas, user stories, exigences, risques |
| [`docs/02-conception.md`](docs/02-conception.md) | Architecture, MCD, schéma BDD, API, sécurité, i18n, IA, wireframes |
| [`docs/03-plan-implementation.md`](docs/03-plan-implementation.md) | Tableau de bord : checklist des phases 0 → 6 |
| [`docs/04-journal-avancement.md`](docs/04-journal-avancement.md) | Journal daté par session de travail |

---

## Démarrage (développement)

> ⚠️ En cours de mise en place (Phase 0). Les commandes ci-dessous seront opérationnelles à la fin de la Phase 0 / Phase 1.

### Prérequis
- JDK 25
- Node.js LTS (pour le frontend)
- PostgreSQL installé localement (port 5432) + pgAdmin
- Créer, via pgAdmin, les bases `educa` (dev) et `educa_test` (tests)

### Configuration
```bash
cp .env.example .env      # secrets : mot de passe PostgreSQL, secret JWT, clé API Claude
```

### Backend
```bash
cd backend
./mvnw spring-boot:run    # http://localhost:8080  (Swagger UI : /swagger-ui.html) — profil dev
./mvnw test               # profil test → base educa_test
```

### Frontend seul
```bash
cd frontend
npm install
npm start                 # http://localhost:4200
npm test
```

---

## État d'avancement

**Phase 0 — Cadrage** (documentation-first). Voir [`docs/03-plan-implementation.md`](docs/03-plan-implementation.md).

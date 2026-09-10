# CLAUDE.md — Contexte projet educa

> Fichier lu en premier à chaque session. Résumé condensé du projet.
> Pour l'état d'avancement réel : voir `docs/03-plan-implementation.md` (tableau de bord) et `docs/04-journal-avancement.md` (journal daté).

## 1. Objectif

**educa** : plateforme e-learning (PFE). Trois rôles : **apprenant**, **formateur**, **administrateur**.
Parcours cœur : un formateur crée des formations (cours → chapitres → contenus), un **contrôle** par chapitre et un **examen final** par cours ;
un apprenant s'inscrit, consulte les cours, passe les contrôles, atteint 100 % de progression, débloque et passe l'examen final.
**Note finale pondérée = 40 % moyenne des contrôles + 60 % examen final** ; si elle atteint le seuil du cours (défaut 70 %), un **certificat PDF vérifiable** est délivré.
Fonctionnalités différenciantes : interface multilingue **FR / EN / AR** (avec RTL) et **chatbot pédagogique** basé sur l'API Claude (Anthropic).

## 2. Stack

| Couche | Techno | Notes |
|---|---|---|
| Frontend | **Angular 19.2** (standalone components) + TypeScript | dev server :4200 ; i18n via `@ngx-translate/core` (Phase 4), RTL arabe. Version bloquée à 19 tant que Node < 24.15 sur le poste |
| Backend | **Java 25 + Spring Boot 4.1.1** (Maven) | Structure en couches : `controller` → `service` → `repository` ; package racine `com.educa.backend` |
| Persistance | **PostgreSQL** + Spring Data JPA / Hibernate | Migrations versionnées avec **Flyway** dès la Phase 1 |
| BDD (dev) | **PostgreSQL installé en local + pgAdmin** | Pas de Docker pour l'instant. Bases : `educa` (dev), `educa_test` (tests) |
| Auth | **Spring Security + JWT** (access + refresh), hachage **BCrypt** | Middleware/filtre de rôle réutilisable (RBAC) |
| IA | API Claude / Anthropic | Isolée dans `com.educa.backend.ai` — derrière l'interface `AiAssistant`, fallback si indispo |
| Stockage fichiers | Interface `storage` — **dev : système de fichiers local** ; cible : S3-compatible | Vidéos / documents de cours ; impl. S3/MinIO ajoutée plus tard |
| PDF certificats | openhtmltopdf / OpenPDF (à figer en Phase 3) | Template HTML → PDF |
| Conteneurisation | `docker-compose.yml` (racine) : `backend/Dockerfile` (JRE 25), `frontend/Dockerfile` (Angular → nginx + reverse-proxy `/api`), `postgres:18` — profil `prod` (`application-prod.yml`). Voir `docs/07-deploiement.md`. **Écrit mais pas encore exécuté** (poste sans Docker). En dev : toujours PostgreSQL local, pas de Docker |
| Tests backend | JUnit 5 + Spring Boot Test ; BDD de test = `educa_test` locale | Testcontainers seulement si Docker dispo (CI). a minima : auth, quiz, certification |
| Tests frontend | Karma/Jasmine (défaut Angular) | a minima : parcours login + passage de quiz |

> ⚠️ Le document de brief initial mentionnait Next.js + Node/Express/Prisma. Décision actée : **Angular + Spring Boot**. Voir `docs/02-conception.md` §Choix techniques.

## 3. Structure du repo

```
/
├── backend/          # Spring Boot (Maven) — API REST
│   ├── pom.xml
│   └── src/main/java/com/educa/backend/
├── frontend/         # Application Angular (à initialiser en Phase 1)
├── docs/
│   ├── 01-analyse.md
│   ├── 02-conception.md
│   ├── 03-plan-implementation.md   # ← tableau de bord vivant (checklist)
│   └── 04-journal-avancement.md    # ← journal daté par session
├── CLAUDE.md
└── README.md
```

> `docker-compose.yml` (racine) = stack de déploiement (voir `docs/07-deploiement.md`). En **dev**, PostgreSQL reste installé localement et administré avec pgAdmin (pas de Docker sur le poste).

## 4. Commandes utiles

| But | Commande |
|---|---|
| Prérequis dev | PostgreSQL local démarré (port 5432), bases `educa` + `educa_test` créées via pgAdmin |
| Backend — build | `cd backend && ./mvnw clean package` |
| Backend — run | `cd backend && ./mvnw spring-boot:run` (profil `dev`, API sur **:8081**) |
| Backend — tests | `cd backend && ./mvnw test` (profil `test` → `educa_test`) |
| Nettoyer le schéma dev | `cd backend && ./mvnw org.flywaydb:flyway-maven-plugin:12.4.0:clean -Dflyway.url=jdbc:postgresql://localhost:5432/educa -Dflyway.user=postgres -Dflyway.password=<mdp> -Dflyway.cleanDisabled=false` |
| Frontend — run | `cd frontend && npm start` (Angular 19.2, dev server sur **:4200**) |
| Frontend — build | `cd frontend && npm run build` |
| Frontend — tests | `cd frontend && npm test` (Karma) |

## 5. Règles de travail (imposées par le brief)

1. **Pas de code avant validation humaine** de la doc Phase 0 (analyse + conception, en particulier le MCD et l'architecture).
2. **Avancer phase par phase** dans l'ordre du plan d'implémentation. Jamais tout d'un coup.
3. **Priorité absolue au périmètre MVP (« Must have »)** — voir `docs/03-plan-implementation.md`.
4. **Mettre à jour la doc à la fin de chaque phase / session significative** :
   - cocher les tâches dans `03-plan-implementation.md`
   - ajouter une entrée dans `04-journal-avancement.md`
   - répercuter dans `02-conception.md` tout écart d'archi / de schéma / de choix technique
   - mettre à jour ce `CLAUDE.md` si la structure ou les commandes changent

## 6. Conventions de code

- **Architecture modulaire (*package-by-feature*)** : chaque entité / concept métier = **un module autonome** contenant toutes ses couches (entité, repository, service, controller, DTO, mapper). Organisation par domaine, jamais par couche technique — pas de packages transverses `controllers/`, `services/`, `repositories/`. Détail et arborescence : `docs/02-conception.md §1.1`.
- **Backend** : modules `user`, `course`, `enrollment`, `quiz`, `certificate`, `storage`, `ai` + `common` (erreurs, web, util) et `security` (transverses). DTO en entrée/sortie de `controller`, entités JPA jamais exposées. Un module dépend du *service* d'un autre, jamais de son repository/ses entités ; pas de cycle. Gestion d'erreurs centralisée via `@RestControllerAdvice`. Validation par `jakarta.validation`.
- **Frontend** : un dossier `feature/<x>/` autonome par domaine (composants + routing + modèles + `<X>ApiService`), `core/` (services transverses, intercepteurs HTTP, guards), `shared/` (composants réutilisables). Appels API centralisés dans les services de feature, jamais dans les composants. Chargement lazy par route.
- **API REST** : préfixe `/api`, versionnée `/api/v1`. Noms de ressources au pluriel. Codes HTTP standards.
- **Langue** : code et identifiants en anglais ; documentation (`docs/`) en français.
- **Commits** : format court `type(scope): message` (`feat`, `fix`, `docs`, `chore`, `test`, `refactor`). **Tous les commits sont faits au nom de `mariam Balde <mariambalde95@gmail.com>` (auteur principal du dépôt) — aucune ligne de co-auteur ni d'attribution Claude/Claude Code dans les messages.** Config posée en `--local` sur le dépôt.

## 7. État actuel

**Phases 0 → 5 (MVP) terminées** (reste 1.10 Swagger ; multilingue Phase 4 validé au navigateur, chatbot IA live en attente d'une vraie clé Anthropic). **Phase 6 (rédaction finale & soutenance) en cours** : `README.md` réécrit (état réel, port 8081, comptes démo) ; `docs/05-demo-soutenance.md` créé (scénario pas à pas ~15 min) ; **jeu de démo enrichi** (6.2 : 4 comptes dont `diplome@educa.dev`, 2 cours publiés « Introduction à Python » + « Les bases de Git », parcours du compte diplômé rejoué → certificat pré-émis) ; **diagrammes exportés** (6.4 : `docs/assets/architecture.*` + `docs/assets/mcd.*` en .mmd/.svg/.png) ; **vérif bout-en-bout API** (6.5 : `scripts/e2e-mvp.mjs` rejoue tout le parcours MVP contre `:8081` → **47/47 PASS** ; rapport `docs/06-verification-mvp.md`) ; **déploiement Docker** (6.6 : `docker-compose.yml` + `backend/Dockerfile` + `frontend/Dockerfile` (nginx, reverse-proxy `/api`) + `application-prod.yml` + `docs/07-deploiement.md` ; frontend passé à une API relative `/api/v1` + proxy Angular en dev ; **fichiers Docker non encore exécutés**, poste sans Docker). Reste : relecture UI au navigateur (checklist `docs/06` §4), passe finale `docs/01`+`02` (6.1).
Phase 5 : revue RBAC endpoint par endpoint (sans faille) ; uploads durcis (taille bornée + liste blanche MIME → `413`/`415`), téléchargement en `attachment` par défaut + `X-Content-Type-Options: nosniff`, `GlobalExceptionHandler` renvoie `413`/`415`/`400` au lieu de `500` ; **tests frontend** (`npm run test:ci` headless → **13 verts**) ; **frontend responsive** (barre nav, tableaux, cartes) ; `/security-review` → **0 finding**.
Backend **opérationnel et testé** (`./mvnw test` → **23 tests verts** : auth 8, course 5, quiz 4, ai 2, upload-security 3, contexte 1), API sur **:8081** :
- modules `user` (auth JWT), `course` (CRUD + catalogue + publication), `enrollment` (inscription + progression %), `storage` (FS local + upload/download multipart), `quiz` (contrôle + examen final, correction auto, note pondérée 40/60, déverrouillage à 100 %), `certificate` (PDF via openhtmltopdf, n° de série, vérif publique par code), `ai` (chatbot via SDK Anthropic `anthropic-java`, repli `degraded` si clé absente/erreur) ;
- sécurité : `SecurityConfig` stateless, `@PreAuthorize` INSTRUCTOR/ADMIN + contrôle objet, `CurrentUser.hasRole/optionalId` ; `GlobalExceptionHandler` (400/401/403/404/409/500 homogènes) ;
- migrations Flyway `V1`+`V2` (aucune nouvelle depuis) ; `DevDataInitializer` : 4 comptes (admin, formateur, apprenant, `diplome@educa.dev`) + 2 cours de démo publiés (contrôle + examen final chacun) + parcours du compte diplômé rejoué → certificat pré-émis.
Frontend **Angular 19.2** (`npm run build` OK) : `core/` auth + courses + enrollments + quiz + certificates + ai + i18n ; `feature/` catalog, course (page cours : contenus + progression + quiz + évaluation + certificat + widget chatbot), quiz (`quiz-take`), instructor (`course-editor` + `quiz-editor` + `course-results`), certificate (`my-certificates` + `verify/:code` public), dashboard.
**i18n** : `@ngx-translate` v18, `public/i18n/{fr,en,ar}.json`, sélecteur de langue (barre) + RTL arabe + persistance `localStorage`/`PATCH /auth/me`.
⚠️ Le chatbot renvoie un message de repli tant que `.env` n'a pas de vraie `ANTHROPIC_API_KEY` (`AI_MODEL` défaut `claude-sonnet-5`).
Git : `main` sur `origin`, commits au nom de mariam Balde.
PostgreSQL local : bases `educa` et `educa_test` créées. **Pas de Docker.**

⚠️ **`.env` à la racine requis** (`cp .env.example .env` + `POSTGRES_PASSWORD`). Le mot de passe PostgreSQL local est actuellement `change-me`. Port API = **8081** (8080 pris par `mysqld`).
Comptes de démo (profil `dev`) : `admin@educa.dev` / `formateur@educa.dev` / `apprenant@educa.dev`, mot de passe `password123`.

### Choix techniques figés en Phase 1
- **Lombok** (`@Getter/@Setter/@NoArgsConstructor` sur les entités) + **MapStruct** (`@Mapper(componentModel = "spring")`) — configurés via `annotationProcessorPaths` du `maven-compiler-plugin` (ordre : lombok, mapstruct-processor, lombok-mapstruct-binding). ⚠️ l'IDE doit avoir le plugin Lombok activé.
- **DTO = `record`** Java.
- **Jackson 3** (`tools.jackson`) : c'est le défaut de Spring Boot 4 (Jackson 2 n'est présent qu'en transitif runtime de jjwt). Ne pas importer `com.fasterxml.jackson.databind.ObjectMapper`.
- Refresh token opaque aléatoire, stocké haché (SHA-256), rotation à chaque `/refresh`.
- `SecurityConfig` en `@Configuration(proxyBeanMethods = false)`.

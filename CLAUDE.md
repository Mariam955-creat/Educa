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
| Conteneurisation | `docker-compose.yml` (racine) : `backend/Dockerfile` (JRE 25), `frontend/Dockerfile` (Angular → nginx + reverse-proxy `/api`), `postgres:18` (volume monté sur `/var/lib/postgresql`, pas `…/data` — convention v18+) — profil `prod` (`application-prod.yml`). Voir `docs/07-deploiement.md`. **`docker compose build` + `up` vérifiés le 2026-09-14** (stack complète fonctionnelle). En dev : toujours PostgreSQL local, pas de Docker |
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

**Phases 0 → 5 (MVP) terminées** (multilingue Phase 4 validé au navigateur sur tout le périmètre depuis le 2026-09-13, chatbot IA live en attente d'une vraie clé Anthropic). **Phase 6 (rédaction finale & soutenance) terminée** (6.1→6.6 faites) : `README.md` réécrit (état réel, port 8081, comptes démo) ; `docs/05-demo-soutenance.md` créé (scénario pas à pas ~15 min) ; **jeu de démo enrichi** (6.2 : 4 comptes dont `diplome@educa.dev`, 2 cours publiés « Introduction à Python » + « Les bases de Git », parcours du compte diplômé rejoué → certificat pré-émis) ; **diagrammes exportés** (6.4 : `docs/assets/architecture.*` + `docs/assets/mcd.*` en .mmd/.svg/.png) ; **vérif bout-en-bout API** (6.5 : `scripts/e2e-mvp.mjs` rejoue tout le parcours MVP contre `:8081` → **47/47 PASS** ; rapport `docs/06-verification-mvp.md`) ; **déploiement Docker** (6.6 : `docker-compose.yml` + `backend/Dockerfile` + `frontend/Dockerfile` (nginx, reverse-proxy `/api`) + `application-prod.yml` + `docs/07-deploiement.md` ; frontend passé à une API relative `/api/v1` + proxy Angular en dev ; **`docker compose build` + `up` vérifiés le 2026-09-14** — voir ci-dessous) ; **passe doc finale** (6.1 : `docs/01` encadré « état de réalisation », `docs/02` nouvelle §0 « écarts conception ↔ implémentation ») ; **relecture UI au navigateur faite** (6.5, checklist `docs/06` §4, 2026-09-13) → **3 bugs réels trouvés et corrigés** : certificat `500` pour un cours sans contrôle (contrainte `NOT NULL`), i18n incomplet sur 8+ pages (retrofit complet FR/EN/AR), formulaire du chatbot qui rechargeait la page au lieu d'envoyer le message (détail : `docs/04-journal-avancement.md` 2026-09-13). **Le plan d'implémentation (Phases 0 → 6) est intégralement terminé.**

**Swagger UI intégré le 2026-09-15** (tâche 1.10, résiduelle depuis la Phase 1) : `springdoc-openapi-starter-webmvc-ui` 3.1.1 (release compatible Spring Boot 4 depuis sa 3.0.1) ajouté au `pom.xml` ; `com.educa.backend.config.OpenApiConfig` déclare le schéma de sécurité `bearer-jwt` (bouton *Authorize*) ; `/v3/api-docs` et `/swagger-ui.html` (→ `/swagger-ui/index.html`) passés en `permitAll` dans `SecurityConfig` ; **désactivé en profil `prod`** (`springdoc.api-docs.enabled=false` / `swagger-ui.enabled=false`) — jamais exposé publiquement. Vérifié : les 29 endpoints des 9 contrôleurs apparaissent dans la spec, `./mvnw test` → 30/30 verts sans régression. Accessible en dev sur `http://localhost:8081/swagger-ui.html`.

**Module admin (Should have) partiellement développé le 2026-09-15** : `AdminUserController` (`com.educa.backend.user`) — `GET /admin/users?q=` (recherche paginée nom/email), `PATCH /admin/users/{id}/roles` (`400` si rôle inconnu, `409` si un ADMIN retire son propre rôle), `PATCH /admin/users/{id}/status` (`409` si un ADMIN se désactive lui-même) ; `AuthService.refresh()` bloque désormais un compte désactivé (`401`), pas seulement le login. `AdminCertificateController` (`com.educa.backend.certificate`) — `GET /admin/certificates` (registre paginé). Frontend : `/admin` réécrit (`feature/admin`, `core/admin/AdminApiService`) avec deux onglets Utilisateurs (recherche, sélecteur de rôle, activer/désactiver — actions désactivées sur son propre compte) et Certificats (registre en lecture seule). **Gestion des langues actives non développée** (`GET/PATCH /admin/languages`, Should have — pas de table `languages`). Vérifié : `./mvnw test` → 39/39 verts (7 nouveaux tests `AdminUserControllerTest` + 2 `AdminCertificateControllerTest`), `npm run build` OK, endpoints revérifiés via l'API réelle (`admin@educa.dev` → `200`, `apprenant@educa.dev` → `403`).
**Régression frontend pré-existante corrigée le 2026-09-15** (découverte pendant la session admin, non liée au module en lui-même) : `quiz-take.component.spec.ts` ne fournissait pas `TranslateService` dans son `TestBed` — cassé depuis le retrofit i18n du 2026-09-13 qui a ajouté `TranslatePipe`/`TranslateService` à `QuizTakeComponent` sans mettre à jour le test (`NullInjectorError: No provider for TranslateService`, 4 tests en échec / 13). Corrigé par `provideTranslateService({})` dans les providers du test, même patron déjà utilisé par `login.component.spec.ts`. `npm run test:ci` → **13/13 verts**.

**Docker vérifié le 2026-09-14** : le blocage précédemment attribué à VT-x désactivé au BIOS était un **faux négatif** (`VirtualizationFirmwareEnabled` reste `False` en WMI même virtualisation active, car un hyperviseur Windows tourne déjà — `HypervisorPresent=True`, socle de WSL2) ; le vrai souci était que Docker Desktop n'était pas lancé. Une fois lancé, `docker compose build` puis `up` fonctionnent. Un vrai bug a été trouvé et corrigé au passage : avec `postgres:18-alpine`, le volume `db-data` doit être monté sur `/var/lib/postgresql` (pas `…/data`, nouvelle convention `pg_ctlcluster` de l'image v18+) — déjà corrigé dans `docker-compose.yml`. Stack testée bout en bout (`db` healthy, `backend` connecté, `frontend` + reverse-proxy `/api` répondent `200`). Détail : journal 2026-09-14.
Phase 5 : revue RBAC endpoint par endpoint (sans faille) ; uploads durcis (taille bornée + liste blanche MIME → `413`/`415`), téléchargement en `attachment` par défaut + `X-Content-Type-Options: nosniff`, `GlobalExceptionHandler` renvoie `413`/`415`/`400` au lieu de `500` ; **tests frontend** (`npm run test:ci` headless → **13 verts**) ; **frontend responsive** (barre nav, tableaux, cartes) ; `/security-review` → **0 finding**.
Backend **opérationnel et testé** (`./mvnw test` → **39 tests verts** : auth 8, course 5, quiz 4, ai 9, upload-security 3, contexte 1, admin-user 7, admin-certificate 2), API sur **:8081** :
- modules `user` (auth JWT + admin utilisateurs), `course` (CRUD + catalogue + publication), `enrollment` (inscription + progression %), `storage` (FS local + upload/download multipart), `quiz` (contrôle + examen final, correction auto, note pondérée 40/60, déverrouillage à 100 %), `certificate` (PDF via openhtmltopdf, n° de série, vérif publique par code, registre admin), `ai` (chatbot via SDK Anthropic `anthropic-java`, repli `degraded` si clé absente/erreur) ;
- sécurité : `SecurityConfig` stateless, `@PreAuthorize` INSTRUCTOR/ADMIN + contrôle objet, `CurrentUser.hasRole/optionalId` ; `GlobalExceptionHandler` (400/401/403/404/409/500 homogènes) ;
- migrations Flyway `V1`+`V2` (aucune nouvelle depuis) ; `DevDataInitializer` : 4 comptes (admin, formateur, apprenant, `diplome@educa.dev`) + 2 cours de démo publiés (contrôle + examen final chacun) + parcours du compte diplômé rejoué → certificat pré-émis.
Frontend **Angular 19.2** (`npm run build` OK) : `core/` auth + courses + enrollments + quiz + certificates + ai + admin + i18n ; `feature/` catalog, course (page cours : contenus + progression + quiz + évaluation + certificat + widget chatbot), quiz (`quiz-take`), instructor (`course-editor` + `quiz-editor` + `course-results`), certificate (`my-certificates` + `verify/:code` public), admin (`admin-dashboard` : onglets utilisateurs/certificats), dashboard.
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

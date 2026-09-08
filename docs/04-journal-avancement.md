# 04 — Journal d'avancement

Log daté, une entrée par session de travail significative.
Format : `## [AAAA-MM-JJ] Phase X — <titre>` puis Fait / Décisions techniques / Écarts / Bloquants / Prochaine étape.

---

## [2026-09-08] Phase 0 — Cadrage (documentation initiale)

**Fait**
- Exploration du dépôt existant : `backend/` = squelette Spring Boot 4.1.1 généré (Maven, Java 25, package `com.educa.backend`, seule dépendance `spring-boot-starter-security`) ; `frontend/` vide ; dépôt non initialisé sous git ; dossier `.github/modernize/java-upgrade` (outillage d'assistant, sans lien avec l'app).
- Rédaction de `docs/01-analyse.md` : contexte, 3 personas, 20 user stories, 39 exigences fonctionnelles priorisées (M/S/C), 10 exigences non fonctionnelles, contraintes, 8 risques + mitigation.
- Rédaction de `docs/02-conception.md` : architecture (Mermaid), MCD (Mermaid `erDiagram` + cardinalités), schéma PostgreSQL détaillé (tables MVP + traductions + chat), spécification API REST du périmètre MVP, stratégie de sécurité + matrice RBAC, stratégie i18n (interface vs contenu, RTL), stratégie IA (interface `AiAssistant`, prompt, coûts, repli), tableau des choix techniques justifiés, wireframes texte des 5 écrans principaux, 6 points ouverts.
- Création de `docs/03-plan-implementation.md` (checklist des Phases 0→6), `docs/04-journal-avancement.md` (ce fichier), `CLAUDE.md`, `README.md`.
- Dépôt GitHub distant créé par l'utilisatrice : `https://github.com/Mariam955-creat/Educa.git`.

**Décisions techniques**
- **Stack backend = Java 25 + Spring Boot 4.1.1 (Maven)** au lieu de Node.js/Express/Prisma prévus dans le brief initial — le squelette et le contexte académique l'imposent.
- **Frontend = Angular** au lieu de Next.js (consigne de l'utilisatrice).
- **Nom du projet = « educa »** (au lieu de « Eduora » dans le brief).
- Persistance : PostgreSQL + Spring Data JPA, migrations **Flyway** (pas de `ddl-auto` hors dev).
- Auth : **JWT** (access court + refresh haché, révocable) + **BCrypt**. Backend sans état.
- Stockage fichiers : **service S3-compatible**, MinIO en dev, URLs pré-signées.
- IA : encapsulée dans `com.educa.backend.service.ai` derrière une interface, avec implémentation de repli et interrupteur `ai.enabled`.
- i18n : `@ngx-translate/core` côté frontend (bascule à chaud + `dir=rtl`) ; traductions de contenu en base = Should have.
- Tests d'intégration backend avec **Testcontainers** (vraie PostgreSQL).
- **Architecture modulaire *package-by-feature*** : chaque entité / concept métier est un module autonome regroupant model + repository + service + controller + DTO + mapper. Pas de packages transverses par couche. Modules backend : `user`, `course`, `enrollment`, `quiz`, `certificate`, `storage`, `ai` (+ `common`, `security`). Miroir côté frontend avec les dossiers `feature/<x>/`. Détaillé dans `02-conception.md §1.1`.

**Écarts par rapport au plan**
- Le tableau « technologies » du brief est remplacé (Angular + Spring Boot). Documenté dans `CLAUDE.md` et `02-conception.md §8`.
- Aucun code écrit à ce stade, conformément à la règle « documentation-first ».

**Bloquants / points à trancher** (voir `02-conception.md §10`)
1. Rôle unique vs multi-rôles par utilisateur pour le MVP.
2. Quiz rattaché au cours seulement, ou aussi au chapitre.
3. Upload fichiers : multipart via l'API vs URL pré-signées directes vers MinIO.
4. Condition de délivrance du certificat : quiz final réussi seul, ou + progression 100 %.
5. Persistance du chat (`chat_messages`) dès le MVP ou plus tard.
6. Fournisseur LLM cible + budget.
7. **Validation humaine du MCD et de l'architecture requise avant de démarrer la Phase 1.**

**Prochaine étape**
- Relecture / validation de `docs/01-analyse.md` et `docs/02-conception.md` par l'utilisatrice.
- Puis fin de Phase 0 : `git init` + connexion du remote, `.gitignore`, `docker-compose.yml` (postgres + minio) + `.env.example`, config de formatage.
- Ensuite Phase 1 (socle technique).

---

## [2026-09-08] Phase 0 — Revue de cadrage : 6 décisions + modèle de notation

**Fait**
- Session de questions/réponses avec l'utilisatrice : les 6 points ouverts de `02-conception.md §10` sont tranchés.
- `git config --local` du dépôt posé : tous les commits au nom de `mariam Balde <mariambalde95@gmail.com>`, **sans** ligne `Co-Authored-By` ni `Claude-Session` (rendu PFE). Consigne inscrite dans `CLAUDE.md §6` + mémoire projet.
- Ajout dans `CLAUDE.md` et `02-conception.md §1.1` du principe d'**architecture modulaire *package-by-feature*** (chaque entité = un module model + repo + service + controller + DTO + mapper).
- Propagation du modèle de notation dans toute la doc : `01-analyse.md` (EF-17→25, US-F4), `02-conception.md` (MCD, schéma `courses`/`quizzes`/`certificates`, section « Règle de notation et de certification », endpoints API, wireframe résultat quiz, §10), `03-plan-implementation.md` (Phase 3 réécrite, +1.0 squelette modules), `CLAUDE.md` (objectif).

**Décisions actées**
1. **Un seul rôle actif** par utilisateur (`LEARNER` par défaut ; promotion `INSTRUCTOR` par l'admin). Table `user_roles` N–N conservée.
2. **Deux niveaux de quiz** : contrôle par chapitre (`CONTROL`, tentatives illimitées) + examen final par cours (`FINAL_EXAM`, tentatives limitées, défaut 3).
3. **Upload fichiers** : multipart via l'API. *(Précisé plus bas : écriture sur système de fichiers local en dev, S3 en cible.)*
4. **Certification — modèle de notation** :
   - Score retenu par quiz = meilleure tentative.
   - Moyenne des contrôles = moyenne des meilleurs scores par chapitre ; contrôle non tenté = 0.
   - Progression 100 % = tous les contenus vus **ET** tous les contrôles tentés → déverrouille l'examen final.
   - **Note finale pondérée = 40 % moyenne des contrôles + 60 % meilleur score d'examen final** (`courses.control_weight` / `exam_weight`).
   - Seuil de certification configurable par cours (`courses.pass_threshold`, défaut 70 %).
   - Certificat généré dès que la note finale ≥ seuil ; stocke `controls_average`, `final_exam_score`, `final_grade` ; l'`enrollment` passe à `COMPLETED`.
5. **Chatbot sans persistance** au MVP ; `chat_messages` = Should have.
6. **Fournisseur IA = API Claude / Anthropic** ; clé avec plafond de dépense ; modèle exact figé en Phase 4.

**Écarts par rapport au plan**
- Le brief initial prévoyait un unique « quiz final » par cours ; le modèle retenu est plus riche (contrôles + examen final pondérés 40/60). Documenté dans `02-conception.md §3` et `§10`.

**Bloquants / points à trancher**
- Aucun bloquant. Reste la **relecture finale humaine** du MCD et de la matrice RBAC avant de démarrer la Phase 1 (règle du brief).

**Prochaine étape**
- Validation finale MCD + RBAC par l'utilisatrice.
- Voir l'entrée suivante (revue infra) pour la fin de Phase 0.

---

## [2026-09-08] Phase 0 — Revue infra : pas de Docker, PostgreSQL local + pgAdmin

**Fait**
- Décision de l'utilisatrice : **pas de Docker pour l'instant**. En dev, PostgreSQL est **installé localement** et administré avec **pgAdmin** (installation en cours de son côté).
- `docker-compose.yml` : **reporté** (sera ajouté plus tard comme confort de dev + cible de déploiement).
- **Stockage fichiers** : on ne démarre pas sur MinIO/S3. Le module `storage` expose `StorageService` avec une impl. `FileSystemStorageService` (dossier `backend/var/storage/`, gitignoré) ; l'impl. S3-compatible + URL pré-signées sera branchée plus tard sans toucher au métier.
- **Tests backend** : base `educa_test` **PostgreSQL locale** (profil `test`, Flyway rejoué). Testcontainers repoussé à la CI (si Docker dispo).
- Propagation dans `CLAUDE.md` (stack, structure, commandes, état), `README.md` (prérequis, démarrage), `02-conception.md` (diagramme d'archi, principes, §8 choix techniques, dépendances Maven, §10 point 3, endpoints `/contents/{id}/file`), `01-analyse.md` (ENF-05, risque R4), `03-plan-implementation.md` (Phase 0 : 0.10 = install PostgreSQL + pgAdmin, 0.11 = `.env.example` ; Phase 1 : 1.1 sans s3/testcontainers, 1.2 profils dev/test locaux, suppression tâche Dockerfile ; Phase 2 : 2.4 `FileSystemStorageService`).
- Unification du package IA : `com.educa.backend.ai` (au lieu de `…​.service.ai`).
- `.gitignore` complété : `backend/var/`.

**Décisions actées**
7. **Pas de Docker** au MVP. PostgreSQL local + pgAdmin. `docker-compose.yml` reporté.
8. **Stockage fichiers dev** = système de fichiers local (`FileSystemStorageService`) derrière l'interface `StorageService` ; S3-compatible = cible ultérieure.
9. **Tests** sur base `educa_test` PostgreSQL locale (profil `test`).

**Bloquants / points à trancher**
- Aucun. Reste la relecture finale humaine du MCD + matrice RBAC.

**Prochaine étape**
- Validation finale MCD + RBAC.
- Voir l'entrée suivante.

---

## [2026-09-08] Phase 0 — Repo initialisé et poussé

**Fait**
- Bases PostgreSQL `educa` et `educa_test` créées par l'utilisatrice via pgAdmin (connexion `localhost:5432`, l'erreur initiale venait d'un hôte `postgres` hérité de la config Docker).
- Ajout `.env.example` (PostgreSQL, JWT, stockage local, clé API Claude), `.editorconfig`, `.gitattributes` (LF partout, CRLF pour `*.bat`/`*.cmd`).
- **Premier commit** `d564d67` — `chore: cadrage Phase 0 …` — auteur `mariam Balde <mariambalde95@gmail.com>`, sans trailer.
- **Push** de `main` vers `origin` (`Mariam955-creat/Educa`) : OK (dépôt distant était vide).
- Checklist Phase 0 : 0.10 → 0.13 cochées dans `03-plan-implementation.md`.

**Bloquants / points à trancher**
- Aucun. **Seul reste avant la Phase 1 : la relecture humaine du MCD (`02-conception.md §2`) et de la matrice RBAC (`§5`).**

**Prochaine étape**
- Relecture MCD + RBAC par l'utilisatrice → clôture de la Phase 0.
- Phase 1 : commencer par la tâche 1.0 (squelette *package-by-feature*), puis 1.1 (dépendances Maven), 1.2 (profils `dev`/`test`), 1.3 (migration Flyway `V1__init.sql`).

---

## [2026-09-08] Phase 0 clôturée — Phase 1 démarrée (socle backend / authentification)

**Fait**
- Relecture MCD + matrice RBAC : validée par l'utilisatrice. **Phase 0 terminée.**
- **Squelette backend *package-by-feature*** : `common/error`, `config`, `security`, `user` ; `package-info.java` pour `course`, `enrollment`, `quiz`, `certificate`, `storage`, `ai`.
- **pom.xml** : ajout `spring-boot-starter-web`, `-validation`, `-data-jpa`, `flyway-core` + `flyway-database-postgresql`, `postgresql`, `jjwt 0.12.6`. Corrigé l'artefact de test (`spring-security-test`).
- **Config** : `application.yml` + profils `dev`/`test` ; `spring.config.import: optional:file:../.env[.properties]` (lance `mvn` depuis `backend/`) ; `spring.jpa.hibernate.ddl-auto=validate` ; `EducaProperties` (`educa.jwt|cors|storage|ai`).
- **Migrations Flyway** : `V1__init.sql` (15 tables : users, roles, user_roles, refresh_tokens, courses, chapters, contents, quizzes, questions, answer_options, enrollments, progress, quiz_attempts, attempt_answers, certificates ; CHECK poids 40/60, index partiels 1 contrôle/chapitre & 1 examen/cours) ; `V2__seed_roles.sql` (LEARNER, INSTRUCTOR, ADMIN).
- **Module `user`** : entités `User`, `Role`, `RefreshToken` (Lombok) ; repos ; `AuthService` (register/login/refresh avec rotation/logout ; refresh token aléatoire, stocké haché SHA-256) ; `UserService` (`/me`) ; `AuthController` (`/api/v1/auth/**`) ; `UserMapper` (MapStruct) ; DTO en `record`.
- **Sécurité** : `SecurityConfig` (`proxyBeanMethods=false`, stateless, CORS, points d'entrée JSON 401/403), BCrypt, `JwtService` (HS256, jjwt), `JwtAuthenticationFilter` (identité depuis les claims, pas d'accès base), `AppUserDetailsService`, `CurrentUser`.
- **Erreurs** : `GlobalExceptionHandler` (`@RestControllerAdvice`) + `ApiError` + hiérarchie `ApiException`.
- **Vérifs** : `./mvnw clean compile` et `./mvnw test-compile` → OK (Lombok + MapStruct actifs, `UserMapperImpl` généré). `./mvnw test` : le contexte démarre, câblage OK, **échec attendu** sur la connexion PostgreSQL (mot de passe par défaut `postgres` incorrect — il faut le vrai mot de passe dans `.env`).

**Décisions techniques**
- **Lombok + MapStruct** (sur demande). Un 1er essai « sans annotation processor » échouait (accesseurs non générés) ; corrigé en déclarant explicitement `annotationProcessorPaths` sur le `maven-compiler-plugin` (ordre : `lombok`, `mapstruct-processor`, `lombok-mapstruct-binding`). Lombok 1.18.46 (géré par Boot), MapStruct 1.6.3. `UserMapperImpl` bien généré en `@Component`. ⚠️ activer le plugin Lombok dans l'IDE.
- **DTO en `record`** Java.
- **Jackson 3** : Spring Boot 4 fournit `tools.jackson.*` (Jackson 3) ; `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2) n'est présent qu'en transitif *runtime* de `jjwt-jackson`. `SecurityConfig` n'utilise donc plus `ObjectMapper` : la réponse d'erreur 401/403 est sérialisée à la main.
- `SecurityConfig` en `@Configuration(proxyBeanMethods = false)` : contourne un échec d'enhancement CGLIB observé au runtime.
- Refresh token : valeur opaque aléatoire (32 octets hex), stockée **hachée** (SHA-256) ; rotation à chaque `/refresh` ; révocable pour `/logout`.

**Bloquant**
- **`./mvnw test` échoue tant que le fichier `.env` (racine) ne contient pas le vrai mot de passe PostgreSQL.** À créer : `cp .env.example .env` puis renseigner `POSTGRES_PASSWORD` (et plus tard `ANTHROPIC_API_KEY`).

**Prochaine étape**
- Créer `.env` avec le mot de passe PostgreSQL → relancer `./mvnw test` (valide le contexte + les migrations).
- Tâche 1.9 : `DataInitializer` (profil `dev`) — 1 admin / 1 formateur / 1 apprenant de démo.
- Tâche 1.15 : tests d'auth (register/login/refresh, accès refusé sans rôle).
- Tâches 1.11–1.14 : init du frontend Angular (structure, login/register, intercepteur, guards, dashboards vides).

---

## [2026-09-08] Phase 1 — Backend d'authentification opérationnel et testé

**Fait**
- `.env` créé (valeurs d'exemple) ; mot de passe PostgreSQL aligné sur `change-me`.
- **Compat Spring Boot 4** — trois obstacles levés :
  1. `flyway-core` seul ne migre plus au démarrage → ajout du module `spring-boot-flyway`.
  2. `preferred_language`/`language` en `CHAR(2)` → Hibernate 7 valide `varchar` → passés en `VARCHAR(2)` dans `V1`.
  3. `AutoConfigureMockMvc` a changé de package (`org.springframework.boot.webmvc.test.autoconfigure`) et vit dans `spring-boot-starter-webmvc-test` (non transitif) → dépendance ajoutée.
- **Port 8081** : le 8080 est occupé par un service `mysqld` local (non arrêtable sans admin). `SERVER_PORT` et le défaut de `application.yml` passés à 8081 ; `.env.example` aussi.
- `flyway:clean` autorisé en dev/test (`spring.flyway.clean-disabled=false`). Base `educa` nettoyée + migrations rejouées proprement.
- **`DevDataInitializer`** (profil `dev`) : crée `admin@educa.dev` / `formateur@educa.dev` / `apprenant@educa.dev`, mot de passe `password123`.
- **`AuthControllerTest`** : 8 tests d'intégration (MockMvc + `educa_test`). `./mvnw test` → **9/9 verts**.
- Vérification manuelle (`./mvnw spring-boot:run`) : démarrage OK sur 8081, Flyway `V1`+`V2`, schéma validé, `POST /auth/register` → 201, `POST /auth/login` (mauvais mdp) → 401 JSON.
- Note : un compte réel `amina@example.com` a été créé dans `educa` lors d'un test manuel (données de dev, sans importance).

**Décisions techniques**
- Nom des tests d'intégration : suffixe `Test` (exécutés par Surefire sur `./mvnw test`), pas `IT` (qui nécessiterait Failsafe + `verify`).
- Migrations désormais **append-only** (`V3`, `V4`, …) — plus de modification de `V1`, donc plus de recréation de base nécessaire.

**Bloquant** — aucun.

**Prochaine étape**
- Frontend Angular : `ng new` dans `frontend/`, structure `core/feature/shared`, pages `login`/`register`, `AuthService` + intercepteur (Bearer + refresh), `authGuard`/`roleGuard`, dashboards vides par rôle (`/dashboard`, `/instructor`, `/admin`). → clôture de la Phase 1.
- Puis tâche 1.10 (Swagger) si une version springdoc compatible Spring Boot 4 est dispo.

---

## [2026-09-08] Phase 1 — Frontend Angular (login / register / dashboards)

**Fait**
- Vérifié : base `educa` intacte (16 tables, rôles seedés) — la manip pgAdmin de l'utilisatrice ne concernait que les *connexions serveur* pgAdmin, pas les bases. Mot de passe PostgreSQL confirmé = `change-me` (psql se connecte).
- **Angular** : CLI *latest* exige Node ≥ 24.15 (poste en 24.12) → projet généré avec **`@angular/cli@19.2.15`** (`--style=scss --ssr=false --skip-git --defaults`). Standalone components.
- `app.config.ts` : `provideHttpClient(withInterceptors([authInterceptor]))` ajouté.
- **`core/auth/`** : `AuthService` (signals `user`/`isAuthenticated`, persistance `localStorage`, `login`/`register`/`refresh`/`logout`, `hasRole`/`hasAnyRole`, `homePathForRole`) ; `authInterceptor` (ajoute `Bearer`, tente un `refresh` unique sur 401 puis rejoue, sinon `logout`) ; `authGuard` + `roleGuard(...roles)` fonctionnels.
- **`feature/auth/`** : `LoginComponent` et `RegisterComponent` (Reactive Forms, gestion 401/409/400, connexion auto après inscription).
- **`feature/{dashboard,instructor,admin}/`** : composants placeholder par rôle, chargés en lazy.
- `AppComponent` = shell : barre de navigation filtrée par rôle + bouton déconnexion + `<router-outlet>`. `styles.scss` : thème clair minimal (variables CSS, police incluant Noto Sans Arabic en fallback).
- `app.routes.ts` : `/login`, `/register`, `/dashboard` (`authGuard`), `/instructor` (`roleGuard INSTRUCTOR/ADMIN`), `/admin` (`roleGuard ADMIN`), lazy `loadComponent`.
- `index.html` : `<title>educa</title>`.
- **`npm run build` → succès** (bundles main + chunks lazy login/register/dashboards).

**Décisions techniques**
- Angular **19.2** (pas 20+) tant que Node reste en 24.12 sur le poste.
- Jetons stockés en `localStorage` (backend sans état, pas de cookie) ; refresh géré dans l'intercepteur.
- Frontend cible l'API sur `http://localhost:8081/api/v1` (`core/api.ts`).

**Bloquant** — aucun. Il reste la tâche **1.10 (Swagger)** pour clôturer complètement la Phase 1 : à faire quand une version `springdoc-openapi` compatible Spring Boot 4 / Spring 7 est disponible (sinon reporter en Phase 5).

**Prochaine étape**
- Lancer les deux serveurs et valider le parcours visuel (inscription → dashboard).
- Décision sur 1.10 (Swagger maintenant ou reporté).
- Démarrer la **Phase 2** (gestion des formations : module `course`, `enrollment`, stockage local des fichiers).

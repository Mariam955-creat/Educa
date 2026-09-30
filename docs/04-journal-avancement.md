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

---

## [2026-09-08] Phase 2 — Backend : cours, chapitres, contenus, inscription, progression, stockage

**Fait**
- **Module `course`** : entités `Course` / `Chapter` / `Content` (Lombok ; agrégat, relations bidirectionnelles maintenues en mémoire ; `instructorId` en `Long`, pas de relation JPA inter-module) ; repos ; `CourseMapper` (MapStruct) ; DTO en `record` (`CourseSummaryDto`, `CourseDetailDto`, `ChapterDto`, `ContentDto`, requêtes).
- `CourseService` : `create` (slug auto-unique via `common.Slugs`), `update`, `delete`, `setPublished`, `catalog` (recherche paginée `q`/`language`), `listByInstructor`, `getDetailBySlug` (contenus masqués si non inscrit/non propriétaire), helpers `requireOwned` / `requireCourse` / `contentCount` / `isOwnerOrAdmin`. `ChapterService`, `ContentService` (upload/suppression de fichier, contrôle de position).
- `CourseController` (`/api/v1/courses`, `/api/v1/instructor/courses`), `ChapterController`, `ContentController` (dont `POST|GET /contents/{id}/file`). RBAC : `@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")` + contrôle objet « est-ce mon cours ? » dans le service.
- **Module `storage`** : `StorageService` + `FileSystemStorageService` (racine `${STORAGE_LOCAL_PATH}`, nom régénéré en UUID, garde anti-path-traversal, `delete` idempotent).
- **Module `enrollment`** : entités `Enrollment` / `Progress` (ids `Long`) ; repos ; `EnrollmentService` (`enroll` sur cours publié + unicité 409, `myEnrollments` avec %, `completeContent`, `courseProgress`, `isEnrolled`) ; `EnrollmentController` (`/courses/{id}/enroll`, `/enrollments/me`, `/contents/{id}/complete`, `/courses/{id}/progress`).
- `CurrentUser` étendu : `hasRole`, `isAdmin`, `optionalId` (null si anonyme).
- `SecurityConfig` : `GET /api/v1/courses/**` public (les sous-ressources sensibles sont gardées par `CurrentUser.id()` / `@PreAuthorize`).
- `DevDataInitializer` : + cours de démo publié « Introduction à Python » (2 chapitres, 4 contenus TEXT) appartenant à `formateur@educa.dev`.
- **`CourseFlowTest`** : 5 tests d'intégration → `./mvnw test` = **14 verts**.
- **Vérif manuelle** (`spring-boot:run` sur 8081) : catalogue OK, création cours/chapitre/contenus, apprenant → 403 sur création, upload d'un fichier sur un contenu DOCUMENT, inscription, `complete` → 50 %, download du fichier par l'apprenant inscrit, `GET /enrollments/me` cohérent.

**Décisions / corrections techniques**
- Couplage inter-module par **id** (`instructorId`, `userId`, `courseId` en `Long`), pas de `@ManyToOne` vers l'entité d'un autre module — conforme à la règle *package-by-feature* (« dépendre du service, pas de l'entité/repo »).
- Pas de cycle de dépendances : `enrollment` → `course` (jamais l'inverse) ; la visibilité « inscrit ? » est calculée dans le **contrôleur** (`CourseController` injecte `EnrollmentService`).
- `CourseRepository.searchPublished` : `cast(:q as string)` obligatoire — sinon PostgreSQL lève `function lower(bytea) does not exist` quand le paramètre est `null`.
- `isOwnerOrAdmin(Course)` utilise `CurrentUser.optionalId()` (et non `id()`) pour ne pas lever 401 sur une requête anonyme (catalogue public).
- `ChapterService.create` / `ContentService.create` maintiennent la collection parente en mémoire (`course.addChapter` / `chapter.addContent`) pour éviter une lecture périmée dans une même transaction.
- Aucune nouvelle migration : le schéma `enrollments` / `progress` / `courses` … était déjà dans `V1`.

**Bloquant** — aucun.

**Prochaine étape**
- Frontend Phase 2 : espace formateur (CRUD cours/chapitres/contenus + upload), catalogue + page cours (inscription, « marquer terminé »), dashboard apprenant branché sur `GET /enrollments/me`.

---

## [2026-09-08] Phase 2 — Frontend Angular (catalogue, page cours, éditeur formateur, progression)

**Fait**
- `core/courses/` : `course.models.ts` + `CourseApiService` (catalogue paginé, détail, CRUD cours/chapitres/contenus, `uploadFile` FormData, `downloadFile` en blob).
- `core/enrollments/` : `EnrollmentApiService` (`enroll`, `myEnrollments`, `completeContent`, `courseProgress`).
- `feature/catalog` : liste des cours publiés + champ de recherche (`debounceTime`/`distinctUntilChanged`).
- `feature/course/course-detail` (`/courses/:slug`) : bouton « S'inscrire » si non inscrit ; sinon barre de progression + chapitres/contenus. Contenus masqués tant que non inscrit. Lecture des `TEXT`, ouverture des fichiers via `URL.createObjectURL` (le `GET` blob passe par l'intercepteur → jeton Bearer). Bouton « Marquer comme terminé » par contenu, désactivé si déjà fait ; met à jour `progressPercent` + `completedContentIds`.
- `feature/instructor/instructor-dashboard` : tableau « mes cours » (chapitres, statut, Éditer / Publier-Dépublier / Supprimer).
- `feature/instructor/course-editor` (`/instructor/courses/new` et `/instructor/courses/:slug/edit`) : formulaire cours (create → redirige vers l'édition), ajout de chapitres (titre + position), ajout de contenus (select chapitre, type TEXT/VIDEO/DOCUMENT, position, `textBody` si TEXT sinon `<input type=file>` → `addContent` puis `uploadFile`), suppression chapitre/contenu.
- `feature/dashboard/learner-dashboard` : branché sur `GET /enrollments/me`, cartes cliquables avec barre de % (`completedContents / totalContents`), lien vers le catalogue.
- `app.routes.ts` : `/catalog`, `/courses/:slug`, `/instructor/courses/new`, `/instructor/courses/:slug/edit` (guards `authGuard` / `roleGuard`). Lien « Catalogue » dans la barre de navigation.

**Corrections**
- Template `course-detail` : `@else if (course(); as c)` refusé par Angular (`as` seulement sur le `@if` primaire, NG5002) → restructuré avec `@if (course(); as c) { … } @else if (loading()) { … } @else { … }`.

**Vérification** : `npm run build` → succès (chunks lazy pour catalog / course-detail / course-editor / dashboards).
Reste à valider visuellement dans le navigateur (les endpoints backend correspondants sont déjà testés).

**Bloquant** — aucun.

**Prochaine étape**
- Validation visuelle Phase 2 (catalogue → inscription → progression ; côté formateur : créer un cours complet).
- **Phase 3** : évaluation & certification (module `quiz` : contrôles de chapitre + examen final, note pondérée 40/60, génération du certificat PDF).

---

## [2026-09-08] Phase 3 — Évaluation & certification (backend + frontend)

**Backend — module `quiz`**
- Entités `Quiz` (`CONTROL`/`FINAL_EXAM`), `Question`, `AnswerOption`, `QuizAttempt`, `AttemptAnswer` (`selected_option_ids` en `bigint[]` via `@JdbcTypeCode(ARRAY)` — schéma `V1` inchangé, validé par Hibernate).
- `QuizService` : CRUD (contrôle par chapitre, examen final par cours, questions + options) ; validation ≥2 options / ≥1 correcte / `TRUE_FALSE`=2 / `SINGLE_CHOICE`=1 correcte. `getViewForCurrentUser` (tout en une transaction — un `GET` hors tx levait `LazyInitializationException` en prod ; le test `@Transactional` le masquait).
- `GradingService` : correction auto ; question juste = l'ensemble des options cochées == l'ensemble des bonnes ; score = points obtenus / total.
- `QuizUnlockService` (service dédié pour casser le cycle `QuizService` ⇄ `QuizAttemptService`) : examen final débloqué à 100 % de progression + tous les contrôles tentés.
- `QuizAttemptService` : `submit` (inscription + verrouillage + `max_attempts` + note pondérée `40/60` depuis `courses.control_weight`/`exam_weight` ; déclenche le certificat), `courseGrade`, `courseResults`.
- Contrôleurs : `QuizController` (+ `GET /courses/{id}/quizzes`), `QuizAttemptController` (`/attempts`, `/attempts/me`, `/courses/{id}/grade`), `InstructorResultsController`.

**Backend — module `certificate`**
- `Certificate` + `CertificateService` : `issueIfAbsent` (n° `EDUCA-AAAA-000001` calculé **avant** l'insert — un `save` puis `setSerialNumber` violait `NOT NULL` ; corrigé), code de vérification aléatoire, **PDF via openhtmltopdf** (`openhtmltopdf-pdfbox` 1.0.10) rendu depuis un gabarit HTML/CSS et stocké via `StorageService` (`store(byte[], folder, ext)` ajouté).
- `CertificateController` : `/certificates/me`, `/{id}/download` (propriétaire ou ADMIN), `/verify/{code}` (public).
- `enrollment` : `contentsFullyCompleted`, `progressPercent`, `markCompleted`, `enrolledUserIds`.
- `GlobalExceptionHandler` : log des 500.
- `DevDataInitializer` : + un contrôle sur le chapitre 1 et un examen final sur le cours de démo.

**Frontend**
- `core/quiz` (`QuizApiService` + modèles), `core/certificates` (`CertificateApiService`).
- `feature/quiz/quiz-take` : passage d'un quiz (radio / checkbox selon `SINGLE`/`MULTIPLE`/`TRUE_FALSE`), écran résultat (score ; pour l'examen : moyenne contrôles + note finale + lien certificat).
- `feature/instructor/quiz-editor` : ajout de questions (énoncé, type, points, options avec cases « correcte »).
- `feature/certificate/my-certificates` (liste + téléchargement PDF) ; `feature/certificate/verify` (route publique `/verify/:code`, sans guard).
- `feature/instructor/course-results` : tableau moyenne contrôles / examen / note finale / certifié par apprenant.
- `course-detail` : liens vers les contrôles (+ meilleur score), panneau « Évaluation » (progression, moyenne contrôles, examen final déverrouillé ou 🔒, note finale, bouton « Télécharger le certificat »).
- `course-editor` : créer/éditer un contrôle par chapitre et l'examen final.
- Routes ajoutées + lien « Mes certificats » dans la nav.
- Templates : `@else if (x; as y)` interdit par Angular → passage à `@let`.

**Vérifications**
- `./mvnw test` → **18 tests verts** (`QuizFlowTest` : quiz non visible si non inscrit, options sans bonnes réponses côté apprenant, examen verrouillé avant 100 %, **parcours complet jusqu'au certificat + vérification par code**).
- `npm run build` (frontend) → OK.
- Live : `GET /quizzes/1` (cours de démo) → 200 après correction du lazy-loading.

**Bloquant** — aucun.

**Prochaine étape**
- Validation visuelle Phase 3 (apprenant : contrôle → examen final → certificat ; formateur : créer un quiz).
- **Phase 4** : multilingue FR/EN/AR (+ RTL) et chatbot pédagogique (API Claude, module `ai`).

---

## [2026-09-08] Phase 4 — Multilingue (FR/EN/AR + RTL) & chatbot IA

**Backend — module `ai`**
- SDK officiel **`com.anthropic:anthropic-java` 2.34** (consulté via la référence Claude API).
- `AiAssistant` (interface) ; `ClaudeAiAssistant` (appel `client.messages().create(...)`, `maxTokens=1024`, system prompt + historique + question ; **toute `RuntimeException` → réponse `degraded`**, jamais propagée) ; `DisabledAiAssistant` (repli). `AiConfig` choisit le bean : `ClaudeAiAssistant` si `educa.ai.enabled` **et** clé présente, sinon `DisabledAiAssistant`.
- `AiChatService` : contrôle d'accès (inscription active ou propriétaire/ADMIN, sinon `403`) ; contexte borné via `CourseService.aiContext(courseId, maxContextChars)` (titre + description + chapitres + contenus TEXT tronqués) ; system prompt = « réponds seulement dans le périmètre du cours, dis quand l'info manque, réponds dans la langue de la question ».
- `AiController` : `POST /api/v1/ai/chat` `{courseId, message, history?}` → `{reply, degraded}`.
- `GlobalExceptionHandler` : `HttpMessageNotReadableException` → **400** (au lieu de 500).
- Config : `AI_MODEL` (défaut `claude-sonnet-5`), `AI_ENABLED`, `ANTHROPIC_API_KEY`, `AI_TIMEOUT_MS`, `AI_MAX_CONTEXT_CHARS` (déjà dans `EducaProperties` / `.env.example`).
- `AiChatTest` (2 tests) : `403` non inscrit, `{degraded:true}` quand IA désactivée (profil `test`). `./mvnw test` → **20 verts**.

**Frontend — i18n**
- `@ngx-translate/core` **v18** + `@ngx-translate/http-loader` ; `provideTranslateService({ fallbackLang:'fr', loader: provideTranslateHttpLoader({prefix:'i18n/', suffix:'.json'}) })`.
- `public/i18n/{fr,en,ar}.json` (nav, boutons communs, pages login/register, dashboard apprenant, libellés de langue).
- `LanguageService` : `init()` (langue depuis `user.preferredLanguage` → `localStorage` → `fr`) ; `set(lang)` → `translate.use`, `html[lang]` + `html[dir]` (rtl pour `ar`), persistance `localStorage` + `PATCH /auth/me` (`AuthService.updatePreferredLanguage`).
- `AppComponent` : `lang.init()` au constructeur ; `<select>` de langue dans la barre (authentifié ou non) ; libellés de nav via `| translate`.
- `login` / `register` / `learner-dashboard` : templates passés au pipe `translate` (messages d'erreur via `TranslateService.instant`).
- `styles.scss` : overrides `[dir='rtl']` (alignement texte, nav `row-reverse`, listes `padding-right`) — le reste (flexbox/grid + `gap`) se retourne seul.

**Frontend — chatbot**
- `AiApiService.chat(courseId, message, history)` ; `CourseChatComponent` (widget « 💬 Assistant du cours » injecté dans `course-detail` quand `contentsVisible`) : fil de discussion, saisie, historique local (7 derniers échanges envoyés au backend).

**Vérifications live**
- `POST /ai/chat` (clé factice) → `200 {degraded:true, reply:"L'assistant est momentanément indisponible…"}` — repli propre.
- `POST /ai/chat` course inexistant / non inscrit → `403`.
- `npm run build` (frontend) → OK.

**Bloquant** — aucun. Pour des réponses réelles du chatbot : mettre une vraie `ANTHROPIC_API_KEY` dans `.env` (sinon repli permanent — comportement attendu et testé).

**Reste (Should/Could have, hors MVP)** : tables de traduction du contenu pédagogique (4.9), persistance de l'historique de chat (4.10), génération de quiz par IA (4.11), recommandations (4.12), certificat PDF localisé (4.13).

**Prochaine étape**
- Validation visuelle Phase 4 (bascule de langue + RTL ; widget chatbot).
- **Phase 5** : tests & durcissement (parcours critiques bout-en-bout, revue de sécurité RBAC, `/security-review`), puis **Phase 6** (doc finale + jeu de démo + soutenance).

---

## [2026-09-09] Phase 5 — Durcissement des téléversements/téléchargements + revue RBAC

**Fait**
- **Revue RBAC endpoint par endpoint** (tâche 5.3) : les mutations `course`/`chapter`/`content`/`quiz`/`question` sont toutes gardées par `@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")` **plus** un contrôle « propriétaire ou ADMIN » dans le service (`requireOwned` / `requireOwnedByCourse`). Les endpoints sans `@PreAuthorize` sont soit publics et assumés (`/auth/{register,login,refresh}`, `GET /courses/**`, `/certificates/verify/{code}`), soit gardés par `CurrentUser.id()` + contrôle objet (`enrollment`, `quiz/attempts`, `certificate/{id}/download`, `ai/chat`). Aucune faille évidente relevée.
- **Durcissement des uploads** (`ContentService.validateUpload`, tâches 5.4 / 5.7) :
  - taille bornée par `educa.storage.max-file-size-mb` (en plus du plafond servlet `multipart`) → `413 Payload Too Large` ;
  - **liste blanche de types MIME** par nature de contenu : `VIDEO` → `video/{mp4,webm,ogg,quicktime}` ; `DOCUMENT` → PDF, Office (doc/docx/ppt/pptx/xls/xlsx), `text/{plain,csv,markdown}`, images `png/jpeg/gif/webp`, `application/zip`. Type absent ou hors liste → `415 Unsupported Media Type` ;
  - nom de fichier stocké réduit à son *basename* (`StringUtils.getFilename`), type MIME stocké normalisé en minuscules.
- **Durcissement du téléchargement** (`ContentController.downloadFile`) :
  - le type MIME (fourni par le téléverseur) est parsé défensivement ; un type invalide retombe sur `application/octet-stream` ;
  - `Content-Disposition: inline` **uniquement** pour PDF, images (hors SVG), audio, vidéo ; tout le reste en `attachment` → empêche l'exécution d'un HTML/SVG piégé sur l'origine de l'API ;
  - en-tête `X-Content-Type-Options: nosniff` ajouté ; nom de fichier assaini (retrait de `"`, `\`, `/`, CR/LF) dans l'en-tête.
- **`GlobalExceptionHandler`** : ajout de `MaxUploadSizeExceededException` → `413` et `MethodArgumentTypeMismatchException` → `400` (au lieu de `500` sur un id de chemin non numérique).
- **Tests** (tâche 5.1) : nouvelle classe `UploadSecurityTest` (3 tests) — rejet d'un `text/html` (`415`), acceptation d'un PDF servi `inline` + `nosniff`, type autorisé non-inline (`application/zip`) servi en `attachment`. `./mvnw test` → **23 verts**.
- **Secrets** (tâche 5.5) : vérifié — `.env` bien git-ignoré (`git check-ignore` OK), `.env.example` à jour, aucune valeur réelle en clair dans le dépôt.

**Décisions techniques**
- Liste blanche MIME côté service (pas de sniffing de contenu réel type Apache Tika au MVP — le `Content-Type` déclaré suffit combiné au `attachment` par défaut au téléchargement).
- En-tête `X-Content-Type-Options` posé en dur (`"X-Content-Type-Options"`) : la constante `HttpHeaders.X_CONTENT_TYPE_OPTIONS` n'existe pas dans Spring 7.

**Bloquant** — aucun.

**Reste Phase 5**
- 5.2 tests frontend (login, passage de quiz).
- 5.6 exécuter `/security-review` et traiter les findings.
- 5.8 vérifier la performance des listes (pagination/index) sur le jeu de démo.
- Résiduel : `server.error.include-message: always` (acceptable en dev/PFE, à repasser à `never` pour un déploiement public).

---

## [2026-09-10] Phase 4 — Validation navigateur du multilingue

**Fait**
- Backend + frontend relancés (`:8081` / `:4200`). Fichiers `public/i18n/{fr,en,ar}.json` servis en 200.
- **Test manuel multilingue avec l'utilisatrice — tout vert** :
  - bascule Français ↔ English (libellés de nav mis à jour sans rechargement) ;
  - arabe → interface en arabe **et** passage RTL (barre de nav et alignement inversés) ;
  - langue conservée après rechargement (F5) → persistance `localStorage` OK ;
  - langue conservée après déconnexion/reconnexion → persistance serveur `preferredLanguage` (`PATCH /auth/me`) OK.
- **Chatbot** : widget affiché, saisie fonctionnelle, contrôle d'accès (`403` si non inscrit) et dégradation propre (`{degraded:true}`, jamais de 500) déjà vérifiés (curl + logs). La clé `ANTHROPIC_API_KEY` de `.env` est le placeholder → Anthropic renvoie « API key is invalid » → repli. **Réponse IA réelle : en attente d'une vraie clé de compte Anthropic** (à fournir par l'utilisatrice ; côté code, rien à changer — mise à jour `.env` + redémarrage backend suffisent).

**Bloquant** — aucun pour le périmètre MVP. La réponse IA live nécessite une clé payante côté compte utilisateur, non fournissable par l'assistant.

**Prochaine étape**
- Reprendre la **Phase 5** (5.2 tests frontend, 5.6 `/security-review`, 5.8 perf listes).

---

## [2026-09-10] Phase 5 — Tests frontend (login + passage de quiz)

**Fait**
- **`karma.conf.js`** ajouté (exécution locale navigateur + launcher `ChromeHeadlessNoSandbox`) et script **`npm run test:ci`** (`ng test --watch=false --browsers=ChromeHeadlessNoSandbox`).
- **`core/auth/auth.service.spec.ts`** (4) — login stocke access/refresh/user + passe `isAuthenticated`, `homePathForRole` selon le rôle, `logout` appelle l'API + purge le stockage + redirige `/login`. `HttpTestingController`.
- **`feature/auth/login.component.spec.ts`** (3) — formulaire invalide → pas d'appel API + champs marqués touched ; identifiants valides → `auth.login` puis `router.navigateByUrl(homePath)` ; `401` → message `auth.login.errorCredentials`, pas de navigation. `provideRouter([])` + stub `AuthService`.
- **`feature/quiz/quiz-take.component.spec.ts`** (4) — chargement du quiz depuis l'id de route ; `toggle` choix unique = une seule option, choix multiple = accumulation ; `submit` mappe la sélection en `{questionId, selectedOptionIds}` et affiche le résultat. Stubs `QuizApiService` + `ActivatedRoute`.
- **`app.component.spec.ts`** réparé : cassé depuis la Phase 4 (`LanguageService` → `TranslateService` sans provider) → ajout `provideTranslateService({})` + `provideHttpClientTesting()`.
- `npm run test:ci` → **13 verts**. `npm run build` → OK.

**Décisions techniques**
- Specs de composants : on ne teste que la logique (classe). Pour `login` le template rend un `RouterLink` inconditionnel → `provideRouter([])` (routeur réel) plutôt qu'un stub incomplet ; pour `quiz-take` les `RouterLink` sont derrière `@if`, un stub `ActivatedRoute` suffit.
- Aucune dépendance à un backend lancé : tout est mocké (`HttpTestingController` / spies).

**Reste Phase 5** : 5.6 `/security-review`, 5.8 perf des listes (pagination/index) sur le jeu de démo. Résiduel : `server.error.include-message: always` → `never` pour un déploiement public.

---

## [2026-09-10] Phase 5 — Revue perf des listes (5.8)

**Fait**
- **Pagination** : seule liste potentiellement volumineuse = le catalogue public (`GET /courses`). Paginée (`PageRequest`), `page` ≥ 0 et `size` borné à `[1, 100]` côté contrôleur. `PageResponse` renvoie `totalElements`/`totalPages`.
- **Index** : toutes les listes filtrées par clé étrangère ont un index couvrant — `idx_courses_instructor`, `idx_courses_published`, `idx_enrollments_user`, `idx_enrollments_course`, `idx_progress_enrollment`, `idx_quiz_attempts_user_quiz`, `idx_attempt_answers_attempt`, `idx_certificates_verification_code`, `idx_refresh_tokens_user`. Les autres listes (chapitres/contenus/quiz d'un cours, résultats d'un cours) sont bornées par l'agrégat cours.
- Jeu de démo (3 cours, ~4 contenus/cours) : aucune requête lente.

**Résiduel (hors MVP)** : la recherche catalogue fait `LOWER(title/description) LIKE %q%` → *seq scan*. À l'échelle, ajouter une extension `pg_trgm` + index GIN (`V?__catalog_trgm.sql`). Sans impact sur le périmètre PFE.

**Reste Phase 5** : 5.6 `/security-review` (déclenché par l'utilisatrice). Ensuite Phase 6 (doc finale, jeu de démo, soutenance).

---

## [2026-09-10] Phase 5 — Passage responsive du frontend

**Fait**
- `index.html` : balise `viewport` déjà présente. Layout déjà fluide (conteneurs `max-width` centrés, grilles `auto-fill`, `flex-wrap` sur la plupart des barres d'action). Corrections ciblées des points qui cassaient en dessous de ~760 px :
  - **Barre de navigation** (`app.component.scss`) : `@media (max-width: 760px)` → `.topbar` passe sur 2 lignes (marque + compte, puis liens de nav en pleine largeur), nom complet masqué. RTL préservé (`margin-inline-start`).
  - **Tableaux** (`styles.scss`, global) : `@media (max-width: 640px)` → `table { display:block; overflow-x:auto; white-space:nowrap }` — défilement horizontal au lieu de déborder (espace formateur, résultats de cours).
  - **Cartes certificat** (`my-certificates`) : `@media (max-width: 560px)` → passage en colonne, bouton pleine largeur.
  - **Catalogue** : grille `minmax(min(260px, 100%), 1fr)` — plus de débordement sur très petit écran.
  - **Éditeur de quiz** / **espace formateur** : `.row` et `.head` passent en `flex-wrap: wrap`.
  - `img, video { max-width: 100%; height: auto }` global.
- `npm run build` → OK. `npm run test:ci` → 13 verts (styles seuls).

**À vérifier au navigateur** : DevTools → mode appareil (Ctrl+Shift+M), largeurs 360 / 414 / 768 px, sur catalogue, page cours, espace formateur, certificats — en FR et en AR (RTL).

---

## [2026-09-10] Phase 5 — `/security-review` (5.6) & clôture

**Fait**
- **`/security-review`** exécuté sur le diff de la branche (4 commits Phase 5 vs `origin/main`). **0 finding.**
  - `ContentController` / `ContentService` : le diff est du durcissement pur (allowlist MIME, `attachment` par défaut, `nosniff`, nom de fichier assaini, `parseMediaType` défensif). Pas de nouvelle surface.
  - Résiduel identifié **LOW** (sous le seuil de report) : l'allowlist se base sur le `Content-Type` déclaré par le téléverseur, pas sur les magic bytes. Neutralisé par `X-Content-Type-Options: nosniff` + `Content-Disposition: attachment` par défaut → pas de XSS stocké exploitable. Un sniffing réel (Apache Tika) serait de la défense en profondeur, hors MVP.
  - `GlobalExceptionHandler.handleTypeMismatch` : `ex.getName()` = nom du paramètre du contrôleur (constante côté code), pas une entrée requête → pas d'injection.
- Suivi global du plan nettoyé (lignes de phases dupliquées supprimées).

**Bilan Phase 5**
- Tests : **23 backend + 13 frontend** verts.
- Revue RBAC : endpoint par endpoint, contrôle objet vérifié, aucune faille.
- Durcissement : uploads (taille + MIME), downloads (`attachment`/`nosniff`), codes HTTP (`413`/`415`/`400` au lieu de `500`).
- Perf : listes paginées + indexées.
- Frontend responsive (barre nav, tableaux, cartes, grille catalogue).
- `/security-review` : 0 finding.

**Résiduel (hors MVP, avant déploiement public)** : `server.error.include-message: always` → `never` ; Apache Tika sur les uploads ; `pg_trgm` + GIN pour la recherche catalogue ; Swagger (tâche 1.10) ; clé Anthropic réelle pour le chatbot live.

**Prochaine étape — Phase 6** : mise à jour finale `docs/01`→`04` + `README.md`, jeu de démo complet (script de seed), scénario de soutenance, diagrammes propres (archi + MCD), vérification bout-en-bout du périmètre « Must have », instructions de déploiement.

---

## [2026-09-10] Phase 6 — Démarrage : README réécrit + scénario de soutenance

**Fait**
- **`README.md` réécrit** (tâche 6.1, partiel) : il était resté figé à la Phase 0 (port 8080, « Swagger UI : /swagger-ui.html » inexistant, `npm test`, « en cours de mise en place »). Nouvelle version = état réel : stack complète (Angular 19.2, port **8081**, openhtmltopdf, SDK `anthropic-java`, RBAC), structure *package-by-feature*, table des 3 comptes de démo, commandes exactes (`./mvnw spring-boot:run`, `npm run test:ci`), section « État d'avancement » (Phases 0→5 terminées, Phase 6 en cours) + résiduel connu. Lien ajouté vers `docs/05`.
- **`docs/05-demo-soutenance.md` créé** (tâche 6.3) : déroulé pas à pas ~15 min pour la soutenance — préparation/prérequis, parcours formateur (créer + publier un cours avec contrôle + examen), parcours apprenant (inscription → progression 100 % → déverrouillage), certification (note 40/60 → certificat PDF → vérification publique), volet multilingue (FR/EN/AR + RTL + persistance), volet chatbot (contexte borné + repli `degraded`), points sécurité/qualité, conclusion, et une annexe « Plan B » si un serveur ne démarre pas.
- `03-plan-implementation.md` : Phase 6 passée à `en cours` ; 6.3 coché `fait`, 6.1 `en cours`, 6.2/6.4/6.5/6.6 annotées avec l'état de départ.

**Décisions techniques** — aucune (documentation seule ; pas de changement de code).

**Bloquant** — aucun.

**Reste Phase 6**
- 6.1 : passe finale sur `01-analyse.md` / `02-conception.md` (cohérence avec l'implémentation réelle) + `CLAUDE.md`.
- 6.2 : enrichir `DevDataInitializer` — un 2ᵉ cours complet + un apprenant ayant déjà un certificat (pour montrer « Mes certificats » et la page de vérification sans dérouler tout le parcours).
- 6.4 : exporter les diagrammes Mermaid de `02-conception.md` (archi + MCD) en PNG/SVG dans `docs/assets/`.
- 6.5 : dérouler la checklist de `docs/05` sur l'appli lancée (avec l'utilisatrice).
- 6.6 : `docker-compose` de déploiement — **cible à confirmer** avec l'utilisatrice (fournisseur cloud, avec ou sans conteneur pour PostgreSQL).

---

## [2026-09-10] Phase 6 — Jeu de données de démonstration enrichi (6.2)

**Fait**
- **`DevDataInitializer` réécrit** (profil `dev`) :
  - **4ᵉ compte** `diplome@educa.dev` / « Sara Diplômée » (`LEARNER`, `password123`) — l'apprenant déjà certifié, pour montrer « Mes certificats » et la page de vérification publique sans dérouler tout le parcours en soutenance ;
  - **2ᵉ cours publié** « Les bases de Git » (slug `les-bases-de-git`) du formateur de démo : 2 chapitres, 4 contenus TEXT, 1 contrôle (chapitre 1), 1 examen final (`max_attempts` 3). Le cours « Introduction à Python » est inchangé ;
  - **parcours complet rejoué** pour Sara sur le cours Git **via les services réels** (`EnrollmentService.enroll` + `completeContent` sur chaque contenu, `QuizAttemptService.submit` avec toutes les bonnes réponses pour le contrôle puis l'examen) → note pondérée 100/100 ≥ seuil 70 → `CertificateService.issueIfAbsent` déclenché, `enrollment` passé à `COMPLETED`.
- **Idempotence** : chaque bloc est gardé (`existsBySlug`, `existsBy…Type`, `certificateIdFor != null`). Le parcours certifié est enveloppé dans un `try/catch(RuntimeException)` qui **logue seulement** — un aléa de seed ne doit jamais bloquer le démarrage.
- **Vérifications** :
  - `./mvnw test` → **23 verts** (le profil `test` n'active pas `DevDataInitializer`, `@Profile("dev")`).
  - Boot dev réel : log `Parcours de démo joué pour diplome@educa.dev sur « Les bases de Git » — certificat #N` ; `GET /certificates/me` (Bearer Sara) renvoie le certificat (`EDUCA-2026-0000NN`, note 100) ; `GET /certificates/verify/{code}` public → `{valid:true, …}` ; `GET /certificates/{id}/download` → `200 application/pdf` (`%PDF`), `inline` + `X-Content-Type-Options: nosniff` ; le catalogue liste « Les bases de Git ».

**Décisions techniques**
- Le certificat de démo est produit par **le vrai chemin métier** (grading + déverrouillage + émission + PDF), pas inséré à la main — il reste correct si la logique évolue.
- `DevDataInitializer` dépend désormais de `EnrollmentService`, `QuizAttemptService`, `CertificateService` (beans `@Service`, pas de cycle : l'`ApplicationRunner` n'est dépendance de personne).
- Piège rencontré : après édition, un `spring-boot:run` a échoué sur `NoClassDefFoundError: AnswerOption` (artefact de compilation incrémentale corrompu dans `target/`). Résolu par `./mvnw clean compile`. Sans impact sur `./mvnw test` (qui recompile).

**Bloquant** — aucun.

**Note pour la soutenance** : la base `educa` de dev contient encore des cours résiduels de tests manuels (« Bases du Java », « Bases du Git »…). Repartir d'une base propre (`flyway:clean` dev + redémarrage) avant la démo — déjà indiqué en `docs/05-demo-soutenance.md` §0.5.

**Reste Phase 6** : 6.1 (passe finale `01`/`02`), 6.4 (export diagrammes), 6.5 (vérif bout-en-bout avec l'utilisatrice), 6.6 (`docker-compose`, cible à confirmer).

---

## [2026-09-10] Phase 6 — Diagrammes archi + MCD exportés (6.4)

**Fait**
- Nouveau dossier **`docs/assets/`** avec, pour chaque diagramme, la source Mermaid (`.mmd`) + un rendu **SVG** (vectoriel, pour le mémoire) + un **PNG** (aperçu / insertion Word) :
  - `architecture.{mmd,svg,png}` — reprise du `flowchart` de `02-conception.md §1`, nettoyée : `\n` → `<br/>`, libellés remis à jour (Angular 19, Spring Boot 4, port 8081, noms de modules réels, SDK `anthropic-java`, repli IA).
  - `mcd.{mmd,svg,png}` — `erDiagram` **réécrit avec les attributs** des 15 tables du MVP réellement migrées (Flyway `V1`/`V2`) : clés PK/FK/UK, types, énumérations, valeurs par défaut (poids 40/60, seuil 70), `selected_option_ids bigint[]`, `serial_number EDUCA-AAAA-000001`… Les entités `*_TRANSLATION` / `LANGUAGE` / `CHAT_MESSAGE` (Should have) sont **écartées** de l'export — elles restent dans le MCD de conception de `02-conception.md`.
- **`docs/assets/README.md`** : table récap + procédure de régénération.
- `02-conception.md` §1 et §2 : ajout d'un renvoi vers les fichiers exportés sous chaque bloc Mermaid.

**Outillage**
- Rendu via **`@mermaid-js/mermaid-cli@11`** (`npx`, pas d'ajout au projet) piloté sur le **Chrome du poste** (`-p puppeteer.json` avec `executablePath`), `PUPPETEER_SKIP_DOWNLOAD=1` → aucun téléchargement de Chromium.
- Pièges : (1) `executablePath` doit être en slashes `/` dans le JSON (un `\P` casse le parse JSON) ; (2) en `erDiagram`, un libellé de relation contenant une apostrophe (`s'inscrit`) doit être **entre guillemets** sinon `Parse error`.

**Bloquant** — aucun.

**Reste Phase 6** : 6.5 (vérif bout-en-bout du « Must have » avec l'utilisatrice, sur base propre), 6.6 (`docker-compose` de déploiement — cible à confirmer), 6.1 (passe finale `01-analyse.md` / `02-conception.md`).

---

## [2026-09-10] Phase 6 — Vérification bout-en-bout du « Must have » (6.5)

**Fait**
- **`scripts/e2e-mvp.mjs`** : script Node (fetch natif, aucune dépendance) qui rejoue **tout le parcours MVP contre l'API réelle** (`:8081`), de façon **non destructive** — formateur = compte de démo, apprenants créés à la volée, slug de cours unique par run.
- Couverture : auth/JWT (register/login, rotation du refresh + **ancien jeton rejeté**, logout révoque), RBAC (LEARNER → 403 sur mutations cours et sur résultats formateur), CRUD formation (cours/chapitre/contenus/contrôle/examen), validations (options invalides → 400, 2ᵉ contrôle → 409), catalogue (brouillon absent → publié présent, contenus masqués si non inscrit), inscription (unicité 409), progression 100 %, contrôle & examen (verrouillage avant 100 % → 403, déverrouillage, scores), **note pondérée 40/60 démontrée avec deux cas** (100/100 → 100 et certificat ; 0/100 → 60 et **pas** de certificat car < seuil 70), certificat (PDF `%PDF` + `nosniff`, vérification publique par code, code invalide → `valid:false`), résultats formateur, chatbot (403 non inscrit, `{degraded:true}` sans clé — jamais 500).
- **Résultat : 47 / 47 PASS.** (2 « FAIL » au 1er passage = faux négatifs du script : les `POST /attempts` renvoient `201` et non `200` — assertion corrigée, aucun défaut produit.)
- En complément, ré-exécuté ce jour : backend `./mvnw test` → **23/23**, frontend `npm run test:ci` → **13/13**, `npm run build` → OK.
- **`docs/06-verification-mvp.md`** : rapport (résultat global + table détaillée des 47 contrôles + matrice de couverture des exigences MVP + checklist UI restante + résiduel hors MVP).

**Décisions techniques**
- Le script est gardé au dépôt (`scripts/`) : il sert aussi de **plan B « démo à froid »** pour la soutenance et de smoke-test avant démo.
- Instructeur du script = `formateur@educa.dev` (l'inscription publique crée un `LEARNER`, pas un `INSTRUCTOR` — conforme à la décision de cadrage « un seul rôle actif, promotion INSTRUCTOR par l'admin »).

**Bloquant** — aucun. Reste la **relecture UI au navigateur** (checklist `docs/06` §4), à faire avec l'utilisatrice sur base propre (`flyway:clean` dev + redémarrage), en suivant `docs/05-demo-soutenance.md`.

**Reste Phase 6** : 6.6 (`docker-compose` de déploiement — cible à confirmer), 6.1 (passe finale `01-analyse.md` / `02-conception.md`).

---

## [2026-09-10] Phase 6 — Déploiement Docker (6.6)

**Fait**
- **`backend/Dockerfile`** — multi-étapes : `eclipse-temurin:25-jdk` (build Maven via `./mvnw`, cache `dependency:go-offline`) → `eclipse-temurin:25-jre`, utilisateur non-root `educa`, `EXPOSE 8081`, `SPRING_PROFILES_ACTIVE=prod`, `-XX:MaxRAMPercentage=75`. `backend/.dockerignore`.
- **`frontend/Dockerfile`** — `node:22-alpine` (`npm ci` + `npm run build`) → `nginx:1.27-alpine` servant `dist/frontend/browser`. `frontend/.dockerignore`.
- **`frontend/nginx.conf`** — fallback SPA (`try_files … /index.html`), **reverse-proxy `location /api/` → `http://backend:8081`** (en-têtes `X-Forwarded-*`, `client_max_body_size 210m` aligné sur `STORAGE_MAX_FILE_SIZE_MB`), cache long des fichiers hashés.
- **`docker-compose.yml`** (racine) — 3 services : `db` (`postgres:18-alpine` + volume `db-data` + healthcheck `pg_isready`), `backend` (build, `depends_on: db healthy`, env `POSTGRES_HOST=db` + secrets `${…:?}` requis, volume `storage-data`), `frontend` (build, seul à publier un port `${WEB_PORT:-8080}:80`). Compose lit le `.env` racine.
- **`backend/src/main/resources/application-prod.yml`** (nouveau profil `prod`) — datasource depuis `POSTGRES_*` (sans défaut), `hibernate.show_sql=false`, `flyway.clean-disabled=true`, **`server.error.include-message=never`** (ferme le résiduel Phase 5), `logging.root=INFO`.
- **Frontend : API relative.** `core/api.ts` passe de `http://localhost:8081/api/v1` en dur à **`/api/v1`** (avec surcharge runtime optionnelle `window.EDUCA_API_BASE_URL`). En dev : nouveau **`frontend/proxy.conf.json`** (`/api` → `localhost:8081`) et `npm start` = `ng serve --proxy-config proxy.conf.json`. → même origine en dev (proxy Angular) et en prod (nginx), plus de CORS déclenché côté navigateur.
- **`.env.docker.example`** + **`docs/07-deploiement.md`** (déploiement local pas à pas ; cloud : VM Docker, base managée = retirer `db` + pointer `POSTGRES_HOST`, PaaS conteneur, TLS par reverse-proxy devant `frontend`, stockage fichiers ; checklist mise en ligne).
- `README.md` (section Déploiement + table docs + structure), `CLAUDE.md` (table stack + §7) mis à jour.

**Vérifications**
- `cd frontend && npm run build` → OK ; `npm run test:ci` → **13/13** (les specs utilisent la constante `API_BASE_URL`, pas d'URL en dur → inchangées).
- `cd backend && ./mvnw test` → **23/23** (profil `test` inchangé ; `application-prod.yml` n'est pas chargé).
- `git check-ignore` : aucun des nouveaux fichiers n'est ignoré (`*.env` ne matche pas `.env.docker.example`).

**Non fait / limite**
- ⚠️ **Les images Docker n'ont pas été construites** : le poste de dev n'a pas Docker installé (contrainte projet). `docker compose build` doit être lancé sur la machine cible avant la soutenance. Points à surveiller : disponibilité des tags `eclipse-temurin:25-*`, `postgres:18-alpine`, `node:22-alpine` ; réseau pendant le build Maven (téléchargement du wrapper + dépendances).
- Le profil `prod` ne seede rien : pour une démo cloud, `BACKEND_PROFILE=dev` dans `.env` (documenté dans `docs/07` §1).

**Reste Phase 6** : 6.5 relecture UI au navigateur (checklist `docs/06` §4, avec l'utilisatrice), 6.1 passe finale `01-analyse.md` / `02-conception.md`.

---

## [2026-09-10] Phase 6 — Passe documentaire finale (6.1)

**Fait**
- **`docs/01-analyse.md`** : statut passé à « validée / MVP réalisé » ; date au 2026-09-10 ; **encadré « État de réalisation »** en tête (toutes les exigences *M* implémentées et testées → `docs/06` ; liste des *S* non réalisées : reset mot de passe, traductions de contenu, historique chat, génération de quiz IA, **module admin**). ENF-05 et ENF-09 mis à jour (Docker livré en 6.6, stockage via module `storage`).
- **`docs/02-conception.md`** : statut/date ; **nouvelle §0 « État de réalisation — écarts conception ↔ implémentation »** (tableau) :
  - IA : `LlmAiAssistant`/`ai.provider` → réel `ClaudeAiAssistant`/`DisabledAiAssistant` + vars `AI_*` ;
  - **module admin non développé** (Should have) — pas de `/admin/**` ;
  - `CHAR(2)` → **`VARCHAR(2)`** (`preferred_language`, `courses.language`) ; tables i18n de contenu & `chat_messages` absentes des migrations ;
  - API : champ `isCorrect` → `correct` ; `verify` renvoie `finalGrade` (pas `score`) ; examen verrouillé → `403` (pas `409`) ;
  - i18n : `src/assets/i18n/` → **`public/i18n/`** ;
  - §8 : PDF figé (`openhtmltopdf-pdfbox` 1.0.10), **Swagger non intégré** (SB4), **conteneurisation livrée en 6.6** ;
  - rate limiting = Should have non fait.
  Corrections inline répercutées en §3 (types de colonnes), §6.1 (chemin + version ngx-translate), §7.2 (classes + config IA réelles), §8 (3 lignes : PDF / conteneurisation / doc API + liste des dépendances réellement ajoutées), §10 point 3 (Docker).
- Le corps de `02` (MCD, schéma, API §4, sécurité, wireframes) est conservé tel quel : il décrit la cible et reste fidèle à ~95 % ; la §0 lève toute ambiguïté pour le jury.
- `03-plan-implementation.md` : 6.1 coché `fait`, Phase 6 → `quasi terminée` (reste 6.5 relecture UI).

**Bloquant** — aucun.

**Reste (Phase 6)** : **6.5** — relecture visuelle au navigateur (checklist `docs/06-verification-mvp.md` §4), à faire avec l'utilisatrice sur base propre. Hors doc : `docker compose build` à lancer sur une machine dotée de Docker.

---

## [2026-09-10] Phase 6 — Renforcement des tests du module IA

**Fait**
- **`AiChatTest`** passé de **2 à 9 tests** (module `ai` = le moins couvert). Ajouts :
  - `chat_refuse_si_non_authentifie` → **401** sans jeton ;
  - `chat_autorise_pour_le_proprietaire_du_cours_non_inscrit` → **200 `degraded`** (branche `isOwnerOrAdmin` : le formateur teste son assistant sans être « inscrit ») ;
  - `chat_autorise_pour_un_admin_non_inscrit` → **200 `degraded`** ;
  - `chat_accepte_un_historique_dans_la_requete` → payload avec `history:[{role,content}...]` désérialisé sans erreur → **200** ;
  - `chat_rejette_un_message_vide` / `..._trop_long` (2001 car.) / `..._sans_courseId` → **400** (contraintes `@NotNull` / `@Size(1..2000)` de `AiChatRequest`).
  - Helpers factorisés : `tokenWithRole(email, RoleName)`, `adminToken`, `enrolledLearnerToken`.
- `./mvnw test` → **30 verts** (auth 8, course 5, quiz 4, **ai 9**, upload-security 3, contexte 1).
- Comptes de test des références : `CLAUDE.md`, `README.md`, `docs/02` §8, `docs/03` (5.1 + 6.5), `docs/06` §1 & matrice de couverture (chatbot) — passés de 23 à **30**. Les entrées datées du journal (Phases 4/5) conservent leur compte d'époque.

**Note** — la classe réelle `ClaudeAiAssistant` (try/catch → `degraded`) reste non couverte par un test unitaire dédié (mock du SDK Anthropic fragile) ; le contrat « jamais d'exception vers l'appelant » est garanti côté frontière par `AiChatTest` + le scénario E2E.

---

## [2026-09-13] Phase 6 — Relecture UI au navigateur (6.5) : 3 bugs trouvés et corrigés

**Fait**
- **6.5 terminée** : checklist `docs/06-verification-mvp.md` §4 rejouée à deux (base dev réinitialisée par `flyway:clean`) — parcours formateur (`course-editor`/`quiz-editor` + upload réel), parcours apprenant (inscription → progression → contrôle → examen final → certificat), `my-certificates` + `/verify/:code`, bascule FR/EN/AR + RTL, responsive 360/768 px, widget chatbot. Les 5 items sont validés ; 3 régressions/lacunes réelles détectées en cours de route et corrigées immédiatement :
  1. **Certificat non délivré (`500`) pour un cours avec examen final mais sans contrôle** : `QuizAttemptService.controlsAverage()` renvoie `null` quand `controls.isEmpty()`, or `certificates.controls_average` est `NOT NULL` (`V1__init.sql`) → `ConstraintViolationException` à l'insertion, non catchée (le `try/catch` du rendu PDF ne couvre pas le `save()`). Corrigé dans `CertificateService.create()` : `controlsAverage == null ? BigDecimal.ZERO : controlsAverage`. Reproduit et vérifié par appel API direct avant/après correctif.
  2. **i18n très incomplet** : seuls `nav`/`dashboard`/`auth` avaient des clés de traduction. **8 templates n'utilisaient jamais le pipe `translate`** (`catalog`, `my-certificates`, `course-chat`, `course-detail`, `course-editor`, `instructor-dashboard`, `quiz-editor`, `quiz-take`) + 3 templates inline (`course-results`, `verify`, `admin-dashboard`) — tout le texte hors login/nav/dashboard restait figé en français quel que soit la langue choisie. Rattrapé intégralement : nouvelles clés `catalog.*`, `certificates.*`, `verify.*`, `course.*`, `chat.*`, `courseEditor.*`, `instructorDashboard.*`, `quizEditor.*`, `quizTake.*`, `courseResults.*`, `admin.*` dans `public/i18n/{fr,en,ar}.json` ; les 11 composants basculés sur `TranslatePipe`/`TranslateService.instant()` (labels, boutons, en-têtes de tableau, `confirm()`, messages d'erreur/flash). Le badge de type de contenu (`TEXT`/`VIDEO`/`DOCUMENT`) était aussi affiché brut, désormais traduit. **Précision actée** : les titres de cours/chapitres/contenus restent en français (contenu saisi par le formateur, pas texte d'interface — cf. `course_translations` = Should have non fait, `docs/02` §0).
  3. **Chatbot muet** (`CourseChatComponent`) : `<form (ngSubmit)="send()">` sans `[formGroup]`/`FormsModule` → aucune directive ne fournit la sortie `ngSubmit`, donc le clic sur « Envoyer » déclenchait une **vraie soumission HTML native** (rechargement de page, perte du fil de discussion) au lieu d'appeler `send()`. Jamais détecté avant faute de test bout-en-bout du widget. Corrigé : `(submit)="onSubmit($event)"` + `event.preventDefault()` explicite avant `send()`.
- `npm run build` OK après chaque correctif frontend ; correctif backend vérifié par appel direct à `POST /quizzes/{id}/attempts` (avant : `500` reproductible à volonté ; après : `201` + certificat émis + PDF téléchargeable `200`).

**Décisions techniques**
- Le fix certificat traite « pas de contrôle sur ce cours » comme `controls_average = 0` en base (cohérent avec `weightedFinalGrade` qui pondère déjà 100 % sur l'examen dans ce cas) plutôt que de rendre la colonne nullable — évite une migration Flyway pour un cas limite.
- i18n : paramètres d'interpolation (`{{ '...' | translate: { ... } }}`) pour toutes les valeurs dynamiques (scores, dates, noms), pas de concaténation de HTML traduit (évite tout risque XSS via `[innerHTML]`).

**Écarts par rapport au plan**
- `docs/03-plan-implementation.md` (tâche 4.1) et le README marquaient le multilingue « validé au navigateur » depuis le 2026-09-10 — en réalité seule une fraction de l'interface l'était. Statuts corrigés.

**Bloquant** — aucun.

**Reste (Phase 6)** : uniquement `docker compose build` sur une machine dotée de Docker (poste de dev toujours sans Docker). Phase 6 sinon **terminée**.

**Bloquant** — aucun. **Reste Phase 6** : 6.5 relecture UI au navigateur.

---

## [2026-09-13] Phase 6 — Tentative `docker compose build` (6.6) : bloquée par la virtualisation désactivée au BIOS

**Fait**
- Constat : **Docker Desktop 29.7.2 est maintenant installé** sur le poste de dev (n'était pas le cas lors de la rédaction de 6.6) — CLI présent, mais le daemon (moteur Linux, `dockerDesktopLinuxEngine`) ne répondait pas.
- Lancement de Docker Desktop (`shell:AppsFolder\Docker.DockerForWindows.Settings`) : après démarrage, le pipe apparaît puis le daemon répond `500` de façon persistante sur `_ping`/`version`.
- Docker Desktop affiche l'écran d'erreur explicite : **« Prise en charge de la virtualisation non détectée »**.
- Diagnostic confirmé en PowerShell : `(Get-CimInstance Win32_Processor).VirtualizationFirmwareEnabled` → **`False`**. Machine physique (ASUS Vivobook 17 X1704VA, `Win32_ComputerSystem` confirme que ce n'est pas une VM). **VT-x est désactivé dans le BIOS/UEFI** — condition nécessaire à Docker Desktop (moteur WSL2/Hyper-V), non activable en logiciel ni à distance : nécessite un redémarrage + modification du firmware.
- `docker compose --env-file .env.docker.example build` a donc échoué (`exited with code 1`, daemon injoignable), sans rapport avec le contenu des `Dockerfile`/`docker-compose.yml` eux-mêmes.

**Décisions techniques**
- Pas de contournement logiciel : la case à cocher est un vrai blocage matériel/firmware, à traiter par l'utilisatrice (redémarrage → BIOS/UEFI → activer *Intel Virtualization Technology* (VT-x) → sauvegarder/quitter). Documenté dans `docs/07-deploiement.md`.

**Écarts par rapport au plan**
- `03-plan-implementation.md` / `CLAUDE.md` disaient « poste sans Docker » : à corriger en « Docker installé, bloqué par VT-x désactivé au BIOS ».

**Bloquant** — VT-x désactivé au BIOS/UEFI du poste de dev. Nécessite une action physique de l'utilisatrice (redémarrage + BIOS) hors de portée d'un agent logiciel.

**Prochaine étape** — une fois VT-x activé et Docker Desktop opérationnel : relancer `docker compose --env-file .env.docker.example build` (ou `cp .env.docker.example .env` puis `docker compose up -d --build`) pour finir la 6.6.

---

## [2026-09-14] Phase 6 — `docker compose build`/`up` vérifiés (6.6 clôturée) : le blocage était un faux négatif, un vrai bug corrigé

**Fait**
- **Le diagnostic VT-x du 2026-09-13 était un faux négatif.** `(Get-CimInstance Win32_Processor).VirtualizationFirmwareEnabled` renvoie toujours `False`, mais `Win32_ComputerSystem.HypervisorPresent` = `True` : un hyperviseur (Hyper-V / Windows Hypervisor Platform, socle de WSL2) tourne déjà sur la machine, ce qui masque les indicateurs bruts de virtualisation vus depuis la partition racine — comportement WMI connu, sans rapport avec un VT-x réellement désactivé au BIOS. Le vrai blocage : **l'application Docker Desktop n'était simplement pas lancée** (aucun processus `Docker*`, pipe `dockerDesktopLinuxEngine` absent). Un `Start-Process "shell:AppsFolder\Docker.DockerForWindows.Settings"` suivi de l'attente du daemon (`docker info`) a suffi ; le moteur est monté sur un noyau `6.18…-microsoft-standard-WSL2`.
- **`docker compose --env-file .env.docker.example build`** → **succès** : `educa-backend` (677 Mo, JRE 25) et `educa-frontend` (74 Mo, nginx + Angular) construits sans erreur.
- **`docker compose up`** a révélé un **vrai bug** dans `docker-compose.yml` : `db` (health-check) restait `unhealthy`. Logs `educa-db-1` → l'image `postgres:18-alpine` a changé de convention de stockage en v18+ (structure façon `pg_ctlcluster`, sous-répertoire versionné) et refuse de démarrer si le volume est monté directement sur `/var/lib/postgresql/data`. **Corrigé** : volume remonté sur `/var/lib/postgresql` (racine attendue par l'image v18+, qui gère elle-même le sous-répertoire versionné).
- Après correction : stack complète démarrée (`db` healthy, `backend` connecté — Flyway + Hibernate OK, log `Started BackendApplication` —, `frontend` up) ; vérifié en conditions réelles sur un port alternatif (`WEB_PORT=8090`, le `8080` étant pris par `mysqld` local comme `8081` le serait sans le remap déjà en place) : `GET http://localhost:8090/` → `200` (SPA), `GET http://localhost:8090/api/v1/courses` → `200` (reverse-proxy nginx → `backend:8081` fonctionnel).
- Stack redescendue proprement (`docker compose down`, sans `-v`) après vérification — images conservées, volumes de test purgés lors du premier essai raté (avant le fix) puis re-créés sains lors du second.
- Mise à jour doc : `docs/07-deploiement.md` (encart ⚠️ → ✅, note sur le volume `postgres:18`), `03-plan-implementation.md` (6.6 fait et vérifié, Phase 6 → **terminée**), ce journal.

**Décisions techniques**
- Pas de changement d'image PostgreSQL (rester sur `18-alpine`, cohérent avec `application-prod.yml` et le reste de la stack) : le fix est uniquement le point de montage du volume, conforme à la documentation officielle de l'image pour la v18+.

**Écarts par rapport au plan**
- Le blocage documenté le 2026-09-13 (« VT-x désactivé au BIOS, action physique requise ») était erroné dans son diagnostic final — corrigé ici. Aucune action BIOS n'a en réalité été nécessaire.

**Bloquant** — aucun. **Le plan d'implémentation est intégralement terminé (Phases 0 → 6).**

**Prochaine étape** — aucune tâche du MVP restante. Résiduel hors MVP inchangé (cf. bilan Phase 5/6) : Swagger (1.10), `pg_trgm`/GIN catalogue, sniffing réel des uploads (Tika), clé Anthropic réelle pour des réponses IA live, module admin (Should have), traductions de contenu pédagogique.

---

## [2026-09-14] Phase 6 (résiduel) — Passe de cohérence Docker sur toute la doc + retest local complet + 1 bug UI corrigé

**Fait**
- **Cohérence documentaire Docker** : relecture ciblée de `CLAUDE.md`, `README.md`, `docs/01`, `docs/02`, `docs/03`, `docs/05`, `docs/06`, `docs/07` pour purger les mentions devenues fausses après la vérification Docker de ce même jour (« poste sans Docker », « reste `docker compose build` », résiduel `include-message` déjà résolu par le profil `prod`). Un vrai problème pratique trouvé au passage dans `docs/05-demo-soutenance.md` : le repli Docker de démo utilisait le port `8080` par défaut (déjà pris par `mysqld` local sur ce poste) et proposait `cp .env.docker.example .env`, qui aurait écrasé le `.env` local utilisé par `mvnw`/`npm` — corrigé en `WEB_PORT=8090` + `--env-file` (non destructif).
- **Retest local** : backend (`./mvnw spring-boot:run`, profil `dev`) et frontend (`npm start`) relancés sans souci — Flyway OK, seed dev rejoué, `/api/v1/courses` → `200`, SPA → `200`.
- **Parcours certifiant rejoué de bout en bout via l'API** avec `apprenant@educa.dev` sur « Introduction à Python » (cours différent de celui déjà certifié en seed, pour un test propre) : inscription (`201`) → doublon rejeté (`409`) → 4 contenus marqués vus → progression **100 %** → contrôle chapitre 1 (bonnes réponses relues côté formateur pour fiabiliser le test) → score **100** → examen final déverrouillé (`finalExamUnlocked:true`) → examen final → score **100** → note pondérée **100** (40 × 100 % + 60 × 100 %) → certificat émis automatiquement (`EDUCA-2026-000003`) → PDF téléchargeable (`200`, `application/pdf`, `nosniff`) → vérification publique par code → `valid:true` (et `valid:false` sur un code bidon).
- **1 bug réel (gap UX) trouvé pendant ce test et corrigé** : la page publique `/verify/:code` fonctionne très bien une fois l'URL connue, mais **rien dans l'interface n'y renvoyait** — sur « Mes certificats », le code de vérification n'était affiché qu'en texte brut, sans lien. Une utilisatrice testant elle-même le parcours n'a pas su comment y accéder. Corrigé : lien **« Vérifier publiquement ↗ »** ajouté sur chaque carte de `my-certificates.component.html` (`routerLink="/verify/<code>"`, nouvel onglet), nouvelle clé i18n `certificates.verifyLink` en FR/EN/AR.
- `npm run build` (frontend) → OK après le correctif.

**Décisions techniques**
- Test de contrôle/examen fiabilisé en relisant les bonnes réponses via le compte `formateur@educa.dev` (`answersVisible:true` pour le propriétaire du cours) plutôt qu'en devinant depuis l'énoncé — évite un score < 100 % par erreur de lecture.

**Bloquant** — aucun.

**Prochaine étape** — aucune tâche du MVP restante ; le plan d'implémentation (Phases 0 → 6) reste intégralement terminé. Résiduel hors MVP inchangé (voir entrée précédente).

---

## [2026-09-14] Phase 6 (résiduel) — Retest chatbot + côté formateur : 1 bug backend corrigé (405 au lieu de 500)

**Fait**
- **Chatbot IA rejoué via l'API** avec le vrai `.env` local (`AI_ENABLED=true`, clé placeholder) : sans jeton → `401` ; inscrit sur le cours → `200 {degraded:true, reply:"…"}` (jamais d'erreur malgré l'échec d'appel Anthropic) ; apprenant **non inscrit** → `403 "Inscrivez-vous au cours pour utiliser l'assistant"`. Conforme à `docs/06-verification-mvp.md` (AI-01/AI-02).
- **Parcours formateur rejoué via l'API** (`formateur@educa.dev`) sur un cours créé pour l'occasion (« Découverte du Web ») : création (`201`), ajout chapitre + contenu `TEXT` (validation de position vérifiée), RBAC apprenant → `403` sur la création de cours, cours non publié absent du catalogue, publication puis présence au catalogue, ajout d'un contrôle + question (validation `position` requise sur question/options vérifiée par un `400`), ajout de l'examen final + question, vue « Résultats » formateur (`200`, vide) et RBAC apprenant dessus (`403`).
- **1 bug réel trouvé pendant ce test** : en testant la publication avec le mauvais verbe HTTP (`PUT` au lieu de `POST` sur `/courses/{id}/publish`), l'API renvoyait **`500 "Erreur interne"`** au lieu du `405 Method Not Allowed` attendu — `GlobalExceptionHandler` n'avait pas de handler dédié pour `HttpRequestMethodNotSupportedException`, qui tombait donc dans le handler générique `Exception.class`. **Corrigé** : nouveau handler `@ExceptionHandler(HttpRequestMethodNotSupportedException.class)` → `405`, cohérent avec les autres mappings (`400`/`401`/`403`/`413`) déjà en place depuis la Phase 5.
- Backend recompilé et redémarré avec le correctif ; revérifié : `PUT /courses/{id}/publish` → `405` (message clair, plus de `500`). `./mvnw test` → **30/30 verts**, `BUILD SUCCESS` (aucune régression).

**Décisions techniques**
- Handler ajouté au même niveau que les autres exceptions techniques mappées explicitement (pas de changement de la hiérarchie des exceptions applicatives `ApiException`).

**Écarts par rapport au plan**
- Aucun test automatisé existant ne couvrait ce cas (mauvais verbe HTTP) — resté invisible jusqu'à ce test manuel. Pas de nouveau test ajouté (bug de robustesse mineur, hors périmètre des suites `*FlowTest` orientées parcours métier).

**Bloquant** — aucun.

**Prochaine étape** — aucune tâche du MVP restante. Résiduel hors MVP inchangé (voir entrées précédentes).

---

## [2026-09-15] Phase 1 (résiduel) — Swagger UI intégré (1.10 clôturée)

**Fait**
- **Vérifié qu'une version de `springdoc-openapi` compatible Spring Boot 4 existe désormais** : `springdoc-openapi-starter-webmvc-ui` 3.1.1 (les releases majeures `3.x` suivent Spring Boot 4 en lockstep depuis la 3.0.1). Ajoutée au `pom.xml`.
- **`com.educa.backend.config.OpenApiConfig`** : bean `OpenAPI` avec métadonnées (titre, description, version `v1`) et schéma de sécurité HTTP Bearer `bearer-jwt` — permet de coller un JWT dans le bouton *Authorize* de Swagger UI et d'appeler les endpoints protégés directement depuis l'interface.
- **`SecurityConfig`** : `/v3/api-docs`, `/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**` ajoutés en `permitAll` (à côté des routes déjà publiques) — nécessaire car la chaîne de sécurité est `authenticated()` par défaut.
- **`application.yml`** : chemins `springdoc.api-docs.path` / `springdoc.swagger-ui.path` explicités. **`application-prod.yml`** : `springdoc.api-docs.enabled=false` + `springdoc.swagger-ui.enabled=false` — la doc interactive n'est **jamais** exposée en production (les routes `permitAll` de `SecurityConfig` ne fuitent donc rien : sans le bean springdoc actif, elles ne sont mappées à aucun contrôleur et répondent `404`).
- **Vérifié en conditions réelles** : backend lancé en profil `dev`, `GET /swagger-ui.html` → `302` vers `/swagger-ui/index.html` → `200` ; `GET /v3/api-docs` → JSON OpenAPI 3.1 valide listant les **29 endpoints des 9 contrôleurs** (`auth`, `courses`, `chapters`, `contents`, `enrollments`, `quizzes`/`questions`, `certificates`, `ai`, `instructor`) avec le schéma `bearer-jwt` en sécurité globale. `./mvnw test` → **30/30 verts** (aucune régression).
- Doc mise à jour : `docs/03-plan-implementation.md` (1.10 fait, Phase 1 → `terminée`, résiduel Phase 5 nettoyé), `docs/02-conception.md` (§0 écarts + tableau §Doc API + liste des dépendances), `README.md` (résiduel + URL Swagger), `CLAUDE.md` (§7).

**Décisions techniques**
- Doc interactive strictement réservée au dev/test (jamais en prod), cohérent avec la posture de sécurité déjà actée en Phase 5 (pas d'exposition d'informations internes en production).
- Pas de génération de client à partir de la spec (hors périmètre MVP) ; la spec `/v3/api-docs` reste la source de vérité machine-lisible, les endpoints restent aussi décrits manuellement en `docs/02-conception.md §4`.

**Écarts par rapport au plan**
- Aucun — tâche 1.10 déjà identifiée comme résiduelle, simplement débloquée par la disponibilité d'une version springdoc compatible Spring Boot 4.

**Bloquant** — aucun.

**Prochaine étape** — aucune tâche du MVP restante ; le plan d'implémentation (Phases 0 → 6) reste intégralement terminé, résiduel hors MVP réduit à : clé Anthropic réelle pour le chatbot live, `pg_trgm`/GIN pour la recherche catalogue à l'échelle, sniffing réel des uploads (Apache Tika), module admin (Should have), traductions de contenu pédagogique (Should have).

---

## [2026-09-15] Module admin (Should have) — utilisateurs + registre des certificats

**Fait**
- **Backend, `com.educa.backend.user`** : `AdminUserController` (`/api/v1/admin/users`, `@PreAuthorize("hasRole('ADMIN')")`) — `GET ?q=` (recherche paginée nom/email insensible à la casse, `UserRepository.search`), `PATCH /{id}/roles` (`UpdateRolesRequest{roles}`, `400` si nom de rôle inconnu), `PATCH /{id}/status` (`UpdateStatusRequest{enabled}`). Nouveau DTO `AdminUserDto` (id, email, fullName, roles, enabled, createdAt) + mapping MapStruct (`UserMapper.toAdminDto`).
- **Garde-fous anti-auto-verrouillage** (pas dans la conception initiale, ajoutés en découvrant le risque en implémentant) : un ADMIN ne peut ni se désactiver lui-même, ni retirer son propre rôle ADMIN (`409 Conflict` sinon) — sans ça, un clic malheureux dans l'UI aurait pu verrouiller le seul compte admin. `UserService.updateRoles`/`updateStatus` comparent l'id ciblé à `CurrentUser.id()`.
- **Bug de sécurité corrigé au passage** : désactiver un compte (`enabled=false`) ne bloquait que le *login* (`AppUserDetailsService` — déjà correct) mais pas le **refresh token** : `AuthService.refresh()` rejouait `issueTokens()` sans revérifier `user.isEnabled()`, donc un utilisateur désactivé gardait un accès complet tant qu'il ne se déconnectait pas (refresh token valable jusqu'à 14 j par défaut). Corrigé : `refresh()` renvoie `401 "Compte désactivé"` si le compte est désactivé.
- **Backend, `com.educa.backend.certificate`** : nouveau `AdminCertificateController` (`GET /api/v1/admin/certificates`, ADMIN, paginé) — registre de tous les certificats délivrés. `CertificateRepository.findAllByOrderByIssuedAtDesc` + `CertificateService.registry()` réutilisent le `CertificateDto` existant (aucun nouveau DTO). Séparé de `CertificateController` (et non nesté dessous) car son préfixe `/api/v1/certificates` aurait produit `/api/v1/certificates/admin/certificates` au lieu de `/api/v1/admin/certificates` (erreur faite puis corrigée en cours de session, avant tout commit).
- **`GET/PATCH /admin/languages` non implémenté** (Should have du Should have — pas de table `languages` en base, cf. écarts déjà actés en Phase 4/6) : périmètre volontairement limité à utilisateurs + registre certificats pour cette session.
- **Tests** : `AdminUserControllerTest` (7 : 403 non-admin, liste + recherche, promotion de rôle, rôle inconnu → 400, désactivation d'un autre compte, 409 auto-désactivation, 409 auto-retrait du rôle admin) + `AdminCertificateControllerTest` (2 : 403 non-admin, 200 admin). `./mvnw test` → **39/39 verts** (30 précédents + 9 nouveaux), aucune régression.
- **Frontend** : `core/admin/{admin.models,admin-api.service}.ts` (`AdminUser`, `CertificateRegistryEntry`, `Page<T>` local au module). `feature/admin/admin-dashboard.component.{ts,html,scss}` entièrement réécrit (était un placeholder statique depuis la Phase 1) : deux onglets — **Utilisateurs** (recherche avec `Entrée`, tableau avec sélecteur de rôle par ligne et bouton activer/désactiver, sélecteur et bouton désactivés sur sa propre ligne, message flash sur erreur serveur lisant `err.error.message`) et **Certificats** (registre en lecture seule, chargé à la demande au premier clic sur l'onglet). i18n FR/EN/AR complètes (`admin.tabs.*`, `admin.roles.*`, `admin.users.*`, `admin.certificates.*`) ; `admin.placeholder` (devenue obsolète) retirée.
- **Vérifié** : `npm run build` OK ; bout-en-bout via l'API réelle (`curl` direct sur `:8081` **et** au travers du proxy Angular `:4200` — confirme le câblage `API_BASE_URL` relatif) : `admin@educa.dev` liste/recherche/filtre les utilisateurs et voit le registre (dont le certificat déjà émis `EDUCA-2026-000003`), `apprenant@educa.dev` reçoit `403` sur les deux routes admin. **Pas de vérification visuelle au navigateur** : l'extension Claude in Chrome a été proposée puis déclinée par l'utilisatrice pour cette session — seule la couche réseau/API a donc été validée en conditions réelles, pas le rendu ni les interactions de la page Angular elle-même.
- **Régression pré-existante découverte (non liée à ce travail, non corrigée)** : `npm run test:ci` → 9/13 verts au lieu des 13 attendus. Les 4 échecs (`QuizTakeComponent`, `NullInjectorError: No provider for TranslateService`) sont reproduits à l'identique sur `main` avant tout changement de cette session (vérifié par `git stash` + relance des tests) — cause probable : le retrofit i18n du 2026-09-13 (commit `c945410`) a ajouté `TranslatePipe`/`TranslateService` à `QuizTakeComponent` sans mettre à jour `quiz-take.component.spec.ts` (son `TestBed` ne fournit toujours pas `TranslateService`). Signalé à l'utilisatrice sans être corrigé (hors périmètre de cette session).

**Décisions techniques**
- Endpoints admin logés dans les modules métier existants (`user`, `certificate`), pas de nouveau module `admin` transverse — cohérent avec le patron déjà en place pour `/instructor/**` (logé dans `course`).
- Un seul rôle actif par utilisateur pris en compte côté UI (`<select>` avec la valeur la « plus haute » via `primaryRole()` : ADMIN > INSTRUCTOR > LEARNER) — cohérent avec la décision actée en conception (§10 : un seul rôle courant, modèle N–N conservé pour l'évolutivité) ; `PATCH .../roles` accepte toujours un tableau côté API pour rester fidèle au modèle de données.
- Pas de garde applicative empêchant un ADMIN de rétrograder un *autre* administrateur (seule l'auto-rétrogradation est bloquée) — assumé : un ADMIN qui gère d'autres ADMIN doit pouvoir le faire, le risque de verrouillage total (plus aucun admin) reste possible mais volontairement non traité ici (hors périmètre Should have).

**Écarts par rapport au plan**
- Le module admin n'a jamais eu de ligne numérotée dans `docs/03-plan-implementation.md` (seulement mentionné dans les écarts `docs/02` §0) ; pas de nouvelle numérotation ajoutée a posteriori, cette entrée de journal + la mise à jour de `docs/02` §0/§4 font foi.

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP : clé Anthropic réelle, `pg_trgm`/GIN, sniffing Tika des uploads, gestion des langues actives (`/admin/languages`), traductions de contenu pédagogique (Should have).

---

## [2026-09-15] Frontend — correctif de la régression `QuizTakeComponent` (test)

**Fait**
- Corrigé la régression signalée dans l'entrée précédente : `quiz-take.component.spec.ts` ne fournissait pas `TranslateService` à son `TestBed`, alors que `QuizTakeComponent` l'injecte (`inject(TranslateService)`) depuis le retrofit i18n du 2026-09-13 (`TranslatePipe` ajouté au template + `translate.instant(...)` dans les branches d'erreur). Les 4 tests concernés échouaient avec `NullInjectorError: No provider for TranslateService` dès `TestBed.createComponent()`, avant même tout rendu de template.
- Correctif : ajout de `provideTranslateService({})` (import `@ngx-translate/core`) aux `providers` du `TestBed`, même patron déjà utilisé par `login.component.spec.ts` et `app.component.spec.ts`. Une ligne d'import + une ligne de provider, aucun changement du composant ni des autres tests.
- Vérifié : `npm run test:ci` → **13/13 verts** (était 9/13). `./mvnw clean compile` toujours OK côté backend (non affecté, changement frontend uniquement).

**Décisions techniques**
- Aucune — fix minimal alignant le test sur le patron déjà établi ailleurs dans le projet, pas de nouvelle convention introduite.

**Écarts par rapport au plan**
- Aucun.

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP inchangé : clé Anthropic réelle, `pg_trgm`/GIN, sniffing Tika des uploads, gestion des langues actives (`/admin/languages`), traductions de contenu pédagogique (Should have).

---

## [2026-09-15] Module admin (Should have) — gestion des langues actives

**Fait**
- **Migration `V3__languages.sql`** : nouvelle table `languages` (`code VARCHAR(2) PK`, `name`, `active BOOLEAN`), seedée avec `fr`/`en`/`ar` actives. Décision : pas de colonne `rtl` (contrairement à l'ébauche de conception §3) — le sens RTL de l'*interface* reste dérivé côté frontend indépendamment de cette table, qui ne régit que les **langues de contenu des cours**.
- **Nouveau module backend `com.educa.backend.language`** : `Language` (entité), `LanguageRepository`, `LanguageService` (`listActive`, `listAll`, `isActive`, `setActive` avec garde-fou — impossible de désactiver la dernière langue active, `409`), `LanguageController` (`GET /api/v1/languages`, public, langues actives — ajouté en `permitAll` dans `SecurityConfig`), `AdminLanguageController` (`GET/PATCH /api/v1/admin/languages/{code}`, ADMIN, `404` si code inconnu).
- **Intégration réelle dans le module `course`** (sans quoi le toggle admin n'aurait aucun effet visible) : `CourseService` injecte `LanguageService` et rejette (`400`) la création ou le **changement** de langue d'un cours vers une langue inactive. Point d'attention trouvé en implémentant : valider `request.language()` à *chaque* update (y compris quand elle ne change pas) aurait cassé l'édition de tout cours existant dont la langue a été désactivée entre-temps, puisque le formulaire Angular renvoie toujours le champ langue même non modifié — corrigé en ne validant que si la langue **change réellement** (`course.getId() == null || !request.language().equals(course.getLanguage())`).
- **Tests** : `AdminLanguageControllerTest` (7 : liste publique sans authentification, 403 non-admin, liste admin complète, activation/désactivation, 409 dernière langue active, 404 code inconnu, 400 à la création d'un cours avec langue désactivée). `./mvnw test` → **46/46 verts** (39 précédents + 7 nouveaux), aucune régression.
- **Frontend** : `core/language/language-api.service.ts` (`LanguageApiService`, `GET /languages` public — distinct de `core/i18n/language.service.ts` qui gère la langue d'*interface*, aucun lien entre les deux). `core/admin/admin-api.service.ts` étendu (`languages()`, `setLanguageActive()`). `feature/admin/admin-dashboard` : troisième onglet **Langues** (tableau code/nom/statut + bouton activer/désactiver, chargé à la demande). `feature/instructor/course-editor` : le `<select>` de langue de cours n'a plus d'options `fr/en/ar` en dur — chargé dynamiquement depuis `GET /languages` au `ngOnInit` ; si la langue déjà choisie par un cours existant a depuis été désactivée, elle reste affichée mais en option désactivée (`selectableLanguages`, évite de perdre silencieusement l'affichage de la sélection).
- **Bug latent corrigé au passage** (révélé en ajoutant la validation serveur du champ langue) : `CourseEditorComponent.saveCourse()` n'avait **aucun gestionnaire d'erreur** sur son appel API — tout rejet serveur (`400` sur les poids contrôle/examen, et désormais sur une langue indisponible) échouait silencieusement, sans aucun retour visuel pour le formateur. Corrigé : `.subscribe({next, error})` avec un message flash (rouge, nouvelle classe `.flash.error`) affichant `err.error.message` ou un message générique traduit (`courseEditor.saveError`, FR/EN/AR).
- i18n complétées : `admin.tabs.languages`, `admin.languages.*` (headerCode, headerName, activate, deactivate) et `courseEditor.saveError` en FR/EN/AR.
- **Vérifié bout-en-bout via l'API réelle** (`:8081`, backend `dev`) : `GET /languages` → 3 langues actives ; `PATCH /admin/languages/ar {active:false}` (admin) → `200`, `GET /languages` n'en renvoie plus que 2 ; `POST /courses {language:"ar"}` (formateur) → **`400`** pendant que `ar` est désactivée ; réactivation → `200`, langue de nouveau disponible ; `GET /admin/languages` en tant qu'apprenant → `403` ; `GET /languages` sans jeton → `200`. `npm run build` + `npm run test:ci` (13/13) OK après les changements frontend.
- **Pas de vérification visuelle au navigateur** (extension Claude in Chrome toujours déclinée pour cette session) — seule la couche réseau/API et les tests unitaires ont été validés, pas le rendu réel de l'onglet Langues ni du sélecteur dynamique dans `course-editor`.

**Décisions techniques**
- La table `languages` ne pilote que le **contenu des cours**, jamais la langue d'interface (`preferredLanguage` de l'utilisateur, formulaire d'inscription) : ces derniers restent sur la validation statique `@Pattern(regexp="fr|en|ar")` côté backend et des `<option>` en dur côté frontend, parce qu'ils sont indissociables des fichiers de traduction `public/i18n/{fr,en,ar}.json` embarqués au build — désactiver « ar » comme langue de contenu ne doit pas priver un utilisateur arabophone de l'interface en arabe. Distinction documentée dans `docs/02-conception.md` §3.
- Pas de contrainte FK SQL entre `courses.language` et `languages.code` (reste un simple `VARCHAR(2)` comme en Phase 1) : la validation vit dans `CourseService`, cohérent avec le reste du projet (pas de logique métier en base) ; une langue désactivée n'est de toute façon jamais supprimée de la table, donc une FK n'aurait rien empêché de plus que la validation applicative déjà en place.
- Diagrammes `docs/assets/mcd.{mmd,svg,png}` **non régénérés** dans cette session (mise à jour purement documentaire de génération d'image, hors périmètre fonctionnel) — `docs/02-conception.md` et `docs/assets/README.md` signalent explicitement l'écart pour ne pas laisser une doc silencieusement fausse.

**Écarts par rapport au plan**
- Conception initiale (`docs/02-conception.md`, ébauche §3) prévoyait une colonne `languages.rtl` — non ajoutée (raison ci-dessus, déjà actée dans l'écart §0).

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP : clé Anthropic réelle, `pg_trgm`/GIN, sniffing Tika des uploads, traductions de contenu pédagogique (`course_translations`/`chapter_translations`, Should have) ; régénération optionnelle de `docs/assets/mcd.*` pour inclure `languages`.

---

## [2026-09-15] Sécurité (résiduel Phase 5) — sniffing réel du contenu des uploads (Apache Tika)

**Fait**
- **Constat du problème** : `ContentService.validateUpload` (Phase 5) validait le type de fichier contre une liste blanche, mais à partir de `file.getContentType()` — l'en-tête `Content-Type` de la partie multipart, **déclaré par le client**, jamais vérifié contre le contenu réel. N'importe qui pouvait forger cet en-tête (curl, requête brute) pour faire passer un fichier dangereux comme « `application/pdf` » ou tout autre type whitelisté.
- **Choix technique vérifié empiriquement avant implémentation** (probe jetable dans `src/test`, supprimée ensuite) : `org.apache.tika:tika-core` **seul** (sans le plus lourd `tika-parsers`) suffit pour ce projet. Vérifié avec de vrais octets de signature (« magic bytes ») : PDF, PNG, ZIP, MP4, WEBM détectés correctement par le seul contenu ; DOCX/PPTX/XLSX/DOC/XLS/PPT nécessitent en plus le nom de fichier comme indice (un ZIP quelconque renommé `.docx` est alors accepté comme faux-positif docx — limite acceptée, documentée) ; surtout, un contenu réellement dangereux (`MZ...` exécutable, `<script>` HTML) est **toujours** détecté pour ce qu'il est, même avec une extension et un `Content-Type` trompeurs — magic bytes prioritaires sur le nom de fichier en cas de conflit. C'est la propriété de sécurité qui compte réellement ici.
- **Implémentation** : `com.educa.backend.storage.FileTypeDetector` (nouveau, `@Component`, façade `Tika` réutilisée) — `detect(MultipartFile)` lit le flux et renvoie le type détecté. `ContentService.validateUpload` appelle ce détecteur au lieu de lire `file.getContentType()`, et **renvoie** le type détecté (signature changée de `void` à `String`) ; `attachFile` stocke ce type détecté dans `Content.mimeType` (au lieu du type déclaré par le client) — cohérent avec le fait que ce champ pilote ensuite le `Content-Type` de réponse et la décision « inline vs pièce jointe » au téléchargement (`ContentController.downloadFile`).
- **Tests** : 2 nouveaux dans `UploadSecurityTest` — (1) upload d'un vrai PNG faussement déclaré `.pdf`/`application/pdf` → accepté mais stocké **et servi** sous `image/png`, jamais `application/pdf` (démontre que le mensonge du client n'a plus d'effet) ; (2) upload d'un faux exécutable (`MZ...`) faussement déclaré `.pdf`/`application/pdf` → `415`, message révélant le vrai type détecté `application/x-msdownload`. Les 3 tests déjà existants passent toujours, mais désormais sur une détection réellement basée sur le contenu (vérifié qu'ils ne passaient pas « par coincidence » : le test de rejet HTML, par exemple, est confirmé rejeté parce que le nom `.html` + le contenu texte donnent bien `text/html`, absent de la liste blanche). `./mvnw test` → **48/48 verts** (46 précédents + 2 nouveaux).
- **Vérifié en conditions réelles** (backend `dev`, `curl` avec de vrais octets) : PNG réel accepté ; PDF réel accepté ; faux exécutable `MZ` déclaré `.pdf` → `415` (`application/x-msdownload` révélé) ; PNG réel mais déclaré `.pdf` → `200`, stocké comme `image/png` ; script HTML déclaré `.pdf` → `415` (`text/html` révélé).

**Décisions techniques**
- `tika-core` seul plutôt que `tika-parsers-standard-package` : ce dernier tirerait Apache POI/PDFBox (déjà présent via `openhtmltopdf`) avec un risque de conflit de versions, pour un gain de précision marginal (distinguer un docx authentique d'un zip quelconque renommé) qui ne change rien à la propriété de sécurité réellement recherchée (rejeter le contenu actif/exécutable). Cohérent avec la discipline du projet (pas de dépendance pour un scénario hypothétique).
- Pas de contrainte stricte « le type détecté doit correspondre au `Content-Type` déclaré » (rejet en cas de divergence) : le type détecté est simplement la seule source de vérité utilisée, le déclaré est ignoré — plus simple, et évite de rejeter à tort un navigateur qui déclarerait un `Content-Type` générique correct mais légèrement différent de la valeur canonique attendue.

**Écarts par rapport au plan**
- Aucun — résiduel déjà identifié depuis la Phase 5, simplement traité maintenant.

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP : clé Anthropic réelle, `pg_trgm`/GIN pour la recherche catalogue à l'échelle, traductions de contenu pédagogique (`course_translations`/`chapter_translations`, Should have) ; régénération optionnelle de `docs/assets/mcd.*` pour inclure `languages`.

---

## [2026-09-15] Performance (résiduel) — `pg_trgm` + index GIN pour la recherche catalogue à l'échelle

**Fait**
- **Constat** : `CourseRepository.searchPublished` filtre avec `lower(title) LIKE lower('%...%')` (Phase 2). Un motif à joker en tête (`%...`) ne peut jamais utiliser un index B-tree classique — chaque recherche fait un balayage séquentiel complet de `courses`, indolore aujourd'hui (5 cours de démo) mais qui dégraderait linéairement avec le catalogue.
- **Migration `V4__catalog_search_trgm.sql`** : `CREATE EXTENSION IF NOT EXISTS pg_trgm;` puis `CREATE INDEX idx_courses_title_trgm ON courses USING GIN (lower(title) gin_trgm_ops);` — un index d'expression sur `lower(title)`, choisi pour correspondre exactement au prédicat existant sans toucher au code Java/JPQL.
- **Vérifié que l'index est réellement utilisé** (pas seulement créé) : `EXPLAIN ANALYZE` en conditions synthétiques réalistes — 50 000 lignes insérées dans une transaction PostgreSQL, dont une fraction minoritaire correspond au terme recherché (`ANALYZE` puis requête), **`ROLLBACK`** ensuite (base `educa` dev revenue à son état d'origine, 5 cours, vérifié après coup). Le plan choisi est un `Bitmap Index Scan on idx_courses_title_trgm` (17 ms), pas un balayage séquentiel. Contre-vérifié qu'avec une sélectivité artificiellement à 100 % (chaque ligne matchant le terme, jeu de données non réaliste testé par erreur en premier), le planificateur choisit à raison un `Seq Scan` — comportement normal de PostgreSQL (un index n'aide que quand peu de lignes correspondent), pas un signe que l'index serait inutilisable.
- `./mvnw test` → **48/48 verts** après application de `V4` sur `educa_test` (aucune régression, aucun changement de code Java).

**Décisions techniques**
- Index sur l'**expression** `lower(title)` plutôt que sur `title` brut, pour correspondre exactement à la requête JPQL existante (`lower(c.title) like lower(...)`) sans avoir à la réécrire en `ILIKE` natif — changement strictement additif côté base, zéro risque de régression fonctionnelle.
- Recherche restée limitée au `title` (comme avant) — pas d'extension à `description`, hors périmètre de cette tâche (résiduelle Phase 2, purement performance, pas une nouvelle fonctionnalité de recherche).

**Écarts par rapport au plan**
- Aucun — résiduel déjà identifié depuis la Phase 5/6, simplement traité maintenant.

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP restant : clé Anthropic réelle pour le chatbot live, traductions de contenu pédagogique (`course_translations`/`chapter_translations`, Should have) ; régénération optionnelle de `docs/assets/mcd.*` pour inclure `languages`.

---

## [2026-09-15] Traductions de contenu des cours (Should have, tâche 4.9)

**Fait**
- **Migration `V5__content_translations.sql`** : tables `course_translations` (course_id, language_code, title, description) et `chapter_translations` (chapter_id, language_code, title), FK `ON DELETE CASCADE` vers `courses`/`chapters` et FK vers `languages(code)` (contrairement à `courses.language`, resté un simple `VARCHAR(2)` sans FK depuis la Phase 1 — écart assumé, documenté dans `docs/02` §3 : les traductions sont des enregistrements entièrement nouveaux, sans contrainte de compatibilité historique).
- **Backend, module `course`** (pas de nouveau module transverse — même patron que `/instructor/**` logé dans `course`) : entités `CourseTranslation`/`ChapterTranslation` **découplées** de `Course`/`Chapter` (`Long courseId`/`chapterId` simples, pas de relation JPA — même patron que `Course.instructorId`), pour ne pas toucher aux entités existantes ni risquer du N+1 sur les listes de cours. `CourseTranslationService` (nouveau) : `listLanguages`, `edit`, `save`, `delete`, RBAC via `CourseService.requireOwned` (propriétaire du cours ou ADMIN). `CourseTranslationController` : `GET /courses/{id}/translations` (langues déjà traduites), `GET .../translations/{lang}` (vue d'édition : titre/description traduits + titre original/traduit de chaque chapitre), `PUT .../translations/{lang}` (upsert en un seul appel : titre/description du cours + titre de chaque chapitre fourni — titre de chapitre vide = retire sa traduction, `400` si un `chapterId` n'appartient pas à ce cours, `400` si la langue est inactive via `LanguageService.isActive`), `DELETE .../translations/{lang}` (retire la traduction du cours **et** de tous ses chapitres dans cette langue).
- **Lecture** : `CourseService.catalog`/`getDetailBySlug` acceptent un paramètre `displayLanguage` optionnel ; si une traduction existe pour cette langue, titre/description de cours et titres de chapitres sont substitués, sinon repli silencieux sur l'original — **même forme de DTO**, pas de champ `translated` séparé (écart assumé vs l'ébauche de conception qui envisageait un indicateur explicite). `CourseController` expose `?displayLanguage=` sur `GET /courses` et `GET /courses/{slug}`. Les méthodes internes existantes (`summary()`, `aiContext()`, utilisées par le certificat et le chatbot) restent **inchangées**, toujours sur le texte original — un certificat ou le contexte IA ne doivent jamais dépendre de la langue d'affichage courante du visiteur.
- **Bug évité avant tout commit** (trouvé en écrivant le frontend, pas en production) : le premier jet de `CourseApiService.detail()` appliquait `displayLanguage` automatiquement à **tous** les appels, y compris depuis `course-editor` (formulaire d'édition du formateur). Un formateur ouvrant l'éditeur avec son interface dans une langue pour laquelle une traduction existe aurait vu les champs « Titre »/« Description » pré-remplis avec le texte **traduit**, et un simple clic sur Enregistrer aurait **écrasé le texte original** avec cette traduction. Corrigé avant tout commit : `detail(slug, translated = false)` — la traduction est **opt-in explicite**, seule la page de consultation publique (`course-detail.component`) passe `translated: true` ; `course-editor` et `course-results` gardent le défaut (toujours l'original).
- **Frontend** : `core/courses/course-api.service.ts` étendu (`translationLanguages`, `translation`, `saveTranslation`, `deleteTranslation`, + le fix `detail(slug, translated)` ci-dessus) ; nouveaux modèles `CourseTranslationEdit`/`ChapterTranslationItem`/`UpdateCourseTranslationRequest`. `feature/instructor/course-editor` : nouvel onglet « Traductions » — sélecteur de langue (parmi les langues actives), champs titre/description du cours + un champ par chapitre (placeholder = titre original), boutons Enregistrer/Supprimer cette langue ; erreurs serveur affichées via le mécanisme de `flash` déjà en place. `feature/course/course-detail.component` : `detail(slug, true)` — bascule transparente de la traduction si elle existe.
- **Tests** : `CourseTranslationControllerTest` (8 : 403 apprenant, 403 formateur non-propriétaire, ajout + relecture complète d'une traduction, titre de chapitre vide efface sa traduction, suppression de la langue retire cours + chapitres, langue désactivée rejetée en écriture `400`, chapitre d'un autre cours rejeté `400`, catalogue et détail affichent la traduction si `displayLanguage` demandée sinon l'original). `./mvnw test` → **56/56 verts** (48 précédents + 8 nouveaux), aucune régression. `npm run build` + `npm run test:ci` (13/13) OK.
- **Vérifié bout-en-bout** : backend réel (`:8081`) — création d'un cours FR + traduction EN (titre, description, 1 chapitre) → catalogue/détail sans `displayLanguage` renvoient l'original, avec `displayLanguage=en` renvoient la traduction ; **et** à travers le proxy Angular (`:4200`) pour confirmer le câblage `API_BASE_URL` relatif. Cours de test supprimé après vérification.

**Décisions techniques**
- Édition « une langue à la fois, un seul écran » (titre + description + tous les titres de chapitres dans un seul `PUT`) plutôt que des endpoints CRUD séparés par champ/par chapitre — plus simple à implémenter et à utiliser, correspond à la façon dont un formateur pense la tâche (« ajouter la version anglaise de ce cours »).
- Pas de contrainte empêchant de traduire un cours vers sa propre langue d'origine — cas limite sans conséquence, pas bloqué pour éviter une règle superflue.
- La langue d'affichage (`displayLanguage`) ne pilote que le **contenu des cours**, jamais la langue d'interface ni la langue de préférence utilisateur — distinction déjà actée avec le module `language` (2026-09-15, admin), reconfirmée ici.

**Écarts par rapport au plan**
- Conception initiale (§6.2) envisageait `Accept-Language` / `?lang=` + indicateur `translated: false` — implémenté à la place avec `?displayLanguage=` (nom plus explicite, cohérent avec le paramètre déjà existant `language` qui filtre par langue *d'origine*) et repli silencieux sans indicateur séparé (le DTO reste de forme identique, plus simple pour le frontend).

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP restant : clé Anthropic réelle pour le chatbot live ; régénération optionnelle de `docs/assets/mcd.*` pour inclure `languages`/`course_translations`/`chapter_translations`.

---

## [2026-09-15] Clôture de session — push vers `origin/main` + mise à jour du tableau de bord

**Fait**
- **Récapitulatif de la session** (résidus hors MVP traités un par un, chacun testé et vérifié en conditions réelles avant commit) :
  1. **Swagger UI** (tâche 1.10, résiduelle depuis la Phase 1) — `springdoc-openapi-starter-webmvc-ui` 3.1.1, désormais compatible Spring Boot 4.
  2. **Module admin** — gestion des utilisateurs (recherche, rôles, statut, garde-fous anti-auto-verrouillage, fix du refresh token qui ignorait un compte désactivé) et registre des certificats délivrés.
  3. **Régression pré-existante corrigée** — `quiz-take.component.spec.ts` (4 tests cassés depuis le retrofit i18n du 2026-09-13, `TranslateService` manquant au `TestBed`).
  4. **Module admin — langues actives** (tâche 4.9, partie 1) — table `languages`, `GET /languages` public, `GET/PATCH /admin/languages`, validation appliquée à la création/au changement de langue d'un cours.
  5. **Sécurité — sniffing réel des uploads** (résiduel Phase 5) — Apache Tika (`tika-core`) remplace la confiance aveugle dans le `Content-Type` déclaré par le client.
  6. **Performance — `pg_trgm` + GIN** (résiduel Phase 5/2.13) pour la recherche catalogue à l'échelle.
  7. **Traductions de contenu des cours** (tâche 4.9, partie 2) — `course_translations`/`chapter_translations`, édition par le formateur, lecture via `?displayLanguage=` avec repli sur l'original.
- **Documentation mise à jour au fil de l'eau** à chaque étape (cette entrée + les 6 précédentes du 2026-09-15, `docs/02-conception.md` §0/§3/§4/§6, `docs/03-plan-implementation.md`, `CLAUDE.md`, `README.md`) — voir le nouveau tableau **« Résiduel post-plan (Should have) »** ajouté dans `docs/03-plan-implementation.md` juste avant « Suivi global », qui consolide ces 7 éléments (R.1→R.7) avec renvoi vers le détail de chaque entrée de journal.
- **Poussé vers `origin/main`** : `git push` — 7 commits (`ab2e9c1`→`162e4e5`), `main` et `origin/main` synchronisés, arbre de travail propre.
- **État final vérifié** : backend `./mvnw test` → **56/56 verts** ; frontend `npm run build` OK + `npm run test:ci` → **13/13 verts** (régression du 2026-09-13 comprise) ; migrations Flyway `V1`→`V5` appliquées sans erreur.

**Décisions techniques**
- Aucune nouvelle — session de traitement de résidus, pas de nouvelle décision d'architecture.

**Écarts par rapport au plan**
- Aucun nouveau — les écarts introduits par ces résidus sont déjà documentés dans leurs entrées de journal respectives et dans `docs/02-conception.md` §0.

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP restant : clé Anthropic réelle pour le chatbot live (nécessite une vraie clé, pas une tâche de code) ; régénération optionnelle de `docs/assets/mcd.*` pour refléter `languages`/`course_translations`/`chapter_translations`.

---

## [2026-09-15] Régénération des diagrammes MCD (tâche 6.4, résiduel R.8)

**Fait**
- `docs/assets/mcd.mmd` complété avec les 3 tables ajoutées cette session (`LANGUAGE`, `COURSE_TRANSLATION`, `CHAPTER_TRANSLATION`) et leurs attributs réels (`code`/`name`/`active` ; `course_id`/`language_code`/`title`/`description` ; `chapter_id`/`language_code`/`title`), plus les 4 relations correspondantes (`COURSE ||--o{ COURSE_TRANSLATION`, `LANGUAGE ||--o{ COURSE_TRANSLATION`, `CHAPTER ||--o{ CHAPTER_TRANSLATION`, `LANGUAGE ||--o{ CHAPTER_TRANSLATION`).
- Régénéré `mcd.svg`/`mcd.png` avec la commande déjà documentée dans `docs/assets/README.md` (`@mermaid-js/mermaid-cli` + Chrome du poste) — vérifié après coup que les 3 nouvelles entités apparaissent bien dans le SVG généré (8 occurrences chacune, cohérent avec leur bloc d'attributs + leurs relations).
- Mis à jour les mentions du nombre de tables (« 15 » → **« 18 »**) et « pas encore régénéré » → « régénéré le 2026-09-15 » dans `docs/assets/README.md` et `docs/02-conception.md` §2. `docs/03-plan-implementation.md` : nouvelle ligne R.8 dans le tableau « Résiduel post-plan », résiduel restant réduit à la seule clé Anthropic.
- Fichier temporaire `puppeteer.json` (config Chrome pour `mermaid-cli`) supprimé après génération, comme documenté (non versionné).

**Décisions techniques**
- Aucune — tâche purement documentaire, suit le mode d'emploi déjà en place.

**Écarts par rapport au plan**
- Aucun.

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP restant : clé Anthropic réelle pour le chatbot live (nécessite une vraie clé, pas une tâche de code). Plus aucun autre résidu identifié.

---

## [2026-09-16] Revue de code (`/code-review medium`) sur le dernier commit

**Fait**
- `/code-review` (effort `medium`) lancé sur l'état courant du dépôt (dernier commit `88d5b93`, arbre de travail propre, synchronisé avec `origin/main`).
- **Aucun finding** : le commit ne touche que de la documentation/des diagrammes (`docs/02-conception.md`, `docs/03-plan-implementation.md`, `docs/04-journal-avancement.md`, `docs/assets/README.md`, `docs/assets/mcd.{mmd,svg,png}`) — pas de code applicatif dans le périmètre, donc rien à relever côté correction/simplification/efficacité.

**Décisions techniques**
- Aucune.

**Écarts par rapport au plan**
- Aucun.

**Bloquant** — aucun.

**Prochaine étape** — résiduel hors MVP restant : clé Anthropic réelle pour le chatbot live (nécessite une vraie clé, pas une tâche de code). Plus aucun autre résidu identifié.

---

## [2026-09-16] Extension post-MVP — mise en forme CSS + paiements (abonnement plateforme)

**Fait**
1. **Mise en forme CSS** : accent du site passé du bleu générique (`#2563eb`) à un violet (`#7e22ce`, inspiré d'Udemy — leur `#a435f0` exact évité, contraste amélioré ~7:1 vs ~4.8:1) via la variable CSS `--accent` déjà centralisée (`styles.scss`) + les 2 fallbacks hardcodés. Couleur du cadre/titre du PDF de certificat alignée par la même occasion.
2. **QR code de vérification sur le certificat** : `com.google.zxing:core` 3.5.3, généré en PNG embarqué (data URI) dans le HTML rendu en PDF par `CertificateService` — pointe vers `{publicBaseUrl}/verify/{code}`. Nouvelle propriété `educa.public-base-url` (`EducaProperties`). Vérifié visuellement (PDF régénéré pour le certificat de démo, QR lisible en bas à droite du cadre).
3. **Paiements — abonnement plateforme (nouveau périmètre, hors MVP initial)** : educa passe d'un accès gratuit à un abonnement payant mensuel/annuel donnant accès à tous les cours. Décisions actées avec l'utilisatrice en cours de session : Stripe pour l'Europe (cartes/Apple Pay/Google Pay/SEPA) ; côté Afrique, **CinetPay envisagé puis écarté** au profit d'une **intégration directe Orange Money** (l'utilisatrice voulait explicitement éviter un agrégateur, malgré le fait que CinetPay inclue déjà Orange Money/Moov comme canaux — clarifié en session). Nouveau module backend `payment` (`Subscription`/`Payment`, migration `V6__payments.sql`), interface `PaymentGateway` + `StripePaymentGateway` (SDK `stripe-java` 33.4.2, Checkout mode `subscription`, `price_data` inline, webhooks signés) + `OrangeMoneyPaymentGateway` (REST direct via `RestClient`, pas de SDK officiel, ⚠️ champs à revérifier contre la doc Orange Developer live — inaccessible depuis cette session, 403/404) + repli `DisabledPaymentGateway` (503) si clé absente, même patron que `ai`. Gating centralisé dans `EnrollmentService` (`isEnrolled`/`requireActiveEnrollment`/`enroll`), `INSTRUCTOR`/`ADMIN` exemptés, certificats déjà délivrés toujours accessibles. `SubscriptionController`, `PaymentWebhookController` (public/permitAll, vérifié par signature Stripe ou revérification serveur Orange Money — jamais par JWT), `AdminPaymentController` (registre, patron `AdminCertificateController`, 4ᵉ onglet admin). Frontend : `core/payment`, `feature/billing` (tarifs + statut + retours succès/annulation), lien nav, message « abonnement requis » sur `course-detail`.
4. **Effet de bord corrigé** : la contrainte d'abonnement cassait 3 tests existants (`AiChatTest`, `CourseFlowTest`, `QuizFlowTest` — tous enrôlent un apprenant) et aurait cassé le seed de démo (`DevDataInitializer` rejoue l'inscription du compte diplômé via les services réels). Corrigé en amont : les 3 tests seedent désormais un abonnement actif via `SubscriptionRepository` avant de créer leur token apprenant (même patron que l'attribution de rôle déjà utilisée) ; `DevDataInitializer` seed un abonnement actif pour `apprenant@educa.dev` et `diplome@educa.dev` au démarrage (profil `dev`).
5. **Vérifié** : `./mvnw test` → **64/64 verts** (56 existants + 8 nouveaux : `SubscriptionFlowTest` 6, `AdminPaymentControllerTest` 2). `npm run build` + `npm run test:ci` (13/13) OK. Vérification API réelle bout-en-bout après redémarrage du backend (migration `V6` appliquée) : `/subscriptions/me` reflète l'abonnement seedé, `POST /subscriptions/checkout` → `503` (prestataires désactivés sans clé, comportement attendu), inscription à un cours → `402` sans abonnement / `201` avec (testé avec un compte apprenant jetable, nettoyé après coup).

**Décisions techniques**
- Accès abonnement vérifié à **chaque** accès au contenu (pas seulement à l'inscription) : `isEnrolled`/`requireActiveEnrollment` re-checkent `hasActiveAccess`, cohérent avec un modèle type Netflix (perdre l'accès si l'abonnement expire), pas seulement une porte d'entrée à l'inscription.
- `educa.payment.webhook-base-url` distinct de `educa.public-base-url` : en dev les origines backend (`:8081`, joignable par les prestataires) et frontend (`:4200`, redirections navigateur) diffèrent ; en prod les deux valent la même origine publique (nginx expose `/api` dessus).
- Stripe : abonnement créé avec `price_data` inline (pas de Produits/Prix pré-provisionnés dans le dashboard) pour rester buildable sans configuration manuelle préalable côté Stripe.

**Écarts par rapport au plan**
- Le plan approuvé initialement prévoyait CinetPay ; remplacé par Orange Money direct sur demande explicite de l'utilisatrice en cours d'implémentation (avant tout code CinetPay commité autre que le fichier de migration, vite corrigé).

**Bloquant / limitation connue**
- **Aucune clé de test réelle disponible dans cette session** : le paiement réel (checkout effectif, webhooks reçus) n'a pas pu être testé de bout en bout avec de vrais prestataires — seul le repli "désactivé" (503) a été vérifié. Nécessite que l'utilisatrice fournisse une clé Stripe de test (`sk_test_...`) et des identifiants marchand Orange Developer pour aller plus loin.
- Les noms de champs exacts de l'API Orange Money Web Payment (endpoints, JSON) suivent la documentation publique connue mais n'ont pas pu être revérifiés contre la doc live (bloquée depuis cette session) — à confirmer à l'intégration réelle.

**Prochaine étape** — obtenir des clés de test (Stripe + Orange Money sandbox) pour un test bout-en-bout réel du paiement ; envisager un tunnel (ngrok ou équivalent) pour recevoir les webhooks Orange Money en dev local.

---

## [2026-09-16] Revue de code (`/code-review`) sur le module paiement — 8 findings, 7 corrigés

**Fait**
- `/code-review` (effort medium, ~8 min sur le gros diff paiement) : **8 findings**, tous confirmés à la relecture.
- **Corrigés (7)** :
  1. `SubscriptionService.hasActiveAccess`/`mySubscription` ne vérifiaient que `current_period_end` sans re-vérifier `status == ACTIVE` — un abonnement `EXPIRED` (ex. Stripe `past_due` non mappé sur `CANCELLED`) gardait l'accès tant que la période calculée n'était pas dépassée. Extrait un helper unique `isActive(Subscription)` réutilisé aux deux endroits (corrige aussi la duplication signalée séparément).
  2. `confirmStripeCheckout` : si l'appel Stripe pour lire `current_period_end` échouait (panne transitoire), l'abonnement était activé avec une période `null` → accès bloqué **définitivement** malgré un paiement réussi, sans rattrapage. Repli sur une période estimée (30/365 jours), corrigée au prochain `customer.subscription.updated`.
  3. `EnrollmentService.isEnrolled` intègre désormais la vérification d'abonnement, mais `QuizService` (×2), `QuizAttemptService.submit` et `ContentController.downloadFile` levaient un `403` « non inscrit » générique sur `!isEnrolled(...)` — un apprenant réellement inscrit mais dont l'abonnement a expiré recevait ce message trompeur au lieu du `402` avec redirection facturation. Nouvelle méthode `EnrollmentService.requireCourseAccess(userId, courseId)` (réutilise `requireActiveEnrollment`, ne renvoie rien pour ne pas exposer l'entité `Enrollment` hors du module) branchée aux 4 points d'accès concernés.
  4. `handleOrangeMoneyWebhook` ne comparait jamais le montant reçu dans la requête webhook (publique, non authentifiée) au montant du `Payment` enregistré au checkout avant de prolonger l'abonnement. Ajout d'une vérification stricte (`payment.getAmount() == amount`) avant tout appel à `isTransactionConfirmed`.
  5. `syncStripeSubscription`/`confirmStripeCheckout` : en cas d'évènement Stripe reçu avant son prédécesseur (ordre de livraison des webhooks non garanti par Stripe), l'abonnement/paiement local introuvable était simplement logué puis ignoré — la mise à jour était perdue. Les deux méthodes lèvent désormais une `ApiException(409)` : le contrôleur ne renvoie plus `200`, ce qui déclenche le mécanisme de nouvelle tentative automatique de Stripe (jusqu'à 3 jours) — pattern standard pour ce problème, pas de réconciliation maison.
  6. Doublon `hasActiveAccess`/`mySubscription` (même règle réimplémentée deux fois) — corrigé par le même helper `isActive` que le point 1.
  7. Référence obsolète à `CinetPayPaymentGateway` dans les Javadoc de `PaymentGateway.java` et `package-info.java` (renommage Orange Money non propagé) — corrigée.
- **Non corrigé, assumé** : duplication du helper de test `grantActiveSubscription` sur 4 classes de test (`AiChatTest`, `CourseFlowTest`, `QuizFlowTest`, `SubscriptionFlowTest`). Laissé tel quel : cohérent avec le patron déjà établi dans **toute** la suite de tests du projet (`register`/`login`/`instructorToken`/`adminToken` sont déjà dupliqués de la même façon dans 6+ fichiers existants, avant même cette session) — introduire une classe de base partagée juste pour ce helper créerait une incohérence stylistique plutôt que de la résoudre.
- **Vérifié après corrections** : `./mvnw test` → **64/64 toujours verts**. Backend redémarré (migration inchangée), retest API réel : `/subscriptions/me` correct, `enroll` sans abonnement → toujours `402`.

**Décisions techniques**
- Retourner `409` (au lieu de `200` silencieux) quand un webhook Stripe référence un enregistrement local pas encore visible : laisse le mécanisme de retry de Stripe gérer le désordre de livraison plutôt que de construire une file de réconciliation.
- `EnrollmentService.requireCourseAccess` ajouté plutôt que de rendre `requireActiveEnrollment` public : l'entité `Enrollment` ne doit jamais sortir du module (convention déjà en place), donc un point d'entrée `void` dédié pour les appelants externes qui n'ont besoin que de la garde d'accès.

**Bloquant** — aucun.

**Prochaine étape** — inchangée : clés de test réelles (Stripe + Orange Money) pour un test bout-en-bout du paiement effectif.

---

## [2026-09-22] Paiements — remplacement de l'abonnement plateforme par un achat par cours + factures

**Contexte** : à la reprise de session, le dépôt contenait déjà (arbre de travail non commité) un remodelage
substantiel du module `payment` : abandon de l'abonnement plateforme (mensuel/annuel) au profit d'un
**paiement par cours** — chaque formateur fixe le prix de son cours (`courses.price`, `0` = gratuit),
payé une seule fois par l'apprenant (`V7__course_pricing.sql` : `subscriptions` supprimée, `payments`
recréée sur `course_id`). `EnrollmentService.enroll` exige un paiement `SUCCEEDED` pour un cours payant
au lieu d'un abonnement actif ; `CourseCheckoutController` (`POST /courses/{id}/checkout`) remplace
`SubscriptionController` ; `StripePaymentGateway` passe en mode `payment` (achat unique) ; frontend
`feature/billing` supprimé, achat intégré directement à `course-detail` (boutons Stripe/Orange Money,
redirection avec `?payment=success|cancelled`, auto-inscription au retour). Une seconde migration,
`V8__invoices.sql`, ajoutait déjà `invoice_number`/`pdf_key` à `payments` et un DTO `InvoiceDto`, mais
sans service ni contrôleur pour les exploiter — **le build ne compilait pas** (`PaymentService.registry()`
appelait le constructeur `AdminPaymentDto` avec un argument manquant).

**Fait**
1. **Corrigé le blocage de compilation** et **terminé la fonctionnalité de facturation** laissée à moitié faite :
   - `PaymentService.nextInvoiceNumber()` — numérotation séquentielle par année (`INV-<année>-<séquence>`,
     même patron que le n° de série des certificats), assignée à la confirmation du paiement (webhook
     Stripe `checkout.session.completed` et webhook Orange Money confirmé), pas à la génération du PDF.
   - `PaymentService.downloadInvoice` — PDF (openhtmltopdf) généré **paresseusement** au premier
     téléchargement et mis en cache via `StorageService` (`invoices/{id}.pdf`), exactement le patron déjà
     utilisé par `CertificateService.download`. Contrôle d'accès : propriétaire du paiement ou `ADMIN`.
   - `PaymentService.myInvoices(userId)` — liste des factures de l'apprenant connecté (paiements `SUCCEEDED`).
   - Nouveau `PaymentController` (`GET /payments/me`, `GET /payments/{id}/invoice/download`).
   - `AdminPaymentDto` complété (`invoiceNumber`) — c'est cet ajout de champ, fait sans mettre à jour
     l'appelant, qui cassait la compilation.
2. **Frontend** : `PaymentApiService.myInvoices()`/`downloadInvoice()` ; nouvelle page `feature/payment/my-invoices.component` (patron `my-certificates.component`), route `/invoices`, lien nav « Mes achats » (FR/EN/AR) ; colonne « N° facture » ajoutée au registre admin des paiements (`admin.models.ts`, `admin-dashboard.component.html`).
3. **Test ajouté** : `CoursePaymentFlowTest.facture_generee_et_telechargeable_apres_paiement_reussi` — vérifie que `/payments/me` renvoie la facture, que le propriétaire peut la télécharger (`200`) et qu'un autre apprenant ne le peut pas (`403`).
4. **Doc** : `docs/02-conception.md §11` réécrite (achat par cours + factures, avec un encart expliquant l'abandon du modèle d'abonnement du 2026-09-16), arborescence des modules mise à jour (`payment/`, `frontend feature/payment`) ; `docs/03-plan-implementation.md` — nouvelle section « achat individuel de cours » (tableau Q.1→Q.8), ancienne section « abonnement plateforme » conservée avec un encart d'abandon (historique de session, pas réécrite).
5. **Vérifié** : `./mvnw test` → **64/64 verts** (`CoursePaymentFlowTest` 6 tests, dont la nouvelle facture ; `AdminPaymentControllerTest` 2), `npm run build` OK, `npm run test:ci` → **13/13 verts**.

**Décisions techniques**
- Numéro de facture assigné **à la confirmation du paiement**, pas à la génération du PDF (contrairement au `pdfKey`, généré paresseusement) : une facture doit avoir un numéro séquentiel stable dès l'instant du paiement, indépendamment de si/quand l'apprenant la télécharge — même raisonnement que le n° de série des certificats.
- Pas de re-vérification de l'accès à chaque consultation de cours (contrairement à l'ancien modèle d'abonnement) : un achat par cours est définitif, cohérent avec le patron marketplace (Udemy) plutôt que locatif (Netflix).

**Écarts par rapport au plan**
- Le modèle d'abonnement plateforme du 2026-09-16 est abandonné (voir `docs/02-conception.md §11`, encart). Décision déjà actée dans le code trouvé au début de cette session ; cette session a fini le sous-chantier laissé incomplet (factures) et mis la documentation à jour en conséquence.

**Bloquant** — aucun.

**Prochaine étape** — inchangée : clés de test réelles (Stripe + Orange Money sandbox) pour un test bout-en-bout du paiement effectif et des webhooks.

---

## 2026-09-26 — Test Stripe réel (mode test) + cours payants + facture lisible

**Contexte** : premier test du paiement avec de vraies clés Stripe de test. En le préparant, l'utilisatrice
a constaté que **tous les cours étaient gratuits** dans l'interface, alors qu'elle ne l'avait jamais demandé.

**Cause trouvée**
- `V7__course_pricing.sql` a ajouté `courses.price NUMERIC(10,2) NOT NULL DEFAULT 0` : tous les cours existants de la base locale sont passés à `0`.
- `DevDataInitializer` ne touche pas un cours déjà présent : le prix 29,99 € prévu pour Python n'a jamais été appliqué ; Git avait été rendu gratuit exprès (session du 2026-09-22) pour la démo.
- `course-editor` proposait `0` comme prix par défaut à la création.
- `apprenant@educa.dev` s'était inscrit à tous les cours quand ils étaient gratuits → aucun bouton d'achat visible pour lui.

**Fait**
1. **Prix** (choix de l'utilisatrice : tous payants) — base locale : Python 29,99 €, Git 19,99 €, les autres 14,99 € (JavaScript reste 19,99 €). Seed : Git 19,99 € ; le compte `diplome@educa.dev` reçoit un achat `SUCCEEDED` avec facture (`PaymentService.recordDemoPurchase`, réservé au seed) avant son inscription, sinon son parcours pré-joué échouerait en `402` sur une base neuve (même correction appliquée à la main dans la base locale).
2. **`course-editor`** : plus de prix par défaut — champ vide et obligatoire (`0` reste possible), message d'erreur FR/EN/AR (`courseEditor.priceRequired`).
3. **Test Stripe réel (mode test)** : `stripe login` (le CLI est appairé au compte en mode *live*, donc `stripe listen` est lancé avec `--api-key <sk_test_…>` pour rester en test ; le secret `whsec_…` du relais correspond à `STRIPE_WEBHOOK_SECRET`). Parcours vérifié sur un compte neuf : inscription avant paiement → `402` ; Checkout payé avec `4242…` → `checkout.session.completed` relayé → `200` → paiement `SUCCEEDED` + `INV-2026-000002` → inscription active → `/payments/me` liste la facture → PDF `200 application/pdf`.
4. **Facture PDF lisible** : date `dd/MM/yyyy à HH:mm` (Europe/Paris), montant au format français (`19,99 €`, `20 000 F CFA` ; espaces fines `U+202F` remplacées par des insécables, absentes de la police PDF par défaut), statut « Payé », moyen de paiement lisible, bloc vendeur (educa + `publicBaseUrl`). Tests `InvoiceFormatTest` (2).
5. **Bug** : une route API inconnue renvoyait `500` (`NoResourceFoundException` attrapée par le catch-all de `GlobalExceptionHandler`) → désormais `404`.
6. Doc : `CLAUDE.md`, `README.md`, `02-conception.md §11.6`, `05-demo-soutenance.md` (achat en étape 3.3 + lancement de `stripe listen` avant la démo), `03` (résiduel).

**Vérifié** : `./mvnw test` → **66/66 verts** ; `npm run build` OK ; `npm run test:ci` → **13/13 verts**.

**Non traité / à savoir**
- La facture ne porte ni adresse légale du vendeur ni mention de TVA : à ajouter si educa devient une vraie activité commerciale (mentions dépendantes du statut juridique, non inventées ici).
- ~~La page « Mes achats » affiche encore le montant au format `19.99 EUR`.~~ **Corrigé le jour même** : pipe `money` (`core/i18n/money.pipe.ts`, `Intl.NumberFormat`, suit la langue de l'interface — `19,99 €` en fr, `€19.99` en en, chiffres latins en ar) appliqué à « Mes achats », au prix de `course-detail` et au registre admin des paiements. Même correction pour les **dates** : `DatePipe` suivait `LOCALE_ID` (en-US, aucune autre locale enregistrée), d'où « September 26, 2026 » même en français → pipe `localDate` (`Intl.DateTimeFormat`, locale partagée via `core/i18n/intl-locale.ts`) dans « Mes achats », « Mes certificats », la page publique `/verify/:code` et les registres admin ; `DatePipe` n'est plus utilisé nulle part. 9 tests ajoutés (`npm run test:ci` → **22/22**).)
- Un ancien paiement `SUCCEEDED` de `apprenant@educa.dev` (antérieur aux factures) n'a pas de numéro de facture — donnée locale uniquement (V7 et V8 ont été livrées dans le même commit).

**Prochaine étape** : Orange Money — nécessite des identifiants sandbox Orange Developer.

---

## 2026-09-26 (suite) — Trois langues supplémentaires : espagnol, portugais, allemand

**Demande** : « ajoute 3 autres langues » — choix de l'utilisatrice : **es / pt / de**, pour l'**interface et le contenu des cours**.

**Fait**
- **Contenu des cours** : migration `V9__more_languages.sql` (es « Español », pt « Português », de « Deutsch », actives, `ON CONFLICT DO NOTHING`). Elles apparaissent donc automatiquement dans le sélecteur de langue de `course-editor`, l'onglet Traductions et l'admin des langues (tout lit déjà `GET /languages`).
- **`CourseRequest.language`** : `@Pattern("fr|en|ar")` remplacé par le format seul `[a-z]{2}` — la liste réelle est déjà validée par `CourseService` contre la table `languages` (langue inconnue ou inactive → `400`). Le motif en dur aurait sinon refusé toute nouvelle langue ajoutée en base.
- **Langue d'interface** : `RegisterRequest`/`UpdateMeRequest` acceptent `fr|en|ar|es|pt|de` ; frontend `AppLang` + `SUPPORTED` étendus, locales `Intl` `es-ES`/`pt-PT`/`de-DE` (dates et montants), le formulaire d'inscription liste désormais les langues depuis `LanguageService` au lieu de trois `<option>` en dur.
- **Traductions** : `public/i18n/{es,pt,de}.json` complets (234 clés, parité vérifiée par script avec `fr.json`) ; noms des langues ajoutés dans `fr/en/ar.json`. Portugais au registre européen (« palavra-passe », « ficheiro »), allemand au vouvoiement (« Sie »).
- Tests : `AdminLanguageControllerTest` adapté (6 langues ; « dernière langue active » désactive désormais en, ar, es, pt, de avant fr) + nouveau test « créer un cours en allemand → 201 ».

**Vérifié** : `./mvnw test` → **67/67** ; `npm run build` OK ; `npm run test:ci` → **22/22**.

**À savoir** : traductions rédigées sans relecture native — à faire relire avant une mise en production. Le certificat PDF et la facture restent en français, quelle que soit la langue (écart préexistant : `docs/02-conception.md §6` prévoyait des bundles `messages_{fr,en,ar}.properties` jamais implémentés). La base `educa` locale recevra `V9` au prochain démarrage du backend.

---

## 2026-09-26 (suite) — Certificat et facture traduits (6 langues, arabe RTL)

**Demande** : traduire aussi le certificat et la facture (jusqu'ici en français quelle que soit la langue — `docs/02-conception.md §6.3` le prévoyait sans que ce soit réalisé).

**Fait**
- **`com.educa.backend.common.pdf.PdfDocuments`** (nouveau, partagé par `certificate` et `payment`) : résolution de langue, libellés via `MessageSource`, dates localisées, rendu HTML → PDF. Police **DejaVu Sans** (regular + bold) embarquée dans `resources/fonts/` avec sa licence — la police par défaut d'openhtmltopdf n'a aucun glyphe arabe. Dépendance **`openhtmltopdf-rtl-support` 1.0.10** (ICU4J) : ordre bidirectionnel + mise en forme des lettres arabes (sans elle, les lettres arabes s'afficheraient isolées et dans le mauvais ordre).
- **Libellés** : `messages.properties` (fr, défaut) + `messages_{en,ar,es,pt,de}.properties` ; `spring.messages.fallback-to-system-locale: false` (sinon, sur une machine en anglais, une demande en français renverrait l'anglais, faute de `messages_fr`).
- **Langue** : `GET /certificates/{id}/download?lang=` et `GET /payments/{id}/invoice/download?lang=` ; le frontend (`CertificateApiService`, `PaymentApiService`) envoie la langue de l'interface. Absente ou non supportée → langue de préférence du titulaire → français.
- **Plus de cache PDF** : générés à chaque téléchargement (quelques ko). Un cache unique aurait figé la langue du premier téléchargement. Colonnes `pdf_key` supprimées (`V10__drop_pdf_cache.sql`) + champs retirés des entités ; le certificat n'est plus pré-rendu à l'émission. Les anciens fichiers `certificates/` et `invoices/` du stockage local sont orphelins (supprimables).
- **Arabe** : `dir="rtl"`, colonnes de la facture inversées, QR code du certificat à gauche, pas de `letter-spacing` sur le titre (casserait les liaisons). Vérifié visuellement (certificat + facture de `diplome@educa.dev`).
- **Montants** : `PaymentService.formatAmount` prend la locale (`19,99 €` fr, `€19.99` en, chiffres latins en ar).
- Tests : `QuizFlowTest` télécharge le certificat en fr (défaut), de, en, ar et une langue inconnue (→ fr) et vérifie le texte extrait du PDF (PDFBox, `support/PdfText`) ; `CoursePaymentFlowTest` idem pour la facture ; `InvoiceFormatTest` +1.

**Vérifié** : `./mvnw test` → **68/68** ; `npm run build` OK ; `npm run test:ci` → **22/22** ; backend relancé sur la base locale (V9 + V10 appliquées), PDF arabe et allemand contrôlés à l'œil.

**Incident** : l'arrêt « mémoire faible » de Claude Code avait tué le shell mais pas le processus Java du backend, resté sur :8081 — le nouveau backend échouait au démarrage (`Port 8081 was already in use`) et c'est l'ancien code qui répondait. Processus orphelin arrêté, backend relancé.

**À savoir** : traductions des PDF non relues par des locuteurs natifs (comme l'interface).

---

## 2026-09-28 — Langues réduites à quatre : FR / EN / DE / NL

**Demande** : « garde que 4 langues : fr, anglais, allemand, néerlandais » — arabe, espagnol et portugais retirés, néerlandais ajouté (interface **et** contenu des cours **et** PDF).

**Fait**
- **Base** : `V11__four_languages.sql` — ajoute `nl` (« Nederlands »), supprime les traductions de contenu en ar/es/pt, repasse en `fr` les cours et préférences utilisateur dans une langue retirée, puis supprime ar/es/pt de `languages`.
- **Backend** : `RegisterRequest`/`UpdateMeRequest` → `fr|en|de|nl` ; `PdfDocuments` : locales fr/en/de/nl (`nl-NL`), **support RTL supprimé** (plus aucune langue RTL) — `isRtl`, bidi ICU4J et la dépendance `openhtmltopdf-rtl-support` retirés, gabarits certificat/facture simplifiés (plus de `dir`/colonnes inversées). `messages_{ar,es,pt}.properties` supprimés, `messages_nl.properties` créé (mêmes clés que `messages.properties`).
- **Frontend** : `AppLang` = `fr|en|de|nl`, locale `Intl` `nl-NL`, `isRtl`/attribut `dir` et règles `[dir='rtl']` + police arabe retirés de `styles.scss` ; `public/i18n/{ar,es,pt}.json` supprimés, `nl.json` créé (parité des clés vérifiée par script avec `fr.json`).
- **Tests** : arabe remplacé par le néerlandais (`AdminLanguageControllerTest`, `CourseTranslationControllerTest`, PDF `?lang=nl` dans `QuizFlowTest`/`CoursePaymentFlowTest` ; `?lang=ar` retombe désormais sur le français), specs des pipes `money`/`localDate` en `nl` ; `scripts/e2e-mvp.mjs` (AUTH-08) utilise `nl`.
- Docs : `README.md`, `CLAUDE.md`, `02-conception.md` (§0 écart + §6), `05-demo-soutenance.md` (étape 5.2 EN → NL).

**Vérifié** : `./mvnw clean test` → **68/68** ; `npm run build` OK ; `npm run test:ci` → **22/22**.

**À savoir** : traductions néerlandaises non relues par un locuteur natif. La base `educa` locale recevra `V11` au prochain démarrage du backend. Les exigences d'origine de `01-analyse.md` (EF-26/27 : FR/EN/AR + RTL) sont conservées telles quelles comme trace de l'analyse ; l'écart est consigné dans `02-conception.md §0`.

---

## 2026-09-29 — Relecture des traductions DE / NL

**Demande** : relire les traductions allemandes et néerlandaises (interface + PDF).

**Constat** : traductions globalement correctes ; corrections de terminologie et de naturel.

**Corrigé**
- **Allemand** : seuil de réussite « Schwelle » → « Bestehensgrenze » ; « Echtes Zertifikat » → « Gültiges Zertifikat » ; « Einfachauswahl » → « Einzelauswahl » ; « Kein passender Kurs. » → « Keine passenden Kurse gefunden. » ; « Zurückziehen » → « Veröffentlichung zurückziehen » ; rôle « Kursleiter » → « Kursleiter/in » (cohérent avec « Lernende/r ») ; PDF : « Prüfung: Code » → « Verifizierung: Code » (« Prüfung » = examen, ambigu), « Bankkarte » → « Kartenzahlung ».
- **Néerlandais** : « drempel » → « slaaggrens » ; « Echt certificaat » → « Geldig certificaat » ; « vraag/vragen » → « vraag(en) » (même forme que « hoofdstuk(ken) ») ; « Waar / Onwaar » → « Waar / Niet waar » ; « Markeren als afgerond » → « Als afgerond markeren » ; « Actie niet toegestaan » → « Actie niet mogelijk » ; guillemets droits → ‘…’ (cohérence) ; facture : « uitgegeven op » → « opgemaakt op », « Bankkaart » → « Betaalkaart ».

**Vérifié** : JSON valides, `QuizFlowTest` + `CoursePaymentFlowTest` (PDF de/nl) → 10/10.

**À savoir** : relecture faite par Claude, pas par un locuteur natif — suffisante pour la soutenance, une relecture humaine reste conseillée avant une vraie mise en production.

---

## 2026-09-29 (suite) — Répétition de la démo : bug de suppression de cours

**Demande** : répétition de la démo de soutenance (`docs/05-demo-soutenance.md`).

**Constat**
- Frontend ouvert sans backend lancé → catalogue vide (pas un bug : les deux serveurs sont nécessaires).
- `scripts/e2e-mvp.mjs` → 47/47, mais il laissait son cours de test « E2E — Parcours vérif … » **publié** dans le catalogue de démo.
- En voulant le supprimer : **`500`**. Cause : `attempt_answers → questions` n'a pas de `ON DELETE CASCADE`. Plus grave : toutes les autres FK vers `courses` (inscriptions, certificats, paiements…) **sont** en cascade — seule cette contrainte, par hasard, empêchait la suppression d'un cours d'effacer les certificats et factures de ses apprenants (un cours avec inscrits mais sans tentative de quiz aurait été supprimé avec tout ça).

**Corrigé**
- `CourseService.delete` : refus explicite **`409`** si le cours a des inscrits. Point d'extension `course.CourseDeletionGuard` implémenté par `enrollment.EnrollmentCourseDeletionGuard` (`existsByCourseId`) — inversion de dépendance, `enrollment` dépendant déjà de `course`.
- Espace formateur : l'erreur de suppression était ignorée (clic sans effet) → message traduit (`instructorDashboard.deleteBlocked`/`deleteError`, fr/en/de/nl) invitant à dépublier.
- `e2e-mvp.mjs` : vérifie le `409` puis **dépublie** son cours de test (`CAT-DEL-01/02`) → catalogue de démo propre ; cours de test existant dépublié.
- `docs/05-demo-soutenance.md` : chiffres rafraîchis (tests, migrations `V1`→`V11`, compte apprenant neuf, extensions réalisées).

**Vérifié** : `./mvnw test` → **69/69** (+1 `CourseFlowTest`) ; `npm run build` OK, `npm run test:ci` → **22/22** ; backend relancé, `node scripts/e2e-mvp.mjs` → **49/49**.

---

## 2026-09-29 (suite) — Catalogue de démo enrichi

**Demande** : « ajoute des nouveaux cours et leur prix et des chapitres ».

**Fait** : 4 cours publiés et payants ajoutés au seed `DevDataInitializer` (profil `dev`, idempotent par slug — recréés sur toute base neuve, poste de soutenance ou Docker compris) :

| Cours | Prix | Chapitres |
|---|---|---|
| SQL et bases de données relationnelles | 34,99 € | Le modèle relationnel · Interroger avec SELECT · Jointures et agrégations |
| Algorithmique : les fondamentaux | 24,99 € | Penser un algorithme · Conditions et boucles · Rechercher et trier |
| Cybersécurité : les bons réflexes | 14,99 € | Mots de passe et authentification · Reconnaître l'hameçonnage · Mises à jour et sauvegardes |
| Java : programmation orientée objet | 39,99 € | Classes et objets · Encapsulation · Héritage et polymorphisme |

Chaque cours : 3 chapitres × 2 contenus texte, **un contrôle par chapitre** (2 questions) et un examen final (3 questions, 3 tentatives). Décrits de façon déclarative (`CourseSpec`/`ChapterSpec`) et créés par une méthode générique `seedCatalogCourse` plutôt que par un bloc copié-collé par cours ; Python et Git restent inchangés.

**Vérifié** : backend relancé sur la base `educa` locale → les 4 cours apparaissent au catalogue (9 cours publiés au total) avec 3 contrôles + 1 examen final chacun (`GET /courses/{id}/quizzes`).

**À savoir** : une recompilation incrémentale a laissé une classe manquante (`NoClassDefFoundError: AnswerOption` au démarrage) → `./mvnw clean compile` a suffi.

---

## 2026-09-29 (suite) — Paiement du cours SQL bloqué : payé 3 fois, jamais débloqué

**Symptôme** : « je n'arrive pas à faire le paiement pour un cours SQL ». Registre : 3 paiements Stripe `PENDING` d'`apprenant@educa.dev` pour « SQL et bases de données relationnelles ».

**Causes**
1. Le relais `stripe listen` n'était pas lancé → Stripe a bien encaissé les 3 sessions (mode test, `payment_status=paid`), mais aucun webhook `checkout.session.completed` n'a atteint le backend → paiements restés `PENDING`, cours jamais débloqué. De plus, depuis Stripe CLI 1.51, `stripe listen` refuse de démarrer sans `--events` (commande documentée obsolète).
2. **Défaut UX** : au retour de Stripe, la page du cours tentait l'inscription **une seule fois** (→ `402` si le webhook n'était pas encore arrivé), puis réaffichait les boutons « Payer » — d'où les 2 paiements supplémentaires.

**Corrigé**
- `course-detail` : après `?payment=success`, l'inscription est retentée sur `402` (5 × 2 s) le temps que la confirmation arrive ; si le cours est **déjà payé** mais pas encore inscrit, la page affiche « S'inscrire » au lieu des boutons de paiement (détection via `GET /payments/me`, uniquement si connecté).
- `InvoiceDto` expose désormais `courseId` (nécessaire à cette détection) ; assertion ajoutée dans `CoursePaymentFlowTest`.
- Doc (`05-demo-soutenance.md`, `docs/cheat.md`) : `stripe listen … --events checkout.session.completed …` + procédure de rattrapage `stripe events resend <evt_…>`.
- Données locales : relais relancé, confirmation de la dernière session renvoyée (`stripe events resend`) → paiement `SUCCEEDED`, facture `INV-2026-000003`. Les 2 autres sessions payées restent `PENDING` volontairement (mode test, pas d'argent réel ; éviter 3 factures pour le même cours).

**Vérifié** : `./mvnw clean test` → **69/69** ; `npm run build` OK, `npm run test:ci` → **22/22** ; backend relancé, `GET /payments/me` renvoie `courseId`.

## 2026-09-30 — Passe documentaire avant soutenance

**Objectif** : aligner les docs sur le code livré (plusieurs passages dataient d'avant les paiements et du passage à FR/EN/DE/NL).

**Mis à jour**
- `03-plan-implementation.md` : 4.13 (certificat PDF localisé) passée à « fait » (2026-09-26) ; 4.2/4.3 (RTL, police arabe) marquées retirées le 2026-09-28 ; note sur les langues en Phase 4 et dans le suivi global ; nouvelle section **« Évolutions post-MVP et corrections »** (E.1→E.9, 2026-09-26 → 2026-09-30) avec le dernier état vérifié et ce qui reste avant la soutenance.
- `02-conception.md` : §0 complété (suppression de `pdf_key`, achat par cours, `409` à la suppression d'un cours avec inscrits ; mention RTL obsolète retirée) ; §2 MCD avec `PAYMENT` ; §3 `courses.price`, `certificates.pdf_key` retirée, langues fr/en/de/nl ; §11.3/§11.4 : factures régénérées à chaque téléchargement (plus de `pdf_key`) ; §11.6 : les 6 cours de démo et leurs prix.
- `docs/assets/mcd.*` régénérés : **19 tables** (ajout de `PAYMENT`, `courses.price`, `certificates.pdf_key` retirée, langues fr/en/de/nl). `architecture.*` régénérés : module `payment` + Stripe / Orange Money.
- `01-analyse.md` : encadré « état de réalisation » à jour (EF-28 et module admin faits, certificat localisé fait, écarts langues et paiement).
- `06-verification-mvp.md` : dernier état (69/69, 22/22, E2E 49/49) et note sur les langues.

**Reste** : relecture humaine du MCD et de la matrice RBAC (0.7), clé Anthropic réelle, identifiants sandbox Orange Money.

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

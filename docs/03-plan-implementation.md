# 03 — Plan d'implémentation (tableau de bord)

Projet **educa**. Checklist vivante — mise à jour à la fin de chaque phase / session.
Légende statut : `à faire` · `en cours` · `fait` · `bloqué`.
Dernière mise à jour : 2026-09-08.

> **Règle MVP** : le périmètre « Must have » (Phases 0 → 3, + i18n interface & chatbot en Phase 4) est prioritaire sur tout le reste. Les tâches *Should / Could have* sont marquées `(S)` / `(C)`.

---

## Phase 0 — Cadrage  ·  statut : `en cours`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 0.1 | Rédiger `docs/01-analyse.md` | `docs/01-analyse.md` | fait | 2026-09-08 |
| 0.2 | Rédiger `docs/02-conception.md` (archi, MCD, schéma, API, sécurité, i18n, IA, choix, wireframes) | `docs/02-conception.md` | fait | 2026-09-08 |
| 0.3 | Créer `docs/03-plan-implementation.md` (ce fichier) | `docs/03-plan-implementation.md` | fait | 2026-09-08 |
| 0.4 | Créer `docs/04-journal-avancement.md` | `docs/04-journal-avancement.md` | fait | 2026-09-08 |
| 0.5 | Créer `CLAUDE.md` | `CLAUDE.md` | fait | 2026-09-08 |
| 0.6 | Créer `README.md` | `README.md` | fait | 2026-09-08 |
| 0.7 | **Validation humaine du MCD et de la matrice RBAC** | `docs/02-conception.md` | à faire | — |
| 0.8 | Initialiser git + connecter le remote GitHub + `user.*` en `--local` (commits au nom de l'autrice, sans trailer) | repo | fait | 2026-09-08 |
| 0.9 | Créer `.gitignore` (racine : secrets, Maven, Angular, IDE) | `.gitignore` | fait | 2026-09-08 |
| 0.10 | Installer PostgreSQL en local + pgAdmin ; créer les bases `educa` et `educa_test` | pgAdmin | fait | 2026-09-08 |
| 0.11 | Créer `.env.example` (PostgreSQL, JWT, clé API Claude, chemin `storage`) | `.env.example` | fait | 2026-09-08 |
| 0.12 | Config format : `.editorconfig` + `.gitattributes` (LF). ESLint/Prettier frontend = à l'init Angular (tâche 1.11) | `.editorconfig`, `.gitattributes` | fait | 2026-09-08 |
| 0.13 | Premier commit + push vers `origin/main` (au nom de mariam Balde, sans trailer) | repo | fait | 2026-09-08 |

> `docker-compose.yml` : **reporté**. Sera ajouté plus tard (confort de dev + déploiement).

**Livrable Phase 0** : documentation validée + PostgreSQL local opérationnel + repo poussé sur GitHub (git, `.gitignore`, `.gitattributes`, `.editorconfig`, `.env.example`) — sans fonctionnalité métier. **Reste : relecture humaine du MCD + matrice RBAC.**

---

## Phase 1 — Socle technique  ·  statut : `quasi terminée` (reste 1.10 Swagger)

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 1.0 | Squelette *package-by-feature* : `common/error`, `config`, `security`, `user` + `package-info.java` pour `course`/`enrollment`/`quiz`/`certificate`/`storage`/`ai`. Patron figé : **Lombok** sur les entités, **MapStruct** pour les mappers (`annotationProcessorPaths`), DTO en `record` | `com.educa.backend.*` | fait | 2026-09-08 |
| 1.1 | Dépendances Maven : web, validation, data-jpa, **spring-boot-flyway** + flyway-core (+`flyway-database-postgresql`), postgresql, security, jjwt 0.12.6, Lombok, MapStruct 1.6.3 (+ `annotationProcessorPaths`), **spring-boot-starter-webmvc-test** (tests). springdoc → tâche 1.10 | `backend/pom.xml` | fait | 2026-09-08 |
| 1.2 | `application.yml` (+ `-dev` / `-test`), `spring.config.import` du `.env` racine, `ddl-auto: validate`, propriétés `educa.*` (`EducaProperties`), **port `8081`** (8080 pris par un `mysqld` local) | `backend/src/main/resources/` | fait | 2026-09-08 |
| 1.3 | Migration Flyway `V1__init.sql` (15 tables + contraintes + index partiels quiz ; `VARCHAR(2)` pour les codes langue) et `V2__seed_roles.sql` — **appliquées et validées sur `educa`** | `backend/.../db/migration/` | fait | 2026-09-08 |
| 1.4 | Entités JPA + repositories — **module `user`** (`User`, `Role`, `RefreshToken` + repos). Autres domaines : Phases 2–3 | `com.educa.backend.user` | fait | 2026-09-08 |
| 1.5 | Sécurité : `SecurityConfig` (stateless, CORS, entrypoints JSON 401/403), `PasswordEncoder` BCrypt, `JwtService` (HS256), `JwtAuthenticationFilter`, `AppUserDetailsService`, `CurrentUser` | `com.educa.backend.security` | fait | 2026-09-08 |
| 1.6 | Auth : `POST /auth/register` (201), `/login`, `/refresh` (rotation), `/logout`, `GET/PATCH /auth/me` | `com.educa.backend.user` | fait | 2026-09-08 |
| 1.7 | RBAC : `RoleName` `LEARNER/INSTRUCTOR/ADMIN`, `@EnableMethodSecurity`, `CurrentUser.id()`. `@PreAuthorize` + helper « propriétaire » viendront avec les endpoints métier (Phase 2) | `com.educa.backend.security` | en cours | 2026-09-08 |
| 1.8 | Gestion d'erreurs `@RestControllerAdvice` (`GlobalExceptionHandler`) + `ApiError` homogène + `ApiException`/`ResourceNotFoundException`/`ConflictException` | `com.educa.backend.common.error` | fait | 2026-09-08 |
| 1.9 | Seed rôles (via `V2`) + `DevDataInitializer` (profil `dev`) : `admin@educa.dev`, `formateur@educa.dev`, `apprenant@educa.dev` — mot de passe `password123` | `com.educa.backend.config` | fait | 2026-09-08 |
| 1.10 | Swagger UI (springdoc) accessible en dev — en attente d'une version compatible Spring Boot 4 | `backend/pom.xml`, config | à faire | — |
| 1.11 | Projet **Angular 19.2** dans `frontend/` (Node 24.12 → CLI *latest* refusée, CLI 19.2 utilisée) ; standalone, `provideRouter` + `provideHttpClient(withInterceptors)` ; structure `core/` + `feature/` ; `ng build` OK | `frontend/` | fait | 2026-09-08 |
| 1.12 | Frontend : shell `AppComponent` avec navigation conditionnée par rôle + déconnexion ; pages **login** / **register** (formulaires réactifs, connexion auto après inscription) | `frontend/src/app` | fait | 2026-09-08 |
| 1.13 | Frontend : `AuthService` (signals, localStorage), `authInterceptor` (Bearer + refresh auto sur 401), `authGuard` + `roleGuard(...)` | `frontend/src/app/core/auth` | fait | 2026-09-08 |
| 1.14 | Frontend : dashboards par rôle `/dashboard` (apprenant), `/instructor`, `/admin` (placeholders) + lazy loading | `frontend/src/app/feature` | fait | 2026-09-08 |
| 1.15 | Tests backend : `AuthControllerTest` (8 tests) — register 201 / doublon 409 / payload invalide 400 / login 200 / mauvais mdp 401 / `/me` sans jeton 401 / `/me` avec jeton 200 / refresh rotation. `./mvnw test` → **9 tests verts** (dont `contextLoads`) sur `educa_test` | `backend/src/test/...` | fait | 2026-09-08 |

**Livrable démontrable** : ✅ un utilisateur ouvre `http://localhost:4200`, crée un compte ou se connecte (`apprenant@educa.dev` / `password123`), et voit le tableau de bord correspondant à son rôle. Navigation et accès aux pages `/instructor` et `/admin` filtrés par `roleGuard`.

**Commande de lancement** : backend `cd backend && ./mvnw spring-boot:run` (port 8081) · frontend `cd frontend && npm start` (port 4200).

---

## Phase 2 — Gestion des formations  ·  statut : `terminée (build OK, à valider au navigateur)`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 2.1 | CRUD `courses` : `POST/PUT/DELETE /courses`, `slug` auto-unique, `publish`/`unpublish`, `GET /instructor/courses` ; contrainte `control_weight + exam_weight = 100` | `com.educa.backend.course` | fait | 2026-09-08 |
| 2.2 | CRUD `chapters` ordonnés : `POST /courses/{id}/chapters`, `PUT/DELETE /chapters/{id}` (unicité de `position`) | `com.educa.backend.course` | fait | 2026-09-08 |
| 2.3 | CRUD `contents` (`TEXT/VIDEO/DOCUMENT`) ordonnés : `POST /chapters/{id}/contents`, `PUT/DELETE /contents/{id}` | `com.educa.backend.course` | fait | 2026-09-08 |
| 2.4 | Module `storage` : `StorageService` + `FileSystemStorageService` (dossier `${STORAGE_LOCAL_PATH}`, anti-path-traversal) ; `POST /contents/{id}/file` (multipart), `GET /contents/{id}/file` (flux binaire) | `com.educa.backend.storage` | fait | 2026-09-08 |
| 2.5 | Catalogue public `GET /courses` (`q`, `language`, pagination `PageResponse`) + `GET /courses/{slug}` (détail, contenus masqués si non inscrit) | `com.educa.backend.course` | fait | 2026-09-08 |
| 2.6 | `enrollments` : `POST /courses/{id}/enroll` (publié uniquement, unicité), `GET /enrollments/me` (avec %) | `com.educa.backend.enrollment` | fait | 2026-09-08 |
| 2.7 | `progress` : `POST /contents/{id}/complete`, `GET /courses/{id}/progress`, calcul du % (contenus vus / total) | `com.educa.backend.enrollment` | fait | 2026-09-08 |
| 2.8 | Contrôle d'accès objet : `CourseService.requireOwned` (propriétaire ou ADMIN) ; contenus + fichier visibles seulement si inscrit / propriétaire / ADMIN ; `CurrentUser.hasRole` / `optionalId` | `com.educa.backend.course`, `.security` | fait | 2026-09-08 |
| 2.9 | Frontend : `instructor-dashboard` (liste « mes cours », publier/dépublier/supprimer) + `course-editor` (créer/éditer cours, ajouter chapitres, ajouter contenus TEXT/fichier avec upload). `CourseApiService` + modèles | `frontend/src/app/feature/instructor`, `core/courses` | fait | 2026-09-08 |
| 2.10 | Frontend : `catalog` (liste publiés + recherche debounce) + `course-detail` (`/courses/:slug` : s'inscrire, contenus masqués si non inscrit, lecture TEXT, ouverture de fichier via blob, « marquer comme terminé », barre de progression) | `frontend/src/app/feature/catalog`, `.../course` | fait | 2026-09-08 |
| 2.11 | Frontend : `learner-dashboard` branché sur `GET /enrollments/me` (cartes + barres de %) ; `EnrollmentApiService` | `frontend/src/app/feature/dashboard`, `core/enrollments` | fait | 2026-09-08 |
| 2.12 | Tests backend : `CourseFlowTest` (5 tests) — création + slug, apprenant → 403, catalogue = publiés seulement, inscription + progression + doublon 409, contenus masqués si non inscrit. `./mvnw test` → **14 verts** | `backend/src/test/...` | fait | 2026-09-08 |
| 2.13 | (S) Recherche/filtrage avancé du catalogue | — | à faire | — |

**Livrable démontrable** : ✅ backend vérifié en conditions réelles (formateur crée « Bases du Git » + chapitre + contenus + fichier, publie ; apprenant s'inscrit, complète un contenu → 50 %, télécharge le fichier). Reste le frontend (2.9–2.11).

**Note** : `DevDataInitializer` seede aussi un cours de démo publié « Introduction à Python » (2 chapitres, 4 contenus) appartenant à `formateur@educa.dev`.

---

## Phase 3 — Évaluation & certification  ·  statut : `terminée (build/tests OK, à valider au navigateur)`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 3.1 | CRUD `quizzes` : `CONTROL` (`POST /chapters/{id}/control-quiz`, 1/chapitre) et `FINAL_EXAM` (`POST /courses/{id}/final-exam`, 1/cours) ; `PUT/DELETE /quizzes/{id}` | `com.educa.backend.quiz` | fait | 2026-09-08 |
| 3.2 | CRUD `questions` + `answer_options` (`SINGLE/MULTIPLE/TRUE_FALSE`) ; validation ≥2 options, ≥1 correcte, `TRUE_FALSE`=2, `SINGLE`=1 correcte | `com.educa.backend.quiz` | fait | 2026-09-08 |
| 3.3 | `GET /quizzes/{id}` : version propriétaire (`answersVisible`) vs apprenant (options sans `correct`) ; `403` si non inscrit ou `FINAL_EXAM` verrouillé. `GET /courses/{id}/quizzes` (liste contrôles + examen) | `com.educa.backend.quiz` | fait | 2026-09-08 |
| 3.4 | `POST /quizzes/{id}/attempts` : `GradingService` (score = points obtenus/total ; question juste = ensemble des options cochées == bonnes) ; `max_attempts` ; `409` verrouillé/épuisé | `com.educa.backend.quiz` | fait | 2026-09-08 |
| 3.5 | Persistance `quiz_attempts` + `attempt_answers` (`selected_option_ids bigint[]`) ; score retenu = meilleure tentative | `com.educa.backend.quiz` | fait | 2026-09-08 |
| 3.6 | `QuizUnlockService` : examen final débloqué à 100 % des `contents` vus **et** tous les contrôles tentés | `com.educa.backend.quiz`, `.enrollment` | fait | 2026-09-08 |
| 3.7 | Note finale pondérée (`control_weight`/`exam_weight` du cours) ; moyenne des meilleurs contrôles (non tenté = 0 ; aucun contrôle → note = examen) ; `GET /courses/{id}/grade` | `com.educa.backend.quiz` | fait | 2026-09-08 |
| 3.8 | `GET /quizzes/{id}/attempts/me` + `GET /instructor/courses/{id}/results` (moyenne contrôles, examen, note finale, certifié — par apprenant) | `com.educa.backend.quiz` | fait | 2026-09-08 |
| 3.9 | `openhtmltopdf-pdfbox` 1.0.10 ; gabarit HTML/CSS du certificat (A4 paysage, note finale + composantes) | `backend/pom.xml`, `com.educa.backend.certificate` | fait | 2026-09-08 |
| 3.10 | Génération auto du `certificate` quand note finale ≥ `courses.pass_threshold` : `serial_number` (`EDUCA-AAAA-000001`), `verification_code`, `controls_average`/`final_exam_score`/`final_grade`, `pdf_key` (via `storage`) ; `enrollment` → `COMPLETED` | `com.educa.backend.certificate` | fait | 2026-09-08 |
| 3.11 | `GET /certificates/me`, `GET /certificates/{id}/download` (propriétaire/ADMIN), `GET /certificates/verify/{code}` (public) | `com.educa.backend.certificate` | fait | 2026-09-08 |
| 3.12 | Frontend : `course-editor` — créer un contrôle par chapitre / l'examen final ; `quiz-editor` — ajout de questions (énoncé, type, options + bonnes réponses) | `frontend/src/app/feature/instructor` | fait | 2026-09-08 |
| 3.13 | Frontend : `quiz-take` — passage d'un quiz (radio/checkbox selon le type), écran résultat (score, note finale + certificat si examen final) ; examen final grisé si non déverrouillé | `frontend/src/app/feature/quiz` | fait | 2026-09-08 |
| 3.14 | Frontend : `course-detail` — panneau évaluation (moyenne contrôles, meilleur score par contrôle, note finale, bouton certificat) ; `my-certificates` (liste + PDF) ; `verify/:code` (page publique) ; `course-results` (formateur) | `frontend/src/app/feature/certificate`, `.../course` | fait | 2026-09-08 |
| 3.15 | Tests backend : `QuizFlowTest` (4 tests, dont parcours complet contrôle → déverrouillage → examen final → certificat → vérification). `./mvnw test` → **18 verts** | `backend/src/test/...` | fait | 2026-09-08 |

**Livrable démontrable** : ✅ (test d'intégration) un apprenant passe le contrôle, atteint 100 %, débloque et réussit l'examen final, la note pondérée atteint le seuil, un certificat PDF est généré et vérifiable par code. Reste la validation au navigateur.

---

## Phase 4 — Multilingue & IA  ·  statut : `terminée (MVP) — multilingue validé au navigateur (2026-09-10) ; réponse IA réelle en attente d'une clé Anthropic`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 4.1 | Frontend : `@ngx-translate/core` v18 + `provideTranslateHttpLoader` ; `public/i18n/{fr,en,ar}.json` ; `LanguageService` (init depuis `user.preferredLanguage` ou `localStorage`) ; sélecteur de langue dans la barre ; persistance `localStorage` + `PATCH /auth/me` (`updatePreferredLanguage`) | `frontend/public/i18n`, `core/i18n` | fait | 2026-09-08 |
| 4.2 | Frontend : `document.documentElement.dir = rtl` pour l'arabe (+ `lang`) ; overrides `[dir='rtl']` dans `styles.scss` (le reste en flexbox/grid se retourne seul) | `frontend/src/styles`, `LanguageService` | fait | 2026-09-08 |
| 4.3 | Frontend : police avec fallback arabe (`'Noto Sans Arabic'` dans la stack `body`) | `frontend/src/styles.scss` | fait | 2026-09-08 |
| 4.4 | Backend : module `ai` — interface `AiAssistant`, `ClaudeAiAssistant` (SDK officiel `com.anthropic:anthropic-java` 2.34), `DisabledAiAssistant` (repli) ; `AiConfig` choisit le bean selon `educa.ai.enabled` + clé présente | `com.educa.backend.ai` | fait | 2026-09-08 |
| 4.5 | Backend : `POST /ai/chat` — contexte cours borné (`CourseService.aiContext`, `max-context-chars`), system prompt séparé de la question, historique transmis par le client, **toute erreur/timeout → `{reply, degraded:true}`** (jamais d'exception) | `com.educa.backend.ai` | fait | 2026-09-08 |
| 4.6 | Backend : `AiChatService` exige une inscription active (ou propriétaire/ADMIN) → `403` sinon ; `educa.ai.*` depuis l'env (`ANTHROPIC_API_KEY`, `AI_MODEL` défaut `claude-sonnet-5`, `AI_ENABLED`, `AI_TIMEOUT_MS`) | `com.educa.backend.ai` | fait | 2026-09-08 |
| 4.7 | Frontend : `CourseChatComponent` (widget « Assistant du cours » sur la page cours, visible si inscrit) ; `AiApiService` ; historique local (7 derniers échanges) | `frontend/src/app/feature/course`, `core/ai` | fait | 2026-09-08 |
| 4.8 | Tests backend : `AiChatTest` — `403` si non inscrit ; `{degraded:true}` quand `ai.enabled=false` (profil `test`). `./mvnw test` → **20 verts** | `backend/src/test/...` | fait | 2026-09-08 |
| 4.9 | (S) Tables `languages`, `course_translations`, `chapter_translations` | migration `V?__i18n.sql` | à faire (Should have) | — |
| 4.10 | (S) Persistance `chat_messages` | `com.educa.backend.ai` | à faire (Should have) | — |
| 4.11 | (S) Génération assistée de quiz par IA | — | à faire (Should have) | — |
| 4.12 | (C) Recommandation de formations | — | à faire (Could have) | — |
| 4.13 | (C) Certificat PDF localisé | — | à faire (Could have) | — |

**Livrable démontrable** : ✅ le sélecteur de langue bascule l'interface FR/EN/AR (avec passage en RTL pour l'arabe, préférence persistée). Le widget chatbot sur la page cours envoie la question à `POST /ai/chat` ; sans clé API réelle il renvoie proprement un message de repli (`degraded`), avec une vraie clé il répond dans le périmètre du cours. Reste la validation au navigateur (et une clé Claude réelle pour des réponses live).

---

## Phase 5 — Tests & durcissement  ·  statut : `en cours`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 5.1 | Tests d'intégration bout-en-bout des parcours critiques (auth, inscription→cours, quiz→certificat) | `backend/src/test/...` | en cours (23 tests ; + `UploadSecurityTest`) | 2026-09-09 |
| 5.2 | Tests frontend : login, passage de quiz | `frontend/src/app/...spec.ts` | fait (`karma.conf.js` + `npm run test:ci` headless ; `auth.service` 4, `login.component` 3, `quiz-take.component` 4, `app.component` 2 → **13 verts**) | 2026-09-10 |
| 5.3 | Revue de sécurité : matrice RBAC vérifiée endpoint par endpoint, accès objet, exposition des bonnes réponses de quiz | `docs/`, code | fait | 2026-09-09 |
| 5.4 | Revue : validation des entrées, tailles/MIME des uploads, en-têtes de sécurité, CORS | code, config | fait (uploads bornés + liste blanche MIME ; download `attachment` par défaut + `nosniff` ; CORS origines explicites) | 2026-09-09 |
| 5.5 | Revue : secrets hors du code, `.env.example` à jour, pas de secret loggé | repo | fait | 2026-09-09 |
| 5.6 | Exécuter `/security-review` et traiter les findings | — | à faire | — |
| 5.7 | Correctifs de bugs identifiés | — | en cours (uploads/downloads durcis ; `413`/`400` au lieu de `500`) | 2026-09-09 |
| 5.8 | Vérifier la performance des listes (pagination, index) sur le jeu de démo | — | fait (catalogue paginé + `size` borné 1..100 ; toutes les listes FK indexées : `idx_courses_*`, `idx_enrollments_*`, `idx_quiz_attempts_user_quiz`, `idx_certificates_verification_code`…) | 2026-09-10 |

**Livrable démontrable** : suite de tests verte sur les parcours critiques ; aucune faille RBAC évidente.

---

## Phase 6 — Rédaction finale & soutenance  ·  statut : `à faire`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 6.1 | Mise à jour finale de `docs/01`→`04` + `CLAUDE.md` + `README.md` | `docs/`, racine | à faire | — |
| 6.2 | Jeu de données de démonstration (script de seed : utilisateurs, cours complets, quiz, 1 certificat) | `backend/.../demo` | à faire | — |
| 6.3 | Script / scénario de démonstration pour la soutenance | `docs/` | à faire | — |
| 6.4 | Diagrammes propres (archi, MCD) exportés pour le mémoire | `docs/assets` | à faire | — |
| 6.5 | Vérification finale : tout le périmètre « Must have » est fonctionnel de bout en bout | — | à faire | — |
| 6.6 | Instructions de déploiement cloud (Docker) | `README.md`, `docs/` | à faire | — |

**Livrable** : application MVP complète et démontrable, documentation à jour, support de soutenance prêt.

---

## Suivi global

| Phase | État | Début | Fin |
|---|---|---|---|
| 0 — Cadrage | terminée | 2026-09-08 | 2026-09-08 |
| 1 — Socle technique | ✅ backend (9 tests) + frontend Angular — parcours inscription/connexion validé au navigateur. Reste 1.10 (Swagger, reporté) | 2026-09-08 | 2026-09-08 |
| 2 — Gestion des formations | backend (14 tests) + frontend Angular (build OK) — à valider au navigateur | 2026-09-08 | 2026-09-08 |
| 3 — Évaluation & certification | backend (18 tests, parcours certificat complet) + frontend (build OK) — à valider au navigateur | 2026-09-08 | 2026-09-08 |
| 4 — Multilingue & IA (MVP) | i18n FR/EN/AR + RTL, chatbot (SDK Anthropic + repli) — backend 20 tests, frontend build OK | 2026-09-08 | 2026-09-08 |
| 2 — Gestion des formations | à faire | — | — |
| 3 — Évaluation & certification | à faire | — | — |
| 4 — Multilingue & IA | à faire | — | — |
| 5 — Tests & durcissement | à faire | — | — |
| 6 — Rédaction finale & soutenance | à faire | — | — |

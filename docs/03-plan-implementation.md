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

## Phase 1 — Socle technique  ·  statut : `en cours`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 1.0 | Squelette *package-by-feature* : `common/error`, `config`, `security`, `user` + `package-info.java` pour `course`/`enrollment`/`quiz`/`certificate`/`storage`/`ai`. Patron figé : **Lombok** sur les entités, **MapStruct** pour les mappers (`annotationProcessorPaths`), DTO en `record` | `com.educa.backend.*` | fait | 2026-09-08 |
| 1.1 | Dépendances Maven : web, validation, data-jpa, flyway (+`flyway-database-postgresql`), postgresql, security, jjwt 0.12.6, Lombok, MapStruct 1.6.3 (+ `annotationProcessorPaths` : lombok / mapstruct-processor / lombok-mapstruct-binding). springdoc → tâche 1.10 | `backend/pom.xml` | fait | 2026-09-08 |
| 1.2 | `application.yml` (+ `-dev` / `-test`), `spring.config.import` du `.env` racine, `ddl-auto: validate`, propriétés `educa.*` (`EducaProperties`) | `backend/src/main/resources/` | fait | 2026-09-08 |
| 1.3 | Migration Flyway `V1__init.sql` (15 tables + contraintes + index partiels quiz) et `V2__seed_roles.sql` | `backend/.../db/migration/` | fait | 2026-09-08 |
| 1.4 | Entités JPA + repositories — **module `user`** (`User`, `Role`, `RefreshToken` + repos). Autres domaines : Phases 2–3 | `com.educa.backend.user` | en cours | 2026-09-08 |
| 1.5 | Sécurité : `SecurityConfig` (stateless, CORS, entrypoints JSON 401/403), `PasswordEncoder` BCrypt, `JwtService` (HS256), `JwtAuthenticationFilter`, `AppUserDetailsService`, `CurrentUser` | `com.educa.backend.security` | fait | 2026-09-08 |
| 1.6 | Auth : `POST /auth/register` (201), `/login`, `/refresh` (rotation), `/logout`, `GET/PATCH /auth/me` | `com.educa.backend.user` | fait | 2026-09-08 |
| 1.7 | RBAC : `RoleName` `LEARNER/INSTRUCTOR/ADMIN`, `@EnableMethodSecurity`, `CurrentUser.id()`. `@PreAuthorize` + helper « propriétaire » viendront avec les endpoints métier (Phase 2) | `com.educa.backend.security` | en cours | 2026-09-08 |
| 1.8 | Gestion d'erreurs `@RestControllerAdvice` (`GlobalExceptionHandler`) + `ApiError` homogène + `ApiException`/`ResourceNotFoundException`/`ConflictException` | `com.educa.backend.common.error` | fait | 2026-09-08 |
| 1.9 | Seed rôles (fait via `V2`). Utilisateurs de démo (admin/formateur/apprenant) : `DataInitializer` profil `dev` — à faire | `backend/.../db/migration/`, `config` | en cours | 2026-09-08 |
| 1.10 | Swagger UI (springdoc) accessible en dev — en attente d'une version compatible Spring Boot 4 | `backend/pom.xml`, config | à faire | — |
| 1.11 | Init projet Angular dans `frontend/` (routing, HttpClient, structure `core/feature/shared`) | `frontend/` | à faire | — |
| 1.12 | Frontend : layout + navigation conditionnée par rôle, pages `login` / `register` | `frontend/src/app/feature/auth` | à faire | — |
| 1.13 | Frontend : intercepteur HTTP (Bearer + refresh), `AuthGuard`, `RoleGuard`, service `AuthService` | `frontend/src/app/core` | à faire | — |
| 1.14 | Frontend : dashboards vides par rôle (`/dashboard`, `/instructor`, `/admin`) | `frontend/src/app/feature` | à faire | — |
| 1.15 | Tests backend : auth (register/login/refresh), accès refusé sans rôle — sur base `educa_test` locale, profil `test`. *(Bloqué : nécessite `.env` avec le mot de passe PostgreSQL.)* | `backend/src/test/...` | à faire | — |

**Livrable démontrable** : un utilisateur s'inscrit, se connecte, voit un dashboard vide correspondant à son rôle.

---

## Phase 2 — Gestion des formations  ·  statut : `à faire`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 2.1 | CRUD `courses` (propriétaire) + `slug` + publish/unpublish | `com.educa.backend.course` | à faire | — |
| 2.2 | CRUD `chapters` ordonnés | `com.educa.backend.course` | à faire | — |
| 2.3 | CRUD `contents` (`TEXT/VIDEO/DOCUMENT`) ordonnés | `com.educa.backend.course` | à faire | — |
| 2.4 | Module `storage` : interface `StorageService` + impl. `FileSystemStorageService` (dossier `backend/var/storage/`) ; upload **multipart** `POST /contents/{id}/file`, download `GET /contents/{id}/file`. Impl. S3/MinIO = plus tard | `com.educa.backend.storage` | à faire | — |
| 2.5 | Catalogue public `GET /courses` (filtre `q`, `language`, pagination) + `GET /courses/{slug}` | `com.educa.backend.course` | à faire | — |
| 2.6 | `enrollments` : `POST /courses/{id}/enroll`, `GET /enrollments/me` | `com.educa.backend.enrollment` | à faire | — |
| 2.7 | `progress` : `POST /contents/{id}/complete`, `GET /courses/{id}/progress`, calcul du % | `com.educa.backend.enrollment` | à faire | — |
| 2.8 | Contrôle d'accès objet : contenu visible seulement si inscrit / propriétaire / admin | `com.educa.backend.security` | à faire | — |
| 2.9 | Frontend : espace formateur — création cours, éditeur de chapitres/contenus, upload | `frontend/src/app/feature/instructor` | à faire | — |
| 2.10 | Frontend : catalogue + page cours (lecture chapitres/contenus, bouton s'inscrire, marquer terminé) | `frontend/src/app/feature/catalog`, `.../course` | à faire | — |
| 2.11 | Frontend : dashboard apprenant — formations en cours + % (branché sur `GET /enrollments/me`) | `frontend/src/app/feature/dashboard` | à faire | — |
| 2.12 | Tests backend : RBAC cours (formateur ≠ propriétaire refusé), inscription, progression | `backend/src/test/...` | à faire | — |
| 2.13 | (S) Recherche/filtrage avancé du catalogue | — | à faire | — |

**Livrable démontrable** : un formateur crée un cours avec chapitres et contenus ; un apprenant s'inscrit, le consulte, sa progression est suivie.

---

## Phase 3 — Évaluation & certification  ·  statut : `à faire`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 3.1 | CRUD `quizzes` : `type=CONTROL` (1 par chapitre, `POST /chapters/{id}/control-quiz`) et `type=FINAL_EXAM` (1 par cours, `POST /courses/{id}/final-exam`) ; contraintes d'unicité | `com.educa.backend.quiz` | à faire | — |
| 3.2 | CRUD `questions` + `answer_options` (types `SINGLE/MULTIPLE/TRUE_FALSE`), validation (≥2 options, ≥1 correcte ; `TRUE_FALSE` = 2 options) | `com.educa.backend.quiz` | à faire | — |
| 3.3 | `GET /quizzes/{id}` : version apprenant (sans `isCorrect`) vs propriétaire/admin ; `403` si `FINAL_EXAM` et progression < 100 % | `com.educa.backend.quiz` | à faire | — |
| 3.4 | `POST /quizzes/{id}/attempts` : correction auto, score %, gestion `max_attempts` (illimité `CONTROL` / limité `FINAL_EXAM`), `409` si verrouillé/épuisé | `com.educa.backend.quiz` | à faire | — |
| 3.5 | Persistance `quiz_attempts` + `attempt_answers` (snapshot des réponses) ; score retenu par quiz = meilleure tentative | `com.educa.backend.quiz` | à faire | — |
| 3.6 | Déverrouillage de l'examen final : 100 % des `contents` vus **et** tous les contrôles tentés | `com.educa.backend.enrollment`, `.quiz` | à faire | — |
| 3.7 | Calcul de la **note finale pondérée** (`control_weight`/`exam_weight`), moyenne des meilleurs contrôles (contrôle non tenté = 0) ; endpoint `GET /courses/{id}/grade` | `com.educa.backend.quiz`, `.course` | à faire | — |
| 3.8 | `GET /quizzes/{id}/attempts/me` + `GET /instructor/courses/{id}/results` (agrégat par apprenant) | `com.educa.backend.quiz` | à faire | — |
| 3.9 | Choisir + intégrer la lib PDF ; gabarit certificat (latin + arabe) affichant moyenne contrôles / score examen / note finale | `backend/pom.xml`, `com.educa.backend.certificate` | à faire | — |
| 3.10 | Génération auto du `certificate` quand note finale ≥ `courses.pass_threshold` : `serial_number`, `verification_code`, `controls_average`, `final_exam_score`, `final_grade`, `pdf_key` ; passage de l'`enrollment` à `COMPLETED` | `com.educa.backend.certificate` | à faire | — |
| 3.11 | `GET /certificates/me`, `GET /certificates/{id}/download`, `GET /certificates/verify/{code}` (public) | `com.educa.backend.certificate` | à faire | — |
| 3.12 | Frontend : éditeur de quiz formateur (contrôle par chapitre + examen final, pondération & seuil du cours) | `frontend/src/app/feature/instructor` | à faire | — |
| 3.13 | Frontend : passage de quiz (navigation, soumission, écrans résultat contrôle vs examen) ; examen final grisé tant que progression < 100 % | `frontend/src/app/feature/quiz` | à faire | — |
| 3.14 | Frontend : page « ma note » (moyenne contrôles, détail par chapitre, note finale) + liste/téléchargement certificats + page publique de vérification | `frontend/src/app/feature/certificate` | à faire | — |
| 3.15 | Tests backend : correction (chaque type), calcul pondéré 40/60, meilleur score, déverrouillage examen, seuil, génération + vérification certificat | `backend/src/test/...` | à faire | — |

**Livrable démontrable** : un apprenant passe les contrôles de chapitre, atteint 100 %, débloque et réussit l'examen final ; sa note finale pondérée atteint le seuil et il télécharge un certificat PDF vérifiable.

---

## Phase 4 — Multilingue & IA  ·  statut : `à faire`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 4.1 | Frontend : `@ngx-translate` + fichiers `fr/en/ar.json`, sélecteur de langue, persistance (`localStorage` + `PATCH /auth/me`) | `frontend/src/assets/i18n`, `core` | à faire | — |
| 4.2 | Frontend : bascule `dir=rtl` pour l'arabe, styles logiques CSS, revue des composants directionnels | `frontend/src/styles`, layout | à faire | — |
| 4.3 | Frontend : polices latin + arabe embarquées | `frontend/src/assets/fonts` | à faire | — |
| 4.4 | Backend : module IA — interface `AiAssistant`, `LlmAiAssistant`, `DisabledAiAssistant` (repli) | `com.educa.backend.ai` | à faire | — |
| 4.5 | Backend : `POST /ai/chat` — contexte cours borné, prompt system/user séparés, timeout + `degraded` | `com.educa.backend.ai` | à faire | — |
| 4.6 | Backend : contrôle d'accès `/ai/chat` (inscription active requise) ; config `ai.enabled`, clé API en env | `com.educa.backend` | à faire | — |
| 4.7 | Frontend : widget chatbot dans la page cours | `frontend/src/app/feature/course` | à faire | — |
| 4.8 | Tests backend : repli IA quand `ai.enabled=false` / timeout ; accès refusé si non inscrit | `backend/src/test/...` | à faire | — |
| 4.9 | (S) Tables `languages`, `course_translations`, `chapter_translations` + fallback langue d'origine | migration `V?__i18n.sql`, `com.educa.backend.course` | à faire | — |
| 4.10 | (S) Persistance `chat_messages` + `GET /ai/chat/{courseId}/history` | `com.educa.backend.ai` | à faire | — |
| 4.11 | (S) Génération assistée de quiz par IA | `com.educa.backend.ai`, `quiz` | à faire | — |
| 4.12 | (C) Recommandation de formations | — | à faire | — |
| 4.13 | (C) Certificat PDF localisé selon la langue de l'apprenant | `com.educa.backend.certificate` | à faire | — |

**Livrable démontrable** : changement de langue (dont RTL arabe) fonctionnel ; le chatbot répond à une question sur un cours, avec repli propre si l'IA est coupée.

---

## Phase 5 — Tests & durcissement  ·  statut : `à faire`

| # | Tâche | Fichiers / modules | Statut | MàJ |
|---|---|---|---|---|
| 5.1 | Tests d'intégration bout-en-bout des parcours critiques (auth, inscription→cours, quiz→certificat) | `backend/src/test/...` | à faire | — |
| 5.2 | Tests frontend : login, passage de quiz | `frontend/src/app/...spec.ts` | à faire | — |
| 5.3 | Revue de sécurité : matrice RBAC vérifiée endpoint par endpoint, accès objet, exposition des bonnes réponses de quiz | `docs/`, code | à faire | — |
| 5.4 | Revue : validation des entrées, tailles/MIME des uploads, en-têtes de sécurité, CORS | code, config | à faire | — |
| 5.5 | Revue : secrets hors du code, `.env.example` à jour, pas de secret loggé | repo | à faire | — |
| 5.6 | Exécuter `/security-review` et traiter les findings | — | à faire | — |
| 5.7 | Correctifs de bugs identifiés | — | à faire | — |
| 5.8 | Vérifier la performance des listes (pagination, index) sur le jeu de démo | — | à faire | — |

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
| 1 — Socle technique | en cours (backend auth compile ; frontend + tests à faire) | 2026-09-08 | — |
| 2 — Gestion des formations | à faire | — | — |
| 3 — Évaluation & certification | à faire | — | — |
| 4 — Multilingue & IA | à faire | — | — |
| 5 — Tests & durcissement | à faire | — | — |
| 6 — Rédaction finale & soutenance | à faire | — | — |

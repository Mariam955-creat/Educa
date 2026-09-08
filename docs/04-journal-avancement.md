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

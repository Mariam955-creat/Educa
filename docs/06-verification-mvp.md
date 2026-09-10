# 06 — Vérification du périmètre « Must have » (tâche 6.5)

Vérification bout-en-bout que **tout le périmètre MVP est fonctionnel**.
Dernière exécution : **2026-09-10**.

---

## 1. Résultat global

| Suite | Résultat |
|---|---|
| Tests d'intégration backend (`cd backend && ./mvnw test`) | **23 / 23 verts** |
| Tests frontend headless (`cd frontend && npm run test:ci`) | **13 / 13 verts** |
| Build frontend (`npm run build`) | **OK** |
| Scénario E2E API (`node scripts/e2e-mvp.mjs`, backend dev lancé) | **47 / 47 PASS** |

Le scénario E2E rejoue le parcours complet contre l'API réelle (`:8081`), de façon non destructive
(formateur = compte de démo, apprenants créés à la volée, slug de cours unique).

---

## 2. Détail du scénario E2E (`scripts/e2e-mvp.mjs`)

| # | Vérification | Attendu |
|---|---|---|
| AUTH-01 | `POST /auth/register` | 201 |
| AUTH-02 | `register` doublon | 409 |
| AUTH-03 | `register` payload invalide | 400 |
| AUTH-04 | `login` mauvais mot de passe | 401 |
| AUTH-05 | `login` | 200 + `accessToken` + `refreshToken` + `user` |
| AUTH-06 | `GET /auth/me` sans jeton | 401 |
| AUTH-07 | `GET /auth/me` avec jeton | 200, bon email |
| AUTH-08 | `PATCH /auth/me` `preferredLanguage=ar` | persiste (relecture) |
| AUTH-09 | `POST /auth/refresh` | rotation + **ancien refresh token rejeté (401)** |
| AUTH-10 | `POST /auth/logout` | le refresh token est révoqué (401 ensuite) |
| RBAC-01 | `LEARNER` → `POST /courses` | 403 |
| RBAC-02 | `LEARNER` → `GET /instructor/courses/{id}/results` | 403 |
| COURSE-01 | `POST /courses` (formateur) | 201 + `slug` auto |
| COURSE-02 | Poids contrôles/examen | 40 / 60 (CHECK BDD `= 100`) |
| COURSE-03 | `POST /courses/{id}/chapters` | 201 |
| COURSE-04 | `POST /chapters/{id}/contents` ×2 (TEXT) | 201 |
| QUIZ-01 | `POST /chapters/{id}/control-quiz` | 201 |
| QUIZ-02 | Question avec 1 option / 0 correcte | 400 |
| QUIZ-03 | Contrôle = 2 questions (SINGLE + TRUE_FALSE) | OK |
| QUIZ-04 | `POST /courses/{id}/final-exam` | 201 |
| QUIZ-05 | 2ᵉ contrôle sur le même chapitre | 409 |
| QUIZ-06 | Examen final tenté avant 100 % de progression | 403 |
| CAT-01 | Catalogue avant publication | cours **absent** |
| CAT-02 | `POST /courses/{id}/publish` | 200, `published=true` |
| CAT-03 | Catalogue après publication | cours **présent** |
| CAT-04 | `GET /courses/{slug}` (non inscrit) | contenus **masqués** |
| ENR-01 | `POST /courses/{id}/enroll` | 200/201 |
| ENR-02 | Double inscription | 409 |
| ENR-03 | `GET /courses/{slug}` (inscrit) | contenus **visibles** |
| PROG-01 | `complete` sur tous les contenus | progression **100 %** |
| QUIZ-07 | Tentative de contrôle (toutes bonnes réponses) | score **100** |
| QUIZ-08 | `GET /courses/{id}/grade` après 100 % + contrôle tenté | `finalExamUnlocked=true` |
| QUIZ-09 | Tentative d'examen final (toutes bonnes réponses) | score **100** |
| GRADE-01 | Note pondérée | `0,4·100 + 0,6·100 = 100` |
| GRADE-02 | Contrôle raté (autre apprenant) | score **0** |
| GRADE-03 | Note pondérée | `0,4·0 + 0,6·100 = 60` |
| GRADE-04 | Note 60 < seuil 70 | **aucun certificat** |
| GRADE-05 | `GET /courses/{id}/grade` cohérent | `certificateId=null`, `finalGrade=60` |
| CERT-01 | Note 100 ≥ seuil 70 | **certificat généré** |
| CERT-02 | `GET /certificates/me` | certificat listé (`serialNumber`) |
| CERT-03 | `GET /certificates/{id}/download` | 200, `%PDF`, `Content-Type: application/pdf`, `X-Content-Type-Options: nosniff` |
| CERT-04 | `GET /certificates/verify/{code}` (sans jeton) | `valid:true` |
| CERT-05 | `verify` code invalide | `valid:false` |
| RES-01 | `GET /instructor/courses/{id}/results` | 1 apprenant `certified=true`, `finalGrade=100` |
| AI-01 | `POST /ai/chat` apprenant **non inscrit** | 403 |
| AI-02 | `POST /ai/chat` apprenant inscrit (sans clé Anthropic réelle) | 200, `{degraded:true, reply:…}` — **jamais 500** |

---

## 3. Couverture des exigences MVP (« Must have »)

| Domaine | Exigences | État |
|---|---|---|
| Comptes & authentification | Inscription, connexion, JWT access + refresh (rotation, révocation), profil, langue préférée | ✅ AUTH-01→10 |
| Rôles (RBAC) | 3 rôles, mutations réservées `INSTRUCTOR`/`ADMIN`, contrôle objet « propriétaire » | ✅ RBAC-01/02 + revue Phase 5 |
| Formations | Cours → chapitres → contenus (TEXT/VIDEO/DOCUMENT), `slug`, publication/dépublication | ✅ COURSE-01→04, CAT-02 |
| Fichiers | Upload multipart borné (taille + MIME), download `attachment`/`nosniff` | ✅ tests `UploadSecurityTest` (Phase 5) |
| Catalogue | Liste publiés + recherche + pagination, détail, contenus masqués si non inscrit | ✅ CAT-01→04 |
| Inscription & progression | S'inscrire (cours publié, unicité), marquer un contenu vu, % de progression | ✅ ENR-01→03, PROG-01 |
| Évaluation | Contrôle par chapitre + examen final, correction auto, tentatives, verrouillage à 100 % | ✅ QUIZ-01→09 |
| Notation | Note pondérée **40 % contrôles + 60 % examen**, seuil de certification par cours | ✅ GRADE-01→05 |
| Certification | Génération auto ≥ seuil, n° de série, PDF, vérification publique par code | ✅ CERT-01→05 |
| Résultats formateur | Moyenne contrôles / examen / note finale / certifié, par apprenant | ✅ RES-01 |
| Multilingue (interface) | FR / EN / AR + RTL, persistance `localStorage` + serveur | ✅ validé au navigateur le 2026-09-10 (journal) ; back : AUTH-08 |
| Chatbot pédagogique | Contexte borné au cours, accès réservé aux inscrits, repli propre si IA indisponible | ✅ AI-01/02 ; réponse **live** en attente d'une clé Anthropic réelle |

---

## 4. Reste à vérifier au navigateur (avec l'utilisatrice)

Le scénario ci-dessus couvre toute la logique métier côté API. La partie **UI** se vérifie à la main
en suivant `docs/05-demo-soutenance.md`, sur une **base propre** (`flyway:clean` dev + redémarrage) :

- [ ] Parcours formateur complet dans `course-editor` / `quiz-editor` (création + upload de fichier réel).
- [ ] Parcours apprenant dans `course-detail` / `quiz-take` (inscription → progression → contrôle → examen → certificat).
- [ ] Pages `my-certificates` et `/verify/:code` (publique).
- [ ] Bascule de langue FR/EN/AR + RTL, aux largeurs 360 / 768 / 1280 px (responsive Phase 5).
- [ ] Widget chatbot sur la page cours (fil de discussion + message de repli).

---

## 5. Résiduel connu (hors périmètre MVP)

- Réponse **IA live** : nécessite une vraie `ANTHROPIC_API_KEY` (compte payant) — rien à changer côté code.
- Swagger UI (tâche 1.10) : springdoc pas encore compatible Spring Boot 4.
- `server.error.include-message: always` → `never` avant un déploiement public.
- Recherche catalogue : `pg_trgm` + index GIN à l'échelle.
- Sniffing réel du contenu des uploads (Apache Tika).

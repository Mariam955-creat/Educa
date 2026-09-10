# 05 — Scénario de démonstration (soutenance)

Déroulé pas à pas pour présenter **educa** en ~15 min. Chaque étape indique l'acteur, l'action et le point à souligner devant le jury.

> Dernière mise à jour : 2026-09-10.

---

## 0. Préparation (avant la soutenance)

| # | Action | Vérification |
|---|---|---|
| 0.1 | PostgreSQL local démarré (port 5432), bases `educa` et `educa_test` présentes | `psql -l` |
| 0.2 | `.env` présent à la racine avec `POSTGRES_PASSWORD` renseigné | `cat .env` |
| 0.3 | Backend lancé : `cd backend && ./mvnw spring-boot:run` | log « Started BackendApplication », Flyway `V1`+`V2`, API sur `:8081` |
| 0.4 | Frontend lancé : `cd frontend && npm start` | `http://localhost:4200` s'ouvre |
| 0.5 | (Optionnel) Base propre : rejouer le seed en repartant d'une base vide (`flyway:clean` dev puis redémarrage backend) | 3 comptes + cours « Introduction à Python » recréés |
| 0.6 | Onglets navigateur prêts : 1 fenêtre normale (formateur) + 1 fenêtre privée (apprenant) pour éviter les collisions de session | — |

**Comptes de démo** (mot de passe commun `password123`) :

| Rôle | Email |
|---|---|
| Administrateur | `admin@educa.dev` |
| Formateur | `formateur@educa.dev` |
| Apprenant | `apprenant@educa.dev` |

---

## 1. Introduction (1 min — sans écran ou sur le README)

- **Sujet** : plateforme e-learning, 3 rôles, parcours certifiant.
- **Différenciateurs** : interface multilingue FR/EN/AR avec RTL, chatbot pédagogique borné au cours (API Claude).
- **Stack** : Angular 19 + Spring Boot 4 (Java 25) + PostgreSQL, architecture *package-by-feature*, sécurité JWT/RBAC.
- Annoncer le plan de la démo : côté formateur → côté apprenant → certification → volets multilingue & IA → sécurité.

---

## 2. Côté formateur — créer et publier une formation (3 min)

| # | Acteur | Action | À souligner |
|---|---|---|---|
| 2.1 | Formateur | Se connecter (`formateur@educa.dev`) → espace formateur (`/instructor`) | Navigation filtrée par rôle (`roleGuard`) : l'apprenant ne voit pas ce menu |
| 2.2 | Formateur | « Nouveau cours » : titre *« Découverte du Web »*, langue FR, description courte → créer | `slug` généré automatiquement et unique ; poids contrôles/examen 40/60 imposés |
| 2.3 | Formateur | Ajouter un chapitre *« HTML de base »* (position 1) | Position unique par cours |
| 2.4 | Formateur | Ajouter 2 contenus : un `TEXT` (rédigé) et un `DOCUMENT` (upload d'un petit PDF) | Upload multipart ; taille bornée + liste blanche MIME (un `.html` serait refusé en `415`) |
| 2.5 | Formateur | Ajouter un **contrôle** au chapitre : 2 questions (choix unique + vrai/faux), marquer les bonnes réponses | Validation : ≥ 2 options, ≥ 1 correcte, `TRUE_FALSE` = 2 options |
| 2.6 | Formateur | Ajouter l'**examen final** du cours : 2 questions, `max_attempts` = 3 | 1 seul examen final par cours (contrainte base) |
| 2.7 | Formateur | **Publier** le cours | Tant que non publié : invisible au catalogue, inscription impossible |

> Repli si le temps manque : utiliser directement le cours seedé « Introduction à Python » (déjà chapitres + contrôle + examen).

---

## 3. Côté apprenant — s'inscrire, apprendre, progresser (3 min)

| # | Acteur | Action | À souligner |
|---|---|---|---|
| 3.1 | Apprenant (fenêtre privée) | Se connecter (`apprenant@educa.dev`) → **Catalogue** | Seuls les cours publiés apparaissent ; recherche avec debounce |
| 3.2 | Apprenant | Ouvrir « Introduction à Python » → contenus **masqués** | Détail public mais corps des contenus caché tant que non inscrit |
| 3.3 | Apprenant | **S'inscrire** → les contenus apparaissent | Unicité de l'inscription (une 2ᵉ tentative → 409) |
| 3.4 | Apprenant | Lire les contenus, cliquer « Marquer comme terminé » sur chacun | Barre de progression = contenus vus / total |
| 3.5 | Apprenant | Passer le **contrôle** du chapitre 1 | Correction automatique ; question juste = ensemble des options cochées == bonnes réponses ; score = points obtenus / total |
| 3.6 | Apprenant | Revenir à la page cours → progression **100 %** | Examen final déverrouillé seulement quand : 100 % des contenus vus **ET** tous les contrôles tentés |

---

## 4. Certification (2 min)

| # | Acteur | Action | À souligner |
|---|---|---|---|
| 4.1 | Apprenant | Passer l'**examen final** | Tentatives limitées (`max_attempts`) ; meilleure tentative retenue |
| 4.2 | Apprenant | Écran résultat : moyenne des contrôles, score examen, **note finale pondérée 40/60** | Contrôle non tenté = 0 ; si aucun contrôle → note = examen |
| 4.3 | Apprenant | Note ≥ seuil (défaut 70 %) → **certificat généré** automatiquement | `enrollment` passe à `COMPLETED` ; n° de série `EDUCA-AAAA-000001` |
| 4.4 | Apprenant | « Mes certificats » → **télécharger le PDF** | Rendu HTML → PDF (openhtmltopdf), A4 paysage, note + composantes |
| 4.5 | N'importe qui | Ouvrir la page publique `/verify/<code>` (sans être connecté) | Vérification publique par `verification_code` : nom, cours, note, date |
| 4.6 | Formateur | Espace formateur → « Résultats » du cours | Tableau par apprenant : moyenne contrôles / examen / note finale / certifié |

---

## 5. Volet multilingue (1,5 min)

| # | Action | À souligner |
|---|---|---|
| 5.1 | Sélecteur de langue (barre) : FR → EN | Libellés de navigation mis à jour **sans rechargement** |
| 5.2 | EN → AR | Interface en arabe **et** passage en **RTL** (barre et alignements inversés) |
| 5.3 | Recharger la page (F5) | Langue conservée → persistance `localStorage` |
| 5.4 | Se déconnecter / reconnecter | Langue conservée → persistance serveur (`PATCH /auth/me`, `preferredLanguage`) |

---

## 6. Volet chatbot pédagogique (1,5 min)

| # | Action | À souligner |
|---|---|---|
| 6.1 | Sur la page d'un cours **où l'apprenant est inscrit** : widget « Assistant du cours » | Visible seulement si inscrit (ou propriétaire / ADMIN) — sinon `403` |
| 6.2 | Poser une question dans le périmètre du cours | Contexte borné : titre + description + chapitres + contenus TEXT tronqués (`max-context-chars`) ; system prompt « répondre uniquement dans le périmètre du cours » |
| 6.3 | Expliquer le repli | Sans clé Anthropic réelle → réponse `{degraded:true}` propre, **jamais** d'erreur 500. Isolation derrière l'interface `AiAssistant` (`ClaudeAiAssistant` / `DisabledAiAssistant` selon la config) |

> Si une vraie `ANTHROPIC_API_KEY` est disponible le jour J : la mettre dans `.env`, redémarrer le backend, aucune autre modification.

---

## 7. Sécurité & qualité (1 min — parler, montrer si demandé)

- **Auth** : JWT access + refresh **opaque haché SHA-256**, rotation à chaque `/refresh`, révocation au `/logout`. BCrypt pour les mots de passe.
- **RBAC** : `@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")` sur les mutations **+** contrôle objet « propriétaire ou ADMIN » dans le service. Revue endpoint par endpoint (Phase 5) sans faille.
- **Uploads** : taille bornée (`413`), liste blanche MIME (`415`), téléchargement en `attachment` par défaut + `X-Content-Type-Options: nosniff`.
- **Erreurs** : `GlobalExceptionHandler` homogène (`400/401/403/404/409/413/415/500`).
- **Tests** : backend **23** (`./mvnw test`), frontend **13** (`npm run test:ci`). `/security-review` → **0 finding**.
- **Migrations** : Flyway append-only (`V1`, `V2`).

---

## 8. Conclusion (30 s)

- Périmètre MVP (« Must have ») **complet et démontré de bout en bout**.
- Extensions identifiées et cadrées : traductions du contenu pédagogique, persistance de l'historique de chat, génération de quiz par IA, recommandations, déploiement Docker/cloud.
- Renvoyer vers `docs/01`→`04` pour l'analyse, la conception et le journal de bord.

---

## Annexe — Plan B si un serveur ne démarre pas

| Problème | Action |
|---|---|
| Backend ne démarre pas (PostgreSQL) | Vérifier le service PostgreSQL + `POSTGRES_PASSWORD` dans `.env` ; `flyway:clean` puis redémarrer |
| Port 8081 occupé | Adapter `SERVER_PORT` dans `.env` + `core/api.ts` côté frontend |
| Frontend ne compile pas | `npm ci` dans `frontend/` ; vérifier Node ≥ 24.12 et Angular CLI 19.2 |
| Chatbot muet | Comportement attendu sans clé réelle : montrer la réponse `degraded` et expliquer l'architecture de repli |
| Démo « à froid » | Montrer les tests d'intégration (`QuizFlowTest` : parcours complet contrôle → examen → certificat → vérification) |

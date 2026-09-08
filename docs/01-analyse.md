# 01 — Analyse

Projet **educa** — plateforme e-learning (Projet de Fin d'Études).
Statut : *brouillon Phase 0, en attente de validation*.
Dernière mise à jour : 2026-09-08.

---

## 1. Contexte et problématique

La formation en ligne s'est généralisée mais les plateformes existantes sont soit lourdes et coûteuses
(LMS d'entreprise), soit trop rudimentaires (simple hébergement de vidéos sans suivi ni évaluation).
Il manque souvent : un **suivi de progression fiable**, une **évaluation certifiante automatisée**,
un **accompagnement de l'apprenant** en dehors des heures de cours, et une **accessibilité linguistique**
réelle (notamment pour les publics arabophones, avec le sens de lecture droite-à-gauche).

**Problématique** : comment concevoir une plateforme e-learning qui permette à un formateur de publier
des formations structurées et évaluées, à un apprenant de se former à son rythme dans sa langue avec un
accompagnement automatisé, et qui délivre une certification vérifiable à l'issue d'un parcours réussi ?

**Objectif du PFE** : livrer un MVP fonctionnel couvrant le parcours complet
*inscription → apprentissage → évaluation → certification*, enrichi d'un volet multilingue (FR/EN/AR)
et d'un chatbot pédagogique s'appuyant sur une API d'IA générative.

---

## 2. Personas

### 2.1 Amina — Apprenante (persona principal)
- **Profil** : 24 ans, en reconversion, se forme le soir sur son ordinateur portable et parfois sur mobile. Langue préférée : arabe, comprend le français.
- **Objectifs** : monter en compétences sur un sujet précis, obtenir une preuve (certificat) à mettre sur son CV, avancer à son rythme.
- **Frustrations** : cours en langue étrangère uniquement, pas de repère sur ce qu'il reste à faire, personne à qui poser une question quand elle bloque à 22h, quiz sans correction expliquée.
- **Cas d'usage typiques** : parcourir le catalogue, s'inscrire à une formation, reprendre un chapitre là où elle s'était arrêtée, passer un quiz, poser une question au chatbot sur un point du cours, télécharger son certificat.

### 2.2 Karim — Formateur
- **Profil** : 38 ans, expert métier, crée du contenu pédagogique en plus de son activité principale.
- **Objectifs** : publier une formation structurée sans compétence technique, réutiliser ses supports (vidéos, PDF), évaluer les acquis automatiquement, voir qui a réussi.
- **Frustrations** : outils d'édition complexes, impossibilité d'organiser le contenu en modules, correction manuelle des évaluations, pas de visibilité sur les résultats.
- **Cas d'usage typiques** : créer un cours, ajouter des chapitres ordonnés, téléverser des contenus, construire un quiz (questions à choix, vrai/faux), définir le seuil de réussite, consulter la liste des inscrits et leurs scores.

### 2.3 Sonia — Administratrice
- **Profil** : 30 ans, responsable de la plateforme pour un organisme de formation.
- **Objectifs** : garder le contrôle sur les comptes et les rôles, s'assurer de la qualité du catalogue, vérifier l'authenticité des certificats délivrés.
- **Frustrations** : pas de gestion fine des droits, contenus publiés sans contrôle, certificats non vérifiables.
- **Cas d'usage typiques** : lister et gérer les utilisateurs, attribuer/retirer le rôle formateur, activer/désactiver un compte, superviser le catalogue, consulter le registre des certificats et leur code de vérification.

---

## 3. User stories

### Apprenant
- US-A1 : En tant qu'apprenant, je veux créer un compte et me connecter, afin d'accéder à mon espace personnel.
- US-A2 : En tant qu'apprenant, je veux parcourir et rechercher les formations du catalogue, afin de trouver celle qui correspond à mon besoin.
- US-A3 : En tant qu'apprenant, je veux m'inscrire à une formation, afin d'y accéder et d'être suivi.
- US-A4 : En tant qu'apprenant, je veux consulter les chapitres et contenus d'un cours, afin d'apprendre à mon rythme.
- US-A5 : En tant qu'apprenant, je veux que ma progression soit enregistrée, afin de reprendre là où je me suis arrêté.
- US-A6 : En tant qu'apprenant, je veux passer un quiz et voir mon score immédiatement, afin de mesurer mes acquis.
- US-A7 : En tant qu'apprenant, je veux obtenir un certificat téléchargeable quand je réussis, afin de valoriser ma formation.
- US-A8 : En tant qu'apprenant, je veux changer la langue de l'interface (FR/EN/AR), afin d'utiliser la plateforme dans ma langue.
- US-A9 : En tant qu'apprenant, je veux poser une question à un assistant à propos du cours que je consulte, afin d'être débloqué sans attendre.

### Formateur
- US-F1 : En tant que formateur, je veux créer, modifier et supprimer un cours, afin de gérer mon offre de formation.
- US-F2 : En tant que formateur, je veux organiser un cours en chapitres ordonnés, afin de structurer la progression.
- US-F3 : En tant que formateur, je veux téléverser et associer des contenus (vidéo, document, texte) à un chapitre, afin de fournir le support pédagogique.
- US-F4 : En tant que formateur, je veux créer un contrôle par chapitre et un examen final, avec questions et réponses, et régler la pondération et le seuil de réussite du cours, afin d'évaluer et de certifier les apprenants.
- US-F5 : En tant que formateur, je veux publier ou dépublier un cours, afin de contrôler sa visibilité dans le catalogue.
- US-F6 : En tant que formateur, je veux consulter la liste des inscrits et leurs résultats, afin de suivre l'efficacité de ma formation.
- US-F7 : En tant que formateur, je veux fournir les traductions du titre et de la description d'un cours, afin de le rendre accessible en FR/EN/AR.

### Administrateur
- US-D1 : En tant qu'administrateur, je veux lister et rechercher les utilisateurs, afin de gérer la communauté.
- US-D2 : En tant qu'administrateur, je veux attribuer ou retirer des rôles, afin de contrôler qui peut créer des formations.
- US-D3 : En tant qu'administrateur, je veux activer/désactiver un compte, afin de gérer les accès.
- US-D4 : En tant qu'administrateur, je veux consulter le registre des certificats émis, afin d'en garantir l'authenticité.
- US-D5 : En tant qu'administrateur, je veux gérer la liste des langues actives, afin de piloter le périmètre multilingue.

---

## 4. Exigences fonctionnelles

> Numérotation traçable (EF-xx). Priorité : **M** = Must have (MVP), **S** = Should have, **C** = Could have.

### Authentification & comptes
- EF-01 (M) : Inscription par email + mot de passe.
- EF-02 (M) : Connexion, déconnexion, jeton d'accès JWT + jeton de rafraîchissement.
- EF-03 (M) : Consultation et modification de son propre profil.
- EF-04 (M) : Trois rôles — `LEARNER`, `INSTRUCTOR`, `ADMIN` — conditionnant les accès.
- EF-05 (S) : Réinitialisation de mot de passe par email.

### Catalogue & formations
- EF-06 (M) : CRUD d'un cours par son formateur propriétaire.
- EF-07 (M) : CRUD des chapitres d'un cours, avec ordre.
- EF-08 (M) : Ajout de contenus à un chapitre (types : `VIDEO`, `DOCUMENT`, `TEXT`).
- EF-09 (M) : Téléversement de fichiers (vidéo/document) vers un stockage objet.
- EF-10 (M) : Publication / dépublication d'un cours.
- EF-11 (M) : Consultation du catalogue des cours publiés (liste + détail) par un apprenant.
- EF-12 (S) : Recherche et filtrage du catalogue (mot-clé, langue).

### Inscription & progression
- EF-13 (M) : Inscription d'un apprenant à une formation.
- EF-14 (M) : Enregistrement de la progression (contenu/chapitre marqué comme vu).
- EF-15 (M) : Reprise du cours au dernier point atteint.
- EF-16 (S) : Tableau de bord apprenant : formations en cours, % d'avancement.

### Évaluation & certification
- EF-17 (M) : Le formateur crée, modifie et supprime les quiz, questions et réponses de ses cours ; types de questions `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE`.
- EF-18 (M) : Deux natures de quiz — un **contrôle** par chapitre et un **examen final** par cours (couvrant tous les chapitres).
- EF-19 (M) : Passage d'un quiz par l'apprenant, correction automatique, score immédiat ; chaque tentative est enregistrée (score, date).
- EF-20 (M) : Contrôles à tentatives **illimitées** ; examen final à tentatives **limitées** et paramétrables par le formateur (défaut 3). Score retenu par quiz = meilleure tentative.
- EF-21 (M) : L'examen final n'est **déverrouillé qu'à 100 % de progression** du cours (tous les contenus consultés et tous les contrôles tentés).
- EF-22 (M) : **Note finale pondérée** = 40 % (moyenne des meilleurs scores de contrôle) + 60 % (meilleur score d'examen final) ; pondération et seuil de réussite **configurables par cours** (défaut 70 %).
- EF-23 (M) : Génération automatique d'un certificat PDF dès que la note finale atteint le seuil, avec numéro de série et code de vérification uniques ; le certificat affiche le détail (moyenne des contrôles, score d'examen, note finale).
- EF-24 (M) : Téléchargement du certificat par l'apprenant ; consultation par l'apprenant de sa note finale et du détail par chapitre.
- EF-25 (S) : Page publique de vérification d'un certificat par son code.

### Multilingue
- EF-26 (M) : Interface disponible en FR, EN, AR ; sélection persistée par l'utilisateur.
- EF-27 (M) : Affichage RTL correct pour l'arabe.
- EF-28 (S) : Traductions du contenu pédagogique (titre/description de cours, énoncés de chapitres) stockées par langue.
- EF-29 (C) : Traduction assistée par IA des contenus.

### IA
- EF-30 (M) : Chatbot pédagogique — l'apprenant pose une question ; la réponse est produite par une API LLM externe, avec pour contexte le cours en cours de consultation.
- EF-31 (M) : Comportement de repli explicite si l'API IA est indisponible (message clair, pas d'erreur brute).
- EF-32 (S) : Historique de conversation par cours.
- EF-33 (S) : Génération assistée de quiz à partir du contenu d'un chapitre.
- EF-34 (C) : Recommandation de formations selon le profil / l'historique.

### Administration
- EF-35 (M) : Liste et recherche des utilisateurs.
- EF-36 (M) : Attribution / retrait de rôle.
- EF-37 (M) : Activation / désactivation de compte.
- EF-38 (S) : Registre des certificats émis.
- EF-39 (S) : Gestion des langues actives.

---

## 5. Exigences non fonctionnelles

- ENF-01 — **Sécurité** : mots de passe hachés (BCrypt), authentification par JWT signé, HTTPS en production, contrôle d'accès par rôle sur chaque endpoint, validation systématique des entrées, protection contre les injections (requêtes paramétrées / JPA), secrets hors du code (variables d'environnement).
- ENF-02 — **Performance** : réponse API < 400 ms en médiane pour les lectures courantes (catalogue, détail cours) sur un jeu de données de démonstration ; pagination obligatoire sur les listes.
- ENF-03 — **Accessibilité** : contraste suffisant, navigation clavier, libellés ARIA sur les composants interactifs, support RTL complet.
- ENF-04 — **Multilingue** : ajout d'une langue d'interface sans changement de code (fichiers de ressources) ; encodage UTF-8 de bout en bout.
- ENF-05 — **Portabilité / repro** : migrations de base versionnées (Flyway) rejouables ; procédure d'installation documentée (PostgreSQL local + pgAdmin ; `docker-compose` reporté).
- ENF-06 — **Maintenabilité** : architecture en couches, séparation par domaine, couverture de tests sur les parcours critiques (auth, quiz, certification).
- ENF-07 — **Observabilité** : logs structurés côté backend, gestion d'erreurs centralisée renvoyant un format d'erreur homogène.
- ENF-08 — **Coût / dépendance IA** : appels IA encapsulés dans un seul module, quota/longueur de contexte bornés, possibilité de désactiver la fonctionnalité par configuration.
- ENF-09 — **Scalabilité (raisonnable PFE)** : backend sans état (JWT), fichiers déportés sur stockage objet, base relationnelle unique — suffisant pour la soutenance, pas d'objectif haute disponibilité.
- ENF-10 — **RGPD (sensibilisation)** : données personnelles limitées au nécessaire (email, nom), suppression de compte possible (S).

---

## 6. Contraintes du projet

- **Délai** : calendrier PFE — quelques mois, une seule personne au développement.
- **Équipe** : solo, d'où la priorité au MVP et le recours à Claude Code pour accélérer.
- **Contexte académique** : le livrable inclut un mémoire et une soutenance ; la documentation (`docs/`) fait partie de l'évaluation.
- **Techniques** : backend imposé Java / Spring Boot (squelette déjà généré) ; frontend Angular ; base PostgreSQL ; dépendance à une API LLM tierce payante (budget à surveiller).
- **Données** : pas de données réelles d'utilisateurs ; un jeu de données de démonstration sera constitué pour la soutenance.

---

## 7. Risques et mitigation

| # | Risque | Impact | Probabilité | Mitigation |
|---|---|---|---|---|
| R1 | Dépendance à l'API IA externe (coût, quota, indisponibilité, latence) | Élevé | Moyenne | Module IA isolé et interchangeable ; timeout + réponse de repli (EF-31) ; longueur de contexte bornée ; fonctionnalité désactivable par config |
| R2 | Complexité du multilingue, surtout RTL arabe | Moyen | Élevée | Traiter l'i18n tôt (Phase 4 mais préparé dès le layout) ; librairie éprouvée (`@ngx-translate`) ; séparer i18n d'interface (fichiers) et traductions de contenu (BDD) |
| R3 | Périmètre trop large pour un PFE solo | Élevé | Élevée | MVP « Must have » verrouillé ; Should/Could traités seulement si le MVP est terminé et stable |
| R4 | Gestion des fichiers volumineux (vidéos) | Moyen | Moyenne | Module `storage` abstrait dès la Phase 2 (système de fichiers local en dev, S3-compatible en cible) ; limite de taille ; pas de transcodage (hors périmètre) |
| R5 | Sécurité mal maîtrisée (RBAC, fuite de secrets) | Élevé | Moyenne | Matrice de permissions explicite (§conception) ; revue de sécurité dédiée en Phase 5 ; secrets en variables d'environnement |
| R6 | Génération PDF des certificats (mise en page, polices arabes) | Moyen | Moyenne | Choisir la lib en Phase 3 avec un prototype ; template HTML simple ; police compatible latin + arabe |
| R7 | Dérive de planning / dette documentaire | Moyen | Moyenne | Mise à jour de la doc imposée en fin de chaque phase (règle §4 du brief) ; `CLAUDE.md` + journal |
| R8 | Divergence stack vs brief initial (Next.js/Node prévus, Angular/Spring retenus) | Faible | Avérée | Décision actée et tracée dans `02-conception.md` ; brief mis à jour |

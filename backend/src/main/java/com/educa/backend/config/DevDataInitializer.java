package com.educa.backend.config;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.certificate.CertificateService;
import com.educa.backend.course.Chapter;
import com.educa.backend.course.Content;
import com.educa.backend.course.ContentType;
import com.educa.backend.course.Course;
import com.educa.backend.course.CourseRepository;
import com.educa.backend.enrollment.EnrollmentService;
import com.educa.backend.payment.PaymentService;
import com.educa.backend.quiz.AnswerOption;
import com.educa.backend.quiz.Question;
import com.educa.backend.quiz.QuestionType;
import com.educa.backend.quiz.Quiz;
import com.educa.backend.quiz.QuizAttemptService;
import com.educa.backend.quiz.QuizRepository;
import com.educa.backend.quiz.QuizType;
import com.educa.backend.quiz.dto.AttemptSubmission;
import com.educa.backend.quiz.dto.AttemptSubmission.AnswerSubmission;
import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;

/**
 * Données de démonstration au démarrage (profil {@code dev} uniquement).
 *
 * <p>Contenu seedé :
 * <ul>
 *   <li>4 comptes (mot de passe commun {@code password123}) : admin, formateur, apprenant, et un
 *       apprenant « diplômé » qui possède déjà un certificat ;</li>
 *   <li>2 cours publiés et payants du formateur de démo (Python 29,99 €, Git 19,99 €), chacun avec chapitres, contenus, un contrôle et un
 *       examen final ;</li>
 *   <li>4 autres cours publiés et payants (SQL 34,99 €, algorithmique 24,99 €, cybersécurité 14,99 €, Java 39,99 €),
 *       3 chapitres chacun, un contrôle par chapitre et un examen final ;</li>
 *   <li>le parcours complet du compte « diplômé » sur le 2ᵉ cours (achat enregistré → inscription → 100 % → contrôle →
 *       examen final réussi → certificat émis), joué via les services réels.</li>
 * </ul>
 */
@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private static final String DEMO_PASSWORD = "password123";
    private static final String PYTHON_COURSE_SLUG = "introduction-a-python";
    private static final String GIT_COURSE_SLUG = "les-bases-de-git";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final EnrollmentService enrollmentService;
    private final QuizAttemptService quizAttemptService;
    private final CertificateService certificateService;
    private final PaymentService paymentService;

    public DevDataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder, CourseRepository courseRepository,
                              QuizRepository quizRepository, EnrollmentService enrollmentService,
                              QuizAttemptService quizAttemptService, CertificateService certificateService,
                              PaymentService paymentService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.enrollmentService = enrollmentService;
        this.quizAttemptService = quizAttemptService;
        this.certificateService = certificateService;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser("admin@educa.dev", "Admin Démo", RoleName.ADMIN);
        User instructor = seedUser("formateur@educa.dev", "Karim Formateur", RoleName.INSTRUCTOR);
        seedUser("apprenant@educa.dev", "Amina Apprenante", RoleName.LEARNER);
        User graduate = seedUser("diplome@educa.dev", "Sara Diplômée", RoleName.LEARNER);

        seedPythonCourse(instructor);
        Course gitCourse = seedGitCourse(instructor);
        catalogCourses().forEach(spec -> seedCatalogCourse(instructor, spec));

        seedCertifiedLearner(graduate, gitCourse);
    }

    private User seedUser(String email, String fullName, RoleName roleName) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Rôle absent : " + roleName));
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            user.setFullName(fullName);
            user.setPreferredLanguage("fr");
            user.addRole(role);
            User saved = userRepository.save(user);
            log.info("Compte de démo : {} ({}) / {}", email, roleName, DEMO_PASSWORD);
            return saved;
        });
    }

    // ---------- Cours 1 : Introduction à Python ----------

    private void seedPythonCourse(User instructor) {
        if (courseRepository.existsBySlug(PYTHON_COURSE_SLUG)) {
            seedPythonQuizzes();
            return;
        }
        Course course = new Course();
        course.setInstructorId(instructor.getId());
        course.setTitle("Introduction à Python");
        course.setSlug(PYTHON_COURSE_SLUG);
        course.setDescription("Un premier pas dans la programmation avec Python : installation, variables, types de base.");
        course.setLanguage("fr");
        course.setPublished(true);
        course.setPrice(BigDecimal.valueOf(29.99));

        Chapter c1 = new Chapter();
        c1.setTitle("Prise en main");
        c1.setPosition(1);
        c1.addContent(textContent("Qu'est-ce que Python ?", 1,
                "Python est un langage de programmation interprété, lisible et très répandu."));
        c1.addContent(textContent("Installer Python", 2,
                "Téléchargez la dernière version depuis python.org et vérifiez avec `python --version`."));
        course.addChapter(c1);

        Chapter c2 = new Chapter();
        c2.setTitle("Les variables");
        c2.setPosition(2);
        c2.addContent(textContent("Déclarer une variable", 1,
                "En Python : `x = 5`. Pas besoin de préciser le type."));
        c2.addContent(textContent("Types de base", 2,
                "int, float, str, bool, list, tuple, dict."));
        course.addChapter(c2);

        courseRepository.save(course);
        log.info("Cours de démo créé : « {} » ({} chapitres)", course.getTitle(), course.getChapters().size());
        seedPythonQuizzes();
    }

    /** Ajoute un contrôle au 1er chapitre + un examen final au cours Python, s'ils manquent. */
    private void seedPythonQuizzes() {
        Course course = courseRepository.findBySlug(PYTHON_COURSE_SLUG).orElse(null);
        if (course == null || course.getChapters().isEmpty()) {
            return;
        }
        Chapter firstChapter = course.getChapters().get(0);

        if (!quizRepository.existsByChapterIdAndType(firstChapter.getId(), QuizType.CONTROL)) {
            Quiz control = new Quiz();
            control.setType(QuizType.CONTROL);
            control.setCourseId(course.getId());
            control.setChapterId(firstChapter.getId());
            control.setTitle("Contrôle — Prise en main");
            control.setPassThreshold(50);
            control.addQuestion(singleChoice("Python est un langage…", 1,
                    "interprété", true, "compilé uniquement", false, "sans variables", false));
            control.addQuestion(trueFalse("La commande `python --version` affiche la version installée.", 2, true));
            quizRepository.save(control);
        }

        if (!quizRepository.existsByCourseIdAndType(course.getId(), QuizType.FINAL_EXAM)) {
            Quiz exam = new Quiz();
            exam.setType(QuizType.FINAL_EXAM);
            exam.setCourseId(course.getId());
            exam.setTitle("Examen final — Introduction à Python");
            exam.setPassThreshold(50);
            exam.setMaxAttempts(3);
            exam.addQuestion(singleChoice("Quelle affectation est correcte en Python ?", 1,
                    "x = 5", true, "int x = 5;", false, "5 =: x", false));
            exam.addQuestion(trueFalse("Une liste et un tuple sont strictement identiques.", 2, false));
            quizRepository.save(exam);
        }
        log.info("Quiz de démo prêts pour « {} »", course.getTitle());
    }

    // ---------- Cours 2 : Les bases de Git ----------

    private Course seedGitCourse(User instructor) {
        Course existing = courseRepository.findBySlug(GIT_COURSE_SLUG).orElse(null);
        if (existing != null) {
            seedGitQuizzes(existing);
            return existing;
        }
        Course course = new Course();
        course.setInstructorId(instructor.getId());
        course.setTitle("Les bases de Git");
        course.setSlug(GIT_COURSE_SLUG);
        course.setDescription("Comprendre le suivi de versions et les commandes essentielles de Git au quotidien.");
        course.setLanguage("fr");
        course.setPublished(true);
        course.setPrice(BigDecimal.valueOf(19.99));

        Chapter c1 = new Chapter();
        c1.setTitle("Démarrer avec Git");
        c1.setPosition(1);
        c1.addContent(textContent("À quoi sert un gestionnaire de versions ?", 1,
                "Git enregistre l'historique d'un projet : chaque `commit` est un instantané auquel on peut revenir."));
        c1.addContent(textContent("Installer et se configurer", 2,
                "Après installation : `git config --global user.name` et `user.email` identifient l'auteur des commits."));
        course.addChapter(c1);

        Chapter c2 = new Chapter();
        c2.setTitle("Le cycle de travail");
        c2.setPosition(2);
        c2.addContent(textContent("add, commit, status", 1,
                "`git add` place des fichiers dans l'index, `git commit` les enregistre, `git status` montre l'état courant."));
        c2.addContent(textContent("Travailler avec des branches", 2,
                "`git branch` liste les branches, `git switch -c` en crée une, `git merge` fusionne le travail."));
        course.addChapter(c2);

        courseRepository.save(course);
        log.info("Cours de démo créé : « {} » ({} chapitres)", course.getTitle(), course.getChapters().size());
        seedGitQuizzes(course);
        return course;
    }

    /** Ajoute un contrôle au 1er chapitre + un examen final au cours Git, s'ils manquent. */
    private void seedGitQuizzes(Course course) {
        if (course.getChapters().isEmpty()) {
            return;
        }
        Chapter firstChapter = course.getChapters().get(0);

        if (!quizRepository.existsByChapterIdAndType(firstChapter.getId(), QuizType.CONTROL)) {
            Quiz control = new Quiz();
            control.setType(QuizType.CONTROL);
            control.setCourseId(course.getId());
            control.setChapterId(firstChapter.getId());
            control.setTitle("Contrôle — Démarrer avec Git");
            control.setPassThreshold(50);
            control.addQuestion(singleChoice("Un `commit` correspond à…", 1,
                    "un instantané de l'état du projet", true,
                    "l'envoi du code au serveur distant", false,
                    "la suppression de l'historique", false));
            control.addQuestion(trueFalse("`git config user.email` sert à identifier l'auteur des commits.", 2, true));
            quizRepository.save(control);
        }

        if (!quizRepository.existsByCourseIdAndType(course.getId(), QuizType.FINAL_EXAM)) {
            Quiz exam = new Quiz();
            exam.setType(QuizType.FINAL_EXAM);
            exam.setCourseId(course.getId());
            exam.setTitle("Examen final — Les bases de Git");
            exam.setPassThreshold(50);
            exam.setMaxAttempts(3);
            exam.addQuestion(singleChoice("Quelle commande enregistre les fichiers de l'index dans l'historique ?", 1,
                    "git commit", true, "git status", false, "git add", false));
            exam.addQuestion(trueFalse("`git switch -c ma-branche` crée puis bascule sur une nouvelle branche.", 2, true));
            quizRepository.save(exam);
        }
        log.info("Quiz de démo prêts pour « {} »", course.getTitle());
    }

    // ---------- Cours 3 à 6 : catalogue enrichi (un contrôle par chapitre + examen final) ----------

    private record ChapterSpec(String title, List<Content> contents, List<Question> control) {
    }

    private record CourseSpec(String slug, String title, String description, String price,
                              List<ChapterSpec> chapters, List<Question> finalExam) {
    }

    /**
     * Crée le cours décrit par {@code spec} s'il n'existe pas, puis ajoute les quiz manquants
     * (contrôle de chaque chapitre, examen final). Idempotent.
     */
    private void seedCatalogCourse(User instructor, CourseSpec spec) {
        Course course = courseRepository.findBySlug(spec.slug()).orElse(null);
        if (course == null) {
            course = new Course();
            course.setInstructorId(instructor.getId());
            course.setTitle(spec.title());
            course.setSlug(spec.slug());
            course.setDescription(spec.description());
            course.setLanguage("fr");
            course.setPublished(true);
            course.setPrice(new BigDecimal(spec.price()));
            int position = 1;
            for (ChapterSpec chapterSpec : spec.chapters()) {
                Chapter chapter = new Chapter();
                chapter.setTitle(chapterSpec.title());
                chapter.setPosition(position++);
                chapterSpec.contents().forEach(chapter::addContent);
                course.addChapter(chapter);
            }
            course = courseRepository.save(course);
            log.info("Cours de démo créé : « {} » ({} chapitres, {} €)",
                    course.getTitle(), course.getChapters().size(), spec.price());
        }

        List<Chapter> chapters = course.getChapters();
        for (int i = 0; i < chapters.size() && i < spec.chapters().size(); i++) {
            Chapter chapter = chapters.get(i);
            if (!quizRepository.existsByChapterIdAndType(chapter.getId(), QuizType.CONTROL)) {
                Quiz control = new Quiz();
                control.setType(QuizType.CONTROL);
                control.setCourseId(course.getId());
                control.setChapterId(chapter.getId());
                control.setTitle("Contrôle — " + chapter.getTitle());
                control.setPassThreshold(50);
                spec.chapters().get(i).control().forEach(control::addQuestion);
                quizRepository.save(control);
            }
        }
        if (!quizRepository.existsByCourseIdAndType(course.getId(), QuizType.FINAL_EXAM)) {
            Quiz exam = new Quiz();
            exam.setType(QuizType.FINAL_EXAM);
            exam.setCourseId(course.getId());
            exam.setTitle("Examen final — " + course.getTitle());
            exam.setPassThreshold(50);
            exam.setMaxAttempts(3);
            spec.finalExam().forEach(exam::addQuestion);
            quizRepository.save(exam);
        }
    }

    private static List<CourseSpec> catalogCourses() {
        return List.of(sqlCourse(), algorithmsCourse(), securityCourse(), javaCourse());
    }

    private static CourseSpec sqlCourse() {
        return new CourseSpec("sql-et-bases-de-donnees-relationnelles", "SQL et bases de données relationnelles",
                "Modéliser des données en tables et les interroger avec SQL : SELECT, filtres, jointures et agrégations.",
                "34.99",
                List.of(
                        new ChapterSpec("Le modèle relationnel", List.of(
                                textContent("Tables, lignes et colonnes", 1,
                                        "Une base relationnelle range les données dans des tables : chaque ligne est un enregistrement, chaque colonne un attribut typé (texte, nombre, date…)."),
                                textContent("Clés primaires et étrangères", 2,
                                        "La clé primaire identifie une ligne de façon unique ; une clé étrangère référence la clé primaire d'une autre table et relie ainsi les données.")),
                                List.of(singleChoice("Une clé primaire…", 1,
                                                "identifie chaque ligne de façon unique", true,
                                                "peut contenir des doublons", false,
                                                "relie deux bases de données différentes", false),
                                        trueFalse("Une clé étrangère référence la clé primaire d'une autre table.", 2, true))),
                        new ChapterSpec("Interroger avec SELECT", List.of(
                                textContent("Sélectionner et filtrer", 1,
                                        "`SELECT nom, ville FROM clients WHERE ville = 'Liège';` renvoie les colonnes demandées des seules lignes qui respectent la condition."),
                                textContent("Trier et limiter", 2,
                                        "`ORDER BY` trie le résultat (`ASC` ou `DESC`) et `LIMIT` restreint le nombre de lignes renvoyées.")),
                                List.of(singleChoice("Quelle clause filtre les lignes d'une requête ?", 1,
                                                "WHERE", true, "ORDER BY", false, "FROM", false),
                                        trueFalse("`ORDER BY prix DESC` trie du plus cher au moins cher.", 2, true))),
                        new ChapterSpec("Jointures et agrégations", List.of(
                                textContent("Les jointures", 1,
                                        "`INNER JOIN` combine les lignes de deux tables dont les clés correspondent : `SELECT * FROM commandes c JOIN clients k ON c.client_id = k.id;`"),
                                textContent("GROUP BY et fonctions d'agrégat", 2,
                                        "`COUNT`, `SUM`, `AVG`, `MIN` et `MAX` résument des groupes de lignes formés par `GROUP BY` ; `HAVING` filtre ces groupes.")),
                                List.of(singleChoice("Quelle fonction compte le nombre de lignes ?", 1,
                                                "COUNT", true, "SUM", false, "AVG", false),
                                        trueFalse("`HAVING` filtre des groupes après un `GROUP BY`.", 2, true)))),
                List.of(singleChoice("Quelle requête renvoie les clients de Namur ?", 1,
                                "SELECT * FROM clients WHERE ville = 'Namur';", true,
                                "SELECT clients WHERE Namur;", false,
                                "GET * FROM clients = 'Namur';", false),
                        singleChoice("Pour relier deux tables par leurs clés, on utilise…", 2,
                                "une jointure (JOIN)", true, "un ORDER BY", false, "un LIMIT", false),
                        trueFalse("`AVG` calcule la moyenne d'une colonne.", 3, true)));
    }

    private static CourseSpec algorithmsCourse() {
        return new CourseSpec("algorithmique-les-fondamentaux", "Algorithmique : les fondamentaux",
                "Apprendre à raisonner comme un programmeur : variables, conditions, boucles, puis premiers algorithmes de recherche et de tri.",
                "24.99",
                List.of(
                        new ChapterSpec("Penser un algorithme", List.of(
                                textContent("Qu'est-ce qu'un algorithme ?", 1,
                                        "Un algorithme est une suite finie d'instructions précises qui transforme des données d'entrée en un résultat, comme une recette de cuisine."),
                                textContent("Variables et affectation", 2,
                                        "Une variable est une case nommée qui contient une valeur. `total ← total + prix` lit l'ancienne valeur, calcule, puis la remplace.")),
                                List.of(singleChoice("Un algorithme est…", 1,
                                                "une suite finie d'instructions précises", true,
                                                "un langage de programmation", false,
                                                "un composant matériel de l'ordinateur", false),
                                        trueFalse("Une affectation remplace la valeur précédente de la variable.", 2, true))),
                        new ChapterSpec("Conditions et boucles", List.of(
                                textContent("Les conditions", 1,
                                        "`SI note ≥ 10 ALORS afficher « réussi » SINON afficher « échoué »` : le programme choisit un chemin selon qu'une condition est vraie ou fausse."),
                                textContent("Les boucles", 2,
                                        "`POUR i DE 1 À 10` répète un bloc un nombre connu de fois ; `TANT QUE` répète tant qu'une condition reste vraie — attention aux boucles infinies.")),
                                List.of(singleChoice("Quelle structure répète un bloc tant qu'une condition est vraie ?", 1,
                                                "TANT QUE", true, "SI … ALORS", false, "une affectation", false),
                                        trueFalse("Une boucle POUR s'utilise quand le nombre de répétitions est connu d'avance.", 2, true))),
                        new ChapterSpec("Rechercher et trier", List.of(
                                textContent("Recherche séquentielle et dichotomique", 1,
                                        "La recherche séquentielle parcourt les éléments un à un ; la recherche dichotomique, sur une liste triée, divise l'intervalle de recherche par deux à chaque étape."),
                                textContent("Le tri par sélection", 2,
                                        "On cherche le plus petit élément, on l'échange avec le premier, puis on recommence sur le reste de la liste jusqu'à ce qu'elle soit triée.")),
                                List.of(singleChoice("La recherche dichotomique exige une liste…", 1,
                                                "triée", true, "vide", false, "de nombres pairs", false),
                                        trueFalse("Le tri par sélection place à chaque étape le plus petit élément restant à sa place.", 2, true)))),
                List.of(singleChoice("Sur 1 000 éléments triés, la recherche dichotomique fait au plus environ…", 1,
                                "10 comparaisons", true, "500 comparaisons", false, "1 000 comparaisons", false),
                        singleChoice("Que risque une boucle TANT QUE dont la condition ne devient jamais fausse ?", 2,
                                "de tourner indéfiniment", true, "de s'arrêter immédiatement", false, "de trier la liste", false),
                        trueFalse("Un algorithme doit se terminer après un nombre fini d'étapes.", 3, true)));
    }

    private static CourseSpec securityCourse() {
        return new CourseSpec("cybersecurite-les-bons-reflexes", "Cybersécurité : les bons réflexes",
                "Protéger ses comptes et ses données au quotidien : mots de passe, hameçonnage, mises à jour et sauvegardes.",
                "14.99",
                List.of(
                        new ChapterSpec("Mots de passe et authentification", List.of(
                                textContent("Un bon mot de passe", 1,
                                        "Long (au moins 12 caractères), unique pour chaque site et difficile à deviner : une phrase de passe est plus sûre qu'un mot court et complexe."),
                                textContent("Gestionnaire et double authentification", 2,
                                        "Un gestionnaire de mots de passe retient pour vous des mots de passe uniques ; la double authentification (2FA) ajoute un code à usage unique.")),
                                List.of(singleChoice("Quel est le meilleur choix ?", 1,
                                                "un mot de passe long et unique par site", true,
                                                "le même mot de passe partout", false,
                                                "sa date de naissance", false),
                                        trueFalse("La double authentification protège le compte même si le mot de passe a fuité.", 2, true))),
                        new ChapterSpec("Reconnaître l'hameçonnage", List.of(
                                textContent("Les signes d'alerte", 1,
                                        "Urgence inhabituelle, expéditeur approximatif, fautes, lien qui ne mène pas au site officiel : autant d'indices d'un message d'hameçonnage (phishing)."),
                                textContent("Les bons gestes", 2,
                                        "Ne cliquez pas : rendez-vous vous-même sur le site officiel, ne communiquez jamais un mot de passe par e-mail et signalez le message.")),
                                List.of(singleChoice("Un e-mail vous demande de « confirmer votre mot de passe sous 24 h ». Que faire ?", 1,
                                                "ne pas cliquer et passer par le site officiel", true,
                                                "répondre avec son mot de passe", false,
                                                "cliquer vite pour éviter la suspension", false),
                                        trueFalse("Une banque demande régulièrement votre mot de passe par e-mail.", 2, false))),
                        new ChapterSpec("Mises à jour et sauvegardes", List.of(
                                textContent("Mettre à jour", 1,
                                        "Les mises à jour corrigent des failles de sécurité connues : activez les mises à jour automatiques du système et des applications."),
                                textContent("La règle 3-2-1", 2,
                                        "Trois copies des données, sur deux supports différents, dont une hors site : une sauvegarde protège aussi contre les rançongiciels.")),
                                List.of(singleChoice("La règle de sauvegarde 3-2-1 prévoit…", 1,
                                                "3 copies, 2 supports, 1 hors site", true,
                                                "3 mots de passe, 2 comptes, 1 e-mail", false,
                                                "une sauvegarde tous les 321 jours", false),
                                        trueFalse("Les mises à jour corrigent souvent des failles de sécurité.", 2, true)))),
                List.of(singleChoice("Qu'est-ce que l'hameçonnage ?", 1,
                                "une tentative de vol d'informations par un faux message", true,
                                "une mise à jour du système", false,
                                "un type de sauvegarde", false),
                        singleChoice("Quel outil aide à avoir un mot de passe unique par site ?", 2,
                                "un gestionnaire de mots de passe", true, "un tableur partagé", false,
                                "un post-it sur l'écran", false),
                        trueFalse("Une sauvegarde hors site aide à récupérer ses données après un rançongiciel.", 3, true)));
    }

    private static CourseSpec javaCourse() {
        return new CourseSpec("java-programmation-orientee-objet", "Java : programmation orientée objet",
                "Classes, objets, encapsulation, héritage et polymorphisme : les piliers de la programmation orientée objet, illustrés en Java.",
                "39.99",
                List.of(
                        new ChapterSpec("Classes et objets", List.of(
                                textContent("Définir une classe", 1,
                                        "Une classe est un plan : `class Compte { double solde; }`. Un objet est une instance de ce plan, créée avec `new Compte()`."),
                                textContent("Constructeurs et méthodes", 2,
                                        "Le constructeur initialise l'objet à sa création ; les méthodes (`deposer(double montant)`) décrivent son comportement.")),
                                List.of(singleChoice("Quel mot-clé crée un objet en Java ?", 1,
                                                "new", true, "class", false, "static", false),
                                        trueFalse("Un constructeur porte le même nom que sa classe.", 2, true))),
                        new ChapterSpec("Encapsulation", List.of(
                                textContent("Visibilité", 1,
                                        "`private` réserve un attribut à sa classe, `public` l'ouvre à tous : on cache l'état interne et on n'expose que le nécessaire."),
                                textContent("Accesseurs", 2,
                                        "Les getters et setters contrôlent l'accès : `setSolde` peut refuser une valeur négative, ce qu'un attribut public ne permettrait pas.")),
                                List.of(singleChoice("Quel modificateur limite un attribut à sa propre classe ?", 1,
                                                "private", true, "public", false, "protected", false),
                                        trueFalse("Un setter peut valider une valeur avant de la stocker.", 2, true))),
                        new ChapterSpec("Héritage et polymorphisme", List.of(
                                textContent("Hériter d'une classe", 1,
                                        "`class CompteEpargne extends Compte` reprend les attributs et méthodes de `Compte` et peut en ajouter ; `super(...)` appelle le constructeur parent."),
                                textContent("Le polymorphisme", 2,
                                        "Une sous-classe peut redéfinir une méthode (`@Override`) : le même appel `compte.calculerFrais()` s'adapte au type réel de l'objet.")),
                                List.of(singleChoice("Quel mot-clé exprime l'héritage entre classes en Java ?", 1,
                                                "extends", true, "implements", false, "import", false),
                                        trueFalse("`@Override` indique qu'une méthode redéfinit celle de la classe parente.", 2, true)))),
                List.of(singleChoice("L'encapsulation consiste à…", 1,
                                "cacher l'état interne et contrôler son accès", true,
                                "copier une classe dans une autre", false,
                                "compiler plus vite le programme", false),
                        singleChoice("Si `CompteEpargne extends Compte`, alors un CompteEpargne…", 2,
                                "est aussi un Compte", true,
                                "ne peut pas utiliser les méthodes de Compte", false,
                                "doit redéclarer tous les attributs", false),
                        trueFalse("Une classe Java peut hériter directement de plusieurs classes.", 3, false)));
    }

    // ---------- Parcours complet du compte « diplômé » ----------

    /**
     * Rejoue le parcours d'un apprenant sur {@code course} via les services réels : inscription,
     * complétion de tous les contenus, réussite du contrôle puis de l'examen final. La note pondérée
     * atteignant le seuil, un certificat est émis. Idempotent (ne fait rien si le certificat existe).
     * Toute erreur est seulement loguée : le seed de démo ne doit jamais empêcher le démarrage.
     */
    private void seedCertifiedLearner(User learner, Course course) {
        if (certificateService.certificateIdFor(learner.getId(), course.getId()) != null) {
            return;
        }
        try {
            if (!enrollmentService.isEnrolled(learner.getId(), course.getId())) {
                if (course.getPrice().signum() > 0) {
                    paymentService.recordDemoPurchase(learner.getId(), course.getId());
                }
                enrollmentService.enroll(learner.getId(), course.getId());
            }
            for (Chapter chapter : course.getChapters()) {
                for (Content content : chapter.getContents()) {
                    enrollmentService.completeContent(learner.getId(), content.getId());
                }
            }
            for (Quiz control : quizRepository.findByCourseIdAndTypeOrderByIdAsc(course.getId(), QuizType.CONTROL)) {
                quizAttemptService.submit(learner.getId(), control.getId(), allCorrectSubmission(control.getId()));
            }
            Quiz exam = quizRepository.findByCourseIdAndType(course.getId(), QuizType.FINAL_EXAM).orElse(null);
            if (exam != null) {
                quizAttemptService.submit(learner.getId(), exam.getId(), allCorrectSubmission(exam.getId()));
            }
            Long certificateId = certificateService.certificateIdFor(learner.getId(), course.getId());
            log.info("Parcours de démo joué pour {} sur « {} » — certificat {}",
                    learner.getEmail(), course.getTitle(),
                    certificateId != null ? "#" + certificateId : "non émis");
        } catch (RuntimeException e) {
            log.warn("Seed du parcours certifié ignoré ({}) : {}", learner.getEmail(), e.getMessage());
        }
    }

    /** Soumission qui coche exactement les bonnes réponses de chaque question du quiz. */
    private AttemptSubmission allCorrectSubmission(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();
        List<AnswerSubmission> answers = quiz.getQuestions().stream()
                .map(question -> new AnswerSubmission(question.getId(),
                        question.getOptions().stream()
                                .filter(AnswerOption::isCorrect)
                                .map(AnswerOption::getId)
                                .toList()))
                .toList();
        return new AttemptSubmission(answers);
    }

    // ---------- fabriques ----------

    private static Question singleChoice(String statement, int position,
                                        String a, boolean aOk, String b, boolean bOk, String c, boolean cOk) {
        Question question = new Question();
        question.setStatement(statement);
        question.setType(QuestionType.SINGLE_CHOICE);
        question.setPoints(1);
        question.setPosition(position);
        question.addOption(option(a, aOk, 1));
        question.addOption(option(b, bOk, 2));
        question.addOption(option(c, cOk, 3));
        return question;
    }

    private static Question trueFalse(String statement, int position, boolean answerIsTrue) {
        Question question = new Question();
        question.setStatement(statement);
        question.setType(QuestionType.TRUE_FALSE);
        question.setPoints(1);
        question.setPosition(position);
        question.addOption(option("Vrai", answerIsTrue, 1));
        question.addOption(option("Faux", !answerIsTrue, 2));
        return question;
    }

    private static AnswerOption option(String label, boolean correct, int position) {
        AnswerOption option = new AnswerOption();
        option.setLabel(label);
        option.setCorrect(correct);
        option.setPosition(position);
        return option;
    }

    private static Content textContent(String title, int position, String body) {
        Content content = new Content();
        content.setType(ContentType.TEXT);
        content.setTitle(title);
        content.setPosition(position);
        content.setTextBody(body);
        return content;
    }
}

package com.educa.backend.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import com.educa.backend.course.CourseCategory;
import com.educa.backend.course.CourseLevel;
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
import com.educa.backend.rating.RatingService;
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
    private final RatingService ratingService;

    public DevDataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder, CourseRepository courseRepository,
                              QuizRepository quizRepository, EnrollmentService enrollmentService,
                              QuizAttemptService quizAttemptService, CertificateService certificateService,
                              PaymentService paymentService, RatingService ratingService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.enrollmentService = enrollmentService;
        this.quizAttemptService = quizAttemptService;
        this.certificateService = certificateService;
        this.paymentService = paymentService;
        this.ratingService = ratingService;
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
        seedDemoPromotion(PYTHON_COURSE_SLUG, "14.99");
        seedDemoPromotion("sql-et-bases-de-donnees-relationnelles", "17.99");
        seedDemoPromotion("java-programmation-orientee-objet", "19.99");
        seedDemoPromotion("html-et-css-creer-son-premier-site", "9.99");
        seedDemoPromotion("design-d-interfaces-les-bases-ui-ux", "17.99");
        seedDemoPromotion("marketing-digital-attirer-ses-premiers-clients", "24.99");
        seedDemoPromotion("gestion-de-projet-agile-et-scrum", "22.99");

        seedCertifiedLearner(graduate, gitCourse);

        seedInstructorProfile(instructor);
        presentations().forEach(this::seedPresentation);
        seedReviews();
    }

    /**
     * Promotion de démo (prix barré) valable 14 jours : posée si le cours n'en a pas, ou si la précédente est
     * terminée, pour que la démo montre toujours des prix promotionnels. Une promotion en cours n'est pas touchée.
     */
    private void seedDemoPromotion(String slug, String promoPrice) {
        Instant now = Instant.now();
        courseRepository.findBySlug(slug)
                .filter(course -> !course.isPromoActive(now))
                .ifPresent(course -> {
                    course.setPromoPrice(new BigDecimal(promoPrice));
                    course.setPromoEndsAt(now.plus(14, ChronoUnit.DAYS));
                    log.info("Promotion de démo : « {} » {} € au lieu de {} €", course.getTitle(), promoPrice, course.getPrice());
                });
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
        return List.of(sqlCourse(), algorithmsCourse(), securityCourse(), javaCourse(),
                htmlCssCourse(), excelCourse(), englishCourse(), uiDesignCourse(), digitalMarketingCourse(), agileCourse());
    }

    private static CourseSpec htmlCssCourse() {
        return new CourseSpec("html-et-css-creer-son-premier-site", "HTML et CSS : créer son premier site web",
                "Structurer une page avec HTML, la mettre en forme avec CSS et la rendre lisible sur téléphone.",
                "19.99",
                List.of(
                        new ChapterSpec("La structure d'une page HTML", List.of(
                                textContent("Les balises", 1,
                                        "Une page HTML est faite de balises : <h1> pour un titre, <p> pour un paragraphe, <a> pour un lien, <img> pour une image."),
                                textContent("Le squelette d'une page", 2,
                                        "Toute page commence par <!doctype html>, puis <html>, <head> (titre, encodage) et <body> (le contenu visible).")),
                                List.of(singleChoice("Quelle balise crée un lien ?", 1,
                                                "<a>", true, "<p>", false, "<img>", false),
                                        trueFalse("Le contenu visible de la page se trouve dans <body>.", 2, true))),
                        new ChapterSpec("Mettre en forme avec CSS", List.of(
                                textContent("Sélecteurs et propriétés", 1,
                                        "Une règle CSS associe un sélecteur (h1, .carte, #menu) à des propriétés : color, font-size, margin, padding…"),
                                textContent("Le modèle de boîte", 2,
                                        "Chaque élément est une boîte : contenu, puis marge intérieure (padding), bordure (border) et marge extérieure (margin).")),
                                List.of(singleChoice("Quelle propriété change la couleur du texte ?", 1,
                                                "color", true, "background", false, "border", false),
                                        trueFalse("Le padding est l'espace à l'extérieur de la bordure.", 2, false))),
                        new ChapterSpec("Un site lisible sur téléphone", List.of(
                                textContent("La balise viewport", 1,
                                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"> indique au téléphone d'afficher la page à sa vraie largeur."),
                                textContent("Les media queries", 2,
                                        "@media (max-width: 600px) { … } applique des règles seulement sur les petits écrans : une colonne au lieu de trois, par exemple.")),
                                List.of(singleChoice("À quoi sert une media query ?", 1,
                                                "adapter le style à la taille de l'écran", true,
                                                "ajouter une vidéo", false, "envoyer un formulaire", false),
                                        trueFalse("Sans balise viewport, une page peut s'afficher en tout petit sur téléphone.", 2, true)))),
                List.of(singleChoice("Quel langage décrit la structure d'une page ?", 1,
                                "HTML", true, "CSS", false, "SQL", false),
                        singleChoice("Où met-on la mise en forme (couleurs, tailles) ?", 2,
                                "dans le CSS", true, "dans le titre de la page", false, "dans l'adresse du site", false),
                        trueFalse("Un site responsive s'adapte à la taille de l'écran.", 3, true)));
    }

    private static CourseSpec excelCourse() {
        return new CourseSpec("excel-de-debutant-a-efficace", "Excel : de débutant à efficace",
                "Saisir et organiser des données, calculer avec les formules essentielles et présenter ses chiffres en graphiques.",
                "24.99",
                List.of(
                        new ChapterSpec("Prendre en main un classeur", List.of(
                                textContent("Cellules, lignes et colonnes", 1,
                                        "Chaque cellule a une adresse : la colonne (lettre) puis la ligne (chiffre), par exemple B3."),
                                textContent("Mettre en forme un tableau", 2,
                                        "Mettez les titres en gras, figez la première ligne et utilisez « Mettre sous forme de tableau » pour trier et filtrer facilement.")),
                                List.of(singleChoice("Que désigne l'adresse C5 ?", 1,
                                                "la colonne C, ligne 5", true, "la ligne C, colonne 5", false,
                                                "la 5e feuille", false),
                                        trueFalse("Figer la première ligne la garde visible quand on fait défiler.", 2, true))),
                        new ChapterSpec("Les formules essentielles", List.of(
                                textContent("SOMME, MOYENNE, MAX", 1,
                                        "Une formule commence par = : =SOMME(B2:B10) additionne une plage, =MOYENNE(B2:B10) en fait la moyenne."),
                                textContent("Le SI", 2,
                                        "=SI(B2>=10;\"Admis\";\"Ajourné\") affiche un texte selon une condition.")),
                                List.of(singleChoice("Par quel caractère commence une formule ?", 1,
                                                "=", true, "#", false, "@", false),
                                        trueFalse("=MOYENNE(A1:A4) additionne les cellules A1 à A4.", 2, false))),
                        new ChapterSpec("Présenter ses données", List.of(
                                textContent("Les graphiques", 1,
                                        "Sélectionnez vos données puis Insertion > Graphique : histogramme pour comparer, courbe pour une évolution, secteurs pour une répartition."),
                                textContent("Le tableau croisé dynamique", 2,
                                        "Il résume un grand tableau en quelques clics : total des ventes par mois et par vendeur, par exemple.")),
                                List.of(singleChoice("Quel graphique montre le mieux une évolution dans le temps ?", 1,
                                                "une courbe", true, "un secteur", false, "un nuage de mots", false),
                                        trueFalse("Un tableau croisé dynamique permet de résumer un grand tableau.", 2, true)))),
                List.of(singleChoice("Quelle formule additionne les cellules A1 à A10 ?", 1,
                                "=SOMME(A1:A10)", true, "=MOYENNE(A1:A10)", false, "=SI(A1:A10)", false),
                        singleChoice("Pour comparer des ventes entre plusieurs vendeurs, on choisit plutôt…", 2,
                                "un histogramme", true, "une seule cellule", false, "un texte en gras", false),
                        trueFalse("Une formule Excel commence toujours par le signe =.", 3, true)));
    }

    private static CourseSpec englishCourse() {
        return new CourseSpec("anglais-professionnel-communiquer-au-travail", "Anglais professionnel : communiquer au travail",
                "Écrire des e-mails clairs, réussir ses appels et ses réunions en anglais, avec les expressions utiles au quotidien.",
                "29.99",
                List.of(
                        new ChapterSpec("Écrire un e-mail professionnel", List.of(
                                textContent("Ouvrir et conclure", 1,
                                        "« Dear Ms Smith, » pour ouvrir, « Kind regards, » pour conclure ; « Hi John, » reste réservé aux collègues proches."),
                                textContent("Être clair et poli", 2,
                                        "Une demande par paragraphe, des phrases courtes, et des formules polies : « Could you please… », « I would appreciate it if… ».")),
                                List.of(singleChoice("Quelle formule conclut un e-mail professionnel ?", 1,
                                                "Kind regards,", true, "See ya!", false, "Bye bye", false),
                                        trueFalse("« Could you please send me the report? » est une demande polie.", 2, true))),
                        new ChapterSpec("Au téléphone et en visio", List.of(
                                textContent("Se présenter", 1,
                                        "« Hello, this is Awa from Ganndal. I'm calling about… » : votre nom, votre entreprise, puis l'objet de l'appel."),
                                textContent("Faire répéter", 2,
                                        "« Sorry, could you repeat that? » ou « Could you speak a little more slowly, please? » : demander de répéter est normal.")),
                                List.of(singleChoice("Pour faire répéter poliment, on dit…", 1,
                                                "Sorry, could you repeat that?", true, "What?", false, "Again!", false),
                                        trueFalse("Au téléphone, on annonce l'objet de l'appel après s'être présenté.", 2, true))),
                        new ChapterSpec("Participer à une réunion", List.of(
                                textContent("Donner son avis", 1,
                                        "« In my opinion… », « I agree with you », « I see your point, but… » permettent de nuancer sans brusquer."),
                                textContent("Résumer les décisions", 2,
                                        "« To sum up, we agreed that… » puis « The next step is… » : chacun repart avec les mêmes informations.")),
                                List.of(singleChoice("Quelle expression exprime un désaccord poli ?", 1,
                                                "I see your point, but…", true, "You are wrong.", false, "No way.", false),
                                        trueFalse("« To sum up » sert à résumer.", 2, true)))),
                List.of(singleChoice("Comment commencer un e-mail à une cliente que vous ne connaissez pas ?", 1,
                                "Dear Ms Diallo,", true, "Hey!", false, "Yo Diallo", false),
                        singleChoice("« Could you speak more slowly? » sert à…", 2,
                                "demander de parler plus lentement", true, "raccrocher", false, "dire au revoir", false),
                        trueFalse("« I agree with you » signifie « Je suis d'accord avec vous ».", 3, true)));
    }

    private static CourseSpec uiDesignCourse() {
        return new CourseSpec("design-d-interfaces-les-bases-ui-ux", "Design d'interfaces : les bases de l'UI/UX",
                "Comprendre les besoins des utilisateurs et concevoir des écrans clairs : hiérarchie, couleurs, typographie et maquettes.",
                "34.99",
                List.of(
                        new ChapterSpec("Penser utilisateur (UX)", List.of(
                                textContent("Partir du besoin", 1,
                                        "Avant de dessiner, on interroge les utilisateurs : qui sont-ils, que veulent-ils faire, qu'est-ce qui les gêne aujourd'hui ?"),
                                textContent("Le parcours utilisateur", 2,
                                        "On décrit les étapes pour atteindre un but (s'inscrire, acheter) et on supprime toutes celles qui ne servent à rien.")),
                                List.of(singleChoice("Par quoi commence une démarche UX ?", 1,
                                                "comprendre les utilisateurs", true, "choisir les couleurs", false,
                                                "écrire le code", false),
                                        trueFalse("Réduire le nombre d'étapes d'un parcours améliore souvent l'expérience.", 2, true))),
                        new ChapterSpec("Les règles visuelles (UI)", List.of(
                                textContent("Hiérarchie et contraste", 1,
                                        "Le plus important doit se voir en premier : taille, graisse et contraste guident l'œil. Un texte doit rester lisible sur son fond."),
                                textContent("Couleurs et typographie", 2,
                                        "Une couleur principale, une ou deux couleurs d'accent et au plus deux polices suffisent à un design cohérent.")),
                                List.of(singleChoice("Combien de polices au maximum pour un design cohérent ?", 1,
                                                "deux", true, "six", false, "une par page", false),
                                        trueFalse("Un texte gris clair sur fond blanc est toujours facile à lire.", 2, false))),
                        new ChapterSpec("De la maquette au prototype", List.of(
                                textContent("Le wireframe", 1,
                                        "Un wireframe est un croquis en noir et blanc qui place les blocs de l'écran, sans se soucier des couleurs."),
                                textContent("Tester le prototype", 2,
                                        "Un prototype cliquable (Figma, par exemple) se fait tester par quelques utilisateurs avant tout développement.")),
                                List.of(singleChoice("Qu'est-ce qu'un wireframe ?", 1,
                                                "un croquis de la disposition d'un écran", true,
                                                "le code final du site", false, "une palette de couleurs", false),
                                        trueFalse("On peut tester un prototype avec des utilisateurs avant de coder.", 2, true)))),
                List.of(singleChoice("Que désigne l'UX ?", 1,
                                "l'expérience vécue par l'utilisateur", true, "le langage de programmation", false,
                                "le nom de domaine", false),
                        singleChoice("Comment faire ressortir l'élément le plus important d'un écran ?", 2,
                                "par la taille, la graisse et le contraste", true, "en le cachant dans un menu", false,
                                "en l'écrivant en tout petit", false),
                        trueFalse("Un prototype se teste idéalement avant le développement.", 3, true)));
    }

    private static CourseSpec digitalMarketingCourse() {
        return new CourseSpec("marketing-digital-attirer-ses-premiers-clients", "Marketing digital : attirer ses premiers clients",
                "Définir sa cible, être visible sur les réseaux sociaux et sur Google, et mesurer ce qui fonctionne vraiment.",
                "39.99",
                List.of(
                        new ChapterSpec("Connaître sa cible", List.of(
                                textContent("Le persona", 1,
                                        "Un persona décrit un client type : âge, métier, besoins, freins. Il aide à choisir le bon message et le bon canal."),
                                textContent("La proposition de valeur", 2,
                                        "En une phrase : pour qui, quel problème vous résolvez et pourquoi vous plutôt qu'un autre.")),
                                List.of(singleChoice("À quoi sert un persona ?", 1,
                                                "représenter un client type", true, "calculer la TVA", false,
                                                "créer un logo", false),
                                        trueFalse("Une proposition de valeur doit tenir en quelques mots clairs.", 2, true))),
                        new ChapterSpec("Être visible", List.of(
                                textContent("Les réseaux sociaux", 1,
                                        "Mieux vaut un ou deux réseaux bien choisis (là où se trouve votre cible) et une publication régulière que tous les réseaux à moitié."),
                                textContent("Le référencement (SEO)", 2,
                                        "Des pages rapides, des titres clairs et un contenu qui répond aux questions de vos clients aident à remonter sur Google.")),
                                List.of(singleChoice("Sur quels réseaux publier ?", 1,
                                                "là où se trouve sa cible", true, "sur tous, sans exception", false,
                                                "aucun", false),
                                        trueFalse("Le SEO vise à être mieux classé dans les moteurs de recherche.", 2, true))),
                        new ChapterSpec("Mesurer et ajuster", List.of(
                                textContent("Les bons indicateurs", 1,
                                        "Visites, taux de clic, taux de conversion (visiteurs devenus clients) : suivez peu d'indicateurs, mais régulièrement."),
                                textContent("Tester et améliorer", 2,
                                        "Changez une chose à la fois (un titre, une image) et comparez les résultats : c'est le principe du test A/B.")),
                                List.of(singleChoice("Le taux de conversion mesure…", 1,
                                                "la part des visiteurs devenus clients", true, "le nombre d'abonnés", false,
                                                "la vitesse du site", false),
                                        trueFalse("Dans un test A/B, on change plusieurs choses à la fois.", 2, false)))),
                List.of(singleChoice("Quelle est la première étape d'une stratégie marketing ?", 1,
                                "définir sa cible", true, "acheter de la publicité", false, "changer de logo", false),
                        singleChoice("Que signifie SEO ?", 2,
                                "l'optimisation pour les moteurs de recherche", true, "un réseau social", false,
                                "un logiciel de comptabilité", false),
                        trueFalse("Mesurer ses résultats permet d'ajuster ses actions.", 3, true)));
    }

    private static CourseSpec agileCourse() {
        return new CourseSpec("gestion-de-projet-agile-et-scrum", "Gestion de projet : méthodes agiles et Scrum",
                "Livrer par petites étapes, organiser le travail d'équipe avec Scrum et suivre l'avancement avec un tableau Kanban.",
                "44.99",
                List.of(
                        new ChapterSpec("L'esprit agile", List.of(
                                textContent("Pourquoi l'agilité", 1,
                                        "Plutôt qu'un grand plan figé, on livre un produit utile par petites étapes et on l'ajuste grâce aux retours des utilisateurs."),
                                textContent("Les 4 valeurs du Manifeste agile", 2,
                                        "Les individus avant les outils, un produit qui fonctionne avant la documentation, la collaboration avec le client et l'adaptation au changement.")),
                                List.of(singleChoice("Que privilégie une démarche agile ?", 1,
                                                "des livraisons courtes et fréquentes", true,
                                                "un seul livrable à la fin du projet", false,
                                                "un plan qu'on ne modifie jamais", false),
                                        trueFalse("L'agilité accepte que les besoins changent en cours de projet.", 2, true))),
                        new ChapterSpec("Scrum en pratique", List.of(
                                textContent("Les rôles", 1,
                                        "Le Product Owner priorise les besoins, le Scrum Master aide l'équipe à bien travailler, l'équipe de développement réalise le produit."),
                                textContent("Le sprint et ses rituels", 2,
                                        "Un sprint dure 1 à 4 semaines : planification, point quotidien de 15 minutes, revue du travail livré puis rétrospective pour s'améliorer.")),
                                List.of(singleChoice("Qui priorise les besoins du produit ?", 1,
                                                "le Product Owner", true, "le Scrum Master", false, "le client final seul", false),
                                        trueFalse("Le point quotidien (daily) dure environ 15 minutes.", 2, true))),
                        new ChapterSpec("Suivre l'avancement", List.of(
                                textContent("Le tableau Kanban", 1,
                                        "Trois colonnes suffisent pour commencer : À faire, En cours, Terminé. On limite le nombre de tâches « En cours » pour finir avant de commencer."),
                                textContent("Les user stories", 2,
                                        "« En tant que [utilisateur], je veux [action] afin de [bénéfice] » : une user story décrit un besoin du point de vue de l'utilisateur.")),
                                List.of(singleChoice("Pourquoi limiter les tâches « En cours » ?", 1,
                                                "pour terminer avant de commencer autre chose", true,
                                                "pour cacher le travail", false, "pour aller plus vite en commençant tout", false),
                                        trueFalse("Une user story décrit un besoin du point de vue de l'utilisateur.", 2, true)))),
                List.of(singleChoice("Combien de temps dure en général un sprint Scrum ?", 1,
                                "de 1 à 4 semaines", true, "une journée", false, "un an", false),
                        singleChoice("À quoi sert la rétrospective ?", 2,
                                "améliorer la façon de travailler de l'équipe", true, "fixer le prix du produit", false,
                                "recruter un nouveau membre", false),
                        trueFalse("Un tableau Kanban rend visible l'avancement des tâches.", 3, true)));
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

    // ---------- Présentation des cours, profil du formateur, avis ----------

    private record PresentationSpec(String slug, String subtitle, CourseCategory category, CourseLevel level,
                                    String durationHours, List<String> objectives, List<String> prerequisites,
                                    String targetAudience) {
    }

    /** Un apprenant de démo qui a suivi le cours et le note (avis écrit facultatif). */
    private record ReviewSpec(String slug, String reviewerEmail, int stars, String comment) {
    }

    private void seedInstructorProfile(User instructor) {
        if (instructor.getHeadline() != null) {
            return; // déjà renseigné (seed précédent ou saisie dans « Mon profil »)
        }
        instructor.setHeadline("Développeur full-stack, formateur depuis 2015");
        instructor.setBio("""
                Ingénieur logiciel pendant dix ans (Java, Python, bases de données), j'enseigne aujourd'hui \
                la programmation à des débutants comme à des développeurs en reconversion. Mes cours vont à \
                l'essentiel : un concept, un exemple concret, un exercice.""");
        userRepository.save(instructor);
    }

    /** Remplit la page de présentation d'un cours de démo, seulement si elle est encore vide. */
    private void seedPresentation(PresentationSpec spec) {
        courseRepository.findBySlug(spec.slug())
                .filter(course -> course.getSubtitle() == null)
                .ifPresent(course -> {
                    course.setSubtitle(spec.subtitle());
                    course.setCategory(spec.category());
                    course.setLevel(spec.level());
                    course.setDurationHours(new BigDecimal(spec.durationHours()));
                    course.setObjectives(String.join("\n", spec.objectives()));
                    course.setPrerequisites(String.join("\n", spec.prerequisites()));
                    course.setTargetAudience(spec.targetAudience());
                    courseRepository.save(course);
                    log.info("Présentation de démo ajoutée : « {} »", course.getTitle());
                });
    }

    /**
     * Chaque avis passe par le parcours réel : achat, inscription, contenus suivis (au-delà des 70 % requis),
     * puis {@link RatingService#rate} — mêmes règles qu'un apprenant. Idempotent : un avis existant est conservé.
     */
    private void seedReviews() {
        List<User> reviewers = List.of(
                seedUser("lucas@educa.dev", "Lucas Martin", RoleName.LEARNER),
                seedUser("fatou@educa.dev", "Fatou Ndiaye", RoleName.LEARNER),
                seedUser("julie@educa.dev", "Julie Peeters", RoleName.LEARNER),
                seedUser("mehdi@educa.dev", "Mehdi Benali", RoleName.LEARNER),
                seedUser("sophie@educa.dev", "Sophie Wagner", RoleName.LEARNER));
        for (ReviewSpec spec : reviews()) {
            User reviewer = reviewers.stream()
                    .filter(u -> u.getEmail().equals(spec.reviewerEmail()))
                    .findFirst()
                    .orElseThrow();
            courseRepository.findBySlug(spec.slug()).ifPresent(course -> seedReview(reviewer, course, spec));
        }
    }

    private void seedReview(User reviewer, Course course, ReviewSpec spec) {
        try {
            if (ratingService.get(course.getId(), reviewer.getId()).myStars() != null) {
                return;
            }
            if (!enrollmentService.isEnrolled(reviewer.getId(), course.getId())) {
                if (course.getPrice().signum() > 0) {
                    paymentService.recordDemoPurchase(reviewer.getId(), course.getId());
                }
                enrollmentService.enroll(reviewer.getId(), course.getId());
            }
            for (Chapter chapter : course.getChapters()) {
                for (Content content : chapter.getContents()) {
                    enrollmentService.completeContent(reviewer.getId(), content.getId());
                }
            }
            ratingService.rate(course.getId(), reviewer.getId(), spec.stars(), spec.comment());
        } catch (RuntimeException e) {
            log.warn("Avis de démo ignoré ({} sur « {} ») : {}", reviewer.getEmail(), course.getTitle(), e.getMessage());
        }
    }

    private static List<PresentationSpec> presentations() {
        return List.of(
                new PresentationSpec("gestion-de-projet-agile-et-scrum",
                        "Organiser une équipe et livrer par petites étapes, sans perdre le client de vue",
                        CourseCategory.BUSINESS, CourseLevel.INTERMEDIATE, "6",
                        List.of("Comprendre les valeurs et principes agiles",
                                "Conduire un sprint Scrum avec ses rôles et ses rituels",
                                "Suivre le travail avec un tableau Kanban et des user stories"),
                        List.of("Avoir déjà participé à un projet, même petit"),
                        "Chefs de projet, développeurs, entrepreneurs et étudiants qui travaillent en équipe."),
                new PresentationSpec("html-et-css-creer-son-premier-site",
                        "Votre premier site web, lisible sur ordinateur comme sur téléphone",
                        CourseCategory.DEVELOPMENT, CourseLevel.BEGINNER, "4",
                        List.of("Structurer une page avec les balises HTML",
                                "Mettre en forme avec CSS",
                                "Adapter la page aux téléphones"),
                        List.of("Savoir utiliser un ordinateur et un navigateur"),
                        "Débutants complets, entrepreneurs qui veulent comprendre leur site, futurs développeurs."),
                new PresentationSpec("excel-de-debutant-a-efficace",
                        "Les formules et graphiques utiles au travail, pas à pas",
                        CourseCategory.OFFICE, CourseLevel.BEGINNER, "5",
                        List.of("Organiser des données dans un tableau propre",
                                "Calculer avec SOMME, MOYENNE et SI",
                                "Présenter ses chiffres en graphiques"),
                        List.of("Avoir Excel ou un tableur gratuit (LibreOffice, Google Sheets)"),
                        "Salariés, étudiants et indépendants qui veulent gagner du temps sur leurs tableaux."),
                new PresentationSpec("anglais-professionnel-communiquer-au-travail",
                        "E-mails, appels et réunions : les expressions qui servent vraiment",
                        CourseCategory.LANGUAGES, CourseLevel.INTERMEDIATE, "6",
                        List.of("Rédiger un e-mail professionnel clair et poli",
                                "Se présenter et faire répéter au téléphone",
                                "Donner son avis et résumer une réunion"),
                        List.of("Un niveau d'anglais scolaire (A2)"),
                        "Professionnels qui travaillent avec des clients ou des collègues anglophones."),
                new PresentationSpec("design-d-interfaces-les-bases-ui-ux",
                        "Concevoir des écrans clairs que les utilisateurs comprennent tout de suite",
                        CourseCategory.DESIGN, CourseLevel.BEGINNER, "6",
                        List.of("Partir des besoins des utilisateurs",
                                "Appliquer les règles de hiérarchie, couleur et typographie",
                                "Passer du croquis au prototype testé"),
                        List.of("Aucun — un compte Figma gratuit suffit pour pratiquer"),
                        "Curieux du design, développeurs, chefs de projet et créateurs d'entreprise."),
                new PresentationSpec("marketing-digital-attirer-ses-premiers-clients",
                        "Une méthode simple pour être visible en ligne et convertir",
                        CourseCategory.BUSINESS, CourseLevel.ALL_LEVELS, "7",
                        List.of("Définir sa cible et sa proposition de valeur",
                                "Choisir ses réseaux sociaux et soigner son référencement",
                                "Mesurer ses résultats et les améliorer"),
                        List.of("Avoir un projet, un produit ou un service à faire connaître"),
                        "Entrepreneurs, commerçants, associations et indépendants qui se lancent."),
                new PresentationSpec(PYTHON_COURSE_SLUG,
                        "Votre premier programme en une heure, sans aucune expérience préalable",
                        CourseCategory.DEVELOPMENT, CourseLevel.BEGINNER, "4",
                        List.of("Installer Python et lancer un script",
                                "Manipuler des variables et les types de base",
                                "Lire et comprendre un programme simple",
                                "Savoir quoi apprendre ensuite"),
                        List.of("Savoir utiliser un ordinateur et installer un logiciel"),
                        "Toute personne curieuse de programmation : étudiants, reconversion, autodidactes."),
                new PresentationSpec(GIT_COURSE_SLUG,
                        "Versionner son code et collaborer sans jamais rien perdre",
                        CourseCategory.DEVELOPMENT, CourseLevel.BEGINNER, "3",
                        List.of("Comprendre le dépôt, le commit et l'historique",
                                "Créer des branches et les fusionner",
                                "Travailler avec un dépôt distant (push, pull)",
                                "Annuler une erreur sans paniquer"),
                        List.of("Être à l'aise avec un terminal (ouvrir un dossier, lancer une commande)"),
                        "Développeurs débutants, étudiants en informatique, toute personne qui écrit du code en équipe."),
                new PresentationSpec("sql-et-bases-de-donnees-relationnelles",
                        "Interroger, filtrer et relier vos données avec le langage SQL",
                        CourseCategory.DATA, CourseLevel.BEGINNER, "6",
                        List.of("Écrire des requêtes SELECT avec filtres et tris",
                                "Relier plusieurs tables avec des jointures",
                                "Calculer des agrégats (COUNT, SUM, GROUP BY)",
                                "Concevoir un schéma relationnel simple"),
                        List.of("Aucune connaissance en programmation n'est requise"),
                        "Analystes, gestionnaires, développeurs : quiconque doit exploiter des données."),
                new PresentationSpec("algorithmique-les-fondamentaux",
                        "Apprendre à raisonner comme un programmeur, quel que soit le langage",
                        CourseCategory.DEVELOPMENT, CourseLevel.ALL_LEVELS, "5",
                        List.of("Décomposer un problème en étapes",
                                "Utiliser conditions, boucles et tableaux",
                                "Comprendre la complexité d'un algorithme",
                                "Écrire des algorithmes de tri et de recherche"),
                        List.of("Notions de mathématiques du secondaire"),
                        "Étudiants qui débutent l'informatique et développeurs qui veulent consolider leurs bases."),
                new PresentationSpec("cybersecurite-les-bons-reflexes",
                        "Protéger vos comptes, vos appareils et vos données au quotidien",
                        CourseCategory.NETWORK_SECURITY, CourseLevel.BEGINNER, "2.5",
                        List.of("Créer et gérer des mots de passe solides",
                                "Repérer un e-mail d'hameçonnage",
                                "Sécuriser son ordinateur et son téléphone",
                                "Réagir après un incident"),
                        List.of("Aucun — le cours s'adresse à tous les utilisateurs"),
                        "Particuliers, salariés et indépendants qui veulent se protéger sans jargon technique."),
                new PresentationSpec("java-programmation-orientee-objet",
                        "Classes, objets, héritage : maîtriser les piliers de la POO avec Java",
                        CourseCategory.DEVELOPMENT, CourseLevel.INTERMEDIATE, "8",
                        List.of("Modéliser un problème avec des classes et des objets",
                                "Protéger l'état d'un objet grâce à l'encapsulation",
                                "Réutiliser du code avec l'héritage",
                                "Exploiter le polymorphisme"),
                        List.of("Connaître les bases d'un langage de programmation (variables, boucles, fonctions)"),
                        "Développeurs débutants qui veulent passer à la programmation orientée objet."));
    }

    private static List<ReviewSpec> reviews() {
        return List.of(
                new ReviewSpec(PYTHON_COURSE_SLUG, "lucas@educa.dev", 5,
                        "Parfait pour démarrer : chaque notion est expliquée simplement, j'ai écrit mon premier script dès le premier chapitre."),
                new ReviewSpec(PYTHON_COURSE_SLUG, "fatou@educa.dev", 4,
                        "Très clair. J'aurais aimé un ou deux exercices supplémentaires sur les listes."),
                new ReviewSpec(PYTHON_COURSE_SLUG, "sophie@educa.dev", 5, null),
                new ReviewSpec(GIT_COURSE_SLUG, "mehdi@educa.dev", 5,
                        "Enfin compris les branches ! Les exemples collent à ce qu'on vit en projet."),
                new ReviewSpec(GIT_COURSE_SLUG, "julie@educa.dev", 4,
                        "Court et efficace, idéal avant un premier projet en équipe."),
                new ReviewSpec("sql-et-bases-de-donnees-relationnelles", "julie@educa.dev", 5,
                        "Les jointures étaient mon point faible, c'est maintenant acquis. Les contrôles aident vraiment à vérifier sa compréhension."),
                new ReviewSpec("sql-et-bases-de-donnees-relationnelles", "lucas@educa.dev", 4,
                        "Bonne progression, du SELECT jusqu'à la conception du schéma."),
                new ReviewSpec("sql-et-bases-de-donnees-relationnelles", "mehdi@educa.dev", 3,
                        "Contenu solide mais un peu rapide sur GROUP BY pour un débutant complet."),
                new ReviewSpec("algorithmique-les-fondamentaux", "fatou@educa.dev", 5,
                        "Le chapitre sur la complexité m'a ouvert les yeux. Je recommande à tous les étudiants de première année."),
                new ReviewSpec("algorithmique-les-fondamentaux", "sophie@educa.dev", 4, null),
                new ReviewSpec("cybersecurite-les-bons-reflexes", "sophie@educa.dev", 5,
                        "Accessible même sans connaissances techniques. J'ai changé tous mes mots de passe dans la foulée !"),
                new ReviewSpec("cybersecurite-les-bons-reflexes", "julie@educa.dev", 4,
                        "Des réflexes simples et utiles, à faire suivre à toute l'équipe."),
                new ReviewSpec("cybersecurite-les-bons-reflexes", "lucas@educa.dev", 5, null),
                new ReviewSpec("java-programmation-orientee-objet", "mehdi@educa.dev", 4,
                        "Très bonne introduction à la POO. Le chapitre sur le polymorphisme mériterait un exemple de plus."),
                new ReviewSpec("java-programmation-orientee-objet", "fatou@educa.dev", 5,
                        "L'encapsulation et l'héritage enfin clairs, avec des exemples concrets (comptes bancaires)."));
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

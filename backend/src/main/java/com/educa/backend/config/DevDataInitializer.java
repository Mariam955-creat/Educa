package com.educa.backend.config;

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
 *   <li>2 cours publiés du formateur de démo, chacun avec chapitres, contenus, un contrôle et un
 *       examen final ;</li>
 *   <li>le parcours complet du compte « diplômé » sur le 2ᵉ cours (inscription → 100 % → contrôle →
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

    public DevDataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder, CourseRepository courseRepository,
                              QuizRepository quizRepository, EnrollmentService enrollmentService,
                              QuizAttemptService quizAttemptService, CertificateService certificateService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.enrollmentService = enrollmentService;
        this.quizAttemptService = quizAttemptService;
        this.certificateService = certificateService;
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

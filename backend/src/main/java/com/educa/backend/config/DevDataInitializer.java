package com.educa.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.course.Chapter;
import com.educa.backend.course.Content;
import com.educa.backend.course.ContentType;
import com.educa.backend.course.Course;
import com.educa.backend.course.CourseRepository;
import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;

/**
 * Données de démonstration au démarrage (profil {@code dev} uniquement).
 * Comptes : mot de passe commun {@code password123}.
 */
@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private static final String DEMO_PASSWORD = "password123";
    private static final String DEMO_COURSE_SLUG = "introduction-a-python";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseRepository courseRepository;

    public DevDataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder, CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser("admin@educa.dev", "Admin Démo", RoleName.ADMIN);
        User instructor = seedUser("formateur@educa.dev", "Karim Formateur", RoleName.INSTRUCTOR);
        seedUser("apprenant@educa.dev", "Amina Apprenante", RoleName.LEARNER);
        seedDemoCourse(instructor);
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

    private void seedDemoCourse(User instructor) {
        if (courseRepository.existsBySlug(DEMO_COURSE_SLUG)) {
            return;
        }
        Course course = new Course();
        course.setInstructorId(instructor.getId());
        course.setTitle("Introduction à Python");
        course.setSlug(DEMO_COURSE_SLUG);
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

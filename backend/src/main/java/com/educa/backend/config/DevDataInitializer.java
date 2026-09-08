package com.educa.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;

/**
 * Crée trois comptes de démonstration au démarrage (profil {@code dev} uniquement).
 * Mot de passe commun : {@code password123}.
 */
@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("admin@educa.dev", "Admin Démo", RoleName.ADMIN);
        seed("formateur@educa.dev", "Karim Formateur", RoleName.INSTRUCTOR);
        seed("apprenant@educa.dev", "Amina Apprenante", RoleName.LEARNER);
    }

    private void seed(String email, String fullName, RoleName roleName) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Rôle absent : " + roleName));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setFullName(fullName);
        user.setPreferredLanguage("fr");
        user.addRole(role);
        userRepository.save(user);

        log.info("Compte de démo créé : {} ({}) / mot de passe : {}", email, roleName, DEMO_PASSWORD);
    }
}

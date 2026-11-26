package com.educa.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.educa.backend.notification.EmailSender;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Validation des cours avant publication (réglage actif, comme en dev et en production) : soumission par le
 * formateur, notification des admins, refus motivé puis resoumission, validation = publication ; notification
 * et email au formateur. Sans {@code @Transactional} : l'email part après validation de la transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "educa.courses.review-required=true")
@Import(CourseReviewTest.RecordingMail.class)
class CourseReviewTest {

    record Mail(String to, String subject, String body) {
    }

    @TestConfiguration
    static class RecordingMail {
        static final List<Mail> SENT = new CopyOnWriteArrayList<>();

        @Bean
        @Primary
        EmailSender recordingEmailSender() {
            return (to, subject, body) -> SENT.add(new Mail(to, subject, body));
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String suffix;

    @BeforeEach
    void setUp() {
        RecordingMail.SENT.clear();
        suffix = String.valueOf(System.nanoTime());
    }

    @Test
    void formateur_ne_publie_pas_sans_validation_et_doit_avoir_un_chapitre() throws Exception {
        String prof = tokenWithRole("prof-a-" + suffix, RoleName.INSTRUCTOR);
        long id = createCourse(prof, "Cours sans validation " + suffix);

        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + prof))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/courses/" + id + "/submit-review").header("Authorization", "Bearer " + prof))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refus_motive_puis_resoumission_puis_validation() throws Exception {
        String admin = tokenWithRole("admin-cr-" + suffix, RoleName.ADMIN);
        String profEmail = "prof-b-" + suffix + "@example.com";
        String prof = tokenWithRole("prof-b-" + suffix, RoleName.INSTRUCTOR);
        String title = "Cours a valider " + suffix;
        long id = createCourse(prof, title);
        addChapter(prof, id);

        // Soumission : en attente, admins notifiés, compteur à jour
        mvc.perform(post("/api/v1/courses/" + id + "/submit-review").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("PENDING"))
                .andExpect(jsonPath("$.published").value(false));
        mvc.perform(post("/api/v1/courses/" + id + "/submit-review").header("Authorization", "Bearer " + prof))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.items[0].type").value("COURSE_SUBMITTED"))
                .andExpect(jsonPath("$.items[0].subject").value(title));
        String queue = mvc.perform(get("/api/v1/admin/course-reviews").param("size", "100")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Object>>read(queue, "$.content[?(@.id == " + id + ")]")).hasSize(1);

        // Refus : motif obligatoire, puis transmis (notification + email)
        mvc.perform(post("/api/v1/admin/course-reviews/" + id + "/reject").header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/course-reviews/" + id + "/reject").header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"Ajoutez un examen final\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("REJECTED"))
                .andExpect(jsonPath("$.reviewReason").value("Ajoutez un examen final"));
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + prof))
                .andExpect(jsonPath("$.items[0].type").value("COURSE_REJECTED"))
                .andExpect(jsonPath("$.items[0].subject").value(title))
                .andExpect(jsonPath("$.items[0].detail").value("Ajoutez un examen final"));
        assertThat(RecordingMail.SENT).anySatisfy(mail -> {
            assertThat(mail.to()).isEqualTo(profEmail);
            assertThat(mail.body()).contains(title).contains("Motif : Ajoutez un examen final");
        });

        // Resoumission après correction, puis validation : le cours est publié
        mvc.perform(post("/api/v1/courses/" + id + "/submit-review").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("PENDING"));
        mvc.perform(post("/api/v1/admin/course-reviews/" + id + "/approve").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.published").value(true));
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + prof))
                .andExpect(jsonPath("$.items[0].type").value("COURSE_APPROVED"));
        assertThat(RecordingMail.SENT).anySatisfy(mail -> {
            assertThat(mail.to()).isEqualTo(profEmail);
            assertThat(mail.subject()).contains(title).contains("validé");
        });

        // Validé : le formateur peut ensuite retirer puis republier sans nouvelle validation
        mvc.perform(post("/api/v1/courses/" + id + "/unpublish").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void seuls_les_admins_valident_et_un_admin_publie_directement() throws Exception {
        String admin = tokenWithRole("admin-pub-" + suffix, RoleName.ADMIN);
        String prof = tokenWithRole("prof-c-" + suffix, RoleName.INSTRUCTOR);
        String learner = tokenWithRole("eleve-" + suffix, null);
        long id = createCourse(prof, "Cours de controle des droits " + suffix);
        addChapter(prof, id);
        mvc.perform(post("/api/v1/courses/" + id + "/submit-review").header("Authorization", "Bearer " + prof));

        mvc.perform(get("/api/v1/admin/course-reviews").header("Authorization", "Bearer " + learner))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/course-reviews/" + id + "/approve").header("Authorization", "Bearer " + prof))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/course-reviews/" + id + "/approve").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        // Admin : publication directe de son propre cours, sans file d'attente
        long own = createCourse(admin, "Cours admin " + suffix);
        mvc.perform(post("/api/v1/courses/" + own + "/publish").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("APPROVED"));
    }

    // --- outils ---

    private long createCourse(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"language\":\"fr\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void addChapter(String token, long courseId) throws Exception {
        mvc.perform(post("/api/v1/courses/" + courseId + "/chapters").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"title\":\"Chapitre 1\",\"position\":1}"))
                .andExpect(status().isCreated());
    }

    /** Crée un compte {@code <nom>@example.com} (avec le rôle donné en plus d'apprenant) et renvoie son jeton. */
    private String tokenWithRole(String name, RoleName role) throws Exception {
        String email = name + "@example.com";
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test " + name + "\"}"))
                .andExpect(status().isCreated());
        if (role != null) {
            User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
            user.getRoles().add(roleRepository.findByName(role).orElseThrow());
            userRepository.save(user);
        }
        String response = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}

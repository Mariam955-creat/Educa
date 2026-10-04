package com.educa.backend.course;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Cartes de cours : image de couverture, nombre d'apprenants et note moyenne. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseCardTest {

    /** Contenus créés par {@link #publishedCourse}, par cours. */
    private final Map<Long, List<Long>> contentIdsByCourse = new HashMap<>();

    private static final int CONTENTS_PER_COURSE = 10;

    private static final byte[] PNG_MAGIC = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void seul_un_inscrit_peut_noter_et_la_moyenne_remonte_dans_le_catalogue() throws Exception {
        String prof = instructorToken("card-prof1@example.com");
        long courseId = publishedCourse(prof, "Cours note par ses eleves");

        String curieux = learnerToken("card-curieux@example.com");
        rate(curieux, courseId, 5).andExpect(status().isForbidden());

        String eleve1 = learnerToken("card-eleve1@example.com");
        String eleve2 = learnerToken("card-eleve2@example.com");
        enroll(eleve1, courseId);
        enroll(eleve2, courseId);

        rate(eleve1, courseId, 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myStars").value(4))
                .andExpect(jsonPath("$.canRate").value(true));
        rate(eleve2, courseId, 5).andExpect(status().isOk());
        // Une nouvelle note remplace la précédente, elle ne s'ajoute pas
        rate(eleve2, courseId, 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(3.5))
                .andExpect(jsonPath("$.count").value(2));

        mvc.perform(get("/api/v1/courses").param("q", "Cours note par ses eleves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].learnerCount").value(2))
                .andExpect(jsonPath("$.content[0].averageRating").value(3.5))
                .andExpect(jsonPath("$.content[0].ratingCount").value(2));

        // Lecture publique : pas de note personnelle ni de droit de noter pour un anonyme
        mvc.perform(get("/api/v1/courses/" + courseId + "/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(3.5))
                .andExpect(jsonPath("$.myStars").doesNotExist())
                .andExpect(jsonPath("$.canRate").value(false));
    }

    @Test
    void un_avis_ecrit_est_publie_sur_la_page_du_cours() throws Exception {
        String prof = instructorToken("card-prof7@example.com");
        long courseId = publishedCourse(prof, "Cours commente");
        String eleve1 = learnerToken("card-eleve4@example.com");
        String eleve2 = learnerToken("card-eleve5@example.com");
        enroll(eleve1, courseId);
        enroll(eleve2, courseId);

        mvc.perform(put("/api/v1/courses/" + courseId + "/rating")
                        .header("Authorization", "Bearer " + eleve1)
                        .contentType(APPLICATION_JSON)
                        .content("{\"stars\":5,\"comment\":\"  Tres clair, merci !  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myComment").value("Tres clair, merci !"));
        // Note seule : comptée dans la moyenne, mais absente de la liste des avis écrits
        rate(eleve2, courseId, 3).andExpect(status().isOk());

        mvc.perform(get("/api/v1/courses/" + courseId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].stars").value(5))
                .andExpect(jsonPath("$.content[0].comment").value("Tres clair, merci !"))
                .andExpect(jsonPath("$.content[0].authorName").value("Test"));
    }

    @Test
    void un_avis_trop_long_est_refuse() throws Exception {
        String prof = instructorToken("card-prof8@example.com");
        long courseId = publishedCourse(prof, "Cours aux avis bornes");
        String eleve = learnerToken("card-eleve6@example.com");
        enroll(eleve, courseId);

        mvc.perform(put("/api/v1/courses/" + courseId + "/rating")
                        .header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content("{\"stars\":4,\"comment\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void un_admin_modere_les_avis_et_la_moyenne_est_recalculee() throws Exception {
        String prof = instructorToken("card-prof9@example.com");
        long courseId = publishedCourse(prof, "Cours modere");
        String eleve1 = learnerToken("card-eleve7@example.com");
        String eleve2 = learnerToken("card-eleve8@example.com");
        enroll(eleve1, courseId);
        enroll(eleve2, courseId);
        rate(eleve1, courseId, 5).andExpect(status().isOk());
        mvc.perform(put("/api/v1/courses/" + courseId + "/rating")
                        .header("Authorization", "Bearer " + eleve2)
                        .contentType(APPLICATION_JSON)
                        .content("{\"stars\":1,\"comment\":\"Avis abusif\"}"))
                .andExpect(status().isOk());

        // Un apprenant ou un formateur n'a pas accès à la modération
        mvc.perform(get("/api/v1/admin/reviews").header("Authorization", "Bearer " + eleve1))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/reviews").header("Authorization", "Bearer " + prof))
                .andExpect(status().isForbidden());

        String admin = adminToken("card-admin@example.com");
        String body = mvc.perform(get("/api/v1/admin/reviews").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment").value("Avis abusif"))
                .andExpect(jsonPath("$.content[0].courseTitle").value("Cours modere"))
                .andExpect(jsonPath("$.content[0].stars").value(1))
                .andReturn().getResponse().getContentAsString();
        long reviewId = ((Number) JsonPath.read(body, "$.content[0].id")).longValue();

        mvc.perform(delete("/api/v1/admin/reviews/" + reviewId).header("Authorization", "Bearer " + eleve2))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/reviews/" + reviewId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/reviews/" + reviewId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/courses/" + courseId + "/rating"))
                .andExpect(jsonPath("$.average").value(5.0))
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(get("/api/v1/courses/" + courseId + "/reviews"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void il_faut_avoir_suivi_70_pourcent_du_cours_pour_le_noter() throws Exception {
        String prof = instructorToken("card-prof10@example.com");
        long courseId = publishedCourse(prof, "Cours a suivre avant de noter");
        String eleve = learnerToken("card-eleve9@example.com");
        enrollOnly(eleve, courseId);

        follow(eleve, courseId, 6);
        mvc.perform(get("/api/v1/courses/" + courseId + "/rating").header("Authorization", "Bearer " + eleve))
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.progressPercent").value(60))
                .andExpect(jsonPath("$.requiredProgress").value(70))
                .andExpect(jsonPath("$.canRate").value(false));
        rate(eleve, courseId, 5).andExpect(status().isForbidden());

        // 7 contenus sur 10 : le seuil de 70 % est atteint
        mvc.perform(post("/api/v1/contents/" + contentIdsByCourse.get(courseId).get(6) + "/complete")
                        .header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk());
        rate(eleve, courseId, 5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(70))
                .andExpect(jsonPath("$.canRate").value(true));
    }

    @Test
    void un_apprenant_liste_et_retire_ses_propres_avis() throws Exception {
        String prof = instructorToken("card-prof11@example.com");
        long courseId = publishedCourse(prof, "Cours de mon espace");
        String eleve = learnerToken("card-eleve10@example.com");
        String autre = learnerToken("card-eleve11@example.com");
        enroll(eleve, courseId);
        enroll(autre, courseId);
        mvc.perform(put("/api/v1/courses/" + courseId + "/rating")
                        .header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content("{\"stars\":4,\"comment\":\"Bien construit\"}"))
                .andExpect(status().isOk());
        rate(autre, courseId, 2).andExpect(status().isOk());

        String body = mvc.perform(get("/api/v1/me/reviews").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseTitle").value("Cours de mon espace"))
                .andExpect(jsonPath("$[0].comment").value("Bien construit"))
                .andReturn().getResponse().getContentAsString();
        long reviewId = ((Number) JsonPath.read(body, "$[0].id")).longValue();

        // L'avis d'un autre apprenant est traité comme inexistant
        mvc.perform(delete("/api/v1/me/reviews/" + reviewId).header("Authorization", "Bearer " + autre))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/me/reviews/" + reviewId).header("Authorization", "Bearer " + eleve))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/reviews").header("Authorization", "Bearer " + eleve))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/me/reviews")).andExpect(status().isUnauthorized());
    }

    @Test
    void une_note_hors_bornes_est_refusee() throws Exception {
        String prof = instructorToken("card-prof2@example.com");
        long courseId = publishedCourse(prof, "Cours aux notes bornees");
        String eleve = learnerToken("card-eleve3@example.com");
        enroll(eleve, courseId);

        rate(eleve, courseId, 0).andExpect(status().isBadRequest());
        rate(eleve, courseId, 6).andExpect(status().isBadRequest());
    }

    @Test
    void un_cours_sans_note_ni_inscrit_affiche_des_compteurs_a_zero() throws Exception {
        String prof = instructorToken("card-prof3@example.com");
        publishedCourse(prof, "Cours tout neuf");

        mvc.perform(get("/api/v1/courses").param("q", "Cours tout neuf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].learnerCount").value(0))
                .andExpect(jsonPath("$.content[0].averageRating").doesNotExist())
                .andExpect(jsonPath("$.content[0].ratingCount").value(0))
                .andExpect(jsonPath("$.content[0].coverImageUrl").doesNotExist());
    }

    @Test
    void le_proprietaire_televerse_une_couverture_servie_publiquement() throws Exception {
        String prof = instructorToken("card-prof4@example.com");
        long courseId = publishedCourse(prof, "Cours illustre");

        String body = mvc.perform(multipart("/api/v1/courses/" + courseId + "/cover")
                        .file(new MockMultipartFile("file", "couverture.png", "image/png", PNG_MAGIC))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").value(startsWith("/api/v1/courses/" + courseId + "/cover?v=")))
                .andReturn().getResponse().getContentAsString();
        String coverUrl = JsonPath.read(body, "$.coverImageUrl");

        // Sans jeton : une balise <img> n'envoie pas le JWT
        mvc.perform(get(coverUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mvc.perform(delete("/api/v1/courses/" + courseId + "/cover").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist());
        mvc.perform(get("/api/v1/courses/" + courseId + "/cover")).andExpect(status().isNotFound());
    }

    @Test
    void une_couverture_qui_nest_pas_une_image_est_refusee() throws Exception {
        String prof = instructorToken("card-prof5@example.com");
        long courseId = publishedCourse(prof, "Cours a la fausse image");

        // PDF déguisé en PNG : le type réel est détecté sur le contenu
        mvc.perform(multipart("/api/v1/courses/" + courseId + "/cover")
                        .file(new MockMultipartFile("file", "couverture.png", "image/png", "%PDF-1.4\n".getBytes()))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void un_autre_formateur_ne_peut_pas_changer_la_couverture() throws Exception {
        String owner = instructorToken("card-owner@example.com");
        long courseId = publishedCourse(owner, "Cours protege");

        String other = instructorToken("card-autre@example.com");
        mvc.perform(multipart("/api/v1/courses/" + courseId + "/cover")
                        .file(new MockMultipartFile("file", "couverture.png", "image/png", PNG_MAGIC))
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
    }

    @Test
    void la_couverture_dun_cours_non_publie_reste_privee() throws Exception {
        String prof = instructorToken("card-prof6@example.com");
        long courseId = createCourse(prof, "Cours en brouillon");
        mvc.perform(multipart("/api/v1/courses/" + courseId + "/cover")
                        .file(new MockMultipartFile("file", "couverture.png", "image/png", PNG_MAGIC))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/courses/" + courseId + "/cover")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/courses/" + courseId + "/cover").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.ResultActions rate(String token, long courseId, int stars)
            throws Exception {
        return mvc.perform(put("/api/v1/courses/" + courseId + "/rating")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content("{\"stars\":" + stars + "}"));
    }

    /** Inscrit l'apprenant et lui fait suivre tout le cours (assez pour pouvoir le noter). */
    private void enroll(String token, long courseId) throws Exception {
        enrollOnly(token, courseId);
        follow(token, courseId, CONTENTS_PER_COURSE);
    }

    private void enrollOnly(String token, long courseId) throws Exception {
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    /** Marque les {@code count} premiers contenus du cours comme terminés. */
    private void follow(String token, long courseId, int count) throws Exception {
        for (long contentId : contentIdsByCourse.get(courseId).subList(0, count)) {
            mvc.perform(post("/api/v1/contents/" + contentId + "/complete").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    /** Cours publié de {@value #CONTENTS_PER_COURSE} contenus texte (1 contenu = 10 % de progression). */
    private long publishedCourse(String token, String title) throws Exception {
        long courseId = createCourse(token, title);
        String chapter = mvc.perform(post("/api/v1/courses/" + courseId + "/chapters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Chapitre 1\",\"position\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long chapterId = ((Number) JsonPath.read(chapter, "$.id")).longValue();
        List<Long> contentIds = new ArrayList<>();
        for (int position = 1; position <= CONTENTS_PER_COURSE; position++) {
            String content = mvc.perform(post("/api/v1/chapters/" + chapterId + "/contents")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content("{\"type\":\"TEXT\",\"title\":\"Lecon " + position + "\",\"position\":"
                                    + position + ",\"textBody\":\"contenu\"}"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            contentIds.add(((Number) JsonPath.read(content, "$.id")).longValue());
        }
        contentIdsByCourse.put(courseId, contentIds);
        mvc.perform(post("/api/v1/courses/" + courseId + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return courseId;
    }

    private long createCourse(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"language\":\"fr\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String instructorToken(String email) throws Exception {
        register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Role instructor = roleRepository.findByName(RoleName.INSTRUCTOR).orElseThrow();
        user.getRoles().add(instructor);
        userRepository.save(user);
        return login(email);
    }

    private String adminToken(String email) throws Exception {
        register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Role admin = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        user.getRoles().add(admin);
        userRepository.save(user);
        return login(email);
    }

    private String learnerToken(String email) throws Exception {
        register(email);
        return login(email);
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test\"}"))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}

package com.educa.backend.course;

import static org.hamcrest.Matchers.contains;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.rating.CourseRating;
import com.educa.backend.rating.CourseRatingRepository;
import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Recherche et filtres du catalogue public (tâche 2.13). Chaque test nomme ses cours avec un mot rare, passé dans
 * {@code q}, pour ne lire que ses propres données.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogSearchTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CourseRatingRepository ratingRepository;

    @Test
    void filtre_par_categorie_niveau_et_texte_du_sous_titre() throws Exception {
        String prof = instructorToken("search-prof1@example.com");
        createCourse(prof, "{\"title\":\"Zorglub design\",\"language\":\"fr\",\"price\":0,"
                + "\"category\":\"DESIGN\",\"level\":\"BEGINNER\"}");
        createCourse(prof, "{\"title\":\"Zorglub Java\",\"language\":\"fr\",\"price\":0,"
                + "\"category\":\"DEVELOPMENT\",\"level\":\"ADVANCED\",\"subtitle\":\"Les flux Quasimodo\"}");
        createCourse(prof, "{\"title\":\"Zorglub Python\",\"language\":\"en\",\"price\":0,"
                + "\"category\":\"DEVELOPMENT\",\"level\":\"BEGINNER\"}");

        mvc.perform(get("/api/v1/courses").param("q", "zorglub").param("category", "DEVELOPMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/courses").param("q", "zorglub").param("category", "DEVELOPMENT")
                        .param("level", "BEGINNER"))
                .andExpect(jsonPath("$.content[*].title", contains("Zorglub Python")));
        mvc.perform(get("/api/v1/courses").param("q", "zorglub").param("language", "fr")
                        .param("level", "BEGINNER"))
                .andExpect(jsonPath("$.content[*].title", contains("Zorglub design")));
        // Le texte cherché est aussi trouvé dans le sous-titre
        mvc.perform(get("/api/v1/courses").param("q", "quasimodo"))
                .andExpect(jsonPath("$.content[*].title", contains("Zorglub Java")));
    }

    @Test
    void prix_maximal_promotions_et_tri_par_prix_suivent_le_prix_paye() throws Exception {
        String prof = instructorToken("search-prof2@example.com");
        String ends = Instant.now().plus(7, ChronoUnit.DAYS).toString();
        createCourse(prof, "{\"title\":\"Krakatoa promo\",\"language\":\"fr\",\"price\":50,"
                + "\"promoPrice\":15,\"promoEndsAt\":\"" + ends + "\"}");
        createCourse(prof, "{\"title\":\"Krakatoa cher\",\"language\":\"fr\",\"price\":25}");
        createCourse(prof, "{\"title\":\"Krakatoa petit prix\",\"language\":\"fr\",\"price\":10}");

        // 50 € barré à 15 € : retenu sous 20 €, car c'est le prix payé qui compte
        mvc.perform(get("/api/v1/courses").param("q", "krakatoa").param("maxPrice", "20")
                        .param("sort", "PRICE_ASC"))
                .andExpect(jsonPath("$.content[*].title", contains("Krakatoa petit prix", "Krakatoa promo")));
        mvc.perform(get("/api/v1/courses").param("q", "krakatoa").param("promo", "true"))
                .andExpect(jsonPath("$.content[*].title", contains("Krakatoa promo")));
        mvc.perform(get("/api/v1/courses").param("q", "krakatoa").param("sort", "PRICE_DESC"))
                .andExpect(jsonPath("$.content[*].title",
                        contains("Krakatoa cher", "Krakatoa promo", "Krakatoa petit prix")));
    }

    @Test
    void tri_par_popularite_et_par_note_avec_note_minimale_et_pagination() throws Exception {
        String prof = instructorToken("search-prof3@example.com");
        long apprecie = createCourse(prof, "{\"title\":\"Tintinnabule apprecie\",\"language\":\"fr\",\"price\":0}");
        long suivi = createCourse(prof, "{\"title\":\"Tintinnabule suivi\",\"language\":\"fr\",\"price\":0}");
        createCourse(prof, "{\"title\":\"Tintinnabule neuf\",\"language\":\"fr\",\"price\":0}");

        Long eleve1 = learner("search-eleve1@example.com", suivi, apprecie);
        Long eleve2 = learner("search-eleve2@example.com", suivi);
        rating(apprecie, eleve1, 5);
        rating(suivi, eleve1, 3);
        rating(suivi, eleve2, 4);

        mvc.perform(get("/api/v1/courses").param("q", "tintinnabule").param("sort", "POPULAR"))
                .andExpect(jsonPath("$.content[0].title").value("Tintinnabule suivi"))
                .andExpect(jsonPath("$.content[1].title").value("Tintinnabule apprecie"));
        // Les cours sans note passent en dernier
        mvc.perform(get("/api/v1/courses").param("q", "tintinnabule").param("sort", "RATING"))
                .andExpect(jsonPath("$.content[*].title",
                        contains("Tintinnabule apprecie", "Tintinnabule suivi", "Tintinnabule neuf")));
        // Moyenne 3,5 pour « suivi » : sous le seuil ; un cours sans note est exclu
        mvc.perform(get("/api/v1/courses").param("q", "tintinnabule").param("minRating", "4"))
                .andExpect(jsonPath("$.content[*].title", contains("Tintinnabule apprecie")));
        // La pagination s'applique après le tri
        mvc.perform(get("/api/v1/courses").param("q", "tintinnabule").param("sort", "RATING")
                        .param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[*].title", contains("Tintinnabule neuf")));
    }

    @Test
    void une_valeur_de_filtre_inconnue_est_refusee() throws Exception {
        mvc.perform(get("/api/v1/courses").param("sort", "CHEAPEST")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/courses").param("category", "COOKING")).andExpect(status().isBadRequest());
    }

    /** Inscrit un nouvel apprenant aux cours donnés et renvoie son id. */
    private Long learner(String email, long... courseIds) throws Exception {
        register(email);
        String token = login(email);
        for (long courseId : courseIds) {
            mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());
        }
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private void rating(long courseId, Long userId, int stars) {
        CourseRating rating = new CourseRating();
        rating.setCourseId(courseId);
        rating.setUserId(userId);
        rating.setStars((short) stars);
        ratingRepository.save(rating);
    }

    /** Crée un cours et le publie (le catalogue ne liste que les cours publiés). */
    private long createCourse(String token, String json) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private String instructorToken(String email) throws Exception {
        register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Role instructor = roleRepository.findByName(RoleName.INSTRUCTOR).orElseThrow();
        user.getRoles().add(instructor);
        userRepository.save(user);
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

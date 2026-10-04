package com.educa.backend.admin;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Corbeille : suppression douce, restauration et suppression définitive des cours, comptes et avis. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TrashTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    // ---------- cours ----------

    @Test
    void un_cours_a_la_corbeille_disparait_puis_se_restaure_en_brouillon() throws Exception {
        String prof = token("trash-prof1@example.com", RoleName.INSTRUCTOR);
        long courseId = publishedCourse(prof, "Cours a jeter");
        String slug = slugOf(prof, courseId);

        mvc.perform(delete("/api/v1/courses/" + courseId).header("Authorization", "Bearer " + prof))
                .andExpect(status().isNoContent());

        // Absent du catalogue, de « Mes cours » et de sa page ; présent dans la corbeille
        mvc.perform(get("/api/v1/courses").param("q", "Cours a jeter")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/instructor/courses").header("Authorization", "Bearer " + prof))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/courses/" + slug).header("Authorization", "Bearer " + prof))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/instructor/trash/courses").header("Authorization", "Bearer " + prof))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Cours a jeter"))
                .andExpect(jsonPath("$[0].deletedAt").exists());

        // Un autre formateur ne peut ni le voir dans sa corbeille ni le restaurer
        String other = token("trash-prof2@example.com", RoleName.INSTRUCTOR);
        mvc.perform(get("/api/v1/instructor/trash/courses").header("Authorization", "Bearer " + other))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/v1/courses/" + courseId + "/restore").header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/courses/" + courseId + "/restore").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));
        mvc.perform(get("/api/v1/instructor/courses").header("Authorization", "Bearer " + prof))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/instructor/trash/courses").header("Authorization", "Bearer " + prof))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void la_suppression_definitive_exige_la_corbeille() throws Exception {
        String prof = token("trash-prof3@example.com", RoleName.INSTRUCTOR);
        long courseId = publishedCourse(prof, "Cours a effacer");

        mvc.perform(delete("/api/v1/courses/" + courseId + "/permanent").header("Authorization", "Bearer " + prof))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/courses/" + courseId).header("Authorization", "Bearer " + prof))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/courses/" + courseId + "/permanent").header("Authorization", "Bearer " + prof))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/courses/" + courseId + "/restore").header("Authorization", "Bearer " + prof))
                .andExpect(status().isNotFound());
    }

    @Test
    void ladmin_voit_la_corbeille_de_tous_les_formateurs() throws Exception {
        String prof = token("trash-prof4@example.com", RoleName.INSTRUCTOR);
        long courseId = publishedCourse(prof, "Cours dans la corbeille admin");
        mvc.perform(delete("/api/v1/courses/" + courseId).header("Authorization", "Bearer " + prof))
                .andExpect(status().isNoContent());

        String admin = token("trash-admin1@example.com", RoleName.ADMIN);
        String trash = mvc.perform(get("/api/v1/admin/trash/courses").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(trash).contains("Cours dans la corbeille admin");
        mvc.perform(get("/api/v1/admin/trash/courses").header("Authorization", "Bearer " + prof))
                .andExpect(status().isForbidden());
    }

    // ---------- comptes ----------

    @Test
    void un_compte_a_la_corbeille_ne_peut_plus_se_connecter_puis_se_restaure() throws Exception {
        String admin = token("trash-admin2@example.com", RoleName.ADMIN);
        token("trash-eleve1@example.com", RoleName.LEARNER);
        Long userId = idOf("trash-eleve1@example.com");

        mvc.perform(delete("/api/v1/admin/users/" + userId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        login("trash-eleve1@example.com").andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/users").param("q", "trash-eleve1").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/admin/users/trash").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$[?(@.email == 'trash-eleve1@example.com')]").exists());

        mvc.perform(post("/api/v1/admin/users/" + userId + "/restore").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
        login("trash-eleve1@example.com").andExpect(status().isOk());
    }

    @Test
    void un_admin_ne_peut_pas_se_supprimer_et_un_formateur_avec_cours_reste_protege() throws Exception {
        String admin = token("trash-admin3@example.com", RoleName.ADMIN);
        mvc.perform(delete("/api/v1/admin/users/" + idOf("trash-admin3@example.com")).header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        String prof = token("trash-prof5@example.com", RoleName.INSTRUCTOR);
        publishedCourse(prof, "Cours d'un formateur supprime");
        Long profId = idOf("trash-prof5@example.com");
        mvc.perform(delete("/api/v1/admin/users/" + profId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/users/" + profId + "/permanent").header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        token("trash-eleve2@example.com", RoleName.LEARNER);
        Long learnerId = idOf("trash-eleve2@example.com");
        mvc.perform(delete("/api/v1/admin/users/" + learnerId + "/permanent").header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict()); // pas encore à la corbeille
        mvc.perform(delete("/api/v1/admin/users/" + learnerId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/users/" + learnerId + "/permanent").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(userRepository.findById(learnerId)).isEmpty();
    }

    // ---------- avis ----------

    @Test
    void un_avis_modere_sort_de_la_moyenne_et_peut_revenir() throws Exception {
        String prof = token("trash-prof6@example.com", RoleName.INSTRUCTOR);
        long courseId = publishedCourse(prof, "Cours note puis modere");
        String eleve = token("trash-eleve3@example.com", RoleName.LEARNER);
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());
        // Cours sans contenu : progression 0 % → on lui ajoute un contenu terminé pour atteindre 100 %
        followAll(prof, eleve, courseId);
        rate(eleve, courseId, 1, "Avis abusif").andExpect(status().isOk());

        String admin = token("trash-admin4@example.com", RoleName.ADMIN);
        String registry = mvc.perform(get("/api/v1/admin/reviews").header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        long reviewId = ((Number) JsonPath.read(registry, "$.content[0].id")).longValue();

        mvc.perform(delete("/api/v1/admin/reviews/" + reviewId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/courses/" + courseId + "/rating")).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get("/api/v1/admin/reviews/trash").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$[0].comment").value("Avis abusif"));

        mvc.perform(post("/api/v1/admin/reviews/" + reviewId + "/restore").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/courses/" + courseId + "/rating")).andExpect(jsonPath("$.count").value(1));

        mvc.perform(delete("/api/v1/admin/reviews/" + reviewId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/reviews/" + reviewId + "/permanent").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/reviews/trash").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void un_apprenant_peut_noter_a_nouveau_apres_moderation() throws Exception {
        String prof = token("trash-prof7@example.com", RoleName.INSTRUCTOR);
        long courseId = publishedCourse(prof, "Cours renote");
        String eleve = token("trash-eleve4@example.com", RoleName.LEARNER);
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());
        followAll(prof, eleve, courseId);
        rate(eleve, courseId, 1, "Premier avis").andExpect(status().isOk());

        String admin = token("trash-admin5@example.com", RoleName.ADMIN);
        String registry = mvc.perform(get("/api/v1/admin/reviews").header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        long reviewId = ((Number) JsonPath.read(registry, "$.content[0].id")).longValue();
        mvc.perform(delete("/api/v1/admin/reviews/" + reviewId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        // L'ancien avis à la corbeille ne bloque pas un nouvel avis (unicité cours/apprenant)
        rate(eleve, courseId, 4, "Avis corrige")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myStars").value(4))
                .andExpect(jsonPath("$.count").value(1));
    }

    // ---------- helpers ----------

    private ResultActions rate(String token, long courseId, int stars, String comment) throws Exception {
        return mvc.perform(put("/api/v1/courses/" + courseId + "/rating").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content("{\"stars\":" + stars + ",\"comment\":\"" + comment + "\"}"));
    }

    /** Ajoute un chapitre + un contenu au cours et le fait terminer par l'apprenant (progression 100 %). */
    private void followAll(String prof, String eleve, long courseId) throws Exception {
        String chapter = mvc.perform(post("/api/v1/courses/" + courseId + "/chapters").header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON).content("{\"title\":\"Chapitre\",\"position\":1}"))
                .andReturn().getResponse().getContentAsString();
        long chapterId = ((Number) JsonPath.read(chapter, "$.id")).longValue();
        String content = mvc.perform(post("/api/v1/chapters/" + chapterId + "/contents").header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"type\":\"TEXT\",\"title\":\"Lecon\",\"position\":1,\"textBody\":\"x\"}"))
                .andReturn().getResponse().getContentAsString();
        long contentId = ((Number) JsonPath.read(content, "$.id")).longValue();
        mvc.perform(post("/api/v1/contents/" + contentId + "/complete").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk());
    }

    private long publishedCourse(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"language\":\"fr\",\"price\":0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private String slugOf(String token, long courseId) throws Exception {
        String list = mvc.perform(get("/api/v1/instructor/courses").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(list, "$[?(@.id == " + courseId + ")].slug").toString().replaceAll("[\\[\\]\"]", "");
    }

    private Long idOf(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"));
    }

    private String token(String email, RoleName role) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test\"}"))
                .andExpect(status().isCreated());
        if (role != RoleName.LEARNER) {
            User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
            user.getRoles().add(roleRepository.findByName(role).orElseThrow());
            userRepository.save(user);
        }
        return JsonPath.read(login(email).andReturn().getResponse().getContentAsString(), "$.accessToken");
    }
}

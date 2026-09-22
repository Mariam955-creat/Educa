package com.educa.backend.course;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void formateur_cree_un_cours_avec_slug() throws Exception {
        String token = instructorToken("prof1@example.com");
        mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Introduction à Python","description":"Bases","language":"fr"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("introduction-a-python"))
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void apprenant_ne_peut_pas_creer_de_cours() throws Exception {
        String token = learnerToken("eleve1@example.com");
        mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Tentative\",\"language\":\"fr\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void catalogue_ne_liste_que_les_cours_publies() throws Exception {
        // catalogue accessible sans filtre (paramètre q null)
        mvc.perform(get("/api/v1/courses")).andExpect(status().isOk());

        String token = instructorToken("prof2@example.com");
        long id = createCourse(token, "Cours brouillon");

        mvc.perform(get("/api/v1/courses").param("q", "brouillon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/courses").param("q", "brouillon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void inscription_puis_progression() throws Exception {
        String prof = instructorToken("prof3@example.com");
        long courseId = createCourse(prof, "Cours suivi");
        long chapterId = createChapter(prof, courseId, "Chapitre 1", 1);
        long contentId = createTextContent(prof, chapterId, "Leçon 1", 1);
        mvc.perform(post("/api/v1/courses/" + courseId + "/publish").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());

        String eleve = learnerToken("eleve3@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progressPercent").value(0));

        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/contents/" + contentId + "/complete").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(100))
                .andExpect(jsonPath("$.completedContents").value(1));
    }

    @Test
    void detail_cache_les_contenus_si_non_inscrit() throws Exception {
        String prof = instructorToken("prof4@example.com");
        long courseId = createCourse(prof, "Cours visibilité");
        long chapterId = createChapter(prof, courseId, "Chapitre 1", 1);
        createTextContent(prof, chapterId, "Leçon secrète", 1);
        mvc.perform(post("/api/v1/courses/" + courseId + "/publish").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());

        // anonyme : pas de contenus
        mvc.perform(get("/api/v1/courses/cours-visibilite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentsVisible").value(false))
                .andExpect(jsonPath("$.chapters[0].contents.length()").value(0));

        // inscrit : contenus visibles
        String eleve = learnerToken("eleve4@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/courses/cours-visibilite").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentsVisible").value(true))
                .andExpect(jsonPath("$.chapters[0].contents.length()").value(1));
    }

    // ---------- helpers ----------

    private long createCourse(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"language\":\"fr\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createChapter(String token, long courseId, String title, int position) throws Exception {
        String body = mvc.perform(post("/api/v1/courses/" + courseId + "/chapters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"position\":" + position + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createTextContent(String token, long chapterId, String title, int position) throws Exception {
        String body = mvc.perform(post("/api/v1/chapters/" + chapterId + "/contents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"type\":\"TEXT\",\"title\":\"" + title + "\",\"position\":" + position
                                + ",\"textBody\":\"contenu\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String learnerToken(String email) throws Exception {
        register(email);
        return login(email);
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

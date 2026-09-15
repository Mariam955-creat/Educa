package com.educa.backend.course;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Tests d'intégration des traductions de contenu des cours (Should have). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseTranslationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void apprenant_ne_peut_pas_gerer_les_traductions() throws Exception {
        String prof = instructorToken("trad-prof1@example.com");
        long courseId = createCourse(prof, "Cours traduit 1");

        String eleve = learnerToken("trad-eleve1@example.com");
        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"Translated title\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void un_autre_formateur_ne_peut_pas_traduire_le_cours_dautrui() throws Exception {
        String owner = instructorToken("trad-owner@example.com");
        long courseId = createCourse(owner, "Cours d'un autre");

        String other = instructorToken("trad-autre@example.com");
        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + other)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"Hijack\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void proprietaire_ajoute_puis_relit_une_traduction_complete() throws Exception {
        String prof = instructorToken("trad-prof2@example.com");
        long courseId = createCourse(prof, "Introduction au Python");
        long chapterId = createChapter(prof, courseId, "Les bases", 1);

        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"Introduction to Python\",\"courseDescription\":\"Learn the basics\","
                                + "\"chapters\":[{\"chapterId\":" + chapterId + ",\"title\":\"The basics\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("en"))
                .andExpect(jsonPath("$.courseTitle").value("Introduction to Python"))
                .andExpect(jsonPath("$.chapters[0].translatedTitle").value("The basics"))
                .andExpect(jsonPath("$.chapters[0].originalTitle").value("Les bases"));

        mvc.perform(get("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseTitle").value("Introduction to Python"));

        mvc.perform(get("/api/v1/courses/" + courseId + "/translations")
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("en"));
    }

    @Test
    void un_titre_de_chapitre_vide_efface_sa_traduction() throws Exception {
        String prof = instructorToken("trad-prof3@example.com");
        long courseId = createCourse(prof, "Cours à corriger");
        long chapterId = createChapter(prof, courseId, "Chapitre unique", 1);

        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"Course to fix\","
                                + "\"chapters\":[{\"chapterId\":" + chapterId + ",\"title\":\"Only chapter\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].translatedTitle").value("Only chapter"));

        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"Course to fix\","
                                + "\"chapters\":[{\"chapterId\":" + chapterId + ",\"title\":\"\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].translatedTitle").doesNotExist());
    }

    @Test
    void suppression_de_la_langue_retire_traduction_de_cours_et_de_chapitres() throws Exception {
        String prof = instructorToken("trad-prof4@example.com");
        long courseId = createCourse(prof, "Cours à retirer");
        long chapterId = createChapter(prof, courseId, "Chapitre", 1);

        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"To remove\","
                                + "\"chapters\":[{\"chapterId\":" + chapterId + ",\"title\":\"Chapter\"}]}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseTitle").doesNotExist())
                .andExpect(jsonPath("$.chapters[0].translatedTitle").doesNotExist());
    }

    @Test
    void langue_desactivee_refusee_a_lecriture() throws Exception {
        String admin = adminToken("trad-admin@example.com");
        mvc.perform(patch("/api/v1/admin/languages/ar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());

        String prof = instructorToken("trad-prof5@example.com");
        long courseId = createCourse(prof, "Cours langue désactivée");

        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/ar")
                        .header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"عنوان\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chapitre_dun_autre_cours_refuse() throws Exception {
        String prof = instructorToken("trad-prof6@example.com");
        long courseId = createCourse(prof, "Cours A");
        long autreCoursId = createCourse(prof, "Cours B");
        long autreChapterId = createChapter(prof, autreCoursId, "Chapitre B", 1);

        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"Course A\","
                                + "\"chapters\":[{\"chapterId\":" + autreChapterId + ",\"title\":\"Hijack\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogue_et_detail_affichent_la_traduction_si_demandee_sinon_loriginal() throws Exception {
        String prof = instructorToken("trad-prof7@example.com");
        long courseId = createCourse(prof, "Cours multilingue unique");
        mvc.perform(put("/api/v1/courses/" + courseId + "/translations/en")
                        .header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseTitle\":\"Unique multilingual course\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/courses/" + courseId + "/publish").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/courses").param("q", "multilingue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Cours multilingue unique"));

        mvc.perform(get("/api/v1/courses").param("q", "multilingue").param("displayLanguage", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Unique multilingual course"));

        String slug = "cours-multilingue-unique";
        mvc.perform(get("/api/v1/courses/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cours multilingue unique"));

        mvc.perform(get("/api/v1/courses/" + slug).param("displayLanguage", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Unique multilingual course"));
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

    private String adminToken(String email) throws Exception {
        register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Role admin = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        user.getRoles().add(admin);
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

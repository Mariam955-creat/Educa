package com.educa.backend.ai;

import static org.springframework.http.MediaType.APPLICATION_JSON;
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

/**
 * Profil {@code test} : {@code educa.ai.enabled=false} → {@link DisabledAiAssistant}.
 * On vérifie le contrôle d'accès, la validation de la requête et la réponse « dégradée ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiChatTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    // ---------- contrôle d'accès ----------

    @Test
    void chat_refuse_si_non_authentifie() throws Exception {
        mvc.perform(post("/api/v1/ai/chat").contentType(APPLICATION_JSON)
                        .content("{\"courseId\":1,\"message\":\"Bonjour\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chat_refuse_si_non_inscrit() throws Exception {
        String prof = instructorToken("ai-prof@example.com");
        long courseId = createPublishedCourse(prof);

        String eleve = learnerToken("ai-eleve1@example.com");
        mvc.perform(post("/api/v1/ai/chat").header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"message\":\"Bonjour\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chat_autorise_pour_le_proprietaire_du_cours_non_inscrit() throws Exception {
        String prof = instructorToken("ai-prof-owner@example.com");
        long courseId = createPublishedCourse(prof);

        // le formateur propriétaire n'est pas « inscrit » mais doit pouvoir tester son assistant
        mvc.perform(post("/api/v1/ai/chat").header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"message\":\"Résume le cours\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded").value(true));
    }

    @Test
    void chat_autorise_pour_un_admin_non_inscrit() throws Exception {
        String prof = instructorToken("ai-prof-admincase@example.com");
        long courseId = createPublishedCourse(prof);

        String admin = adminToken("ai-admin@example.com");
        mvc.perform(post("/api/v1/ai/chat").header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"message\":\"Bonjour\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded").value(true));
    }

    // ---------- réponse dégradée ----------

    @Test
    void chat_repli_quand_ia_desactivee() throws Exception {
        String prof = instructorToken("ai-prof2@example.com");
        long courseId = createPublishedCourse(prof);

        String eleve = learnerToken("ai-eleve2@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/ai/chat").header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"message\":\"Quel est le sujet ?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded").value(true))
                .andExpect(jsonPath("$.reply").isNotEmpty());
    }

    @Test
    void chat_accepte_un_historique_dans_la_requete() throws Exception {
        String prof = instructorToken("ai-prof-hist@example.com");
        long courseId = createPublishedCourse(prof);

        String eleve = learnerToken("ai-eleve-hist@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());

        String body = "{\"courseId\":" + courseId + ",\"message\":\"Et ensuite ?\","
                + "\"history\":[{\"role\":\"user\",\"content\":\"Bonjour\"},"
                + "{\"role\":\"assistant\",\"content\":\"Bonjour, comment puis-je aider ?\"}]}";
        mvc.perform(post("/api/v1/ai/chat").header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded").value(true));
    }

    // ---------- validation de la requête ----------

    @Test
    void chat_rejette_un_message_vide() throws Exception {
        String prof = instructorToken("ai-prof-empty@example.com");
        long courseId = createPublishedCourse(prof);
        String eleve = enrolledLearnerToken("ai-eleve-empty@example.com", courseId);

        mvc.perform(post("/api/v1/ai/chat").header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_rejette_un_message_trop_long() throws Exception {
        String prof = instructorToken("ai-prof-long@example.com");
        long courseId = createPublishedCourse(prof);
        String eleve = enrolledLearnerToken("ai-eleve-long@example.com", courseId);

        String tooLong = "x".repeat(2001);
        mvc.perform(post("/api/v1/ai/chat").header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"message\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_rejette_une_requete_sans_courseId() throws Exception {
        String eleve = learnerToken("ai-eleve-nocourse@example.com");
        mvc.perform(post("/api/v1/ai/chat").header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"Bonjour\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private long createPublishedCourse(String token) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"title\":\"Cours IA\",\"language\":\"fr\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private String learnerToken(String email) throws Exception {
        register(email);
        return login(email);
    }

    private String enrolledLearnerToken(String email, long courseId) throws Exception {
        String token = learnerToken(email);
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        return token;
    }

    private String instructorToken(String email) throws Exception {
        return tokenWithRole(email, RoleName.INSTRUCTOR);
    }

    private String adminToken(String email) throws Exception {
        return tokenWithRole(email, RoleName.ADMIN);
    }

    private String tokenWithRole(String email, RoleName roleName) throws Exception {
        register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Role role = roleRepository.findByName(roleName).orElseThrow();
        user.getRoles().add(role);
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

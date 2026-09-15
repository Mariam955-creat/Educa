package com.educa.backend.language;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/** Tests d'intégration de la gestion admin des langues actives. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminLanguageControllerTest {

    private static final String PUBLIC_LANGUAGES = "/api/v1/languages";
    private static final String ADMIN_LANGUAGES = "/api/v1/admin/languages";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void liste_publique_des_langues_actives_sans_authentification() throws Exception {
        mvc.perform(get(PUBLIC_LANGUAGES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.code=='fr')].active").value(true));
    }

    @Test
    void apprenant_ne_peut_pas_gerer_les_langues() throws Exception {
        String token = learnerToken("nonadmin-lang@example.com");
        mvc.perform(get(ADMIN_LANGUAGES).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_liste_toutes_les_langues() throws Exception {
        String token = adminToken("admin-lang-list@example.com");
        mvc.perform(get(ADMIN_LANGUAGES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void admin_desactive_puis_reactive_une_langue() throws Exception {
        String token = adminToken("admin-lang-toggle@example.com");
        mvc.perform(patch(ADMIN_LANGUAGES + "/ar").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get(PUBLIC_LANGUAGES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(patch(ADMIN_LANGUAGES + "/ar").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void admin_ne_peut_pas_desactiver_la_derniere_langue_active() throws Exception {
        String token = adminToken("admin-lang-lastone@example.com");
        mvc.perform(patch(ADMIN_LANGUAGES + "/en").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(patch(ADMIN_LANGUAGES + "/ar").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());

        mvc.perform(patch(ADMIN_LANGUAGES + "/fr").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_code_langue_inconnu_renvoie404() throws Exception {
        String token = adminToken("admin-lang-404@example.com");
        mvc.perform(patch(ADMIN_LANGUAGES + "/xx").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creer_un_cours_avec_une_langue_desactivee_renvoie400() throws Exception {
        String adminTok = adminToken("admin-lang-course@example.com");
        mvc.perform(patch(ADMIN_LANGUAGES + "/ar").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());

        String instructorTok = instructorToken("prof-lang@example.com");
        mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + instructorTok)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Cours en arabe\",\"language\":\"ar\"}"))
                .andExpect(status().isBadRequest());
    }

    private String learnerToken(String email) throws Exception {
        register(email);
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

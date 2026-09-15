package com.educa.backend.user;

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

import com.jayway.jsonpath.JsonPath;

/**
 * Tests d'intégration du module admin — gestion des utilisateurs (rôles / statut).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserControllerTest {

    private static final String USERS = "/api/v1/admin/users";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void apprenant_ne_peut_pas_lister_les_utilisateurs() throws Exception {
        String token = learnerToken("nonadmin-list@example.com");
        mvc.perform(get(USERS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_liste_et_recherche_les_utilisateurs() throws Exception {
        String adminTok = adminToken("admin-list@example.com");
        learnerToken("cherche-moi@example.com");

        mvc.perform(get(USERS).param("q", "cherche-moi").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("cherche-moi@example.com"))
                .andExpect(jsonPath("$.content[0].enabled").value(true));
    }

    @Test
    void admin_promeut_un_apprenant_en_formateur() throws Exception {
        String adminTok = adminToken("admin-roles@example.com");
        Long learnerId = learnerId("futur-formateur@example.com");

        mvc.perform(patch(USERS + "/" + learnerId + "/roles")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roles\":[\"INSTRUCTOR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("INSTRUCTOR"));
    }

    @Test
    void admin_role_inconnu_renvoie400() throws Exception {
        String adminTok = adminToken("admin-badrole@example.com");
        Long learnerId = learnerId("cible-badrole@example.com");

        mvc.perform(patch(USERS + "/" + learnerId + "/roles")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roles\":[\"SUPERADMIN\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_desactive_un_autre_compte() throws Exception {
        String adminTok = adminToken("admin-status@example.com");
        Long learnerId = learnerId("a-desactiver@example.com");

        mvc.perform(patch(USERS + "/" + learnerId + "/status")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void admin_ne_peut_pas_se_desactiver_lui_meme() throws Exception {
        String email = "admin-self-disable@example.com";
        String adminTok = adminToken(email);
        Long adminId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();

        mvc.perform(patch(USERS + "/" + adminId + "/status")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_ne_peut_pas_retirer_son_propre_role_admin() throws Exception {
        String email = "admin-self-demote@example.com";
        String adminTok = adminToken(email);
        Long adminId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();

        mvc.perform(patch(USERS + "/" + adminId + "/roles")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roles\":[\"LEARNER\"]}"))
                .andExpect(status().isConflict());
    }

    private Long learnerId(String email) throws Exception {
        learnerToken(email);
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
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

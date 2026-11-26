package com.educa.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
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

import com.jayway.jsonpath.JsonPath;

/** Inscription « Je veux enseigner » : demande de rôle formateur, acceptée ou refusée par l'administration. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstructorRequestTest {

    private static final String USERS = "/api/v1/admin/users";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void inscription_formateur_cree_un_apprenant_avec_une_demande() throws Exception {
        register("veut-enseigner@example.com", true);
        User user = userRepository.findByEmailIgnoreCase("veut-enseigner@example.com").orElseThrow();
        assertThat(user.getInstructorRequestedAt()).isNotNull();
        assertThat(user.getRoles()).extracting(r -> r.getName()).containsExactly(RoleName.LEARNER);

        String token = login("veut-enseigner@example.com");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorRequestedAt").isNotEmpty());
    }

    @Test
    void inscription_simple_ne_cree_pas_de_demande() throws Exception {
        register("simple-apprenant@example.com", false);
        assertThat(userRepository.findByEmailIgnoreCase("simple-apprenant@example.com").orElseThrow()
                .getInstructorRequestedAt()).isNull();
    }

    @Test
    void admin_filtre_et_accepte_une_demande() throws Exception {
        String adminTok = adminToken("admin-demandes@example.com");
        register("candidat-ok@example.com", true);
        register("pas-candidat@example.com", false);

        mvc.perform(get(USERS).param("instructorRequests", "true").param("q", "candidat")
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("candidat-ok@example.com"));

        Long id = idOf("candidat-ok@example.com");
        mvc.perform(post(USERS + "/" + id + "/instructor-request/approve").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("LEARNER", "INSTRUCTOR")))
                .andExpect(jsonPath("$.instructorRequestedAt").value(nullValue()));

        // Déjà traitée : une seconde décision est refusée
        mvc.perform(post(USERS + "/" + id + "/instructor-request/reject").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_refuse_une_demande_le_compte_reste_apprenant() throws Exception {
        String adminTok = adminToken("admin-refus@example.com");
        register("candidat-refuse@example.com", true);
        Long id = idOf("candidat-refuse@example.com");

        mvc.perform(post(USERS + "/" + id + "/instructor-request/reject").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("LEARNER")))
                .andExpect(jsonPath("$.instructorRequestedAt").value(nullValue()));
    }

    @Test
    void apprenant_ne_peut_pas_accepter_une_demande() throws Exception {
        register("candidat-x@example.com", true);
        String learnerTok = loginAfterRegister("intrus@example.com");
        mvc.perform(post(USERS + "/" + idOf("candidat-x@example.com") + "/instructor-request/approve")
                        .header("Authorization", "Bearer " + learnerTok))
                .andExpect(status().isForbidden());
    }

    private Long idOf(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private String loginAfterRegister(String email) throws Exception {
        register(email, false);
        return login(email);
    }

    private String adminToken(String email) throws Exception {
        register(email, false);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.getRoles().add(roleRepository.findByName(RoleName.ADMIN).orElseThrow());
        userRepository.save(user);
        return login(email);
    }

    private void register(String email, boolean wantsToTeach) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test\",\"wantsToTeach\":"
                                + wantsToTeach + "}"))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}

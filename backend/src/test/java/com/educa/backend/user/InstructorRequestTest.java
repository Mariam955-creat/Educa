package com.educa.backend.user;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
 * Demandes de rôle formateur (à l'inscription ou depuis l'espace compte), traitées par l'administration,
 * et notification du candidat.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstructorRequestTest {

    private static final String ADMIN = "/api/v1/admin/instructor-requests";
    private static final String MINE = "/api/v1/me/instructor-request";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InstructorRequestRepository requestRepository;

    @Test
    void inscription_formateur_cree_une_demande_en_attente() throws Exception {
        register("veut-enseigner@example.com", true);
        String token = login("veut-enseigner@example.com");

        mvc.perform(get(MINE).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void apprenant_demande_plus_tard_une_seule_demande_en_attente() throws Exception {
        String token = learnerToken("plus-tard@example.com");
        mvc.perform(get(MINE).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());

        mvc.perform(post(MINE).header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                        .content("{\"motivation\":\"Développeuse Java depuis 10 ans\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.motivation").value("Développeuse Java depuis 10 ans"));

        mvc.perform(post(MINE).header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_compte_liste_et_accepte_le_candidat_est_notifie() throws Exception {
        String adminTok = adminToken("admin-demandes@example.com");
        register("candidat-ok@example.com", true);
        long pending = requestRepository.countByStatus(InstructorRequest.Status.PENDING);

        mvc.perform(get(ADMIN + "/count").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(pending));

        Long requestId = pendingRequestId("candidat-ok@example.com");
        mvc.perform(post(ADMIN + "/" + requestId + "/approve").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        User user = userRepository.findByEmailIgnoreCase("candidat-ok@example.com").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(user.getRoles()).extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.LEARNER, RoleName.INSTRUCTOR);

        String candidateTok = login("candidat-ok@example.com");
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + candidateTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].type").value("INSTRUCTOR_REQUEST_APPROVED"));

        // Déjà traitée : une seconde décision est refusée
        mvc.perform(post(ADMIN + "/" + requestId + "/reject").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"trop tard\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void refus_exige_un_motif_transmis_au_candidat_qui_peut_redemander() throws Exception {
        String adminTok = adminToken("admin-refus@example.com");
        register("candidat-refuse@example.com", true);
        Long requestId = pendingRequestId("candidat-refuse@example.com");

        mvc.perform(post(ADMIN + "/" + requestId + "/reject").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post(ADMIN + "/" + requestId + "/reject").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"Présentez un plan de cours détaillé\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        String candidateTok = login("candidat-refuse@example.com");
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + candidateTok))
                .andExpect(jsonPath("$.items[0].type").value("INSTRUCTOR_REQUEST_REJECTED"))
                .andExpect(jsonPath("$.items[0].detail").value("Présentez un plan de cours détaillé"));
        mvc.perform(get(MINE).header("Authorization", "Bearer " + candidateTok))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Présentez un plan de cours détaillé"));

        // Après un refus, une nouvelle demande est possible
        mvc.perform(post(MINE).header("Authorization", "Bearer " + candidateTok).contentType(APPLICATION_JSON)
                        .content("{\"motivation\":\"Voici mon plan\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void notifications_marquees_lues() throws Exception {
        String adminTok = adminToken("admin-notif@example.com");
        register("candidat-notif@example.com", true);
        mvc.perform(post(ADMIN + "/" + pendingRequestId("candidat-notif@example.com") + "/approve")
                .header("Authorization", "Bearer " + adminTok)).andExpect(status().isOk());

        String tok = login("candidat-notif@example.com");
        mvc.perform(post("/api/v1/me/notifications/read-all").header("Authorization", "Bearer " + tok))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + tok))
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.items[0].read").value(true));
    }

    @Test
    void formateur_ne_peut_pas_redemander_et_role_manuel_solde_la_demande() throws Exception {
        String adminTok = adminToken("admin-manuel@example.com");
        register("promu-main@example.com", true);
        Long userId = userRepository.findByEmailIgnoreCase("promu-main@example.com").orElseThrow().getId();

        mvc.perform(patch("/api/v1/admin/users/" + userId + "/roles").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"roles\":[\"LEARNER\",\"INSTRUCTOR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("LEARNER", "INSTRUCTOR")));
        org.assertj.core.api.Assertions.assertThat(
                requestRepository.existsByUserIdAndStatus(userId, InstructorRequest.Status.PENDING)).isFalse();

        String tok = login("promu-main@example.com");
        mvc.perform(post(MINE).header("Authorization", "Bearer " + tok).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void apprenant_ne_peut_pas_traiter_les_demandes() throws Exception {
        String tok = learnerToken("intrus@example.com");
        mvc.perform(get(ADMIN).header("Authorization", "Bearer " + tok)).andExpect(status().isForbidden());
    }

    private Long pendingRequestId(String email) {
        Long userId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
        return requestRepository.findByUserIdAndStatus(userId, InstructorRequest.Status.PENDING).orElseThrow().getId();
    }

    private String learnerToken(String email) throws Exception {
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

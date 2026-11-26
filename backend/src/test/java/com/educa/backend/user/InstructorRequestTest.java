package com.educa.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Demandes de rôle formateur avec pièces jointes (diplôme, CV, pièce d'identité) et motivation ; notification des
 * administrateurs au dépôt ; notification et email au candidat après la décision.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstructorRequestTest {

    private static final String ADMIN = "/api/v1/admin/instructor-requests";
    private static final String MINE = "/api/v1/me/instructor-request";
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj << >> endobj\ntrailer << >>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0x0d, 'I', 'H', 'D', 'R' };

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InstructorRequestRepository requestRepository;

    @Test
    void demande_complete_avec_pieces_et_admin_notifie() throws Exception {
        String adminTok = adminToken("admin-depot@example.com");
        String tok = learnerToken("candidat-complet@example.com");

        submit(tok, "Formatrice Java depuis 10 ans", pdf("diploma", "master.pdf"), pdf("cv", "cv.pdf"),
                png("idDocument", "cni.png"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.documents", hasSize(3)))
                .andExpect(jsonPath("$.documents[*].kind", containsInAnyOrder("DIPLOMA", "CV", "ID_DOCUMENT")));

        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + adminTok))
                .andExpect(jsonPath("$.items[0].type").value("INSTRUCTOR_REQUEST_SUBMITTED"))
                .andExpect(jsonPath("$.items[0].link").value("/admin/instructor-requests"));

        // L'admin télécharge la pièce d'identité : type réel, toujours en pièce jointe, nosniff
        Long requestId = pendingRequestId("candidat-complet@example.com");
        String mine = mvc.perform(get(MINE).header("Authorization", "Bearer " + tok))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(mine, "$.documents[?(@.kind == 'ID_DOCUMENT')].id");
        Integer docId = ids.get(0);
        mvc.perform(get(ADMIN + "/" + requestId + "/documents/" + docId).header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"cni.png\""))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void pieces_et_motivation_obligatoires_types_verifies() throws Exception {
        String tok = learnerToken("candidat-incomplet@example.com");
        submit(tok, "  ", pdf("diploma", "d.pdf"), pdf("cv", "cv.pdf"), png("idDocument", "id.png"))
                .andExpect(status().isBadRequest());
        submit(tok, "Motivation", pdf("diploma", "d.pdf"), pdf("cv", "cv.pdf"))
                .andExpect(status().isBadRequest());
        // Faux PDF (exécutable) : refusé sur son contenu réel, pas sur son nom
        MockMultipartFile fake = new MockMultipartFile("diploma", "diplome.pdf", "application/pdf",
                new byte[] { 'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0, (byte) 0xff, (byte) 0xff });
        submit(tok, "Motivation", fake, pdf("cv", "cv.pdf"), png("idDocument", "id.png"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get(MINE).header("Authorization", "Bearer " + tok)).andExpect(status().isNoContent());
    }

    @Test
    void une_seule_demande_en_attente() throws Exception {
        String tok = learnerToken("candidat-doublon@example.com");
        submitValid(tok).andExpect(status().isCreated());
        submitValid(tok).andExpect(status().isConflict());
    }

    @Test
    void acceptation_notification_et_email() throws Exception {
        String adminTok = adminToken("admin-ok@example.com");
        String tok = learnerToken("candidat-ok@example.com");
        submitValid(tok).andExpect(status().isCreated());
        long pending = requestRepository.countByStatus(InstructorRequest.Status.PENDING);

        mvc.perform(get(ADMIN + "/count").header("Authorization", "Bearer " + adminTok))
                .andExpect(jsonPath("$.pending").value(pending));
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + adminTok))
                .andExpect(jsonPath("$.users.instructorRequests").value(pending));

        Long requestId = pendingRequestId("candidat-ok@example.com");
        mvc.perform(post(ADMIN + "/" + requestId + "/approve").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        User user = userRepository.findByEmailIgnoreCase("candidat-ok@example.com").orElseThrow();
        assertThat(user.getRoles()).extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.LEARNER, RoleName.INSTRUCTOR);
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + tok))
                .andExpect(jsonPath("$.items[0].type").value("INSTRUCTOR_REQUEST_APPROVED"));

        // L'email part après validation de la transaction : vérifié dans InstructorRequestEmailTest
        mvc.perform(post(ADMIN + "/" + requestId + "/reject").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"trop tard\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void refus_motif_obligatoire_transmis_puis_nouvelle_demande() throws Exception {
        String adminTok = adminToken("admin-refus@example.com");
        String tok = learnerToken("candidat-refuse@example.com");
        submitValid(tok).andExpect(status().isCreated());
        Long requestId = pendingRequestId("candidat-refuse@example.com");

        mvc.perform(post(ADMIN + "/" + requestId + "/reject").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(ADMIN + "/" + requestId + "/reject").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"Diplôme illisible\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + tok))
                .andExpect(jsonPath("$.items[0].type").value("INSTRUCTOR_REQUEST_REJECTED"))
                .andExpect(jsonPath("$.items[0].detail").value("Diplôme illisible"));
        mvc.perform(get(MINE).header("Authorization", "Bearer " + tok))
                .andExpect(jsonPath("$.rejectionReason").value("Diplôme illisible"));
        submitValid(tok).andExpect(status().isCreated());
    }

    @Test
    void formateur_ne_peut_pas_demander_et_role_manuel_solde_la_demande() throws Exception {
        String adminTok = adminToken("admin-manuel@example.com");
        String tok = learnerToken("promu-main@example.com");
        submitValid(tok).andExpect(status().isCreated());
        Long userId = userRepository.findByEmailIgnoreCase("promu-main@example.com").orElseThrow().getId();

        mvc.perform(patch("/api/v1/admin/users/" + userId + "/roles").header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"roles\":[\"LEARNER\",\"INSTRUCTOR\"]}"))
                .andExpect(status().isOk());
        assertThat(requestRepository.existsByUserIdAndStatus(userId, InstructorRequest.Status.PENDING)).isFalse();

        submitValid(login("promu-main@example.com")).andExpect(status().isConflict());
    }

    @Test
    void notifications_marquees_lues() throws Exception {
        String adminTok = adminToken("admin-notif@example.com");
        mvc.perform(post("/api/v1/me/notifications/read-all").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isNoContent());
        submitValid(learnerToken("candidat-notif@example.com")).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + adminTok))
                .andExpect(jsonPath("$.unreadCount").value(1));
        mvc.perform(post("/api/v1/me/notifications/read-all").header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", "Bearer " + adminTok))
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.items[0].read").value(true));
    }

    @Test
    void apprenant_ne_peut_pas_traiter_ni_telecharger() throws Exception {
        String tok = learnerToken("intrus@example.com");
        mvc.perform(get(ADMIN).header("Authorization", "Bearer " + tok)).andExpect(status().isForbidden());
        mvc.perform(get(ADMIN + "/1/documents/1").header("Authorization", "Bearer " + tok))
                .andExpect(status().isForbidden());
    }

    // --- outils ---

    private ResultActions submitValid(String token) throws Exception {
        return submit(token, "Je souhaite enseigner", pdf("diploma", "d.pdf"), pdf("cv", "cv.pdf"),
                png("idDocument", "id.png"));
    }

    private ResultActions submit(String token, String motivation, MockMultipartFile... files) throws Exception {
        var request = multipart(MINE).param("motivation", motivation);
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private static MockMultipartFile pdf(String part, String name) {
        return new MockMultipartFile(part, name, "application/pdf", PDF);
    }

    private static MockMultipartFile png(String part, String name) {
        return new MockMultipartFile(part, name, "image/png", PNG);
    }

    private Long pendingRequestId(String email) {
        Long userId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
        return requestRepository.findByUserIdAndStatus(userId, InstructorRequest.Status.PENDING).orElseThrow().getId();
    }

    private String learnerToken(String email) throws Exception {
        register(email);
        return login(email);
    }

    private String adminToken(String email) throws Exception {
        register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.getRoles().add(roleRepository.findByName(RoleName.ADMIN).orElseThrow());
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

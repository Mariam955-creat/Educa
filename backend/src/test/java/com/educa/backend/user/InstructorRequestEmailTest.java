package com.educa.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.educa.backend.notification.EmailSender;
import com.jayway.jsonpath.JsonPath;

/**
 * Email au candidat après la décision. Sans {@code @Transactional} : l'email part après la validation de la
 * transaction, qui doit donc vraiment avoir lieu (comptes à adresse unique, conservés dans la base de test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(InstructorRequestEmailTest.RecordingMail.class)
class InstructorRequestEmailTest {

    private static final byte[] PDF = "%PDF-1.4\n1 0 obj << >> endobj\ntrailer << >>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    record Mail(String to, String subject, String body) {
    }

    /** Remplace l'envoi réel : garde les emails pour les vérifier. */
    @TestConfiguration
    static class RecordingMail {
        static final List<Mail> SENT = new CopyOnWriteArrayList<>();

        @Bean
        @Primary
        EmailSender recordingEmailSender() {
            return (to, subject, body) -> SENT.add(new Mail(to, subject, body));
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InstructorRequestRepository requestRepository;

    @BeforeEach
    void clear() {
        RecordingMail.SENT.clear();
    }

    @Test
    void acceptation_envoie_un_email_au_candidat() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String adminTok = adminToken("mail-admin-" + suffix + "@example.com");
        String candidate = "mail-ok-" + suffix + "@example.com";
        submit(learnerToken(candidate));

        mvc.perform(post("/api/v1/admin/instructor-requests/" + pendingRequestId(candidate) + "/approve")
                .header("Authorization", "Bearer " + adminTok)).andExpect(status().isOk());

        assertThat(RecordingMail.SENT).hasSize(1);
        Mail mail = RecordingMail.SENT.get(0);
        assertThat(mail.to()).isEqualTo(candidate);
        assertThat(mail.subject()).contains("acceptée");
        assertThat(mail.body()).contains("Bonjour Test").contains("/instructor");
    }

    @Test
    void refus_envoie_un_email_avec_le_motif() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String adminTok = adminToken("mail-admin2-" + suffix + "@example.com");
        String candidate = "mail-ko-" + suffix + "@example.com";
        submit(learnerToken(candidate));

        mvc.perform(post("/api/v1/admin/instructor-requests/" + pendingRequestId(candidate) + "/reject")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"CV incomplet\"}"))
                .andExpect(status().isOk());

        assertThat(RecordingMail.SENT).hasSize(1);
        Mail mail = RecordingMail.SENT.get(0);
        assertThat(mail.to()).isEqualTo(candidate);
        assertThat(mail.body()).contains("Motif : CV incomplet").contains("n'a pas été retenue");
    }

    private void submit(String token) throws Exception {
        mvc.perform(multipart("/api/v1/me/instructor-request")
                        .file(new MockMultipartFile("diploma", "d.pdf", "application/pdf", PDF))
                        .file(new MockMultipartFile("cv", "cv.pdf", "application/pdf", PDF))
                        .file(new MockMultipartFile("idDocument", "id.pdf", "application/pdf", PDF))
                        .param("motivation", "Je souhaite enseigner")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private Long pendingRequestId(String email) {
        Long userId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
        return requestRepository.findByUserIdAndStatus(userId, InstructorRequest.Status.PENDING).orElseThrow().getId();
    }

    private String learnerToken(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test\"}"))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String adminToken(String email) throws Exception {
        learnerToken(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.getRoles().add(roleRepository.findByName(RoleName.ADMIN).orElseThrow());
        userRepository.save(user);
        return login(email);
    }

    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}

package com.educa.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.support.PdfText;
import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Profil {@code test} : Stripe/Orange Money désactivés (pas de clés réelles) → {@link DisabledPaymentGateway}.
 * Modèle « paiement par cours » : chaque cours a son propre prix, payé une seule fois par l'apprenant
 * (voir EnrollmentService.enroll).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CoursePaymentFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void checkout_indisponible_sans_cle_configuree() throws Exception {
        String prof = instructorToken("pay-prof1@example.com");
        long courseId = createPublishedCourse(prof, "29.99");

        String eleve = learnerToken("pay-eleve1@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/checkout").header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON).content("{\"provider\":\"STRIPE\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void checkout_refuse_sur_un_cours_gratuit() throws Exception {
        String prof = instructorToken("pay-prof2@example.com");
        long courseId = createPublishedCourse(prof, "0");

        String eleve = learnerToken("pay-eleve2@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/checkout").header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON).content("{\"provider\":\"STRIPE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inscription_a_un_cours_payant_refusee_sans_paiement() throws Exception {
        String prof = instructorToken("pay-prof3@example.com");
        long courseId = createPublishedCourse(prof, "29.99");

        String eleve = learnerToken("pay-eleve3@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void inscription_a_un_cours_payant_autorisee_apres_paiement_reussi() throws Exception {
        String prof = instructorToken("pay-prof4@example.com");
        long courseId = createPublishedCourse(prof, "29.99");

        String eleve = learnerToken("pay-eleve4@example.com");
        User user = userRepository.findByEmailIgnoreCase("pay-eleve4@example.com").orElseThrow();

        Payment payment = new Payment();
        payment.setUserId(user.getId());
        payment.setCourseId(courseId);
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setProviderReference("cs_test_123");
        payment.setAmount(BigDecimal.valueOf(29.99));
        payment.setCurrency("EUR");
        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());
    }

    @Test
    void facture_generee_et_telechargeable_apres_paiement_reussi() throws Exception {
        String prof = instructorToken("pay-prof6@example.com");
        long courseId = createPublishedCourse(prof, "29.99");

        String eleve = learnerToken("pay-eleve6@example.com");
        User user = userRepository.findByEmailIgnoreCase("pay-eleve6@example.com").orElseThrow();

        Payment payment = new Payment();
        payment.setUserId(user.getId());
        payment.setCourseId(courseId);
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setProviderReference("cs_test_456");
        payment.setAmount(BigDecimal.valueOf(29.99));
        payment.setCurrency("EUR");
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setInvoiceNumber("INV-2026-000001");
        paymentRepository.save(payment);

        mvc.perform(get("/api/v1/payments/me").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-2026-000001"))
                .andExpect(jsonPath("$[0].courseTitle").value("Cours payant"))
                .andExpect(jsonPath("$[0].courseId").value(courseId));

        mvc.perform(get("/api/v1/payments/" + payment.getId() + "/invoice/download")
                        .header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk());

        assertThat(invoicePdf(eleve, payment.getId(), "")).contains("FACTURE", "INV-2026-000001", "29,99");
        assertThat(invoicePdf(eleve, payment.getId(), "?lang=de")).contains("RECHNUNG", "Bezahlt", "29,99");
        assertThat(invoicePdf(eleve, payment.getId(), "?lang=en")).contains("INVOICE", "Paid", "€29.99");
        assertThat(invoicePdf(eleve, payment.getId(), "?lang=nl")).contains("FACTUUR", "Betaald", "29,99");
        assertThat(invoicePdf(eleve, payment.getId(), "?lang=ar")).contains("FACTURE");

        String autreEleve = learnerToken("pay-eleve7@example.com");
        mvc.perform(get("/api/v1/payments/" + payment.getId() + "/invoice/download")
                        .header("Authorization", "Bearer " + autreEleve))
                .andExpect(status().isForbidden());
    }

    @Test
    void inscription_a_un_cours_gratuit_autorisee_sans_paiement() throws Exception {
        String prof = instructorToken("pay-prof5@example.com");
        long courseId = createPublishedCourse(prof, "0");

        String eleve = learnerToken("pay-eleve5@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());
    }

    // ---------- helpers ----------

    private long createPublishedCourse(String token, String price) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Cours payant\",\"language\":\"fr\",\"price\":" + price + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private String invoicePdf(String token, long paymentId, String query) throws Exception {
        byte[] pdf = mvc.perform(get("/api/v1/payments/" + paymentId + "/invoice/download" + query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return PdfText.of(pdf);
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

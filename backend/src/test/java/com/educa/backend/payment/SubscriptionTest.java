package com.educa.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

/** Abonnement mensuel : pass de 30 jours donnant accès à tous les cours payants. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void loffre_est_visible_sans_compte() throws Exception {
        mvc.perform(get("/api/v1/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(39.99))
                .andExpect(jsonPath("$.days").value(30))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void un_abonne_sinscrit_a_un_cours_payant_sans_lacheter() throws Exception {
        String prof = instructorToken("sub-prof1@example.com");
        long courseId = createPublishedCourse(prof, "29.99");
        String eleve = learnerToken("sub-eleve1@example.com");

        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isPaymentRequired());

        paymentService.recordDemoSubscription(userId("sub-eleve1@example.com"));
        mvc.perform(get("/api/v1/subscription").header("Authorization", "Bearer " + eleve))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.accessUntil").exists());
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());
    }

    @Test
    void le_pass_est_partage_entre_les_formateurs_des_cours_suivis() throws Exception {
        String prof1 = instructorToken("sub-share-prof1@example.com");
        String prof2 = instructorToken("sub-share-prof2@example.com");
        long course1 = createPublishedCourse(prof1, "29.99");
        long course2 = createPublishedCourse(prof2, "39.99");
        String eleve = learnerToken("sub-share-eleve@example.com");
        paymentService.recordDemoSubscription(userId("sub-share-eleve@example.com"));
        for (long id : new long[] {course1, course2}) {
            mvc.perform(post("/api/v1/courses/" + id + "/enroll").header("Authorization", "Bearer " + eleve))
                    .andExpect(status().isCreated());
        }

        // 39,99 € partagés entre deux cours : 19,99 € chacun (arrondi inférieur)
        for (String prof : new String[] {prof1, prof2}) {
            mvc.perform(get("/api/v1/instructor/sales").header("Authorization", "Bearer " + prof))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].kind").value("SUBSCRIPTION"))
                    .andExpect(jsonPath("$[0].amount").value(19.99));
        }
    }

    @Test
    void a_la_fin_de_labonnement_le_contenu_est_verrouille_puis_rouvert_au_renouvellement() throws Exception {
        String prof = instructorToken("sub-prof2@example.com");
        long courseId = createPublishedCourse(prof, "39.99");
        String eleve = learnerToken("sub-eleve2@example.com");
        Long eleveId = userId("sub-eleve2@example.com");
        paymentService.recordDemoSubscription(eleveId);
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/courses/" + courseId + "/progress").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk());

        expireSubscription(eleveId);
        mvc.perform(get("/api/v1/courses/" + courseId + "/progress").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isPaymentRequired());

        paymentService.recordDemoSubscription(eleveId);
        // La même inscription est réactivée (pas de doublon), l'accès revient
        mvc.perform(get("/api/v1/courses/" + courseId + "/progress").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk());
    }

    @Test
    void un_renouvellement_anticipe_prolonge_lacces_de_30_jours() throws Exception {
        learnerToken("sub-eleve3@example.com");
        Long eleveId = userId("sub-eleve3@example.com");
        paymentService.recordDemoSubscription(eleveId);
        Instant first = latestAccessUntil(eleveId);

        // Un second pass (achat anticipé) part de la fin du premier, pas d'aujourd'hui
        Payment early = new Payment();
        early.setUserId(eleveId);
        early.setKind(PaymentKind.SUBSCRIPTION);
        early.setProvider(PaymentProvider.STRIPE);
        early.setProviderReference("sub-test-renouvellement");
        early.setAmount(new java.math.BigDecimal("39.99"));
        early.setCurrency("EUR");
        early.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(early);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(paymentService, "markSucceeded", early);

        assertThat(latestAccessUntil(eleveId)).isCloseTo(first.plus(30, ChronoUnit.DAYS), within());
    }

    @Test
    void la_facture_dun_abonnement_se_telecharge() throws Exception {
        String eleve = learnerToken("sub-eleve4@example.com");
        paymentService.recordDemoSubscription(userId("sub-eleve4@example.com"));
        String invoices = mvc.perform(get("/api/v1/payments/me").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("SUBSCRIPTION"))
                .andExpect(jsonPath("$[0].courseId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(invoices, "$[0].id")).longValue();
        mvc.perform(get("/api/v1/payments/" + paymentId + "/invoice/download").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk());
    }

    private static org.assertj.core.data.TemporalUnitOffset within() {
        return new org.assertj.core.data.TemporalUnitWithinOffset(5, ChronoUnit.SECONDS);
    }

    private Instant latestAccessUntil(Long userId) {
        return paymentRepository
                .findFirstByUserIdAndKindAndStatusOrderByAccessUntilDesc(userId, PaymentKind.SUBSCRIPTION, PaymentStatus.SUCCEEDED)
                .orElseThrow().getAccessUntil();
    }

    private void expireSubscription(Long userId) {
        paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, PaymentStatus.SUCCEEDED).stream()
                .filter(p -> p.getKind() == PaymentKind.SUBSCRIPTION)
                .forEach(p -> {
                    p.setAccessUntil(Instant.now().minus(1, ChronoUnit.DAYS));
                    paymentRepository.saveAndFlush(p);
                });
    }

    private long createPublishedCourse(String token, String price) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Cours pour abonnes\",\"language\":\"fr\",\"price\":" + price + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private Long userId(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
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

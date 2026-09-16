package com.educa.backend.payment;

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

/**
 * Profil {@code test} : Stripe/Orange Money désactivés (pas de clés réelles) → {@link DisabledPaymentGateway}.
 * On vérifie le repli propre, l'exposition de l'état d'abonnement et le verrouillage de l'inscription
 * aux cours par le modèle « abonnement plateforme » (voir EnrollmentService).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void checkout_indisponible_sans_cle_configuree() throws Exception {
        String token = learnerToken("payer1@example.com");
        mvc.perform(post("/api/v1/subscriptions/checkout").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"plan\":\"MONTHLY\",\"provider\":\"STRIPE\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void me_renvoie_aucun_abonnement_par_defaut() throws Exception {
        String token = learnerToken("payer2@example.com");
        mvc.perform(get("/api/v1/subscriptions/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSubscription").value(false))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void cancel_refuse_sans_abonnement_actif() throws Exception {
        String token = learnerToken("payer3@example.com");
        mvc.perform(post("/api/v1/subscriptions/cancel").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void inscription_a_un_cours_refusee_sans_abonnement() throws Exception {
        String prof = instructorToken("sub-prof1@example.com");
        long courseId = createPublishedCourse(prof);

        String eleve = learnerToken("sub-eleve1@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void inscription_a_un_cours_autorisee_avec_abonnement_actif() throws Exception {
        String prof = instructorToken("sub-prof2@example.com");
        long courseId = createPublishedCourse(prof);

        String eleve = learnerToken("sub-eleve2@example.com");
        grantActiveSubscription("sub-eleve2@example.com");
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isCreated());
    }

    @Test
    void formateur_accede_sans_abonnement() throws Exception {
        String prof = instructorToken("sub-prof3@example.com");
        long courseId = createPublishedCourse(prof);
        // le formateur propriétaire n'a pas besoin de s'inscrire lui-même : on vérifie juste
        // que la contrainte d'abonnement ne le bloque pas ailleurs (résultats du cours).
        mvc.perform(get("/api/v1/instructor/courses/" + courseId + "/results")
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private long createPublishedCourse(String token) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"title\":\"Cours abonnement\",\"language\":\"fr\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private void grantActiveSubscription(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Subscription subscription = new Subscription();
        subscription.setUserId(user.getId());
        subscription.setPlan(SubscriptionPlan.MONTHLY);
        subscription.setProvider(PaymentProvider.STRIPE);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.now().plus(365, ChronoUnit.DAYS));
        subscriptionRepository.save(subscription);
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

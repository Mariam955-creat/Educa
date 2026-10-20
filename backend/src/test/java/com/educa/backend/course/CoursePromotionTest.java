package com.educa.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.payment.PaymentRepository;
import com.educa.backend.payment.PaymentService;
import com.educa.backend.payment.PaymentStatus;
import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Promotions (prix barré) et vidéo de démo d'un cours. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CoursePromotionTest {

    /** En-tête minimal d'un fichier MP4 (boîte « ftyp »), reconnu par la détection de type. */
    private static final byte[] MP4_MAGIC = {
            0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2', 0, 0, 0, 0, 'm', 'p', '4', '2', 'i', 's', 'o', 'm' };

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
    void une_promotion_en_cours_est_affichee_et_facturee() throws Exception {
        String prof = instructorToken("promo-prof1@example.com");
        String ends = Instant.now().plus(7, ChronoUnit.DAYS).toString();
        long courseId = createCourse(prof, "{\"title\":\"Cours en promo\",\"language\":\"fr\",\"price\":49.99,"
                + "\"promoPrice\":19.99,\"promoEndsAt\":\"" + ends + "\"}");

        mvc.perform(get("/api/v1/courses").param("q", "Cours en promo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(49.99))
                .andExpect(jsonPath("$.content[0].promoPrice").value(19.99))
                .andExpect(jsonPath("$.content[0].promoActive").value(true));

        learnerToken("promo-eleve@example.com");
        Long buyerId = userRepository.findByEmailIgnoreCase("promo-eleve@example.com").orElseThrow().getId();
        paymentService.recordDemoPurchase(buyerId, courseId);
        assertThat(paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(buyerId, PaymentStatus.SUCCEEDED))
                .singleElement()
                .satisfies(payment -> assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("19.99")));
    }

    @Test
    void une_promotion_terminee_ne_sapplique_plus() throws Exception {
        String prof = instructorToken("promo-prof2@example.com");
        String ended = Instant.now().minus(1, ChronoUnit.DAYS).toString();
        createCourse(prof, "{\"title\":\"Promo terminee\",\"language\":\"fr\",\"price\":29.99,"
                + "\"promoPrice\":9.99,\"promoEndsAt\":\"" + ended + "\"}");

        mvc.perform(get("/api/v1/courses").param("q", "Promo terminee"))
                .andExpect(jsonPath("$.content[0].promoActive").value(false))
                .andExpect(jsonPath("$.content[0].price").value(29.99));
    }

    @Test
    void le_prix_promotionnel_doit_etre_inferieur_au_prix_normal() throws Exception {
        String prof = instructorToken("promo-prof3@example.com");
        mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Promo trop chere\",\"language\":\"fr\",\"price\":20,\"promoPrice\":25}"))
                .andExpect(status().isBadRequest());

        long courseId = createCourse(prof, "{\"title\":\"Promo retiree\",\"language\":\"fr\",\"price\":20,\"promoPrice\":10}");
        // Remplacement (PUT) sans promotion : elle est retirée
        mvc.perform(put("/api/v1/courses/" + courseId).header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Promo retiree\",\"language\":\"fr\",\"price\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promoPrice").doesNotExist())
                .andExpect(jsonPath("$.promoActive").value(false));
    }

    @Test
    void la_video_de_demo_est_televersee_puis_lue_publiquement() throws Exception {
        String prof = instructorToken("promo-prof4@example.com");
        long courseId = createDraft(prof, "{\"title\":\"Cours avec demo\",\"language\":\"fr\",\"price\":0}");

        // Une image n'est pas une vidéo : le type réel est détecté sur le contenu
        mvc.perform(multipart("/api/v1/courses/" + courseId + "/preview")
                        .file(new MockMultipartFile("file", "demo.mp4", "video/mp4", "%PDF-1.4\n".getBytes()))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isUnsupportedMediaType());

        mvc.perform(multipart("/api/v1/courses/" + courseId + "/preview")
                        .file(new MockMultipartFile("file", "demo.mp4", "video/mp4", MP4_MAGIC))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previewVideoUrl").value(startsWith("/api/v1/courses/" + courseId + "/preview?v=")));

        // Brouillon : invisible du public tant que le cours n'est pas publié
        mvc.perform(get("/api/v1/courses/" + courseId + "/preview")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/courses/" + courseId + "/publish").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/courses/" + courseId + "/preview"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("video/mp4"));

        String learner = learnerToken("promo-curieux@example.com");
        mvc.perform(multipart("/api/v1/courses/" + courseId + "/preview")
                        .file(new MockMultipartFile("file", "demo.mp4", "video/mp4", MP4_MAGIC))
                        .header("Authorization", "Bearer " + learner))
                .andExpect(status().isForbidden());
    }

    /** Crée un cours et le publie (le catalogue ne liste que les cours publiés). */
    private long createCourse(String token, String json) throws Exception {
        long id = createDraft(token, json);
        mvc.perform(post("/api/v1/courses/" + id + "/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private long createDraft(String token, String json) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String instructorToken(String email) throws Exception {
        register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Role instructor = roleRepository.findByName(RoleName.INSTRUCTOR).orElseThrow();
        user.getRoles().add(instructor);
        userRepository.save(user);
        return login(email);
    }

    private String learnerToken(String email) throws Exception {
        register(email);
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

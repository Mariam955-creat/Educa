package com.educa.backend.payment;

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

import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Espace formateur : ventes et avis reçus, limités aux cours du formateur connecté. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstructorSpaceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PaymentService paymentService;

    @Test
    void le_formateur_voit_les_ventes_de_ses_cours_uniquement() throws Exception {
        String prof = instructorToken("studio-prof1@example.com");
        long courseId = createCourse(prof, "Cours vendu", "24.50");
        String other = instructorToken("studio-prof2@example.com");
        long otherCourseId = createCourse(other, "Cours d'un autre", "10.00");

        learnerToken("studio-acheteur@example.com");
        Long buyerId = userRepository.findByEmailIgnoreCase("studio-acheteur@example.com").orElseThrow().getId();
        paymentService.recordDemoPurchase(buyerId, courseId);
        paymentService.recordDemoPurchase(buyerId, otherCourseId);

        mvc.perform(get("/api/v1/instructor/sales").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseTitle").value("Cours vendu"))
                .andExpect(jsonPath("$[0].amount").value(24.5))
                .andExpect(jsonPath("$[0].buyerName").value("Test"));
    }

    @Test
    void un_apprenant_na_pas_acces_a_lespace_formateur() throws Exception {
        String eleve = learnerToken("studio-eleve@example.com");
        mvc.perform(get("/api/v1/instructor/sales").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/instructor/reviews").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isForbidden());
    }

    @Test
    void un_formateur_sans_cours_na_ni_vente_ni_avis() throws Exception {
        String prof = instructorToken("studio-prof3@example.com");
        mvc.perform(get("/api/v1/instructor/sales").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/instructor/reviews").header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long createCourse(String token, String title, String price) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"language\":\"fr\",\"price\":" + price + "}"))
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

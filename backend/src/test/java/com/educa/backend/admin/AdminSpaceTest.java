package com.educa.backend.admin;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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

import com.educa.backend.payment.PaymentService;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Espace admin : indicateurs globaux et liste de tous les cours (publiés ou non). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminSpaceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PaymentService paymentService;

    @Test
    void les_indicateurs_refletent_lactivite_de_la_plateforme() throws Exception {
        String admin = token("adm-stats@example.com", RoleName.ADMIN);
        String before = mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int coursesBefore = JsonPath.read(before, "$.courses.total");
        int draftsBefore = JsonPath.read(before, "$.courses.drafts");
        int salesBefore = JsonPath.read(before, "$.revenue.sales");

        String prof = token("adm-stats-prof@example.com", RoleName.INSTRUCTOR);
        long courseId = createCourse(prof, "Cours compte par les stats", "15");
        token("adm-stats-eleve@example.com", RoleName.LEARNER);
        Long buyerId = userRepository.findByEmailIgnoreCase("adm-stats-eleve@example.com").orElseThrow().getId();
        paymentService.recordDemoPurchase(buyerId, courseId);

        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses.total").value(coursesBefore + 1))
                .andExpect(jsonPath("$.courses.drafts").value(draftsBefore + 1))
                .andExpect(jsonPath("$.revenue.sales").value(salesBefore + 1))
                .andExpect(jsonPath("$.revenue.recentSales").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.users.recent").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.users.admins").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void ladmin_voit_tous_les_cours_y_compris_les_brouillons() throws Exception {
        String prof = token("adm-courses-prof@example.com", RoleName.INSTRUCTOR);
        createCourse(prof, "Brouillon visible par l'admin", "0");
        String admin = token("adm-courses@example.com", RoleName.ADMIN);

        mvc.perform(get("/api/v1/admin/courses").param("q", "Brouillon visible")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].published").value(false));
        mvc.perform(get("/api/v1/admin/courses").param("q", "Brouillon visible").param("published", "true")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/admin/courses").param("q", "Brouillon visible").param("published", "false")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void un_formateur_na_pas_acces_a_ladministration() throws Exception {
        String prof = token("adm-refus@example.com", RoleName.INSTRUCTOR);
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + prof))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/courses").header("Authorization", "Bearer " + prof))
                .andExpect(status().isForbidden());
    }

    private long createCourse(String token, String title, String price) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"language\":\"fr\",\"price\":" + price + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String token(String email, RoleName role) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test\"}"))
                .andExpect(status().isCreated());
        if (role != RoleName.LEARNER) {
            User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
            user.getRoles().add(roleRepository.findByName(role).orElseThrow());
            userRepository.save(user);
        }
        String response = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}

package com.educa.backend.user;

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

/** Inscription et profil enrichis : pays, titre, biographie, téléphone, changement de mot de passe. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserProfileTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void inscription_avec_pays_et_mot_de_passe_sans_chiffre_refuse() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"profil-faible@example.com\",\"password\":\"seulementdeslettres\",\"fullName\":\"Faible\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"profil-pays@example.com\",\"password\":\"password123\",\"fullName\":\"Awa\",\"country\":\"SN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country").value("SN"));

        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"profil-pays2@example.com\",\"password\":\"password123\",\"fullName\":\"X\",\"country\":\"senegal\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void le_profil_se_complete_puis_se_vide() throws Exception {
        String token = registerAndLogin("profil-complet@example.com", "password123");

        mvc.perform(patch("/api/v1/auth/me").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"headline\":\" Developpeuse Java \",\"bio\":\"10 ans d'experience\","
                                + "\"country\":\"BE\",\"phone\":\"+32 470 12 34 56\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Developpeuse Java"))
                .andExpect(jsonPath("$.phone").value("+32 470 12 34 56"))
                // Champ absent de la requête : inchangé
                .andExpect(jsonPath("$.fullName").value("Test"));

        mvc.perform(patch("/api/v1/auth/me").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"headline\":\"\",\"phone\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.bio").value("10 ans d'experience"));

        mvc.perform(patch("/api/v1/auth/me").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"phone\":\"appelez-moi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changer_de_mot_de_passe_revoque_les_autres_sessions() throws Exception {
        registerAndLogin("profil-mdp@example.com", "password123");
        String login = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"profil-mdp@example.com\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.accessToken");
        String oldRefresh = JsonPath.read(login, "$.refreshToken");

        // Mot de passe actuel faux : 400 (pas 401, qui ferait tenter un rafraîchissement côté client)
        changePassword(token, "mauvais123", "nouveau456").andExpect(status().isBadRequest());
        changePassword(token, "password123", "password123").andExpect(status().isBadRequest());
        changePassword(token, "password123", "court1").andExpect(status().isBadRequest());

        changePassword(token, "password123", "nouveau456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        mvc.perform(post("/api/v1/auth/refresh").contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"profil-mdp@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"profil-mdp@example.com\",\"password\":\"nouveau456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void le_profil_public_du_formateur_apparait_sur_la_page_du_cours() throws Exception {
        registerAndLogin("profil-prof@example.com", "password123");
        User user = userRepository.findByEmailIgnoreCase("profil-prof@example.com").orElseThrow();
        user.getRoles().add(roleRepository.findByName(RoleName.INSTRUCTOR).orElseThrow());
        userRepository.save(user);
        String prof = login("profil-prof@example.com", "password123");

        mvc.perform(patch("/api/v1/auth/me").header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"headline\":\"Formatrice Python\",\"bio\":\"J'enseigne depuis 2015.\",\"phone\":\"0601020304\"}"))
                .andExpect(status().isOk());
        String course = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + prof)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Cours avec formatrice\",\"language\":\"fr\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String slug = JsonPath.read(course, "$.slug");

        String detail = mvc.perform(get("/api/v1/courses/" + slug).header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorHeadline").value("Formatrice Python"))
                .andExpect(jsonPath("$.instructorBio").value("J'enseigne depuis 2015."))
                .andReturn().getResponse().getContentAsString();
        // Profil public : jamais le téléphone ni l'email du formateur
        org.assertj.core.api.Assertions.assertThat(detail)
                .doesNotContain("0601020304")
                .doesNotContain("profil-prof@example.com");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(String token, String current, String next)
            throws Exception {
        return mvc.perform(post("/api/v1/auth/me/password").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"));
    }

    private String registerAndLogin(String email, String password) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"fullName\":\"Test\"}"))
                .andExpect(status().isCreated());
        String response = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}

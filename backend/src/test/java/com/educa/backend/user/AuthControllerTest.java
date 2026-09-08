package com.educa.backend.user;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Tests d'intégration du parcours d'authentification (Phase 1, tâche 1.15).
 * S'exécute sur la base {@code educa_test} (profil {@code test}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String ME = "/api/v1/auth/me";

    @Autowired
    private MockMvc mvc;

    @Test
    void register_creeUnCompteApprenant() throws Exception {
        mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content("""
                {"email":"nouveau@example.com","password":"password123","fullName":"Nouveau Compte","preferredLanguage":"fr"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("nouveau@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("LEARNER"));
    }

    @Test
    void register_emailDejaUtilise_renvoie409() throws Exception {
        String body = """
                {"email":"doublon@example.com","password":"password123","fullName":"Doublon"}""";
        mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content(body)).andExpect(status().isConflict());
    }

    @Test
    void register_payloadInvalide_renvoie400() throws Exception {
        mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content("""
                {"email":"pas-un-email","password":"court","fullName":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void login_identifiantsValides_renvoieLesJetons() throws Exception {
        register("login-ok@example.com");
        mvc.perform(post(LOGIN).contentType(APPLICATION_JSON).content("""
                {"email":"login-ok@example.com","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("login-ok@example.com"));
    }

    @Test
    void login_mauvaisMotDePasse_renvoie401() throws Exception {
        register("login-ko@example.com");
        mvc.perform(post(LOGIN).contentType(APPLICATION_JSON).content("""
                {"email":"login-ko@example.com","password":"MAUVAIS-MOT-DE-PASSE"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_sansJeton_renvoie401() throws Exception {
        mvc.perform(get(ME)).andExpect(status().isUnauthorized());
    }

    @Test
    void me_avecJeton_renvoieLeProfil() throws Exception {
        register("moi@example.com");
        String token = login("moi@example.com");
        mvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("moi@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("LEARNER"));
    }

    @Test
    void refresh_rotationDuJeton() throws Exception {
        register("refresh@example.com");
        String loginResponse = mvc.perform(post(LOGIN).contentType(APPLICATION_JSON).content("""
                {"email":"refresh@example.com","password":"password123"}"""))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(loginResponse, "$.refreshToken");

        mvc.perform(post("/api/v1/auth/refresh").contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    private void register(String email) throws Exception {
        mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test\"}"))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String response = mvc.perform(post(LOGIN).contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}

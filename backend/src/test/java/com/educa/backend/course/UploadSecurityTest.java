package com.educa.backend.course;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
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
 * Phase 5 — durcissement des téléversements / téléchargements de fichiers de contenu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UploadSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void rejette_un_type_mime_non_autorise() throws Exception {
        String prof = instructorToken("up-prof1@example.com");
        long contentId = documentContent(prof, "up-cours-1");

        mvc.perform(multipart("/api/v1/contents/" + contentId + "/file")
                        .file(new MockMultipartFile("file", "piege.html", "text/html",
                                "<script>alert(1)</script>".getBytes()))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void ignore_le_content_type_declare_et_stocke_le_type_reel_detecte_dans_le_contenu() throws Exception {
        // Le client ment (Content-Type + extension "pdf") sur une vraie image PNG. Avant le sniffing par
        // contenu, le Content-Type déclaré était accepté tel quel : un contenu pourrait ainsi être stocké/
        // servi (potentiellement en « inline ») sous un type MIME qu'il n'est pas réellement.
        String prof = instructorToken("up-prof-mimeswap@example.com");
        long contentId = documentContent(prof, "up-cours-mimeswap");
        byte[] pngMagic = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

        mvc.perform(multipart("/api/v1/contents/" + contentId + "/file")
                        .file(new MockMultipartFile("file", "image-deguisee.pdf", "application/pdf", pngMagic))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("image/png"));

        mvc.perform(get("/api/v1/contents/" + contentId + "/file")
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
    }

    @Test
    void rejette_un_contenu_dangereux_meme_avec_un_content_type_et_un_nom_falsifies() throws Exception {
        // Le client ment sur le Content-Type ET le nom de fichier ; seul le contenu réel (magic bytes) fait foi.
        String prof = instructorToken("up-prof-spoof@example.com");
        long contentId = documentContent(prof, "up-cours-spoof");

        mvc.perform(multipart("/api/v1/contents/" + contentId + "/file")
                        .file(new MockMultipartFile("file", "rapport.pdf", "application/pdf",
                                "MZ\0\0 -- fake executable payload".getBytes()))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void accepte_un_pdf_et_le_sert_inline() throws Exception {
        String prof = instructorToken("up-prof2@example.com");
        long contentId = documentContent(prof, "up-cours-2");

        mvc.perform(multipart("/api/v1/contents/" + contentId + "/file")
                        .file(new MockMultipartFile("file", "cours.pdf", "application/pdf",
                                "%PDF-1.4 test".getBytes()))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/contents/" + contentId + "/file")
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("inline")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void un_type_autorise_mais_non_inline_est_servi_en_piece_jointe() throws Exception {
        String prof = instructorToken("up-prof3@example.com");
        long contentId = documentContent(prof, "up-cours-3");

        mvc.perform(multipart("/api/v1/contents/" + contentId + "/file")
                        .file(new MockMultipartFile("file", "ressources.zip", "application/zip",
                                "PK test".getBytes()))
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/contents/" + contentId + "/file")
                        .header("Authorization", "Bearer " + prof))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment")));
    }

    // ---------- helpers ----------

    /** Crée un cours publié + chapitre + contenu DOCUMENT, renvoie l'id du contenu. */
    private long documentContent(String token, String title) throws Exception {
        long courseId = createCourse(token, title);
        long chapterId = createChapter(token, courseId, "Chapitre 1", 1);
        String body = mvc.perform(post("/api/v1/chapters/" + chapterId + "/contents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"type\":\"DOCUMENT\",\"title\":\"Support\",\"position\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createCourse(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"language\":\"fr\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createChapter(String token, long courseId, String title, int position) throws Exception {
        String body = mvc.perform(post("/api/v1/courses/" + courseId + "/chapters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"position\":" + position + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String instructorToken(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test\"}"))
                .andExpect(status().isCreated());
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Role instructor = roleRepository.findByName(RoleName.INSTRUCTOR).orElseThrow();
        user.getRoles().add(instructor);
        userRepository.save(user);
        String response = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}

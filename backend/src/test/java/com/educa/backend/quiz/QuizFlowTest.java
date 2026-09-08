package com.educa.backend.quiz;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void quiz_non_visible_si_non_inscrit() throws Exception {
        String prof = instructorToken("qprof1@example.com");
        Fixture f = buildPublishedCourse(prof, "Cours quiz 1");

        String eleve = learnerToken("qeleve1@example.com");
        mvc.perform(get("/api/v1/quizzes/" + f.controlQuizId).header("Authorization", "Bearer " + eleve))
                .andExpect(status().isForbidden());
    }

    @Test
    void apprenant_voit_le_quiz_sans_les_bonnes_reponses() throws Exception {
        String prof = instructorToken("qprof2@example.com");
        Fixture f = buildPublishedCourse(prof, "Cours quiz 2");
        String eleve = enrolledLearner("qeleve2@example.com", f.courseId);

        mvc.perform(get("/api/v1/quizzes/" + f.controlQuizId).header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answersVisible").value(false))
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist());
    }

    @Test
    void examen_final_verrouille_avant_100_pourcent() throws Exception {
        String prof = instructorToken("qprof3@example.com");
        Fixture f = buildPublishedCourse(prof, "Cours quiz 3");
        String eleve = enrolledLearner("qeleve3@example.com", f.courseId);

        // pas encore : contenu non terminé + contrôle non tenté
        mvc.perform(post("/api/v1/quizzes/" + f.finalExamId + "/attempts")
                        .header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON).content("{\"answers\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void parcours_complet_jusqu_au_certificat() throws Exception {
        String prof = instructorToken("qprof4@example.com");
        Fixture f = buildPublishedCourse(prof, "Cours quiz 4");
        long correctControlOption = correctOptionId(prof, f.controlQuizId);
        long correctExamOption = correctOptionId(prof, f.finalExamId);

        String eleve = enrolledLearner("qeleve4@example.com", f.courseId);

        // 1. terminer le contenu
        mvc.perform(post("/api/v1/contents/" + f.contentId + "/complete").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk());

        // 2. passer le contrôle (bonne réponse -> 100)
        mvc.perform(post("/api/v1/quizzes/" + f.controlQuizId + "/attempts")
                        .header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content(answerBody(f.controlQuestionId, correctControlOption)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(100.0));

        // 3. l'examen final est maintenant déverrouillé
        mvc.perform(get("/api/v1/quizzes/" + f.finalExamId).header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk());

        // 4. passer l'examen final (bonne réponse -> note finale 100 -> certificat)
        String result = mvc.perform(post("/api/v1/quizzes/" + f.finalExamId + "/attempts")
                        .header("Authorization", "Bearer " + eleve)
                        .contentType(APPLICATION_JSON)
                        .content(answerBody(f.examQuestionId, correctExamOption)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finalGrade").value(100.0))
                .andExpect(jsonPath("$.certificateId").isNumber())
                .andReturn().getResponse().getContentAsString();

        long certId = ((Number) JsonPath.read(result, "$.certificateId")).longValue();

        // 5. le certificat apparaît dans "mes certificats"
        mvc.perform(get("/api/v1/certificates/me").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) certId))
                .andExpect(jsonPath("$[0].serialNumber").isNotEmpty());

        // 6. la note du cours reflète le certificat
        mvc.perform(get("/api/v1/courses/" + f.courseId + "/grade").header("Authorization", "Bearer " + eleve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalGrade").value(100.0))
                .andExpect(jsonPath("$.certificateId").value((int) certId));

        // 7. vérification publique par code
        String code = JsonPath.read(
                mvc.perform(get("/api/v1/certificates/me").header("Authorization", "Bearer " + eleve))
                        .andReturn().getResponse().getContentAsString(),
                "$[0].verificationCode");
        mvc.perform(get("/api/v1/certificates/verify/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.courseTitle").value("Cours quiz 4"));
    }

    // ---------- fixtures / helpers ----------

    private record Fixture(long courseId, long chapterId, long contentId,
                           long controlQuizId, long controlQuestionId,
                           long finalExamId, long examQuestionId) {
    }

    private Fixture buildPublishedCourse(String profToken, String title) throws Exception {
        long courseId = createCourse(profToken, title);
        long chapterId = createChapter(profToken, courseId, "Chapitre 1", 1);
        long contentId = createTextContent(profToken, chapterId, "Leçon", 1);

        long controlQuizId = postId(profToken, "/api/v1/chapters/" + chapterId + "/control-quiz",
                "{\"title\":\"Contrôle 1\"}");
        long controlQuestionId = questionId(profToken, controlQuizId,
                "{\"statement\":\"2+2 ?\",\"type\":\"SINGLE_CHOICE\",\"position\":1,\"options\":["
                        + "{\"label\":\"4\",\"correct\":true,\"position\":1},"
                        + "{\"label\":\"5\",\"correct\":false,\"position\":2}]}");

        long finalExamId = postId(profToken, "/api/v1/courses/" + courseId + "/final-exam",
                "{\"title\":\"Examen final\",\"maxAttempts\":3}");
        long examQuestionId = questionId(profToken, finalExamId,
                "{\"statement\":\"Capitale de la France ?\",\"type\":\"SINGLE_CHOICE\",\"position\":1,\"options\":["
                        + "{\"label\":\"Paris\",\"correct\":true,\"position\":1},"
                        + "{\"label\":\"Lyon\",\"correct\":false,\"position\":2}]}");

        mvc.perform(post("/api/v1/courses/" + courseId + "/publish").header("Authorization", "Bearer " + profToken))
                .andExpect(status().isOk());

        return new Fixture(courseId, chapterId, contentId, controlQuizId, controlQuestionId, finalExamId, examQuestionId);
    }

    private long correctOptionId(String profToken, long quizId) throws Exception {
        String body = mvc.perform(get("/api/v1/quizzes/" + quizId).header("Authorization", "Bearer " + profToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<Integer> ids = JsonPath.read(body, "$.questions[0].options[?(@.correct == true)].id");
        return ids.get(0).longValue();
    }

    private static String answerBody(long questionId, long optionId) {
        return "{\"answers\":[{\"questionId\":" + questionId + ",\"selectedOptionIds\":[" + optionId + "]}]}";
    }

    private long postId(String token, String url, String json) throws Exception {
        String body = mvc.perform(post(url).header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long questionId(String token, long quizId, String questionJson) throws Exception {
        String body = mvc.perform(post("/api/v1/quizzes/" + quizId + "/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(questionJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<Integer> ids = JsonPath.read(body, "$.questions[*].id");
        return ids.get(ids.size() - 1).longValue();
    }

    private long createCourse(String token, String title) throws Exception {
        return postId(token, "/api/v1/courses", "{\"title\":\"" + title + "\",\"language\":\"fr\"}");
    }

    private long createChapter(String token, long courseId, String title, int position) throws Exception {
        return postId(token, "/api/v1/courses/" + courseId + "/chapters",
                "{\"title\":\"" + title + "\",\"position\":" + position + "}");
    }

    private long createTextContent(String token, long chapterId, String title, int position) throws Exception {
        return postId(token, "/api/v1/chapters/" + chapterId + "/contents",
                "{\"type\":\"TEXT\",\"title\":\"" + title + "\",\"position\":" + position + ",\"textBody\":\"x\"}");
    }

    private String enrolledLearner(String email, long courseId) throws Exception {
        String token = learnerToken(email);
        mvc.perform(post("/api/v1/courses/" + courseId + "/enroll").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        return token;
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

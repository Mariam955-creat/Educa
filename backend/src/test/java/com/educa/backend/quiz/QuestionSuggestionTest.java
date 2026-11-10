package com.educa.backend.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.ai.GeneratedQuiz;
import com.educa.backend.ai.GeneratedQuiz.GeneratedOption;
import com.educa.backend.ai.GeneratedQuiz.GeneratedQuestion;
import com.educa.backend.ai.GeneratedQuiz.Kind;
import com.educa.backend.ai.QuizGenerator;
import com.educa.backend.common.error.ApiException;
import com.educa.backend.user.Role;
import com.educa.backend.user.RoleName;
import com.educa.backend.user.RoleRepository;
import com.educa.backend.user.User;
import com.educa.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Questions proposées par l'IA (tâche 4.11). Le modèle est remplacé par un double de test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuestionSuggestionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private QuizGenerator quizGenerator;

    @Test
    void les_suggestions_valides_suivent_les_questions_existantes_et_rien_n_est_enregistre() throws Exception {
        String prof = instructorToken("suggest-prof1@example.com");
        Course course = courseWithTwoChapters(prof);
        long quizId = controlQuiz(prof, course.chapter1());
        addQuestion(prof, quizId);
        when(quizGenerator.generate(anyString(), anyInt())).thenReturn(new GeneratedQuiz(List.of(
                question("Que fait print ?", Kind.SINGLE_CHOICE, option("Affiche", true), option("Lit", false)),
                // Choix unique avec deux bonnes réponses : écartée
                question("Question incoherente", Kind.SINGLE_CHOICE, option("A", true), option("B", true)),
                question("Python est interprete", Kind.TRUE_FALSE, option("Vrai", true), option("Faux", false)),
                // Vrai/Faux à trois options : écartée
                question("Trois options", Kind.TRUE_FALSE, option("Vrai", true), option("Faux", false),
                        option("Peut-etre", false)),
                // Énoncé vide : écartée
                question(" ", Kind.MULTIPLE_CHOICE, option("A", true), option("B", true)),
                question("Types de base ?", Kind.MULTIPLE_CHOICE, option("int", true), option("str", true),
                        option("table", false)))));

        suggest(prof, quizId, 5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].statement",
                        contains("Que fait print ?", "Python est interprete", "Types de base ?")))
                .andExpect(jsonPath("$[*].position", contains(2, 3, 4)))
                .andExpect(jsonPath("$[0].type").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$[0].points").value(1))
                .andExpect(jsonPath("$[2].options[*].correct", contains(true, true, false)));

        // Les suggestions ne sont pas enregistrées : le quiz garde sa seule question
        mvc.perform(get("/api/v1/quizzes/" + quizId).header("Authorization", "Bearer " + prof))
                .andExpect(jsonPath("$.questions.length()").value(1));
    }

    @Test
    void un_controle_ne_recoit_que_son_chapitre_et_l_examen_final_tout_le_cours() throws Exception {
        String prof = instructorToken("suggest-prof2@example.com");
        Course course = courseWithTwoChapters(prof);
        long control = controlQuiz(prof, course.chapter1());
        long exam = finalExam(prof, course.id());
        when(quizGenerator.generate(anyString(), anyInt())).thenReturn(new GeneratedQuiz(List.of()));

        suggest(prof, control, 3).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        suggest(prof, exam, 3).andExpect(status().isOk());

        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(quizGenerator, org.mockito.Mockito.times(2)).generate(content.capture(), eq(3));
        assertThat(content.getAllValues().get(0))
                .contains("Langue du cours : fr", "Les variables stockent des valeurs")
                .doesNotContain("Une boucle for repete");
        assertThat(content.getAllValues().get(1))
                .contains("Les variables stockent des valeurs", "Une boucle for repete");
    }

    @Test
    void seul_le_formateur_du_cours_peut_demander_des_suggestions() throws Exception {
        String prof = instructorToken("suggest-prof3@example.com");
        Course course = courseWithTwoChapters(prof);
        long quizId = controlQuiz(prof, course.chapter1());

        suggest(instructorToken("suggest-autre@example.com"), quizId, 3).andExpect(status().isForbidden());
        suggest(learnerToken("suggest-eleve@example.com"), quizId, 3).andExpect(status().isForbidden());
        suggest(prof, quizId, 11).andExpect(status().isBadRequest());
        verify(quizGenerator, never()).generate(anyString(), anyInt());
    }

    @Test
    void une_ia_indisponible_renvoie_503() throws Exception {
        String prof = instructorToken("suggest-prof4@example.com");
        Course course = courseWithTwoChapters(prof);
        long quizId = controlQuiz(prof, course.chapter1());
        when(quizGenerator.generate(anyString(), anyInt()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "indisponible"));

        suggest(prof, quizId, 3).andExpect(status().isServiceUnavailable());
    }

    // ---------- helpers ----------

    private record Course(long id, long chapter1) {
    }

    private static GeneratedQuestion question(String statement, Kind kind, GeneratedOption... options) {
        return new GeneratedQuestion(statement, kind, List.of(options));
    }

    private static GeneratedOption option(String label, boolean correct) {
        return new GeneratedOption(label, correct);
    }

    private ResultActions suggest(String token, long quizId, int count) throws Exception {
        return mvc.perform(post("/api/v1/quizzes/" + quizId + "/question-suggestions")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content("{\"count\":" + count + "}"));
    }

    private Course courseWithTwoChapters(String token) throws Exception {
        long courseId = id(mvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content("{\"title\":\"Cours suggestions\",\"language\":\"fr\"}")));
        long chapter1 = chapter(token, courseId, 1, "Les variables stockent des valeurs.");
        chapter(token, courseId, 2, "Une boucle for repete des instructions.");
        return new Course(courseId, chapter1);
    }

    private long chapter(String token, long courseId, int position, String text) throws Exception {
        long chapterId = id(mvc.perform(post("/api/v1/courses/" + courseId + "/chapters")
                .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                .content("{\"title\":\"Chapitre " + position + "\",\"position\":" + position + "}")));
        mvc.perform(post("/api/v1/chapters/" + chapterId + "/contents").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"type\":\"TEXT\",\"title\":\"Lecon\",\"position\":1,\"textBody\":\"" + text + "\"}"))
                .andExpect(status().isCreated());
        return chapterId;
    }

    private long controlQuiz(String token, long chapterId) throws Exception {
        return id(mvc.perform(post("/api/v1/chapters/" + chapterId + "/control-quiz")
                .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                .content("{\"title\":\"Controle\"}")));
    }

    private long finalExam(String token, long courseId) throws Exception {
        return id(mvc.perform(post("/api/v1/courses/" + courseId + "/final-exam")
                .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                .content("{\"title\":\"Examen\"}")));
    }

    private void addQuestion(String token, long quizId) throws Exception {
        mvc.perform(post("/api/v1/quizzes/" + quizId + "/questions").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"statement\":\"Existante\",\"type\":\"TRUE_FALSE\",\"position\":1,\"options\":["
                                + "{\"label\":\"Vrai\",\"correct\":true,\"position\":1},"
                                + "{\"label\":\"Faux\",\"correct\":false,\"position\":2}]}"))
                .andExpect(status().isOk());
    }

    private long id(ResultActions created) throws Exception {
        String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String instructorToken(String email) throws Exception {
        register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Role role = roleRepository.findByName(RoleName.INSTRUCTOR).orElseThrow();
        user.getRoles().add(role);
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

package com.educa.backend.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.config.EducaProperties;
import com.educa.backend.course.CourseService;
import com.educa.backend.enrollment.EnrollmentService;

@Service
public class AiChatService {

    private static final String SYSTEM_TEMPLATE = """
            Tu es l'assistant pédagogique de la plateforme e-learning Omnia.
            Règles :
            - Réponds uniquement dans le périmètre du cours ci-dessous.
            - Si l'information demandée n'est pas dans le cours, dis-le clairement au lieu d'inventer.
            - Réponds dans la langue de la question, de façon concise et bienveillante.

            --- CONTENU DU COURS ---
            %s
            --- FIN DU CONTENU ---
            """;

    /** Échanges précédents (question + réponse) renvoyés au modèle comme contexte. */
    static final int CONTEXT_EXCHANGES = 6;
    /** Messages affichés à l'ouverture du chatbot. */
    static final int HISTORY_LIMIT = 50;

    private final AiAssistant aiAssistant;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final ChatMessageRepository chatMessageRepository;
    private final int maxContextChars;

    public AiChatService(AiAssistant aiAssistant, CourseService courseService,
                         EnrollmentService enrollmentService, ChatMessageRepository chatMessageRepository,
                         EducaProperties properties) {
        this.aiAssistant = aiAssistant;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.chatMessageRepository = chatMessageRepository;
        this.maxContextChars = properties.ai().maxContextChars();
    }

    /**
     * Pose une question au chatbot du cours et enregistre l'échange. Le contexte envoyé au modèle est
     * l'historique enregistré côté serveur : l'historique éventuellement fourni par le client est ignoré
     * (il ne peut donc pas glisser de fausses réponses de l'assistant). Pas de transaction autour de
     * l'appel au modèle, qui peut durer plusieurs secondes.
     */
    public AiReply chat(Long userId, AiChatRequest request) {
        requireAccess(userId, request.courseId());
        String context = courseService.aiContext(request.courseId(), maxContextChars);
        List<AiChatRequest.Turn> history = contextTurns(userId, request.courseId());
        AiReply reply = aiAssistant.ask(SYSTEM_TEMPLATE.formatted(context),
                new AiChatRequest(request.courseId(), request.message(), history));
        chatMessageRepository.saveAll(List.of(
                ChatMessage.of(userId, request.courseId(), ChatMessage.Role.USER, request.message(), false),
                ChatMessage.of(userId, request.courseId(), ChatMessage.Role.ASSISTANT, reply.reply(),
                        reply.degraded())));
        return reply;
    }

    /** Conversation de l'utilisateur pour ce cours, dans l'ordre chronologique (les plus récents seulement). */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> history(Long userId, Long courseId) {
        List<ChatMessage> latest = chatMessageRepository.findByUserIdAndCourseIdOrderByIdDesc(
                userId, courseId, PageRequest.of(0, HISTORY_LIMIT));
        return latest.reversed().stream().map(ChatMessageDto::of).toList();
    }

    /** Efface la conversation de l'utilisateur pour ce cours. */
    @Transactional
    public void clearHistory(Long userId, Long courseId) {
        chatMessageRepository.deleteConversation(userId, courseId);
    }

    private void requireAccess(Long userId, Long courseId) {
        if (!courseService.isOwnerOrAdmin(courseId) && !enrollmentService.isEnrolled(userId, courseId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Inscrivez-vous au cours pour utiliser l'assistant");
        }
    }

    /**
     * Derniers échanges complets (question puis réponse réelle) à renvoyer au modèle. Un échange dont la
     * réponse est un repli est écarté en entier : le modèle ne voit ni la question restée sans réponse ni le
     * message d'indisponibilité.
     */
    private List<AiChatRequest.Turn> contextTurns(Long userId, Long courseId) {
        List<ChatMessage> recent = chatMessageRepository
                .findByUserIdAndCourseIdOrderByIdDesc(userId, courseId, PageRequest.of(0, CONTEXT_EXCHANGES * 4))
                .reversed();
        List<List<AiChatRequest.Turn>> exchanges = new ArrayList<>();
        for (int i = 0; i + 1 < recent.size(); i++) {
            ChatMessage question = recent.get(i);
            ChatMessage answer = recent.get(i + 1);
            if (question.getRole() == ChatMessage.Role.USER && answer.getRole() == ChatMessage.Role.ASSISTANT) {
                if (!answer.isDegraded()) {
                    exchanges.add(List.of(new AiChatRequest.Turn("user", question.getContent()),
                            new AiChatRequest.Turn("assistant", answer.getContent())));
                }
                i++;
            }
        }
        List<List<AiChatRequest.Turn>> kept = exchanges.subList(Math.max(0, exchanges.size() - CONTEXT_EXCHANGES),
                exchanges.size());
        List<AiChatRequest.Turn> turns = new ArrayList<>();
        kept.forEach(turns::addAll);
        return Collections.unmodifiableList(turns);
    }
}

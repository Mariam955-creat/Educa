package com.educa.backend.ai;

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
            Tu es l'assistant pédagogique de la plateforme e-learning educa.
            Règles :
            - Réponds uniquement dans le périmètre du cours ci-dessous.
            - Si l'information demandée n'est pas dans le cours, dis-le clairement au lieu d'inventer.
            - Réponds dans la langue de la question, de façon concise et bienveillante.

            --- CONTENU DU COURS ---
            %s
            --- FIN DU CONTENU ---
            """;

    private final AiAssistant aiAssistant;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final int maxContextChars;

    public AiChatService(AiAssistant aiAssistant, CourseService courseService,
                         EnrollmentService enrollmentService, EducaProperties properties) {
        this.aiAssistant = aiAssistant;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.maxContextChars = properties.ai().maxContextChars();
    }

    @Transactional(readOnly = true)
    public AiReply chat(Long userId, AiChatRequest request) {
        if (!courseService.isOwnerOrAdmin(request.courseId())
                && !enrollmentService.isEnrolled(userId, request.courseId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Inscrivez-vous au cours pour utiliser l'assistant");
        }
        String context = courseService.aiContext(request.courseId(), maxContextChars);
        return aiAssistant.ask(SYSTEM_TEMPLATE.formatted(context), request);
    }
}

package com.educa.backend.ai;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiChatService aiChatService;

    public AiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /** Chatbot pédagogique borné au cours consulté. Réponse : {@code {reply, degraded}}. L'échange est enregistré. */
    @PostMapping("/chat")
    public AiReply chat(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.chat(CurrentUser.id(), request);
    }

    /** Ma conversation avec le chatbot de ce cours (50 derniers messages, ordre chronologique). */
    @GetMapping("/chat/history")
    public List<ChatMessageDto> history(@RequestParam Long courseId) {
        return aiChatService.history(CurrentUser.id(), courseId);
    }

    /** Efface ma conversation avec le chatbot de ce cours. */
    @DeleteMapping("/chat/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory(@RequestParam Long courseId) {
        aiChatService.clearHistory(CurrentUser.id(), courseId);
    }
}

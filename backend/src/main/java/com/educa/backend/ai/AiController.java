package com.educa.backend.ai;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /** Chatbot pédagogique borné au cours consulté. Réponse : {@code {reply, degraded}}. */
    @PostMapping("/chat")
    public AiReply chat(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.chat(CurrentUser.id(), request);
    }
}

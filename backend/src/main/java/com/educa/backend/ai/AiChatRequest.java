package com.educa.backend.ai;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotNull Long courseId,
        @NotNull @Size(min = 1, max = 2000) String message,
        List<Turn> history) {

    /** Un échange précédent : {@code role} = {@code "user"} ou {@code "assistant"}. */
    public record Turn(String role, String content) {
    }
}

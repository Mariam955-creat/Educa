-- Historique du chatbot pédagogique : une conversation par (apprenant, cours), conservée d'une visite à l'autre.
-- degraded = réponse de repli (IA indisponible) : affichée, mais jamais renvoyée au modèle comme contexte.

CREATE TABLE chat_messages (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id   BIGINT      NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    role        VARCHAR(10) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content     TEXT        NOT NULL,
    degraded    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_messages_user_course ON chat_messages (user_id, course_id, id);

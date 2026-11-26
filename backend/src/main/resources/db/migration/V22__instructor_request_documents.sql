-- Pièces jointes d'une demande de rôle formateur : dernier diplôme, CV, pièce d'identité (une de chaque).
-- Les fichiers sont dans le stockage (storage_key) ; mime_type = type réel détecté sur le contenu.
CREATE TABLE instructor_request_documents (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id    BIGINT       NOT NULL REFERENCES instructor_requests (id) ON DELETE CASCADE,
    kind          VARCHAR(20)  NOT NULL CHECK (kind IN ('DIPLOMA', 'CV', 'ID_DOCUMENT')),
    storage_key   VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(120) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (request_id, kind)
);

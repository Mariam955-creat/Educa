-- Demandes de rôle formateur : à l'inscription (« Je veux enseigner ») ou plus tard depuis l'espace compte.
-- Une seule demande en attente par compte ; l'historique (acceptées, refusées avec motif) est conservé.
CREATE TABLE instructor_requests (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    motivation       TEXT,
    status           VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at       TIMESTAMPTZ,
    decided_by       BIGINT      REFERENCES users (id) ON DELETE SET NULL
);
CREATE UNIQUE INDEX uq_instructor_requests_pending ON instructor_requests (user_id) WHERE status = 'PENDING';
CREATE INDEX idx_instructor_requests_status ON instructor_requests (status, created_at);

-- Reprise des demandes faites avec V20 (simple date sur le compte), puis suppression de cette colonne
INSERT INTO instructor_requests (user_id, created_at)
SELECT id, instructor_requested_at FROM users WHERE instructor_requested_at IS NOT NULL;
DROP INDEX idx_users_instructor_requests;
ALTER TABLE users DROP COLUMN instructor_requested_at;

-- Notifications dans l'application (cloche de l'en-tête). Le texte est traduit par l'interface à partir du type ;
-- detail porte une information libre (ex. le motif d'un refus), link une route de l'application.
CREATE TABLE notifications (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(40)  NOT NULL,
    detail     TEXT,
    link       VARCHAR(255),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);

-- Demande de rôle formateur faite à l'inscription (« Je veux enseigner »).
-- Renseignée tant que l'administration n'a ni accepté (rôle INSTRUCTOR ajouté) ni refusé la demande.
ALTER TABLE users ADD COLUMN instructor_requested_at TIMESTAMPTZ;

CREATE INDEX idx_users_instructor_requests ON users (instructor_requested_at)
    WHERE instructor_requested_at IS NOT NULL;

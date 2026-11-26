-- Validation des cours par l'administration avant publication.
-- review_status : DRAFT (jamais soumis), PENDING (soumis, en attente), APPROVED (validé), REJECTED (refusé, motif).
-- « published » reste l'interrupteur de visibilité ; un formateur ne peut l'activer que pour un cours APPROVED.
ALTER TABLE courses
    ADD COLUMN review_status VARCHAR(10) NOT NULL DEFAULT 'DRAFT'
        CHECK (review_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
    ADD COLUMN review_reason TEXT,
    ADD COLUMN submitted_at  TIMESTAMPTZ,
    ADD COLUMN reviewed_at   TIMESTAMPTZ;

-- Les cours déjà publiés (démo, cours existants) sont considérés comme validés
UPDATE courses SET review_status = 'APPROVED' WHERE published;

CREATE INDEX idx_courses_review_pending ON courses (submitted_at) WHERE review_status = 'PENDING';

-- Notifications : objet affiché dans le texte (ex. titre du cours), en plus du détail libre (ex. motif)
ALTER TABLE notifications ADD COLUMN subject VARCHAR(255);

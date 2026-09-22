-- Abandon de l'abonnement plateforme : chaque cours a désormais son propre prix (fixé par le
-- formateur), payé une seule fois par l'apprenant. `payments` est scindé sur `course_id` au lieu de
-- `subscription_id`/`plan` ; `subscriptions` disparaît (plus de récurrence à suivre).

ALTER TABLE courses ADD COLUMN price NUMERIC(10, 2) NOT NULL DEFAULT 0;

DROP TABLE payments;
DROP TABLE subscriptions;

CREATE TABLE payments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id           BIGINT        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    provider            VARCHAR(20)   NOT NULL CHECK (provider IN ('STRIPE', 'ORANGE_MONEY')),
    provider_reference  VARCHAR(255)  NOT NULL,
    amount              NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL,
    status              VARCHAR(10)   NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_reference)
);
CREATE INDEX idx_payments_user ON payments (user_id);
CREATE INDEX idx_payments_course ON payments (course_id);

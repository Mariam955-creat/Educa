-- Abonnement plateforme (accès à tous les cours) + journal des paiements.
-- Deux prestataires : Stripe (Europe, réabonnement automatique) et Orange Money
-- (Afrique, intégration directe — pas de prélèvement récurrent, voir OrangeMoneyPaymentGateway).

CREATE TABLE subscriptions (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan                      VARCHAR(10) NOT NULL CHECK (plan IN ('MONTHLY', 'ANNUAL')),
    provider                  VARCHAR(20) NOT NULL CHECK (provider IN ('STRIPE', 'ORANGE_MONEY')),
    status                    VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED')),
    provider_customer_id      VARCHAR(255),
    provider_subscription_id  VARCHAR(255),
    current_period_end        TIMESTAMPTZ,
    cancel_at_period_end      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_subscriptions_user ON subscriptions (user_id);

CREATE TABLE payments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    subscription_id     BIGINT        REFERENCES subscriptions (id) ON DELETE SET NULL,
    provider            VARCHAR(20)   NOT NULL CHECK (provider IN ('STRIPE', 'ORANGE_MONEY')),
    provider_reference  VARCHAR(255)  NOT NULL,
    plan                VARCHAR(10)   NOT NULL CHECK (plan IN ('MONTHLY', 'ANNUAL')),
    amount              NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL,
    status              VARCHAR(10)   NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_reference)
);
CREATE INDEX idx_payments_user ON payments (user_id);

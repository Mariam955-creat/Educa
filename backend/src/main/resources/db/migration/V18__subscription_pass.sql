-- Abonnement mensuel « accès à tous les cours », sous forme de pass de 30 jours renouvelable (fonctionne avec
-- Stripe comme avec Orange Money, qui ne gère pas le prélèvement automatique). Un paiement d'abonnement n'est
-- lié à aucun cours ; access_until = fin de l'accès accordé par ce paiement.

ALTER TABLE payments ALTER COLUMN course_id DROP NOT NULL;
ALTER TABLE payments ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'COURSE'
    CHECK (kind IN ('COURSE', 'SUBSCRIPTION'));
ALTER TABLE payments ADD COLUMN access_until TIMESTAMPTZ;
ALTER TABLE payments ADD CONSTRAINT chk_payments_kind_course
    CHECK ((kind = 'COURSE' AND course_id IS NOT NULL) OR (kind = 'SUBSCRIPTION' AND course_id IS NULL));
CREATE INDEX idx_payments_subscription ON payments (user_id, access_until) WHERE kind = 'SUBSCRIPTION';

-- Inscription obtenue grâce à l'abonnement (et non par un achat) : l'accès au contenu s'arrête avec l'abonnement,
-- la progression et les certificats déjà obtenus sont conservés.
ALTER TABLE enrollments ADD COLUMN via_subscription BOOLEAN NOT NULL DEFAULT false;

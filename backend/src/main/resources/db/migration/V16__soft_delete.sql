-- Corbeille : suppression douce (restaurable) des cours, des comptes et des avis.
-- La suppression définitive se fait ensuite depuis la corbeille.
ALTER TABLE courses        ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE users          ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE course_ratings ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_courses_trash ON courses (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_users_trash   ON users (deleted_at) WHERE deleted_at IS NOT NULL;

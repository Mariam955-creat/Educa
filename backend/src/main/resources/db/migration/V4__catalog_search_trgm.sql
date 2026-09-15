-- Recherche catalogue à l'échelle (résiduel Phase 2, tâche 2.13) : le filtre `?q=` de
-- CourseRepository.searchPublished (lower(title) LIKE lower('%...%')) ne peut pas utiliser
-- un index B-tree classique (motif à joker en tête). pg_trgm + un index GIN sur lower(title)
-- permet à PostgreSQL d'accélérer cette recherche par sous-chaîne sans changer la requête.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_courses_title_trgm ON courses USING GIN (lower(title) gin_trgm_ops);

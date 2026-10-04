-- Page de présentation d'un cours : accroche, catégorie, niveau, durée, objectifs, prérequis, public visé.
-- Listes (objectifs, prérequis) stockées une entrée par ligne.
ALTER TABLE courses ADD COLUMN subtitle        VARCHAR(200);
ALTER TABLE courses ADD COLUMN category        VARCHAR(30);
ALTER TABLE courses ADD COLUMN level           VARCHAR(20);
ALTER TABLE courses ADD COLUMN duration_hours  NUMERIC(5, 1);
ALTER TABLE courses ADD COLUMN objectives      TEXT;
ALTER TABLE courses ADD COLUMN prerequisites   TEXT;
ALTER TABLE courses ADD COLUMN target_audience TEXT;

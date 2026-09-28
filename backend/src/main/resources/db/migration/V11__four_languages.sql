-- Langues réduites à quatre (interface + contenu des cours) : français, anglais, allemand, néerlandais.
-- Arabe, espagnol et portugais retirés ; le néerlandais est ajouté.
INSERT INTO languages (code, name, active)
VALUES ('nl', 'Nederlands', TRUE)
ON CONFLICT (code) DO NOTHING;

DELETE FROM course_translations WHERE language_code IN ('ar', 'es', 'pt');
DELETE FROM chapter_translations WHERE language_code IN ('ar', 'es', 'pt');
UPDATE courses SET language = 'fr' WHERE language IN ('ar', 'es', 'pt');
UPDATE users SET preferred_language = 'fr' WHERE preferred_language IN ('ar', 'es', 'pt');
DELETE FROM languages WHERE code IN ('ar', 'es', 'pt');

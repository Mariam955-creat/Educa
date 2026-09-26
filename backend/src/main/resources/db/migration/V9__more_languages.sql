-- Trois langues supplémentaires (interface + contenu des cours) : espagnol, portugais, allemand.
INSERT INTO languages (code, name, active)
VALUES ('es', 'Español', TRUE),
       ('pt', 'Português', TRUE),
       ('de', 'Deutsch', TRUE)
ON CONFLICT (code) DO NOTHING;

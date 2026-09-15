-- Langues disponibles pour le contenu des cours (gestion admin) — voir docs/02-conception.md §4 « Administration ».
CREATE TABLE languages
(
    code   VARCHAR(2)  NOT NULL PRIMARY KEY,
    name   VARCHAR(50) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);

INSERT INTO languages (code, name, active)
VALUES ('fr', 'Français', TRUE),
       ('en', 'English', TRUE),
       ('ar', 'العربية', TRUE);

-- Traductions du contenu des cours (Should have) — voir docs/02-conception.md §3.
-- Traduit uniquement le titre/la description du cours et le titre des chapitres
-- (pas les contenus TEXT/VIDEO/DOCUMENT eux-mêmes, hors périmètre).
CREATE TABLE course_translations
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id     BIGINT       NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    language_code VARCHAR(2)   NOT NULL REFERENCES languages (code),
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (course_id, language_code)
);
CREATE INDEX idx_course_translations_course ON course_translations (course_id);

CREATE TABLE chapter_translations
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chapter_id    BIGINT       NOT NULL REFERENCES chapters (id) ON DELETE CASCADE,
    language_code VARCHAR(2)   NOT NULL REFERENCES languages (code),
    title         VARCHAR(200) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (chapter_id, language_code)
);
CREATE INDEX idx_chapter_translations_chapter ON chapter_translations (chapter_id);

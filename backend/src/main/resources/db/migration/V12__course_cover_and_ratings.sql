-- Cartes de cours : image de couverture téléversée par le formateur + notes des apprenants (1 à 5 étoiles).

ALTER TABLE courses ADD COLUMN cover_image_key  VARCHAR(500);
ALTER TABLE courses ADD COLUMN cover_image_type VARCHAR(100);

CREATE TABLE course_ratings (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id   BIGINT      NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    stars       SMALLINT    NOT NULL CHECK (stars BETWEEN 1 AND 5),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (course_id, user_id)
);
CREATE INDEX idx_course_ratings_course ON course_ratings (course_id);

-- ============================================================
-- educa — schéma initial (Phase 1)
-- Voir docs/02-conception.md §3 pour le détail et les règles.
-- ============================================================

-- ---------- Utilisateurs & rôles ----------

CREATE TABLE users (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    full_name          VARCHAR(150) NOT NULL,
    preferred_language CHAR(2)      NOT NULL DEFAULT 'fr',
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- ---------- Formations : cours / chapitres / contenus ----------

CREATE TABLE courses (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instructor_id  BIGINT       NOT NULL REFERENCES users (id),
    title          VARCHAR(200) NOT NULL,
    slug           VARCHAR(220) NOT NULL UNIQUE,
    description    TEXT,
    language       CHAR(2)      NOT NULL DEFAULT 'fr',
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    control_weight INT          NOT NULL DEFAULT 40,
    exam_weight    INT          NOT NULL DEFAULT 60,
    pass_threshold INT          NOT NULL DEFAULT 70,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_courses_weights CHECK (control_weight + exam_weight = 100),
    CONSTRAINT chk_courses_pass_threshold CHECK (pass_threshold BETWEEN 0 AND 100)
);
CREATE INDEX idx_courses_instructor ON courses (instructor_id);
CREATE INDEX idx_courses_published ON courses (published);

CREATE TABLE chapters (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT       NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title     VARCHAR(200) NOT NULL,
    position  INT          NOT NULL,
    UNIQUE (course_id, position)
);

CREATE TABLE contents (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chapter_id BIGINT       NOT NULL REFERENCES chapters (id) ON DELETE CASCADE,
    type       VARCHAR(15)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    position   INT          NOT NULL,
    text_body  TEXT,
    file_key   VARCHAR(500),
    file_name  VARCHAR(255),
    mime_type  VARCHAR(100),
    UNIQUE (chapter_id, position),
    CONSTRAINT chk_contents_type CHECK (type IN ('VIDEO', 'DOCUMENT', 'TEXT'))
);

-- ---------- Évaluation : quiz / questions / options ----------

CREATE TABLE quizzes (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id      BIGINT       NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    chapter_id     BIGINT       REFERENCES chapters (id) ON DELETE CASCADE,
    type           VARCHAR(12)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    pass_threshold INT          NOT NULL DEFAULT 50,
    max_attempts   INT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_quizzes_type CHECK (type IN ('CONTROL', 'FINAL_EXAM')),
    CONSTRAINT chk_quizzes_control_has_chapter
        CHECK ((type = 'CONTROL' AND chapter_id IS NOT NULL)
            OR (type = 'FINAL_EXAM' AND chapter_id IS NULL))
);
-- 1 contrôle max par chapitre, 1 examen final max par cours
CREATE UNIQUE INDEX uq_quiz_control_per_chapter ON quizzes (chapter_id) WHERE type = 'CONTROL';
CREATE UNIQUE INDEX uq_quiz_final_per_course ON quizzes (course_id) WHERE type = 'FINAL_EXAM';

CREATE TABLE questions (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_id   BIGINT      NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    statement TEXT        NOT NULL,
    type      VARCHAR(20) NOT NULL,
    points    INT         NOT NULL DEFAULT 1,
    position  INT         NOT NULL,
    UNIQUE (quiz_id, position),
    CONSTRAINT chk_questions_type CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'))
);

CREATE TABLE answer_options (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id BIGINT  NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    label       TEXT    NOT NULL,
    is_correct  BOOLEAN NOT NULL DEFAULT FALSE,
    position    INT     NOT NULL,
    UNIQUE (question_id, position)
);

-- ---------- Inscription & progression ----------

CREATE TABLE enrollments (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id    BIGINT      NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    status       VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    UNIQUE (user_id, course_id),
    CONSTRAINT chk_enrollments_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_enrollments_user ON enrollments (user_id);
CREATE INDEX idx_enrollments_course ON enrollments (course_id);

CREATE TABLE progress (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enrollment_id BIGINT      NOT NULL REFERENCES enrollments (id) ON DELETE CASCADE,
    content_id    BIGINT      NOT NULL REFERENCES contents (id) ON DELETE CASCADE,
    completed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (enrollment_id, content_id)
);
CREATE INDEX idx_progress_enrollment ON progress (enrollment_id);

-- ---------- Tentatives de quiz ----------

CREATE TABLE quiz_attempts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    quiz_id      BIGINT        NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    score        NUMERIC(5, 2) NOT NULL,
    passed       BOOLEAN       NOT NULL,
    started_at   TIMESTAMPTZ   NOT NULL,
    submitted_at TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_quiz_attempts_user_quiz ON quiz_attempts (user_id, quiz_id);

CREATE TABLE attempt_answers (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attempt_id          BIGINT   NOT NULL REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    question_id         BIGINT   NOT NULL REFERENCES questions (id),
    selected_option_ids BIGINT[] NOT NULL DEFAULT '{}',
    correct             BOOLEAN  NOT NULL
);
CREATE INDEX idx_attempt_answers_attempt ON attempt_answers (attempt_id);

-- ---------- Certificats ----------

CREATE TABLE certificates (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id               BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id             BIGINT        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    final_exam_attempt_id BIGINT        NOT NULL REFERENCES quiz_attempts (id),
    controls_average      NUMERIC(5, 2) NOT NULL,
    final_exam_score      NUMERIC(5, 2) NOT NULL,
    final_grade           NUMERIC(5, 2) NOT NULL,
    serial_number         VARCHAR(40)   NOT NULL UNIQUE,
    verification_code     VARCHAR(64)   NOT NULL UNIQUE,
    issued_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    pdf_key               VARCHAR(500),
    UNIQUE (user_id, course_id)
);
CREATE INDEX idx_certificates_verification_code ON certificates (verification_code);

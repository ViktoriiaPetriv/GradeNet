CREATE TABLE grade (
    id               BIGSERIAL PRIMARY KEY,
    entry_id         BIGINT NOT NULL,
    assessment_date  TIMESTAMP NOT NULL,
    university_grade INT,
    national_grade   INT,
    ects_grade       VARCHAR(2),
    assessment_type  VARCHAR(20),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_grade_entry FOREIGN KEY (entry_id) REFERENCES grade_book_entry(id) ON DELETE CASCADE
);

CREATE INDEX idx_grade_entry ON grade(entry_id);
CREATE TABLE hours (
    id                      BIGSERIAL PRIMARY KEY,
    specialty_discipline_id BIGINT NOT NULL,
    academic_year           VARCHAR(9) NOT NULL,
    ects_credits            INT NOT NULL,
    total_hours             INT NOT NULL,
    classroom_hours         INT NOT NULL,
    lecture_hours           INT NOT NULL DEFAULT 0,
    seminar_hours           INT NOT NULL DEFAULT 0,
    laboratory_hours        INT NOT NULL DEFAULT 0,
    individual_hours        INT NOT NULL DEFAULT 0,
    self_work_hours         INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_hours UNIQUE (specialty_discipline_id, academic_year),
    CONSTRAINT fk_hours_sd FOREIGN KEY (specialty_discipline_id) REFERENCES specialty_discipline(id) ON DELETE CASCADE
);

CREATE INDEX idx_hours_year ON hours(academic_year);
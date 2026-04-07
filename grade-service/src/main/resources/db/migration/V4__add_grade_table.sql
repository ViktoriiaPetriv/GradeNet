CREATE TABLE grade (
                       id                      BIGSERIAL PRIMARY KEY,
                       specialty_discipline_id BIGINT  NOT NULL REFERENCES specialty_discipline (id),
                       student_id              BIGINT  NOT NULL,
                       assessment_date         TIMESTAMP,
                       university_grade        INTEGER,
                       national_grade          INTEGER,
                       ects_grade              VARCHAR(5),
                       assessment              VARCHAR(30),
                       state                   VARCHAR(20)
);
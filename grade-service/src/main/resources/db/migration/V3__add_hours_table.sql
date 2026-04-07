CREATE TABLE hours (
                       id                      BIGSERIAL PRIMARY KEY,
                       specialty_discipline_id BIGINT NOT NULL UNIQUE REFERENCES specialty_discipline (id),
                       ects_hours              INTEGER,
                       all_hours               INTEGER,
                       total_classroom_hours   INTEGER,
                       lecture                 INTEGER,
                       seminar                 INTEGER,
                       laboratory              INTEGER,
                       individual              INTEGER,
                       self_work               INTEGER
);
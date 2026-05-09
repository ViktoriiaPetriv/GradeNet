-- V6__create_hours_template.sql
CREATE TABLE hours_template (
                                id               BIGSERIAL PRIMARY KEY,
                                ects_credits     INT NOT NULL,
                                total_hours      INT NOT NULL,
                                classroom_hours  INT NOT NULL,
                                lecture_hours    INT NOT NULL DEFAULT 0,
                                seminar_hours    INT NOT NULL DEFAULT 0,
                                laboratory_hours INT NOT NULL DEFAULT 0,
                                individual_hours INT NOT NULL DEFAULT 0,
                                self_work_hours  INT NOT NULL DEFAULT 0,

                                CONSTRAINT uq_hours_template UNIQUE (
                                                                     ects_credits, total_hours, classroom_hours,
                                                                     lecture_hours, seminar_hours, laboratory_hours,
                                                                     individual_hours, self_work_hours
                                    )
);
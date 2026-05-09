-- V7__refactor_hours.sql
ALTER TABLE hours DROP COLUMN ects_credits;
ALTER TABLE hours DROP COLUMN total_hours;
ALTER TABLE hours DROP COLUMN classroom_hours;
ALTER TABLE hours DROP COLUMN lecture_hours;
ALTER TABLE hours DROP COLUMN seminar_hours;
ALTER TABLE hours DROP COLUMN laboratory_hours;
ALTER TABLE hours DROP COLUMN individual_hours;
ALTER TABLE hours DROP COLUMN self_work_hours;

ALTER TABLE hours ADD COLUMN template_id BIGINT NOT NULL;

ALTER TABLE hours DROP CONSTRAINT uq_hours;

ALTER TABLE hours ADD CONSTRAINT fk_hours_template
    FOREIGN KEY (template_id) REFERENCES hours_template(id);
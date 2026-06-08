CREATE TABLE course_work_details (
    id                 BIGSERIAL    PRIMARY KEY,
    additional_work_id BIGINT       NOT NULL UNIQUE,
    semester           INT          NOT NULL,
    state              VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',

    CONSTRAINT fk_cwd_work FOREIGN KEY (additional_work_id) REFERENCES additional_work (id) ON DELETE CASCADE
);

CREATE TABLE practice_details (
    id                 BIGSERIAL    PRIMARY KEY,
    additional_work_id BIGINT       NOT NULL UNIQUE,
    organization       VARCHAR(255) NOT NULL,
    course             INT          NOT NULL,
    start_date         DATE         NOT NULL,
    end_date           DATE         NOT NULL,
    work_description   TEXT,
    ects_credits       INT          NOT NULL,
    supervisor_id      BIGINT       NOT NULL,

    CONSTRAINT fk_pd_work FOREIGN KEY (additional_work_id) REFERENCES additional_work (id) ON DELETE CASCADE
);

CREATE TABLE qualification_details (
    id                 BIGSERIAL   PRIMARY KEY,
    additional_work_id BIGINT      NOT NULL UNIQUE,
    supervisor_id      BIGINT      NOT NULL,
    state              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',

    CONSTRAINT fk_qd_work FOREIGN KEY (additional_work_id) REFERENCES additional_work (id) ON DELETE CASCADE
);

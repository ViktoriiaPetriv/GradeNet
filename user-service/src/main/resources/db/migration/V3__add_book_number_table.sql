CREATE TABLE book_number (
    id              BIGSERIAL       PRIMARY KEY,
    number          VARCHAR(20)     NOT NULL    UNIQUE,
    student_id      BIGINT          NOT NULL    REFERENCES user_app (id) ON DELETE RESTRICT,
    reg_start_date  TIMESTAMPTZ     NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    reg_end_date    TIMESTAMPTZ,
    handed_date     TIMESTAMPTZ,
    status          VARCHAR(255)    NOT NULL,
    specialty_id    BIGINT
);
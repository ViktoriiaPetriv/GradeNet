CREATE TABLE additional_work (
    id               BIGSERIAL    PRIMARY KEY,
    book_number_id   BIGINT       NOT NULL,
    commission_id    BIGINT       NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    event_date       DATE,
    university_grade INT,
    national_grade   VARCHAR(20),
    ects_grade       VARCHAR(2),

    CONSTRAINT fk_aw_commission FOREIGN KEY (commission_id) REFERENCES commission (id) ON DELETE RESTRICT
);

CREATE INDEX idx_aw_commission  ON additional_work (commission_id);
CREATE INDEX idx_aw_book_number ON additional_work (book_number_id);

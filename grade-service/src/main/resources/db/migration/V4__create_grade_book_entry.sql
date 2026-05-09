CREATE TABLE grade_book_entry (
    id                      BIGSERIAL PRIMARY KEY,
    book_number_id          BIGINT NOT NULL,
    specialty_discipline_id BIGINT NOT NULL,
    professor_id            BIGINT NOT NULL,
    academic_year           VARCHAR(9) NOT NULL,
    attempt                 INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    result VARCHAR(20),

    CONSTRAINT uq_entry UNIQUE (book_number_id, specialty_discipline_id, attempt),
    CONSTRAINT fk_entry_sd FOREIGN KEY (specialty_discipline_id) REFERENCES specialty_discipline(id) ON DELETE CASCADE
);

CREATE INDEX idx_entry_book ON grade_book_entry(book_number_id);

CREATE INDEX idx_entry_professor ON grade_book_entry(professor_id);
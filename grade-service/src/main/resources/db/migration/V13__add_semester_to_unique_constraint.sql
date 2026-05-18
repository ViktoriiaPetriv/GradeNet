ALTER TABLE grade_book_entry DROP CONSTRAINT uq_entry;

CREATE UNIQUE INDEX uq_entry
    ON grade_book_entry (book_number_id, specialty_discipline_id, attempt, COALESCE(semester, 0));

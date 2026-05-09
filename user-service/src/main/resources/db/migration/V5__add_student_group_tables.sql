CREATE TABLE student_group (
    id      BIGSERIAL       PRIMARY KEY,
    name    VARCHAR(20)     NOT NULL    UNIQUE
);

CREATE TABLE student_group_member (
    book_number_id      BIGINT  PRIMARY KEY     REFERENCES book_number (id) ON DELETE CASCADE,
    student_group_id    BIGINT  NOT NULL        REFERENCES student_group (id) ON DELETE CASCADE
);

CREATE TABLE specialty_discipline (
                                      id            BIGSERIAL PRIMARY KEY,
                                      specialty_id  BIGINT    NOT NULL,
                                      discipline_id BIGINT    NOT NULL REFERENCES discipline (id),
                                      professor_id  BIGINT,
                                      report_date   TIMESTAMP
);

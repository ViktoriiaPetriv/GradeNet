CREATE TABLE discipline (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE UNIQUE INDEX uq_discipline_name_lower ON discipline (LOWER(name));
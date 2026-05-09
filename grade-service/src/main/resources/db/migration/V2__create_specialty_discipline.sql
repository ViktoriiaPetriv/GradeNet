CREATE TABLE specialty_discipline (
    id            BIGSERIAL PRIMARY KEY,
    specialty_id  BIGINT NOT NULL,
    discipline_id BIGINT NOT NULL,

    CONSTRAINT uq_specialty_discipline UNIQUE (specialty_id, discipline_id),
    CONSTRAINT fk_sd_discipline FOREIGN KEY (discipline_id) REFERENCES discipline(id) ON DELETE CASCADE
);

CREATE INDEX idx_sd_specialty ON specialty_discipline(specialty_id);
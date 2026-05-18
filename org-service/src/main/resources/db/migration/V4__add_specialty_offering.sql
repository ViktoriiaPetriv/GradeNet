CREATE TABLE specialty_offering (
    id                BIGSERIAL PRIMARY KEY,
    specialty_id      BIGINT NOT NULL,
    external_id       BIGINT,
    graduation_year   INT NOT NULL,

    CONSTRAINT fk_so_specialty FOREIGN KEY (specialty_id) REFERENCES specialty(id) ON DELETE CASCADE,
    CONSTRAINT uq_specialty_offering UNIQUE (specialty_id, graduation_year)
);

CREATE INDEX idx_so_specialty ON specialty_offering(specialty_id);
CREATE INDEX idx_so_external_id ON specialty_offering(external_id);
